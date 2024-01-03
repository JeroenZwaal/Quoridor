package nl.zwaaltjes.quoridor.server

import nl.zwaaltjes.quoridor.api.Move
import org.apache.pekko.actor.typed.scaladsl.AskPattern.*
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem}
import org.apache.pekko.http.scaladsl.model.headers.*
import org.apache.pekko.http.scaladsl.model.{StatusCodes, Uri}
import org.apache.pekko.http.scaladsl.server.*
import org.apache.pekko.http.scaladsl.server.AuthenticationFailedRejection.{CredentialsMissing, CredentialsRejected}
import org.apache.pekko.http.scaladsl.server.Directives.*
import org.apache.pekko.http.scaladsl.server.directives.Credentials
import org.apache.pekko.util.Timeout

import scala.concurrent.duration.DurationInt

object Router {
  private val SessionCookie = "QuoridorSession"
  private val SessionHeader = "XQuoridorSession"
  private val Realm = "quoridor"

  private val InvalidCredentials = AuthenticationFailedRejection(CredentialsRejected, HttpChallenges.basic(Realm))
  private val MissingCredentials = AuthenticationFailedRejection(CredentialsMissing, HttpChallenges.basic(Realm))

  private val authenticatedAdmin: Directive0 = {
    val directive = authenticateBasic(
      realm = Realm,
      {
        case p @ Credentials.Provided("wortel") if p.verify("p4$sw0rd") => Some(p.identifier)
        case _ => None
      },
    )
    directive.map(_ => ())
  }

  val rejectionHandler: RejectionHandler = RejectionHandler
    .newBuilder()
    .handleAll[MethodRejection] { rejections =>
      val (methods, names) = rejections.map(r => r.supported -> r.supported.name).unzip
      val message = "HTTP method not allowed, supported methods: " + names.mkString(", ")
      complete(StatusCodes.MethodNotAllowed, Seq(Allow(methods)), Json.Error(message))
    }
    .handle {
      case MalformedHeaderRejection(header, message, _) =>
        complete(StatusCodes.BadRequest, Json.Error(s"The value of HTTP header '$header' was malformed: $message"))
    }
    .handle {
      case RequestEntityExpectedRejection =>
        complete(StatusCodes.BadRequest, Json.Error("Request entity expected but not supplied."))
    }
    .handleAll[UnsupportedRequestContentTypeRejection] { rejections =>
      val unsupported = rejections.find(_.contentType.isDefined).flatMap(_.contentType).getOrElse("")
      val supported = rejections.flatMap(_.supported).mkString(", ")
      val expected = if (supported.isEmpty) "" else s" Expected one of these types: $supported"
      complete(StatusCodes.UnsupportedMediaType, Json.Error(s"The request's Content-Type $unsupported is not supported.$expected"))
    }
    .handleAll[UnacceptedResponseContentTypeRejection] { rejections =>
      val supported = rejections.flatMap(_.supported).map(_.format)
      val message = supported.mkString("Resource representation is only available with these types: ", ", ", "")
      complete(StatusCodes.NotAcceptable, Json.Error(message))
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
    .handleNotFound { complete(StatusCodes.NotFound, Json.Error("The requested resource could not be found.")) }
    .result()

  val exceptionHandler: ExceptionHandler = ExceptionHandler {
    case e: Throwable =>
      complete(StatusCodes.InternalServerError, Json.Error(s"Something went terribly wrong: $e"))
  }

  private def setSessionHeader(sessionId: SessionId): Directive0 =
    respondWithHeader(RawHeader(SessionHeader, sessionId.str))
}

class Router(
    httpServer: ActorSystem[HttpServer.Command],
    userServer: ActorRef[UserServer.Command],
    gameServer: ActorRef[GameServer.Command],
) {
  import Router.*

  given ActorSystem[HttpServer.Command] = httpServer
  given Timeout = 5.seconds

  def route(base: Uri): Route =
    concat(
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
          authenticatedAdmin {
            httpServer ! HttpServer.Shutdown
            complete(StatusCodes.Accepted, Json.OK.empty)
          }
        }
      },
      pathPrefix("users") {
        userRoute(Uri("users/").resolvedAgainst(base))
      },
      pathPrefix("games") {
        gameRoute(Uri("games/").resolvedAgainst(base))
      },
      pathEndOrSingleSlash {
        get {
          authenticatedAdmin {
            userRequest(UserServer.GetDump.apply) {
              case UserServer.Dump(userData) =>
                gameRequest(GameServer.GetDump.apply) {
                  case GameServer.Dump(gameData) =>
                    complete(StatusCodes.OK, s"$userData$gameData")
                }
            }
          }
        }
      },
    )

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
                        val uri = Uri(userId.str).resolvedAgainst(base)
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
    )

  private def gameRoute(base: Uri): Route =
    concat(
      pathEnd {
        get {
          authenticatedUser { userId =>
            userRequest(UserServer.ListGames(userId, _)) {
              case UserServer.Games(gameIds) =>
                complete(StatusCodes.OK, Json.OK(Json.Games(gameIds)))
            }
          }
        }
      },
      pathPrefix("invite") {
        parameter("opponent".repeated) { opponents =>
          post {
            authenticatedUser { userId =>
              gameRequest(GameServer.NewGame(userId, opponents.map(UserId.apply).toSeq, _)) {
                case GameServer.Created(gameId) =>
                  val uri = Uri(gameId.str).resolvedAgainst(base)
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
                controllerRequest(gameId, GameController.GetStatus.apply)
              }
            },
            path("accept") {
              post {
                controllerRequest(gameId, GameController.Accept(userId, _))
              }
            },
            path("reject") {
              post {
                controllerRequest(gameId, GameController.Reject(userId, _))
              }
            },
            path("play") {
              post {
                import Json.moveUnmarshaller
                entity(as[Move]) { move =>
                  controllerRequest(gameId, GameController.Play(userId, move, _))
                }
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

  private def controllerRequest[A](gameId: GameId, create: ActorRef[A] => GameController.Command): Route =
    gameRequest(GameServer.FindGame(gameId, _)) {
      case GameServer.Controller(controller) =>
        onSuccess(controller.ask(create)) {
          case GameController.OK(status, game) =>
            complete(StatusCodes.OK, Json.OK(status -> game))
          case GameController.Error(message) =>
            complete(StatusCodes.BadRequest, Json.Error(message))
        }
      case GameServer.Error(message) =>
        complete(StatusCodes.BadRequest, Json.Error(message))
    }

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
