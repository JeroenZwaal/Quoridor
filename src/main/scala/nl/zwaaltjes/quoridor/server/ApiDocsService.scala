package nl.zwaaltjes.quoridor.server

import org.apache.pekko.http.scaladsl.model.{StatusCodes, Uri}
import org.apache.pekko.http.scaladsl.server.Directives.*
import org.apache.pekko.http.scaladsl.server.Route

object ApiDocsService {
  private val Path = "api-docs"
  private val File = "swagger.yaml"

  def route(uri: Uri): Route =
    pathPrefix(Path) {
      get {
        concat(
          pathEnd {
            redirect(s"https://petstore.swagger.io/?url=$uri/$Path/$File", StatusCodes.PermanentRedirect)
          },
          path("swagger.yaml") {
            getFromResource(s"$Path/$File")
          },
        )
      }
    }
}
