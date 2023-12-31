package nl.zwaaltjes.quoridor.server

import nl.zwaaltjes.quoridor.api.{Game, Move}
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import org.apache.pekko.actor.typed.{ActorRef, Behavior}

object GameController {
  sealed trait Command
  final case class Accept(userId: UserId, replyTo: ActorRef[OK | Error]) extends Command
  final case class Reject(userId: UserId, replyTo: ActorRef[OK | Error]) extends Command
  final case class Play(userId: UserId, move: Move, replyTo: ActorRef[OK | Error]) extends Command
  final case class GetStatus(replyTo: ActorRef[OK]) extends Command

  final case class OK(details: GameDetails)
  final case class Error(message: String)

  final case class GameDetails(
      size: Int,
      players: Seq[UserId],
      status: Status,
      currentPlayer: UserId,
      winner: Option[UserId],
      history: IndexedSeq[Move],
      moves: Set[Move],
  )
  enum Status {
    case Invited
    case Started
    case Finished
    case Aborted
  }

  def apply(game: Game, awaiting: Seq[UserId]): Behaviors.Receive[Command] =
    new GameController(game).invited(awaiting)
}

class GameController private (game: Game) {
  import GameController.*

  private def invited(awaiting: Seq[UserId]): Behaviors.Receive[Command] = defaultReceive(Status.Invited) {
    case (_, Accept(userId, replyTo)) =>
      val remaining = awaiting.filterNot(_ == userId)
      if (remaining.size == awaiting.size) {
        replyTo ! Error(s"Unknown player: $userId")
        Behaviors.same
      } else if (remaining.nonEmpty) {
        replyTo ! OK(details(Status.Invited))
        Behaviors.same
      } else {
        replyTo ! OK(details(Status.Started))
        started
      }
    case (_, Reject(userId, replyTo)) =>
      if (!awaiting.contains(userId)) {
        replyTo ! Error(s"Unknown player: $userId")
        Behaviors.same
      } else {
        replyTo ! OK(details(Status.Aborted))
        aborted
      }
    case (_, GetStatus(replyTo)) =>
      replyTo ! OK(details(Status.Invited))
      Behaviors.same
  }

  private def started: Behaviors.Receive[Command] = defaultReceive(Status.Started) {
    case (ctx, Play(userId, move, replyTo)) =>
      game.play(move) match {
        case Left(message) =>
          replyTo ! Error(message)
          Behaviors.same
        case Right(false) =>
          replyTo ! OK(details(Status.Started))
          Behaviors.same
        case Right(true) =>
          replyTo ! OK(details(Status.Finished))
          finished
      }
    case (_, GetStatus(replyTo)) =>
      replyTo ! OK(details(Status.Started))
      Behaviors.same
  }

  private def finished: Behaviors.Receive[Command] = defaultReceive(Status.Finished) {
    case (_, GetStatus(replyTo)) =>
      replyTo ! OK(details(Status.Finished))
      Behaviors.same
  }

  private def aborted: Behaviors.Receive[Command] = defaultReceive(Status.Aborted) {
    case (_, GetStatus(replyTo)) =>
      replyTo ! OK(details(Status.Aborted))
      Behaviors.same
  }

  private def defaultReceive(status: Status)(behavior: PartialFunction[(ActorContext[Command], Command), Behavior[Command]]) =
    Behaviors.receivePartial(behavior.orElse {
      case (_, GetStatus(replyTo)) =>
        replyTo ! OK(details(Status.Invited))
        Behaviors.same
      case (ctx, command) =>
        ctx.log.error(s"Unexpected command in state $status: $command")
        Behaviors.same
    })

  private def details(status: Status): GameDetails =
    GameDetails(
      size = game.size,
      players = game.players.map(UserId.apply),
      status = status,
      currentPlayer = UserId(game.currentPlayer),
      winner = game.winner.map(UserId.apply),
      history = game.history,
      moves = game.playerMoves,
    )
}
