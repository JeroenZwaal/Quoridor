package nl.zwaaltjes.quoridor.server

import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import spray.json.DefaultJsonProtocol.*
import spray.json.RootJsonFormat

import java.util.UUID
import scala.reflect.ClassTag

object UserServer {
  sealed trait Command

  final case class Authenticate(email: String, password: String, replyTo: ActorRef[OK | Error]) extends Command

  final case class Validate(sessionId: String, replyTo: ActorRef[UserData | Error]) extends Command

  final case class GetUser(email: String, replyTo: ActorRef[UserData | Error]) extends Command

  final case class UpdateUser(
    email: String,
    name: String,
    password: String,
    authenticatedEmail: Option[String],
    replyTo: ActorRef[OK | Error]
  ) extends Command

  final case class GetDump(replyTo: ActorRef[Dump]) extends Command

  sealed trait Response

  final case class UserData(email: String, name: String) extends Response

  final case class OK(sessionId: Option[String]) extends Response

  final case class Dump(data: String) extends Response

  sealed trait Error extends Response

  final case class Unauthorized(message: String) extends Error

  final case class UserExists(message: String) extends Error

  final case class UnknownUser(message: String) extends Error

  final case class WrongUser(message: String) extends Error

  implicit val okFormat: RootJsonFormat[OK] = jsonFormat1(OK.apply)
  implicit val userExistsFormat: RootJsonFormat[UserExists] = jsonFormat1(UserExists.apply)
  implicit val unknownUserFormat: RootJsonFormat[UnknownUser] = jsonFormat1(UnknownUser.apply)
  implicit val wrongUserFormat: RootJsonFormat[WrongUser] = jsonFormat1(WrongUser.apply)

  def apply(): Behaviors.Receive[Command] =
    apply(Map.empty, Map.empty)

  private case class User(email: String, name: String, password: String, sessionId: String) {
    override def toString: String =
      s"$name ($email / $password); $sessionId"
  }

  private def apply(users: Map[String, User], sessions: Map[String, User]): Behaviors.Receive[UserServer.Command] = Behaviors.receive {
    case (_, UserServer.Authenticate(email, password, replyTo)) =>
      users.get(email) match {
        case Some(user) if user.password == password =>
          val sessionId = UUID.randomUUID().toString
          replyTo ! UserServer.OK(Some(sessionId))
          val updatedUser = user.copy(sessionId = sessionId)
          val updatedSessions = sessions - user.sessionId
          apply(users = users + (email -> updatedUser), sessions = sessions + (sessionId -> updatedUser) - user.sessionId)
        case _ =>
          replyTo ! UserServer.Unauthorized("Email/password combination is invalid")
          Behaviors.same
      }

    case (_, UserServer.Validate(sessionId: String, replyTo)) =>
      sessions.get(sessionId) match {
        case Some(user) =>
          replyTo ! UserServer.UserData(user.email, user.name)
        case _ =>
          replyTo ! UserServer.Unauthorized("Session is invalid")
      }
      Behaviors.same

    case (_, UserServer.GetUser(email, replyTo)) =>
      users.get(email) match {
        case Some(user) => replyTo ! UserData(email, user.name)
        case None => replyTo ! UnknownUser(s"User does not exist for $email")
      }
      Behaviors.same

    // create new user
    case (_, UserServer.UpdateUser(email, name, password, None, replyTo)) if !users.contains(email) =>
      val sessionId = UUID.randomUUID().toString
      replyTo ! UserServer.OK(Some(sessionId))
      val user = User(email, name, password, sessionId)
      apply(users = users + (email -> user), sessions = sessions + (sessionId -> user))

    // (re)create existing user
    case (_, UserServer.UpdateUser(email, _, _, None, replyTo)) =>
      replyTo ! UserServer.UserExists(s"User already exists for $email")
      Behaviors.same

    // update existing user by owner
    case (_, UserServer.UpdateUser(email, name, password, Some(authenticatedEmail), replyTo)) if authenticatedEmail.contains(email) =>
      users.get(email) match {
        case Some(user) =>
          replyTo ! UserServer.OK(None)
          val updatedUser = user.copy(name = name, password = password)
          apply(users = users + (email -> updatedUser), sessions = sessions + (user.sessionId -> updatedUser))
        case None =>
          replyTo ! UserServer.UnknownUser(s"User does not exist for $email")
          Behaviors.same
      }

    // update other user
    case (_, UserServer.UpdateUser(email, _, _, _, replyTo)) =>
      replyTo ! UserServer.WrongUser(s"Cannot change user for $email")
      Behaviors.same

    case (_, UserServer.GetDump(replyTo)) =>
      val userData = users.map { case (email, user) => s"$email: $user" }.mkString("Users:\n- ", "\n- ", "\n")
      val sessionData = sessions.map { case (sessionId, user) => s"$sessionId: $user" }.mkString("Sessions:\n- ", "\n- ", "\n")
      replyTo ! UserServer.Dump(userData + sessionData)
      Behaviors.same
  }
}
