package nl.zwaaltjes.quoridor.server

import spray.json.{DefaultJsonProtocol, JsonFormat}

opaque type Password = String

object Password {
  given JsonFormat[Password] = DefaultJsonProtocol.StringJsonFormat
  
  def apply(password: String): Password =
    password
    
  def unapply(value: String): Option[Password] =
    Some(value)
}
