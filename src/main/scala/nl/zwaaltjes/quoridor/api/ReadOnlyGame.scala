package nl.zwaaltjes.quoridor.api

trait ReadOnlyGame {
  def size: Int

  def players: IndexedSeq[Player]

  def currentPlayer: Player

  def winner: Option[Player]

  def history: IndexedSeq[(Player, Move)]

  def playerMoves: Set[Move]

  def allowsHorizontalWall(lowerLeft: Position): Boolean

  def allowsVerticalWall(lowerLeft: Position): Boolean

  def reachablePositions(from: Position): Set[Position]
}
