package nl.zwaaltjes.quoridor.impl

import nl.zwaaltjes.quoridor.api.{Player, Position}

private class PlayerImpl(override val name: String, startPosition: Position, val walls: Int, val wins: Position => Boolean)
    extends Player {
  private var currentPosition = startPosition
  private var wallCount = walls

  override def position: Position = currentPosition

  override def wallsLeft: Int = wallCount

  override def winsAt(position: Position): Boolean = wins(position)

  private[impl] def changePosition(newPosition: Position): Unit =
    currentPosition = newPosition

  private[impl] def useWall(): Unit =
    wallCount -= 1
}
