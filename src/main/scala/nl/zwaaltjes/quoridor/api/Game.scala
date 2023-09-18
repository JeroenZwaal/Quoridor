package nl.zwaaltjes.quoridor.api

trait Game {
  def size: Int

  def players: IndexedSeq[Player]

  def currentPlayer: Player

  def winner: Option[Player]

  def history: IndexedSeq[Move]

  def play(move: Move): Either[String, Boolean]

  def playerMoves: Set[Move]

  def allowsHorizontalWall(lowerLeft: Position): Boolean

  def allowsVerticalWall(lowerLeft: Position): Boolean

  def reachablePositions(from: Position): Set[Position]
}
