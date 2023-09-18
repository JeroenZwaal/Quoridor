package nl.zwaaltjes.quoridor.api

sealed trait Move {
  val position: Position
}

final case class PlayerMove(override val position: Position) extends Move

final case class HorizontalWall(override val position: Position) extends Move

final case class VerticalWall(override val position: Position) extends Move
