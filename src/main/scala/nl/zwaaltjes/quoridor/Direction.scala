package nl.zwaaltjes.quoridor

enum Direction(name: String, val rowDelta: Int, val columnDelta: Int, val isHorizontal: Boolean, val isVertical: Boolean) {
  case down extends Direction("down", rowDelta = 1, columnDelta = 0, isHorizontal = false, isVertical = true)
  case up extends Direction("up", rowDelta = -1, columnDelta = 0, isHorizontal = false, isVertical = true)
  case left extends Direction("left", rowDelta = 0, columnDelta = -1, isHorizontal = true, isVertical = false)
  case right extends Direction("right", rowDelta = 0, columnDelta = 1, isHorizontal = true, isVertical = false)

  override def toString: String =
    name
}