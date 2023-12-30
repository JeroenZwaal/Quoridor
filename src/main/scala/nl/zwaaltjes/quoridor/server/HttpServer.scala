package nl.zwaaltjes.quoridor.server

import nl.zwaaltjes.quoridor.impl.QuoridorImpl
import nl.zwaaltjes.quoridor.server
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem}
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.Http.ServerBinding
import org.apache.pekko.http.scaladsl.model.Uri
import org.apache.pekko.http.scaladsl.server.*

import scala.concurrent.ExecutionContext

object HttpServer {
  def main(args: Array[String]): Unit =
    new HttpServer

  sealed trait Command
  private final case class Initialize(system: ActorSystem[HttpServer.Command]) extends Command
  private final case class Bound(binding: ServerBinding) extends Command
  final case class GetVersion(replyTo: ActorRef[Version]) extends Command
  private case object Unbound extends Command
  case object Shutdown extends Command

  final case class Version(version: String)

  private def initial: Behaviors.Receive[Command] = Behaviors.receivePartial {
    case (ctx, Initialize(system)) =>
      given ActorSystem[HttpServer.Command] = system
      import system.executionContext

      val userServer = ctx.spawn(UserServer(), "userServer")
      val gameServer = ctx.spawn(GameServer(QuoridorImpl), "gameServer")
      val router = new Router(system, userServer, gameServer)
      val uri = Uri.from(scheme = "http", host = "localhost", port = 8888)
      Http().newServerAt(uri.authority.host.address, uri.authority.port).bind(router.route(uri)).foreach(ctx.self ! Bound(_))
      binding
  }

  private def binding: Behaviors.Receive[Command] = Behaviors.receivePartial { case (ctx, Bound(binding)) =>
    ctx.log.info(s"Server is listening on http://${binding.localAddress.getHostString}:${binding.localAddress.getPort}/.")
    bound(binding)
  }

  private def bound(binding: ServerBinding): Behaviors.Receive[Command] = Behaviors.receivePartial {
    case (_, GetVersion(replyTo)) =>
      replyTo ! Version("0.0.1-SNAPSHOT")
      Behaviors.same
    case (ctx, Shutdown) =>
      import ctx.executionContext
      ctx.log.info("Shutting down...")
      binding.unbind().foreach(_ => ctx.self ! Unbound)
      unbinding
  }

  private def unbinding: Behaviors.Receive[Command] = Behaviors.receivePartial { case (ctx, Unbound) =>
    ctx.system.terminate()
    Behaviors.stopped
  }
}

class HttpServer {
  import HttpServer.*

  private given system: ActorSystem[HttpServer.Command] = ActorSystem(HttpServer.initial, "quoridor")
  private given ExecutionContext = system.executionContext

  private val httpServer: ActorRef[HttpServer.Command] = system

  httpServer ! HttpServer.Initialize(system)
}
