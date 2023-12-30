package nl.zwaaltjes.quoridor.server

import spray.json.DefaultJsonProtocol.*
import spray.json.*

import java.util.UUID
import scala.collection.immutable.ListMap
import scala.reflect.ClassTag

object Json {
  abstract class Envelope[A](val success: Boolean)
  object Envelope {
    given writer[A](using JsonWriter[A]): RootJsonWriter[Envelope[A]] = {
      case OK(()) =>
        JsObject("success" -> JsBoolean(true))
      case OK(data) =>
        JsObject(ListMap("success" -> JsBoolean(true), "data" -> summon[JsonWriter[A]].write(data)))
      case Error(message) =>
        JsObject(ListMap("success" -> JsBoolean(false), "message" -> JsString(message)))
    }
  }

  final case class OK[A](data: A) extends Envelope[A](success = true)
  object OK {
    val empty: OK[Unit] = OK(())
  }

  final case class Error(message: String) extends Envelope[Unit](success = false)

  final case class UserDetails(name: String, email: Email, password: Password) // TODO: password hash
  object UserDetails {
    given RootJsonFormat[UserDetails] = jsonFormat3(UserDetails.apply)
  }

  final case class UserData(userId: UserId, name: String, email: Email)
  object UserData {
    given RootJsonFormat[UserData] = jsonFormat3(UserData.apply)
  }

  given uuidFormat: JsonFormat[UUID] = new RootJsonFormat[UUID] {
    override def read(json: JsValue): UUID = UUID.fromString(json.convertTo[String])
    override def write(uuid: UUID): JsValue = JsString(uuid.toString)
  }
}
