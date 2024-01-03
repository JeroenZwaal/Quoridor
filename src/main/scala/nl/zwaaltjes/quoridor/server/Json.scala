package nl.zwaaltjes.quoridor.server

import nl.zwaaltjes.quoridor.api.*
import org.apache.pekko.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import org.apache.pekko.http.scaladsl.model.*
import org.apache.pekko.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import spray.json.*
import spray.json.DefaultJsonProtocol.*

import java.util.UUID
import scala.reflect.ClassTag

object Json {
  case class Envelope[+A](success: Boolean, data: Option[A] = None, message: Option[String] = None)
  object Envelope {
    private[Json] given writer[A](using JsonWriter[A]): RootJsonWriter[Envelope[A]] = {
      given JsonFormat[A] = new JsonFormat[A] {
        override def write(a: A): JsValue = jsonWriter[A].write(a)
        override def read(json: JsValue): A = deserializationError("Not implemented")
      }
      jsonFormat3(Envelope.apply[A])
    }
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
    private[Json] given RootJsonFormat[UserDetails] = jsonFormat3(UserDetails.apply)
  }

  final case class UserData(userId: UserId, name: String, email: Email)
  object UserData {
    private[Json] given RootJsonFormat[UserData] = jsonFormat3(UserData.apply)
  }

  final case class Games(gameIds: Seq[GameId])
  object Games {
    private[Json] given RootJsonFormat[Games] = jsonFormat1(Games.apply)
  }

  given noDataMarshaller: ToEntityMarshaller[Envelope[Nothing]] = {
    given JsonWriter[Nothing] = jsonWriter[Unit].asInstanceOf[JsonWriter[Nothing]] // prevent ambiguity on any/all writers
    jsonMarshaller(MediaTypes.`application/json`)
  }
  given versionMarshaller: ToEntityMarshaller[Envelope[String]] = jsonMarshaller(contentType("version", "v0"))
  given userDataMarshaller: ToEntityMarshaller[Envelope[UserData]] = jsonMarshaller(contentType("user", "v0"))
  given gamesMarshaller: ToEntityMarshaller[Envelope[Games]] = jsonMarshaller(contentType("game-list", "v0"))
  given gameDetailsMarshaller: ToEntityMarshaller[Envelope[(GameStatus, ReadOnlyGame)]] = jsonMarshaller(contentType("game", "v0"))

  given moveUnmarshaller: FromEntityUnmarshaller[Move] = jsonUnmarshaller(contentType("move", "v0"))
  given userDetailsUnmarshaller: FromEntityUnmarshaller[UserDetails] = jsonUnmarshaller(contentType("user-details", "v0"))

  private given RootJsonWriter[(GameStatus, ReadOnlyGame)] = new RootJsonWriter[(GameStatus, ReadOnlyGame)] {
    override def write(value: (GameStatus, ReadOnlyGame)): JsValue = {
      val (status, game) = value
      val fields = Seq(
        "size" -> JsNumber(game.size),
        "players" -> JsArray(game.players.map(writePlayer)*),
        "status" -> JsString(status.toString),
        "currentPlayer" -> JsString(game.currentPlayer.name),
        "reachablePositions" -> reachablePositions(game),
        "history" -> JsArray(game.history.map(writeHistory)*),
        "moves" -> determineMoves(game),
      ) ++
        game.winner.map(p => "winner" -> JsString(p.name))
      JsObject(fields*)
    }

    private def writePlayer(player: Player): JsObject =
      JsObject(
        "userId" -> JsString(player.name),
        "position" -> positionFormat.write(player.position),
        "wallsLeft" -> JsNumber(player.wallsLeft),
      )

    private def writeHistory(move: (Player, Move)): JsObject =
      JsObject(
        "userId" -> JsString(move._1.name),
        moveField(move._2),
      )

    private def determineMoves(game: ReadOnlyGame): JsArray = {
      val walls = for {
        row <- 1 to game.size
        column <- 1 to game.size
        position = Position(row = row, column = column)
        horizontal = game.allowsHorizontalWall(position)
        vertical = game.allowsVerticalWall(position)
      } yield Option.when(horizontal)(HorizontalWall(position)) ++ Option.when(vertical)(VerticalWall(position))
      JsArray((game.playerMoves.toSeq ++ walls.flatten).map(moveFormat.write)*) // TODO: sort?
    }

    private def reachablePositions(game: ReadOnlyGame): JsArray = {
      val positions = game.reachablePositions(game.currentPlayer.position).filter(game.currentPlayer.winsAt)
      JsArray(positions.toSeq.map(positionFormat.write)*) // TODO: sort?
    }
  }

  private val MovePlayerKey = "movePlayer"
  private val HorizontalWallKey = "horizontalWall"
  private val VerticalWallKey = "verticalWall"

  private def moveField(move: Move): JsField =
    move match {
      case PlayerMove(position) => MovePlayerKey -> JsString(position.toString)
      case HorizontalWall(position) => HorizontalWallKey -> JsString(position.toString)
      case VerticalWall(position) => VerticalWallKey -> JsString(position.toString)
    }

  private given moveFormat: RootJsonFormat[Move] = new RootJsonFormat[Move] {
    override def write(move: Move): JsValue =
      JsObject(moveField(move))

    override def read(json: JsValue): Move =
      json.asJsObject.fields.toSeq match {
        case Seq(MovePlayerKey -> JsString(position)) => PlayerMove(Position.fromString(position))
        case Seq(HorizontalWallKey -> JsString(position)) => HorizontalWall(Position.fromString(position))
        case Seq(VerticalWallKey -> JsString(position)) => VerticalWall(Position.fromString(position))
        case _ => deserializationError(s"Invalid move value: $json")
      }
  }

  private given positionFormat: RootJsonFormat[Position] = new RootJsonFormat[Position] {
    override def read(json: JsValue): Position = Position.fromString(json.convertTo[String])
    override def write(position: Position): JsValue = JsString(position.toString)
  }

  private[server] given uuidFormat: JsonFormat[UUID] = new RootJsonFormat[UUID] {
    override def read(json: JsValue): UUID = UUID.fromString(json.convertTo[String])
    override def write(uuid: UUID): JsValue = JsString(uuid.toString)
  }

  //  final case class ContentTyper[A](mediaType: MediaType.WithFixedCharset)
  //
  //  given versionContentType: ContentTyper[String] = ContentTyper[String](contentType("version", "v0"))
  //  given userdataContentType: ContentTyper[UserData] = ContentTyper[UserData](contentType("user-data", "v0"))
  //
  //  given envelopeMarshaller[A](using ContentTyper[A], JsonWriter[Envelope[A]]): ToEntityMarshaller[Envelope[A]] = {
  //    val writer = summon[JsonWriter[Envelope[A]]]
  //    val typer = summon[ContentTyper[A]]
  //
  //    def marshaller(mediaType: MediaType.WithFixedCharset): ToEntityMarshaller[Envelope[A]] =
  //      Marshaller.StringMarshaller.wrap(mediaType)(envelope => CompactPrinter(writer.write(envelope)))
  //
  //    Marshaller.oneOf(marshaller(typer.mediaType), marshaller(MediaTypes.`application/json`))
  //  }

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
