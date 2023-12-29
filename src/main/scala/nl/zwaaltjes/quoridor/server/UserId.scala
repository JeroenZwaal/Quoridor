package nl.zwaaltjes.quoridor.server

import org.apache.pekko.http.scaladsl.server.{PathMatcher1, PathMatchers}
import spray.json.{DefaultJsonProtocol, JsonFormat}

opaque type UserId = String

object UserId {
  given JsonFormat[UserId] = DefaultJsonProtocol.StringJsonFormat
  
  val Segment: PathMatcher1[UserId] = PathMatchers.Segment.map(UserId.apply)
  
  def apply(userId: String): UserId =
    userId
    
  extension (userId: UserId) {
    def str: String = userId
  }
}
