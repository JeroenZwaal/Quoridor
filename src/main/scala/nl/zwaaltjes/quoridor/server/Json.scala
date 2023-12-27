package nl.zwaaltjes.quoridor.server

import spray.json.DefaultJsonProtocol.*
import spray.json.RootJsonFormat

object Json {
  final case class Login(email: String, password: String)

  final case class CreateUser(name: String, password: String)

  final case class UserData(email: String, name: String)

  object Login {
    implicit val jsonFormat: RootJsonFormat[Login] = jsonFormat2(Login.apply)
  }

  object CreateUser {
    implicit val jsonFormat: RootJsonFormat[CreateUser] = jsonFormat2(CreateUser.apply)
  }

  object UserData {
    implicit val jsonFormat: RootJsonFormat[UserData] = jsonFormat2(UserData.apply)
  }
}
