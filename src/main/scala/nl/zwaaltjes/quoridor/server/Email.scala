package nl.zwaaltjes.quoridor.server

import spray.json.{DefaultJsonProtocol, JsonFormat}

opaque type Email = String

object Email {
  given JsonFormat[Email] = DefaultJsonProtocol.StringJsonFormat
  
  def apply(email: String): Email =
    email
    
  def unapply(value: String): Option[Email] =
    Some(value)
}
