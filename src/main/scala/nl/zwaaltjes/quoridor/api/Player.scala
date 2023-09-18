package nl.zwaaltjes.quoridor.api

trait Player {
  val name: String
  def position: Position
  def wallsLeft: Int

  def hasWalls: Boolean =
    wallsLeft != 0

  def winsAt(position: Position): Boolean

  override def toString: String =
    name
}
