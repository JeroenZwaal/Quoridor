package nl.zwaaltjes.quoridor.api

final case class Position(row: Int, column: Int) {
  import Position.*

  override def toString: String =
    s"${columnString(column)}${rowString(row)}"
}

object Position {
  def rowString(row: Int): String =
    row.toString

  def columnString(column: Int): String =
    ('A' + column - 1).toChar.toString
}
