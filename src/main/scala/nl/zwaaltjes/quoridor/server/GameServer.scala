package nl.zwaaltjes.quoridor.server

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import nl.zwaaltjes.quoridor.api.{Game, Quoridor}
import spray.json.DefaultJsonProtocol.*
import spray.json.{RootJsonFormat, RootJsonWriter}

import java.util.UUID
import scala.reflect.ClassTag

object GameServer {
  sealed trait Command
  final case class NewGame(players: Seq[String], replyTo: ActorRef[Created | Error]) extends Command

  final case class Created(gameId: String)
  final case class Error(message: String)

  implicit val errorFormat: RootJsonFormat[GameServer.Error] = jsonFormat1(GameServer.Error.apply)
  implicit val createdFormat: RootJsonFormat[GameServer.Created] = jsonFormat1(GameServer.Created.apply)

  implicit def someWriter[A: ClassTag](implicit aWriter: RootJsonFormat[A]): RootJsonWriter[A | GameServer.Error] = {
    case e: GameServer.Error => errorFormat.write(e)
    case a: A => aWriter.write(a)
  }

  def apply(quoridor: Quoridor): Behaviors.Receive[Command] =
    new GameServer(quoridor).apply(Map.empty)
}

class GameServer private(quoridor: Quoridor) {

  private def apply(games: Map[UUID, Game]): Behaviors.Receive[GameServer.Command] = Behaviors.receive {
    case (ctx, GameServer.NewGame(players, replyTo)) =>
      ctx.log.info(s"Starting new game for players ${players.mkString(", ")}")
      quoridor.createGame(players) match {
        case Right(newGame) =>
          val id = UUID.randomUUID()
          replyTo ! GameServer.Created(id.toString)
          apply(games + (id -> newGame))
        case Left(message) =>
          replyTo ! GameServer.Error(message)
          Behaviors.same
      }
  }
}
