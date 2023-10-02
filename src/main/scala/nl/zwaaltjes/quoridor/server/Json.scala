package nl.zwaaltjes.quoridor.server

import spray.json.DefaultJsonProtocol.*
import spray.json.RootJsonFormat

object Json {
  case class CreateUser(name: String, password: String)

  object CreateUser {
    implicit val jsonFormat: RootJsonFormat[CreateUser] = jsonFormat2(CreateUser.apply)
  }
}
