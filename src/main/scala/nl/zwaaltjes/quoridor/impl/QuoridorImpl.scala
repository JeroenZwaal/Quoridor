package nl.zwaaltjes.quoridor.impl

import nl.zwaaltjes.quoridor.api.{Game, Player, Position, Quoridor}

object QuoridorImpl extends Quoridor {
  private final case class Init(position: Position, wins: Position => Boolean)

  private val size = 9
  private val walls = 20
  private val gameInits = {
    val bottom = Init(Position(row = 1, column = (size + 1) / 2), _.row == size)
    val top = Init(Position(row = size, column = (size + 1) / 2), _.row == 1)
    val left = Init(Position(row = (size + 1) / 2, column = 1), _.column == size)
    val right = Init(Position(row = (size + 1) / 2, column = size), _.column == 1)
    Map(2 -> Seq(bottom, top), 4 -> Seq(bottom, left, top, right))
  }

  override def createGame(playerNames: Seq[String]): Either[String, Game] =
    gameInits.get(playerNames.size) match {
      case Some(inits) =>
        val wallCount = walls / playerNames.size
        val players = playerNames.toIndexedSeq.zip(inits).map {
          case (name, init) =>
            PlayerImpl(name, init.position, wallCount, init.wins)
        }
        Right(new GameImpl(size, players))
      case None =>
        Left(s"Game cannot be played with ${playerNames.size} players.")
    }
}
