package nl.zwaaltjes.quoridor.server

import nl.zwaaltjes.quoridor.impl.QuoridorImpl
import nl.zwaaltjes.quoridor.server
import org.apache.pekko.actor.typed.scaladsl.AskPattern.*
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem}
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.Http.ServerBinding
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.*
import org.apache.pekko.http.scaladsl.model.headers.{BasicHttpCredentials, HttpChallenges, RawHeader}
import org.apache.pekko.http.scaladsl.model.{HttpHeader, StatusCodes}
import org.apache.pekko.http.scaladsl.server.Directives.*
import org.apache.pekko.http.scaladsl.server.directives.Credentials
import org.apache.pekko.http.scaladsl.server.*
import org.apache.pekko.util.Timeout
import spray.json.DefaultJsonProtocol.*
import spray.json.RootJsonFormat

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.*
import scala.reflect.ClassTag

object HttpServer {
  def main(args: Array[String]): Unit =
    new HttpServer

  private sealed trait Command

  private final case class Bound(binding: ServerBinding) extends Command

  private final case class GetVersion(replyTo: ActorRef[Version]) extends Command

  private final case class GameRequest(command: GameServer.Command) extends Command

  private final case class UserRequest(command: UserServer.Command) extends Command

  private case object Unbound extends Command

  private case object Shutdown extends Command

  private final case class Version(version: String)

  private implicit val versionFormat: RootJsonFormat[HttpServer.Version] = jsonFormat1(Version.apply)

  private implicit val timeout: Timeout = 5.seconds

  private def binding: Behaviors.Receive[Command] = Behaviors.receivePartial { case (ctx, Bound(binding)) =>
    ctx.log.info(
      s"Server is listening on http://${binding.localAddress.getHostString}:${binding.localAddress.getPort}/."
    )
    val gameServer = ctx.spawn(GameServer(QuoridorImpl), "gameServer")
    val userServer = ctx.spawn(UserServer(), "userServer")
    bound(binding, gameServer, userServer)
  }

  private def bound(
      binding: ServerBinding,
      gameServer: ActorRef[GameServer.Command],
      userServer: ActorRef[UserServer.Command]
  ): Behaviors.Receive[Command] = Behaviors.receivePartial {
    case (_, GameRequest(command)) =>
      gameServer ! command
      Behaviors.same
    case (_, UserRequest(command)) =>
      userServer ! command
      Behaviors.same
    case (_, GetVersion(replyTo)) =>
      replyTo ! Version("0.0.1-SNAPSHOT")
      Behaviors.same
    case (ctx, Shutdown) =>
      import ctx.executionContext
      ctx.log.info("Shutting down...")
      binding.unbind().foreach(_ => ctx.self ! Unbound)
      unbinding
  }

  private def unbinding: Behaviors.Receive[Command] = Behaviors.receivePartial { case (ctx, Unbound) =>
    ctx.system.terminate()
    Behaviors.stopped
  }

  private val SessionCookie = "QuoridorSession"
  private val SessionHeader = "XQuoridorSession"
  private val Realm = "quoridor"
}

class HttpServer {
  import GameServer.*
  import HttpServer.*

  private implicit val system: ActorSystem[HttpServer.Command] = ActorSystem(HttpServer.binding, "quoridor")
  private implicit val executionContext: ExecutionContext = system.executionContext

  private val httpServer: ActorRef[HttpServer.Command] = system
  private val authenticator: Credentials => Option[String] = {
    case p @ Credentials.Provided("wortel") if p.verify("p4$sw0rd") => Some(p.identifier)
    case _ => None
  }

  private def onUserRequest[A](create: ActorRef[A] => UserServer.Command)(handleResponse: A => Route): Route = {
    val reply = httpServer.ask[A] { replyTo => HttpServer.UserRequest(create(replyTo)) }
    onSuccess(reply)(handleResponse)
  }

  private def userRequest[A, B](create: ActorRef[A] => UserServer.Command)(handleResponse: A => Directive1[B]): Directive1[B] = {
    val reply = httpServer.ask[A] { replyTo => HttpServer.UserRequest(create(replyTo)) }
    onSuccess(reply).flatMap(handleResponse)
  }

