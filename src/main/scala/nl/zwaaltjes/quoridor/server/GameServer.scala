package nl.zwaaltjes.quoridor.server

import nl.zwaaltjes.quoridor.api.{Game, Quoridor}
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import spray.json.DefaultJsonProtocol.*

object GameServer {
  sealed trait Command
  final case class NewGame(players: Seq[UserId], replyTo: ActorRef[Created | Error]) extends Command

  final case class Created(gameId: GameId)
  final case class Error(message: String)

  def apply(quoridor: Quoridor): Behaviors.Receive[Command] =
    new GameServer(quoridor).apply(Map.empty)
}

class GameServer private(quoridor: Quoridor) {

  private def apply(games: Map[GameId, Game]): Behaviors.Receive[GameServer.Command] = Behaviors.receive {
    case (ctx, GameServer.NewGame(players, replyTo)) =>
      ctx.log.info(s"Starting new game for players ${players.mkString(", ")}")
      quoridor.createGame(players.map(_.str)) match {
        case Right(newGame) =>
          val id = GameId.create()
          replyTo ! GameServer.Created(id)
          apply(games + (id -> newGame))
        case Left(message) =>
          replyTo ! GameServer.Error(message)
          Behaviors.same
      }
  }
}
