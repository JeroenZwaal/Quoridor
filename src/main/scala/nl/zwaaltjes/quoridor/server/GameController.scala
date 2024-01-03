package nl.zwaaltjes.quoridor.server

import nl.zwaaltjes.quoridor.api.{Game, Move, ReadOnlyGame}
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import org.apache.pekko.actor.typed.{ActorRef, Behavior}

object GameController {
  sealed trait Command
  final case class Accept(userId: UserId, replyTo: ActorRef[OK | Error]) extends Command
  final case class Reject(userId: UserId, replyTo: ActorRef[OK | Error]) extends Command
  final case class Play(userId: UserId, move: Move, replyTo: ActorRef[OK | Error]) extends Command
  final case class GetStatus(replyTo: ActorRef[OK]) extends Command

  final case class OK(status: GameStatus, game: ReadOnlyGame)
  final case class Error(message: String)

  def apply(game: Game, awaiting: Seq[UserId]): Behaviors.Receive[Command] =
    new GameController(game).invited(awaiting)
}

class GameController private (game: Game) {
  import GameController.*

  private def invited(awaiting: Seq[UserId]): Behaviors.Receive[Command] = defaultReceive(GameStatus.Invited) {
    case (_, Accept(userId, replyTo)) =>
      val remaining = awaiting.filterNot(_ == userId)
      if (remaining.size == awaiting.size) {
        replyTo ! Error(s"Unknown player: $userId")
        Behaviors.same
      } else if (remaining.nonEmpty) {
        replyTo ! OK(GameStatus.Invited, game)
        Behaviors.same
      } else {
        replyTo ! OK(GameStatus.Started, game)
        started
      }
    case (_, Reject(userId, replyTo)) =>
      if (!awaiting.contains(userId)) {
        replyTo ! Error(s"Unknown player: $userId")
        Behaviors.same
      } else {
        replyTo ! OK(GameStatus.Aborted, game)
        aborted
      }
    case (_, GetStatus(replyTo)) =>
      replyTo ! OK(GameStatus.Invited, game)
      Behaviors.same
  }

  private def started: Behaviors.Receive[Command] = defaultReceive(GameStatus.Started) {
    case (ctx, Play(userId, move, replyTo)) =>
      game.play(move) match {
        case Left(message) =>
          replyTo ! Error(message)
          Behaviors.same
        case Right(false) =>
          replyTo ! OK(GameStatus.Started, game)
          Behaviors.same
        case Right(true) =>
          replyTo ! OK(GameStatus.Finished, game)
          finished
      }
    case (_, GetStatus(replyTo)) =>
      replyTo ! OK(GameStatus.Started, game)
      Behaviors.same
  }

  private def finished: Behaviors.Receive[Command] = defaultReceive(GameStatus.Finished) {
    case (_, GetStatus(replyTo)) =>
      replyTo ! OK(GameStatus.Finished, game)
      Behaviors.same
  }

  private def aborted: Behaviors.Receive[Command] = defaultReceive(GameStatus.Aborted) {
    case (_, GetStatus(replyTo)) =>
      replyTo ! OK(GameStatus.Aborted, game)
      Behaviors.same
  }

  private def defaultReceive(status: GameStatus)(behavior: PartialFunction[(ActorContext[Command], Command), Behavior[Command]]) =
    Behaviors.receivePartial(behavior.orElse {
      case (_, GetStatus(replyTo)) =>
        replyTo ! OK(GameStatus.Invited, game)
        Behaviors.same
      case (ctx, command) =>
        ctx.log.error(s"Unexpected command in state $status: $command")
        Behaviors.same
    })
}
