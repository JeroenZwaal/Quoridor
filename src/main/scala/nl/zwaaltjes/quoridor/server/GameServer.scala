package nl.zwaaltjes.quoridor.server

import nl.zwaaltjes.quoridor.api.{Game, Quoridor}
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.scaladsl.AskPattern.*
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.util.Timeout
import spray.json.DefaultJsonProtocol.*

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.DurationInt

object GameServer {
  sealed trait Command
  final case class NewGame(userId: UserId, opponents: Seq[UserId], replyTo: ActorRef[Created | Error]) extends Command
  final case class FindGame(gameId: GameId, replyTo: ActorRef[Controller | Error]) extends Command
  final case class GetDump(replyTo: ActorRef[Dump]) extends Command

  final case class Created(gameId: GameId)
  final case class Controller(actor: ActorRef[GameController.Command])
  final case class Error(message: String)
  final case class Dump(data: String)

  def apply(quoridor: Quoridor, userServer: ActorRef[UserServer.Command]): Behaviors.Receive[Command] =
    new GameServer(quoridor, userServer).apply(Map.empty)
}

class GameServer private(quoridor: Quoridor, userServer: ActorRef[UserServer.Command]) {
  import GameServer.*

  private def apply(games: Map[GameId, ActorRef[GameController.Command]]): Behaviors.Receive[Command] = Behaviors.receive {
    case (ctx, NewGame(userId, opponents, replyTo)) =>
      val userIds = userId +: opponents
      ctx.log.info(s"Creating new game for players ${userIds.mkString(", ")}")
      // TODO: verify player existence
      quoridor.createGame(userIds.map(_.str)) match {
        case Right(game) =>
          val gameId = GameId.create()
          val controller = ctx.spawn(GameController(game, opponents), gameId.toString)
          userServer ! UserServer.AddGame(userId, gameId)
          opponents.foreach { opponent =>
            userServer ! UserServer.Invite(opponent, gameId, s"Player $userId invites you to a game of Quoridor.")
          }
          replyTo ! Created(gameId)
          apply(games + (gameId -> controller))
        case Left(message) =>
          replyTo ! Error(message)
          Behaviors.same
      }

    case (_, FindGame(gameId, replyTo)) =>
      games.get(gameId) match {
        case Some(controller) =>
          replyTo ! Controller(controller)
          Behaviors.same
        case None =>
          replyTo ! Error(s"Unknown game: $gameId")
          Behaviors.same
      }

    case (ctx, GetDump(replyTo)) =>
      import ctx.{system, executionContext}
      given Timeout = 5.seconds
      val futureData = games.map {
        case (gameId, controller) =>
          controller.ask(GameController.GetStatus.apply).map(gameId -> _)
      }
      Future.sequence(futureData).foreach { data =>
        val gameData = data.map {
          case (gameId, GameController.OK(status, _)) =>
            s"$gameId: $status"
        }
        replyTo ! Dump(gameData.mkString("Games:\n- ", "\n- ", "\n"))
      }
      Behaviors.same
  }
}
