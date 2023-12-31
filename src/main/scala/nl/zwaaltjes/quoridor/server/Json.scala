package nl.zwaaltjes.quoridor.server

import org.apache.pekko.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import org.apache.pekko.http.scaladsl.model.{ContentTypeRange, ContentTypes, HttpCharsets, MediaType, MediaTypes}
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

    given unmarshaller: ToEntityMarshaller[Envelope[UserData]] = jsonMarshaller(contentType("user-data", "v0"))
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
