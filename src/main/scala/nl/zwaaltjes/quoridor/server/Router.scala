package nl.zwaaltjes.quoridor.server

import org.apache.pekko.actor.typed.scaladsl.AskPattern.*
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem}
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.*
import org.apache.pekko.http.scaladsl.model.{StatusCodes, Uri}
import org.apache.pekko.http.scaladsl.model.headers.*
import org.apache.pekko.http.scaladsl.server.*
import org.apache.pekko.http.scaladsl.server.AuthenticationFailedRejection.{CredentialsMissing, CredentialsRejected}
import org.apache.pekko.http.scaladsl.server.Directives.*
import org.apache.pekko.http.scaladsl.server.directives.Credentials
import org.apache.pekko.util.Timeout
import spray.json.DefaultJsonProtocol.*

import scala.concurrent.duration.DurationInt

// TODO: game protocol
// TODO: content negotiation
object Router {
  private val SessionCookie = "QuoridorSession"
  private val SessionHeader = "XQuoridorSession"
  private val Realm = "quoridor"

  private val InvalidCredentials = AuthenticationFailedRejection(CredentialsRejected, HttpChallenges.basic(Realm))
  private val MissingCredentials = AuthenticationFailedRejection(CredentialsMissing, HttpChallenges.basic(Realm))

  private val adminAuthenticator: Credentials => Option[String] = {
    case p @ Credentials.Provided("wortel") if p.verify("p4$sw0rd") => Some(p.identifier)
    case _ => None
  }

  private val rejectionHandler: RejectionHandler = RejectionHandler
    .newBuilder()
    .handleAll[MethodRejection] { rejections =>
      val (methods, names) = rejections.map(r => r.supported -> r.supported.name).unzip
      val message = "HTTP method not allowed, supported methods: " + names.mkString(", ")
      complete(StatusCodes.MethodNotAllowed, Seq(Allow(methods)), Json.Error(message))
    }
    .handle {
      case MalformedHeaderRejection(header, message, _) =>
        complete(StatusCodes.BadRequest, s"The value of HTTP header '$header' was malformed:\n$message")
    }
    .handle {
      case RequestEntityExpectedRejection =>
        complete(StatusCodes.BadRequest, "Request entity expected but not supplied.")
    }
    .handleAll[UnsupportedRequestContentTypeRejection] { rejections =>
      val unsupported = rejections.find(_.contentType.isDefined).flatMap(_.contentType).fold("")(" [" + _ + "]")
      val supported = rejections.flatMap(_.supported).mkString(" or ")
      val expected = if (supported.isEmpty) "" else s" Expected:\n$supported"
      complete(StatusCodes.UnsupportedMediaType, s"The request's Content-Type$unsupported is not supported.$expected")
    }
    .handleAll[UnacceptedResponseContentTypeRejection] { rejections =>
      val supported = rejections.flatMap(_.supported).map(_.format)
      val message = supported.mkString("Resource representation is only available with these types:\n", "\n", "")
      complete(StatusCodes.NotAcceptable, message)
    }
    .handle {
      case AuthenticationFailedRejection(cause, challenge) =>
        val message = cause match {
          case CredentialsMissing => "The resource requires authentication, which was not supplied with the request."
          case CredentialsRejected => "The supplied authentication is invalid."
        }
        complete(StatusCodes.Unauthorized, Seq(`WWW-Authenticate`(challenge)), Json.Error(message))
    }
    .handle {
      case ValidationRejection(message, _) =>
        complete(StatusCodes.BadRequest, Json.Error(message))
    }
    .handle { case rejection => sys.error(s"Unhandled rejection: $rejection") }
    .handleNotFound { complete(StatusCodes.NotFound, Json.Error("The requested resource could not be found.")) }
    .result()

  private val exceptionHandler: ExceptionHandler = ExceptionHandler {
    case e: Throwable =>
      complete(StatusCodes.InternalServerError, Json.Error(s"Something went terribly wrong.\n\n$e"))
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
    handleExceptions(exceptionHandler) {
      handleRejections(rejectionHandler) {
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
              authenticateBasic(realm = Realm, adminAuthenticator) { _ =>
                httpServer ! HttpServer.Shutdown
                complete(StatusCodes.Accepted, Json.OK.empty)
              }
            }
          },
          pathPrefix("users") {
            userRoute(Uri("users").resolvedAgainst(base))
          },
          pathPrefix("games") {
            gameRoute(Uri("games").resolvedAgainst(base))
          },
        )
      }
    }

  private def userRoute(base: Uri): Route =
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
                        val uri = Uri(s"$userId").resolvedAgainst(base)
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

  private def gameRoute(base: Uri): Route =
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
                  val uri = Uri(s"$gameId").resolvedAgainst(base)
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

  private def userRequest[A](create: ActorRef[A] => UserServer.Command): Directive1[A] =
    onSuccess(userServer.ask(create))

  private def gameRequest[A](create: ActorRef[A] => GameServer.Command): Directive1[A] =
    onSuccess(gameServer.ask(create))

  private def authenticatedUser: Directive1[UserId] =
    sessionHeader | sessionCookie | basicAuthentication

  private def optionalAuthenticatedUser: Directive1[Option[UserId]] =
    (authenticatedUser & cancelRejection(MissingCredentials)).map(Option.apply) | provide(None)

  private val sessionHeader: Directive1[UserId] =
    optionalHeaderValueByName(SessionHeader).flatMap {
      case Some(session) =>
        validate(SessionId(session))
      case None =>
        reject(MissingCredentials)
    }

  private val sessionCookie: Directive1[UserId] =
    optionalCookie(SessionCookie).flatMap {
      case Some(cookie) =>
        validate(SessionId(cookie.value))
      case None =>
        reject(MissingCredentials)
    }

  private val basicAuthentication: Directive1[UserId] =
    extractCredentials.flatMap {
      case Some(BasicHttpCredentials(userId, password)) =>
        authenticate(UserId(userId), Password(password))
      case _ =>
        reject(MissingCredentials)
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
