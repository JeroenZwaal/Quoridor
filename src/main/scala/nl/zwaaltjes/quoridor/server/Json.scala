package nl.zwaaltjes.quoridor.server

import spray.json.DefaultJsonProtocol.*
import spray.json.{JsString, JsValue, JsonFormat, RootJsonFormat}

import java.util.UUID

object Json {
  final case class CreateUser(name: String, email: Email, password: Password)

  final case class UserData(userId: UserId, name: String)

  object CreateUser {
    given RootJsonFormat[CreateUser] = jsonFormat3(CreateUser.apply)
  }

  object UserData {
    given RootJsonFormat[UserData] = jsonFormat2(UserData.apply)
  }
  
  given uuidFormat: JsonFormat[UUID] = new RootJsonFormat[UUID] {
    override def read(json: JsValue): UUID = UUID.fromString(json.convertTo[String])
    override def write(uuid: UUID): JsValue = JsString(uuid.toString)
  }
}