  private val invalidCredentials =
    AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsRejected, HttpChallenges.basic(Realm))

  private val missingCredentials =
    AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsMissing, HttpChallenges.basic(Realm))

  private def sessionHeader(sessionId: String): HttpHeader =
    RawHeader(SessionHeader, sessionId)

  private def setSessionHeader(sessionId: String): Directive0 =
    respondWithHeader(sessionHeader(sessionId))

  private def validate(sessionId: String): Directive1[String] =
    userRequest(UserServer.Validate(sessionId, _)) {
      case UserServer.UserData(email, _) =>
        provide(email)
      case _ =>
        reject(invalidCredentials)
    }

  private def authenticate(email: String, password: String): Directive1[String] =
    userRequest(UserServer.Authenticate(email, password, _)) {
      case UserServer.OK(Some(sessionId)) =>
        setSessionHeader(sessionId).tflatMap { _ =>
          provide(email)
        }
      case _ =>
        reject(invalidCredentials)
    }

  private val sessionHeader: Directive1[Option[String]] =
    optionalHeaderValueByName(SessionHeader).flatMap {
      case Some(sessionId) =>
        validate(sessionId).map(Option.apply)
      case None =>
        provide(None)
    }

  private val sessionCookie: Directive1[Option[String]] =
    optionalCookie(SessionCookie).flatMap {
      case Some(cookie) =>
        validate(cookie.value).map(Option.apply)
      case None =>
        provide(None)
    }

  private val basicAuthentication: Directive1[Option[String]] =
    extractCredentials.flatMap {
      case Some(BasicHttpCredentials(email, password)) =>
        authenticate(email, password).map(Option.apply)
      case _ =>
        provide(None)
    }

  private val optionalAuthenticatedUser: Directive1[Option[String]] =
    sessionHeader | sessionCookie | basicAuthentication

  private val authenticatedUser: Directive1[String] =
    optionalAuthenticatedUser.flatMap {
      case Some(email) =>
        provide(email)
      case None =>
        reject(missingCredentials)
    }

  private val route = concat(
    pathEndOrSingleSlash {
      get {
        getFromResource("www/welcome.html")
      }
    },
    path("version") {
      authenticatedUser { session =>
        get {
          complete(httpServer.ask(HttpServer.GetVersion.apply))
        }
      }
    },
    path("shutdown") {
      post {
        authenticateBasic(realm = Realm, authenticator) { _ =>
          httpServer ! HttpServer.Shutdown
          complete(StatusCodes.Accepted)
        }
      }
    },
    pathPrefix("users") {
      concat(
        pathPrefix(Segment) { email =>
          pathEnd {
            concat(
              get {
                authenticatedUser { _ =>
                  onUserRequest(UserServer.GetUser(email, _)) {
                    case UserServer.UserData(_, name) =>
                      complete(Json.UserData(email = email, name = name))
                    case error: UserServer.UnknownUser =>
                      complete(StatusCodes.NotFound, error)
                  }
                }
              },
              put {
                entity(as[Json.CreateUser]) { user =>
                  optionalAuthenticatedUser { authenticatedEmail =>
                    onUserRequest(UserServer.UpdateUser(email, user.name, user.password, authenticatedEmail, _)) {
                      case ok @ UserServer.OK(Some(sessionId)) =>
                        setSessionHeader(sessionId) {
                          complete(ok)
                        }
                      case ok @ UserServer.OK(None) =>
                        complete(ok)
                      case error: UserServer.UserExists =>
                        complete(StatusCodes.BadRequest, error)
                      case error: UserServer.UnknownUser =>
                        complete(StatusCodes.NotFound, error)
                      case error: UserServer.WrongUser =>
                        complete(StatusCodes.Forbidden, error)
                    }
                  }
                }
              }
            )
          }
        },
        pathEnd {
          get {
            onUserRequest(UserServer.GetDump.apply) { case UserServer.Dump(data) =>
              complete(data)
            }
          }
        }
      )
    }
//    pathPrefix("games") {
//      concat(
//        path("new") {
//          post {
//            formFields("player1", "player2") { (player1, player2) =>
//              complete(httpServer.ask[GameServer.Created | GameServer.Error] { replyTo =>
//                HttpServer.GameRequest(GameServer.NewGame(Seq(player1, player2), replyTo))
//              })
//            }
//          }
//        },
//        pathPrefix(IntNumber) { gameId =>
//          concat(
//            pathEnd {
//              get {
//                complete(StatusCodes.NotImplemented) // TODO
//              }
//            },
//            path("accept") {
//              post {
//                complete(StatusCodes.NotImplemented) // TODO
//              }
//            },
//            path("reject") {
//              post {
//                complete(StatusCodes.NotImplemented) // TODO
//              }
//            },
//            path("move") {
//              post {
//                complete(StatusCodes.NotImplemented) // TODO
//              }
//            }
//          )
//        }
//      )
//    }
  )

  Http().newServerAt("localhost", 8888).bind(route).foreach(binding => httpServer ! HttpServer.Bound(binding))
}
