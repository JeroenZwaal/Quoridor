package nl.zwaaltjes.quoridor.server

import nl.zwaaltjes.quoridor.api.Player
import org.apache.pekko.http.scaladsl.server.{PathMatcher1, PathMatchers}
import spray.json.{DefaultJsonProtocol, JsonFormat}

opaque type UserId = String

object UserId {
  given JsonFormat[UserId] = DefaultJsonProtocol.StringJsonFormat
  
  val Segment: PathMatcher1[UserId] = PathMatchers.Segment.map(UserId.apply)
  
  def apply(userId: String): UserId =
    userId

  def apply(player: Player): UserId =
    player.name
    
  extension (userId: UserId) {
    def str: String = userId
  }
}
