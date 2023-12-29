package nl.zwaaltjes.quoridor.server

import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import spray.json.DefaultJsonProtocol.*
import spray.json.RootJsonFormat

import scala.reflect.ClassTag

object UserServer {
  sealed trait Command
  final case class Authenticate(userId: UserId, password: Password, replyTo: ActorRef[OK | Error]) extends Command
  final case class Validate(sessionId: SessionId, replyTo: ActorRef[UserData | Error]) extends Command
  final case class GetUser(userId: UserId, replyTo: ActorRef[UserData | Error]) extends Command
  final case class UpdateUser(
    userId: UserId,
    email: Email,
    name: String,
    password: Password,
    authenticatedUserId: Option[UserId],
    replyTo: ActorRef[OK | Error]
  ) extends Command
  final case class GetDump(replyTo: ActorRef[Dump]) extends Command

  sealed trait Response
  final case class UserData(userId: UserId, name: String) extends Response
  final case class OK(sessionId: Option[SessionId]) extends Response
  final case class Dump(data: String) extends Response

  sealed trait Error extends Response
  final case class Unauthorized(message: String) extends Error
  final case class UserExists(message: String) extends Error
  final case class UnknownUser(message: String) extends Error
  final case class WrongUser(message: String) extends Error

  given RootJsonFormat[OK] = jsonFormat1(OK.apply)
  given RootJsonFormat[UserExists] = jsonFormat1(UserExists.apply)
  given RootJsonFormat[UnknownUser] = jsonFormat1(UnknownUser.apply)
  given RootJsonFormat[WrongUser] = jsonFormat1(WrongUser.apply)

  def apply(): Behaviors.Receive[Command] =
    apply(Map.empty, Map.empty)

  private case class User(id: UserId, email: Email, name: String, password: Password, sessionId: SessionId) {
    override def toString: String =
      s"$name ($email / $password); $sessionId"
  }

  private def apply(users: Map[UserId, User], sessions: Map[SessionId, User]): Behaviors.Receive[UserServer.Command] = Behaviors.receive {
    case (_, UserServer.Authenticate(userId, password, replyTo)) =>
      users.get(userId) match {
        case Some(user) if user.password == password =>
          val sessionId = SessionId.create()
          replyTo ! UserServer.OK(Some(sessionId))
          val updatedUser = user.copy(sessionId = sessionId)
          val updatedSessions = sessions - user.sessionId
          apply(users = users + (userId -> updatedUser), sessions = sessions + (sessionId -> updatedUser) - user.sessionId)
        case _ =>
          replyTo ! UserServer.Unauthorized("Email/password combination is invalid")
          Behaviors.same
      }

    case (_, UserServer.Validate(sessionId, replyTo)) =>
      sessions.get(sessionId) match {
        case Some(user) =>
          replyTo ! UserServer.UserData(user.id, user.name)
        case _ =>
          replyTo ! UserServer.Unauthorized("Session is invalid")
      }
      Behaviors.same

    case (_, UserServer.GetUser(userId, replyTo)) =>
      users.get(userId) match {
        case Some(user) => replyTo ! UserData(userId, user.name)
        case None => replyTo ! UnknownUser(s"User does not exist for $userId")
      }
      Behaviors.same

    // create new user
    case (_, UserServer.UpdateUser(userId, email, name, password, None, replyTo)) if !users.contains(userId) =>
      val sessionId = SessionId.create()
      replyTo ! UserServer.OK(Some(sessionId))
      val user = User(userId, email, name, password, sessionId)
      apply(users = users + (userId -> user), sessions = sessions + (sessionId -> user))

    // (re)create existing user
    case (_, UserServer.UpdateUser(userId, _, _, _, None, replyTo)) =>
      replyTo ! UserServer.UserExists(s"User already exists for $userId")
      Behaviors.same

    // update existing user by owner
    case (_, UserServer.UpdateUser(userId, email, name, password, authenticatedUserId, replyTo)) if authenticatedUserId == userId =>
      users.get(userId) match {
        case Some(user) =>
          replyTo ! UserServer.OK(None)
          val updatedUser = user.copy(email = email, name = name, password = password)
          apply(users = users + (userId -> updatedUser), sessions = sessions + (user.sessionId -> updatedUser))
        case None =>
          replyTo ! UserServer.UnknownUser(s"User does not exist for $userId")
          Behaviors.same
      }

    // update other user
    case (_, UserServer.UpdateUser(userId, _,  _, _, _, replyTo)) =>
      replyTo ! UserServer.WrongUser(s"Cannot change user for $userId")
      Behaviors.same

    case (_, UserServer.GetDump(replyTo)) =>
      val userData = users.map { case (userId, user) => s"$userId: $user" }.mkString("Users:\n- ", "\n- ", "\n")
      val sessionData = sessions.map { case (sessionId, user) => s"$sessionId: $user" }.mkString("Sessions:\n- ", "\n- ", "\n")
      replyTo ! UserServer.Dump(userData + sessionData)
      Behaviors.same
  }
}
