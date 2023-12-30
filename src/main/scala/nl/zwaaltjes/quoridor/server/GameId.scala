package nl.zwaaltjes.quoridor.server

import org.apache.pekko.http.scaladsl.server.{PathMatcher1, PathMatchers}
import spray.json.JsonFormat

import java.util.UUID

opaque type GameId = UUID

object GameId {
  val Segment: PathMatcher1[GameId] = PathMatchers.JavaUUID.map(GameId.apply)

  given JsonFormat[GameId] = Json.uuidFormat

  def create(): GameId =
    UUID.randomUUID()

  def apply(gameId: UUID): GameId =
    gameId
    
  extension (gameId: GameId) {
    def str: String = gameId.toString
  }
}
