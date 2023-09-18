package nl.zwaaltjes.quoridor.impl

import nl.zwaaltjes.quoridor.api.{Game, Player, Position, Quoridor}

object QuoridorImpl extends Quoridor {
  private final case class Init(position: Position, wins: Position => Boolean)

  private val size = 9
  private val walls = 20
  private val inits = Seq(
    Init(Position(row = 1, column = (size + 1) / 2), _.row == size),
    Init(Position(row = size, column = (size + 1) / 2), _.row == 1),
    Init(Position(row = (size + 1) / 2, column = 1), _.column == size),
    Init(Position(row = (size + 1) / 2, column = size), _.column == 1),
  )

  override def createGame(playerNames: Seq[String]): Game = {
    require(playerNames.size >= 2, "Game needs at least 2 players")
    require(playerNames.size <= 4, "Game needs at most 4 players")

    val wallCount = walls / playerNames.size
    val players = playerNames.toIndexedSeq.zip(inits).map {
      case (name, init) =>
        PlayerImpl(name, init.position, wallCount, init.wins)
    }
    new GameImpl(size, players)
  }
}
