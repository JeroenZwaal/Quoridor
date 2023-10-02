package nl.zwaaltjes.quoridor.server

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import akka.http.javadsl.marshalling.Marshaller
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.model.{HttpResponse, StatusCode}
import nl.zwaaltjes.quoridor.api.Quoridor
import spray.json.DefaultJsonProtocol.*
import spray.json.{RootJsonFormat, RootJsonWriter}

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag

object UserServer {
  sealed trait Command

  final case class UpdateUser(
    email: String,
    name: String,
    password: String,
    sessionId:Option[String],
    replyTo: ActorRef[OK | Error]
  ) extends Command

  final case class OK(sessionId: Option[String])

  final case class Error(message: String)

  implicit val okFormat: RootJsonFormat[UserServer.OK] = jsonFormat1(UserServer.OK.apply)
  implicit val errorFormat: RootJsonFormat[UserServer.Error] = jsonFormat1(UserServer.Error.apply)

  implicit def someWriter[A: ClassTag](implicit aWriter: RootJsonFormat[A]): RootJsonWriter[A | UserServer.Error] = {
    case e: UserServer.Error => errorFormat.write(e)
    case a: A => aWriter.write(a)
  }

  def apply(): Behaviors.Receive[Command] =
    new UserServer().apply(Map.empty)

  private case class User(name: String, password: String, sessionId: String)
}

class UserServer private() {
  import UserServer.*

  private def apply(users: Map[String, User]): Behaviors.Receive[UserServer.Command] = Behaviors.receive {
    case (_, UserServer.UpdateUser(email, name, password, None, replyTo)) if !users.contains(email) =>
      val sessionId = UUID.randomUUID().toString
      replyTo ! UserServer.OK(Some(sessionId))
      apply(users + (email -> User(name, password, sessionId)))
    case (_, UserServer.UpdateUser(email, _, _, None, replyTo)) =>
      replyTo ! UserServer.Error(s"User already exists for $email")
      Behaviors.same
    case (_, UserServer.UpdateUser(email, name, password, Some(sessionId), replyTo)) if users.get(email).exists(_.sessionId == sessionId) =>
      replyTo ! UserServer.OK(None)
      apply(users + (email -> User(name, password, sessionId)))
    case (_, UserServer.UpdateUser(email, _, _, _, replyTo)) if users.contains(email) =>
      replyTo ! UserServer.Error(s"User does not exist for $email")
      Behaviors.same
    case (_, UserServer.UpdateUser(email, _, _, _, replyTo)) =>
      replyTo ! UserServer.Error(s"Cannot change user for $email")
      Behaviors.same
  }
}
