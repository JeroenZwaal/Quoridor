package nl.zwaaltjes.quoridor.server

import org.apache.pekko.actor.typed.scaladsl.AskPattern.*
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem}
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.*
import org.apache.pekko.http.scaladsl.model.{StatusCodes, Uri}
import org.apache.pekko.http.scaladsl.model.headers.{BasicHttpCredentials, HttpChallenges, Location, RawHeader}
import org.apache.pekko.http.scaladsl.server.*
import org.apache.pekko.http.scaladsl.server.Directives.*
import org.apache.pekko.http.scaladsl.server.directives.Credentials
import org.apache.pekko.util.Timeout
import spray.json.DefaultJsonProtocol.*

import scala.concurrent.duration.DurationInt

object Router {
  private val SessionCookie = "QuoridorSession"
  private val SessionHeader = "XQuoridorSession"
  private val Realm = "quoridor"

  private val InvalidCredentials =
    AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsRejected, HttpChallenges.basic(Realm))

  private val MissingCredentials =
    AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsMissing, HttpChallenges.basic(Realm))

  private val authenticator: Credentials => Option[String] = {
    case p @ Credentials.Provided("wortel") if p.verify("p4$sw0rd") => Some(p.identifier)
    case _ => None
  }

  private def setSessionHeader(sessionId: SessionId): Directive0 =
    respondWithHeader(RawHeader(SessionHeader, sessionId.str))

  private given timeout: Timeout = 5.seconds
}

class Router(
    httpServer: ActorSystem[HttpServer.Command],
    userServer: ActorRef[UserServer.Command],
    gameServer: ActorRef[GameServer.Command],
) {
  import Router.{timeout, *}

  given ActorSystem[HttpServer.Command] = httpServer

  def route(base: Uri): Route =
    concat(
      pathEndOrSingleSlash {
        get {
          getFromResource("www/welcome.html")
        }
      },
      path("version") {
        get {
          onSuccess(httpServer.ask(HttpServer.GetVersion.apply)) {
            case HttpServer.Version(version) =>
              complete(StatusCodes.OK, Json.OK(version))
          }
        }
      },
      path("shutdown") {
        post {
          authenticateBasic(realm = Realm, authenticator) { _ =>
            httpServer ! HttpServer.Shutdown
            complete(StatusCodes.Accepted, Json.OK.empty)
          }
        }
      },
      pathPrefix("users") {
        concat(
          pathPrefix(UserId.Segment) { userId =>
            pathEnd {
              concat(
                get {
                  authenticatedUser { _ =>
                    userRequest(UserServer.GetUser(userId, _)) {
                      case UserServer.UserData(_, name, email) =>
                        complete(StatusCodes.OK, Json.OK(Json.UserData(userId = userId, name = name, email = email)))
                      case UserServer.UnknownUser(message) =>
                        complete(StatusCodes.NotFound, Json.Error(message))
                    }
                  }
                },
                put {
                  optionalAuthenticatedUser { authenticatedUserId =>
                    entity(as[Json.UserDetails]) { user =>
                      userRequest(UserServer.UpdateUser(userId, user.email, user.name, user.password, authenticatedUserId, _)) {
                        case ok @ UserServer.OK(Some(sessionId)) =>
                          setSessionHeader(sessionId) {
                            val uri = Uri(s"users/$userId").resolvedAgainst(base)
                            complete(StatusCodes.Created, Seq(Location(uri)), Json.OK.empty)
                          }
                        case ok @ UserServer.OK(None) =>
                          complete(StatusCodes.OK, Json.OK.empty)
                        case UserServer.UserExists(message) =>
                          complete(StatusCodes.BadRequest, Json.Error(message))
                        case UserServer.UnknownUser(message) =>
                          complete(StatusCodes.NotFound, Json.Error(message))
                        case UserServer.WrongUser(message) =>
                          complete(StatusCodes.Forbidden, Json.Error(message))
                      }
                    }
                  }
                },
              )
            }
          },
          pathEnd {
            get {
              userRequest(UserServer.GetDump.apply) {
                case UserServer.Dump(data) =>
                  complete(StatusCodes.OK, data)
              }
            }
          },
        )
      },
      pathPrefix("games") {
        concat(
          pathEnd {
            get {
              authenticatedUser { userId =>
                complete(StatusCodes.NotImplemented) // TODO
              }
            }
          },
          pathPrefix("invite") {
            path(Segment) { invitee =>
              post {
                authenticatedUser { userId =>
                  gameRequest(GameServer.NewGame(Seq(userId, UserId(invitee)), _)) {
                    case GameServer.Created(gameId) =>
                      val uri = Uri(s"games/$gameId").resolvedAgainst(base)
                      complete(StatusCodes.Created, Seq(Location(uri)), Json.OK.empty)
                    case GameServer.Error(message) =>
                      complete(StatusCodes.BadRequest, Json.Error(message))
                  }
                }
              }
            }
          },
          pathPrefix(GameId.Segment) { gameId =>
            authenticatedUser { userId =>
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
                },
              )
            }
          },
        )
      },
    )

  private def userRequest[A](create: ActorRef[A] => UserServer.Command): Directive1[A] =
    onSuccess(userServer.ask(create))

  private def gameRequest[A](create: ActorRef[A] => GameServer.Command): Directive1[A] =
    onSuccess(gameServer.ask(create))

  private def optionalAuthenticatedUser: Directive1[Option[UserId]] =
    sessionHeader | sessionCookie | basicAuthentication

  private def authenticatedUser: Directive1[UserId] =
    optionalAuthenticatedUser.flatMap {
      case Some(userId) =>
        provide(userId)
      case None =>
        reject(MissingCredentials)
    }

  private val sessionHeader: Directive1[Option[UserId]] =
    optionalHeaderValueByName(SessionHeader).flatMap {
      case Some(session) =>
        validate(SessionId(session)).map(Option.apply)
      case None =>
        provide(None)
    }

  private val sessionCookie: Directive1[Option[UserId]] =
    optionalCookie(SessionCookie).flatMap {
      case Some(cookie) =>
        validate(SessionId(cookie.value)).map(Option.apply)
      case None =>
        provide(None)
    }

  private val basicAuthentication: Directive1[Option[UserId]] =
    extractCredentials.flatMap {
      case Some(BasicHttpCredentials(userId, password)) =>
        authenticate(UserId(userId), Password(password)).map(Option.apply)
      case _ =>
        provide(None)
    }

  private def validate(sessionId: SessionId): Directive1[UserId] =
    userRequest(UserServer.Validate(sessionId, _)).flatMap {
      case UserServer.UserData(userId, _, _) =>
        provide(userId)
      case _ =>
        reject(InvalidCredentials)
    }

  private def authenticate(userId: UserId, password: Password): Directive1[UserId] =
    userRequest(UserServer.Authenticate(userId, password, _)).flatMap {
      case UserServer.OK(Some(sessionId)) =>
        setSessionHeader(sessionId).tflatMap { _ =>
          provide(userId)
        }
      case _ =>
        reject(InvalidCredentials)
    }
}
