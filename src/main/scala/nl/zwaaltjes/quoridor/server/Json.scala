package nl.zwaaltjes.quoridor.server

import nl.zwaaltjes.quoridor.api.{HorizontalWall, PlayerMove, Position, VerticalWall}
import org.apache.pekko.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import org.apache.pekko.http.scaladsl.model.*
import org.apache.pekko.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import spray.json.*
import spray.json.DefaultJsonProtocol.*

import java.util.UUID
import scala.reflect.ClassTag

object Json {
  final case class Envelope[+A](success: Boolean, data: Option[A] = None, message: Option[String] = None)
  object Envelope {
    private[Json] given writer[A](using JsonFormat[A]): RootJsonWriter[Envelope[A]] = jsonFormat3(Envelope.apply[A])

    given noDataMarshaller: ToEntityMarshaller[Envelope[Nothing]] = jsonMarshaller(MediaTypes.`application/json`)
    given versionMarshaller: ToEntityMarshaller[Envelope[String]] = jsonMarshaller(contentType("version", "v0"))
  }

  object OK {
    val empty: Envelope[Nothing] = Envelope(success = true)

    def apply[A](data: A): Envelope[A] = Envelope(success = true, data = Some(data))
  }
  object Error {
    def apply(message: String): Envelope[Nothing] = Envelope(success = false, message = Some(message))
  }

  final case class UserDetails(name: String, email: Email, password: Password) // TODO: password hash
  object UserDetails {
    private given RootJsonFormat[UserDetails] = jsonFormat3(UserDetails.apply)

    given unmarshaller: FromEntityUnmarshaller[UserDetails] = jsonUnmarshaller(contentType("user-details", "v0"))
  }

  final case class UserData(userId: UserId, name: String, email: Email)
  object UserData {
    private given RootJsonFormat[UserData] = jsonFormat3(UserData.apply)

    given marshaller: ToEntityMarshaller[Envelope[UserData]] = jsonMarshaller(contentType("user-data", "v0"))
  }

  final case class Games(gameIds: Seq[GameId])
  object Games {
    private given RootJsonFormat[Games] = jsonFormat1(Games.apply)

    given marshaller: ToEntityMarshaller[Envelope[Games]] = jsonMarshaller(contentType("game-list", "v0"))
  }

  type Move = nl.zwaaltjes.quoridor.api.Move
  object Move {
    private given RootJsonFormat[Move] = new RootJsonFormat[Move] {
      private val TypeKey = "type"
      private val PositionKey = "position"
      private val PlayerMoveType = "PlayerMove"
      private val HorizontalWallType = "HorizontalWall"
      private val VerticalWallType = "VerticalWall"
      
      private def moveObject(moveType: String, position: Position): JsValue =
        JsObject(TypeKey -> JsString(moveType), PositionKey -> JsString(position.toString))
        
      override def write(move: Move): JsValue =
        move match {
          case PlayerMove(position) => moveObject(PlayerMoveType, position)
          case HorizontalWall(position) => moveObject(HorizontalWallType, position)
          case VerticalWall(position) => moveObject(VerticalWallType, position)
        }
        
      override def read(json: JsValue): Move =
        json.asJsObject.getFields(TypeKey, PositionKey) match {
          case Seq(JsString(PlayerMoveType), JsString(position)) => PlayerMove(Position.fromString(position))
          case Seq(JsString(HorizontalWallType), JsString(position)) => HorizontalWall(Position.fromString(position))
          case Seq(JsString(VerticalWallType), JsString(position)) => VerticalWall(Position.fromString(position))
          case _ => deserializationError(s"Invalid move value: $json")
        }
    }

    given unmarshaller: FromEntityUnmarshaller[Move] = jsonUnmarshaller(contentType("move", "v0"))
  }

  given uuidFormat: JsonFormat[UUID] = new RootJsonFormat[UUID] {
    override def read(json: JsValue): UUID = UUID.fromString(json.convertTo[String])
    override def write(uuid: UUID): JsValue = JsString(uuid.toString)
  }

  private def jsonMarshaller[A](mediaType: MediaType.WithFixedCharset)(using
      writer: JsonWriter[Envelope[A]],
  ): ToEntityMarshaller[Envelope[A]] = {
    def marshaller(mediaType: MediaType.WithFixedCharset)(using writer: JsonWriter[Envelope[A]]): ToEntityMarshaller[Envelope[A]] =
      Marshaller.StringMarshaller.wrap(mediaType)(value => CompactPrinter(writer.write(value)))

    Marshaller.oneOf(marshaller(mediaType), marshaller(MediaTypes.`application/json`))
  }

  private def jsonUnmarshaller[A](mediaType: MediaType.WithFixedCharset)(using reader: JsonReader[A]): FromEntityUnmarshaller[A] = {
    val allTypes = Seq(mediaType.toContentType, ContentTypes.`application/json`).map(ContentTypeRange.apply)
    Unmarshaller.stringUnmarshaller.forContentTypes(allTypes*).map { data =>
      reader.read(JsonParser(data))
    }
  }

  private def contentType(dataType: String, version: String): MediaType.WithFixedCharset =
    MediaType.customWithFixedCharset("application", s"vnd.zwaaltjes.quoridor.$dataType.$version+json", HttpCharsets.`UTF-8`)
}
