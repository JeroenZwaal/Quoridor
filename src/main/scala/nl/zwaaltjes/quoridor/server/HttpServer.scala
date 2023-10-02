package nl.zwaaltjes.quoridor.server

import akka.actor.typed.scaladsl.AskPattern.*
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.*
import akka.http.scaladsl.model.{HttpHeader, StatusCodes}
import akka.http.scaladsl.model.headers.{CustomHeader, ETag, HttpCookie, RawHeader}
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.{Directive, Directive0, Directive1, Route}
import akka.http.scaladsl.server.directives.Credentials
import akka.util.Timeout
import nl.zwaaltjes.quoridor.impl.QuoridorImpl
import nl.zwaaltjes.quoridor.server
import spray.json.DefaultJsonProtocol.*
import spray.json.RootJsonFormat

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.*
import scala.reflect.ClassTag
import scala.util.{Failure, Success}

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

  private def binding: Behaviors.Receive[Command] = Behaviors.receivePartial {
    case (ctx, Bound(binding)) =>
      ctx.log.info(s"Server is listening on http://${binding.localAddress.getHostString}:${binding.localAddress.getPort}/.")
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

  private def unbinding: Behaviors.Receive[Command] = Behaviors.receivePartial {
    case (ctx, Unbound) =>
      ctx.system.terminate()
      Behaviors.stopped
  }

  private val SessionCookie = "QuoridorSession"
  private val SessionHeader = "XQuoridorSession"

  private def optionalSessionId: Directive1[Option[String]] =
    optionalCookie(SessionCookie).flatMap { sessionCookie =>
      optionalHeaderValueByName(SessionHeader).map { sessionHeader =>
        sessionCookie.map(_.value).orElse(sessionHeader)
      }
    }

  private def setSessionId(sessionId: String): Directive0 =
    setCookie(HttpCookie(SessionCookie, sessionId)) & respondWithHeader(RawHeader(SessionHeader, sessionId))
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

  private val route = concat(
    pathEndOrSingleSlash {
      get {
        getFromResource("www/welcome.html")
      }
    },
    path("version") {
      get {
        complete(httpServer.ask(HttpServer.GetVersion.apply))
      }
    },
    path("shutdown") {
      post {
        authenticateBasic(realm = "quoridor", authenticator) { _ =>
          httpServer ! HttpServer.Shutdown
          complete(StatusCodes.Accepted)
        }
      }
    },
    pathPrefix("users") {
      pathPrefix(Segment) { email =>
        pathEnd {
          concat(
            put {
              optionalSessionId { sessionId =>
                entity(as[Json.CreateUser]) { user =>
                  val reply = httpServer.ask[UserServer.OK | UserServer.Error] { replyTo =>
                    HttpServer.UserRequest(UserServer.UpdateUser(email, user.name, user.password, sessionId, replyTo))
                  }
                  onSuccess(reply) {
                    case ok @ UserServer.OK(Some(sessionId)) =>
                      setSessionId(sessionId) {
                        complete(ok)
                      }
                    case ok @ UserServer.OK(None) => complete(ok)
                    case error: UserServer.Error => complete(StatusCodes.BadRequest, error)
                  }
                }
              }
            }
          )
        }
      }
    },
    pathPrefix("games") {
      concat(
        path("new") {
          post {
            formFields("player1", "player2") { (player1, player2) =>
              complete(httpServer.ask[GameServer.Created | GameServer.Error] { replyTo =>
                HttpServer.GameRequest(GameServer.NewGame(Seq(player1, player2), replyTo))
              })
            }
          }
        },
        pathPrefix(IntNumber) { gameId =>
          concat(
            pathEnd {
              get {
                complete(StatusCodes.NotImplemented) // TODO
              }
            },
            path("accept") {
              post {
                complete(StatusCodes.NotImplemented) // TODO
              }
            },
            path("reject") {
              post {
                complete(StatusCodes.NotImplemented) // TODO
              }
            },
            path("move") {
              post {
                complete(StatusCodes.NotImplemented) // TODO
              }
            }
          )
        }
      )
    }
  )

  Http().newServerAt("localhost", 8888).bind(route).foreach(binding => httpServer ! HttpServer.Bound(binding))
}
