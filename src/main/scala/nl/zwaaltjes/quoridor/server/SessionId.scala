package nl.zwaaltjes.quoridor.server

import spray.json.JsonFormat

import java.util.UUID

opaque type SessionId = UUID

object SessionId {
  given JsonFormat[SessionId] = Json.uuidFormat

  def create(): SessionId =
    UUID.randomUUID()

  def apply(sessionId: String): SessionId =
    UUID.fromString(sessionId)

  extension (sessionId: SessionId) {
    def str: String = sessionId.toString
  }
}
