package nl.zwaaltjes.quoridor

final case class Location(row: Int, column: Int) {
  import Location.*

  def go(direction: Direction): Location = {
    val newRow = row + direction.rowDelta
    val newColumn = column + direction.columnDelta
    Location(newRow, newColumn)
  }

  def wall(side: Direction): Location = {
    val newRow = row + (side.rowDelta - 1) / 2
    val newColumn = column + (side.columnDelta - 1) / 2
    Location(newRow, newColumn)
  }

  def left(implicit amount: Int = 1): Location =
    Location(row, column - amount)

  def right(implicit amount: Int = 1): Location =
    Location(row, column + amount)

  def up(implicit amount: Int = 1): Location =
    Location(row - amount, column)

  def down(implicit amount: Int = 1): Location =
    Location(row + amount, column)

  def isValid(size: Int): Boolean =
    isValid(size, size)

  def isValid(rowCount: Int, columnCount: Int): Boolean =
    isRowValid(rowCount) && isColumnValid(columnCount)

  def isRowValid(rowCount: Int): Boolean =
    (row >= 0) && (row < rowCount)

  def isColumnValid(columnCount: Int): Boolean =
    (column >= 0) && (column < columnCount)

  override def toString: String =
    s"${columnString(column)}${rowString(row)}"
}

object Location {
  def rowString(row: Int): String =
    (row + 1).toString

  def columnString(column: Int): String =
    ('A' + column).toChar.toString
}