package nl.zwaaltjes.quoridor

object Board {
  trait Listener {
    def fieldClicked(location: Location): Unit
    def centerClicked(location: Location): Unit
    def horizontalWallClicked(location: Location): Unit
    def verticalWallClicked(location: Location): Unit
  }
}

class Board(val size: Int, player1: Player, player2: Player) {
  private val horizontalWalls = Array.fill(size - 1, size - 1)(false)
  private val verticalWalls = Array.fill(size - 1, size - 1)(false)
  private val players = IndexedSeq(player1, player2)
  private var playerIndex = 0

  def playerCount: Int =
    players.size

  def player(index: Int): Player =
    players(index)

  def currentPlayer: Player =
    player(playerIndex)

  def hasPlayer(location: Location): Boolean =
    players.exists(_.location == location)

  def movePlayer(location: Location): Unit = {
    currentPlayer.changeLocation(location)
    nextPlayer()
  }

  def hasWall(location: Location, side: Direction): Boolean = {
    val (walls, direction) = if (side.isHorizontal) (verticalWalls, Direction.up) else (horizontalWalls, Direction.left)
    val wallLocation = location.wall(side)
    val secondLocation = wallLocation.go(direction)
    (isValidWall(wallLocation) && walls(wallLocation.row)(wallLocation.column)) ||
      (isValidWall(secondLocation) && walls(secondLocation.row)(secondLocation.column))
  }

  def addWall(location: Location, side: Direction, direction: Direction): Unit = {
    val wallLocation = location.wall(side).wall(direction)
    if (isValidWall(wallLocation)) {
      val walls = if (side.isHorizontal) verticalWalls else horizontalWalls
      walls(wallLocation.row)(wallLocation.column) = true
      nextPlayer()
    }
  }

  def hasHorizontalWall(location: Location): Boolean =
    isValidWall(location) && horizontalWalls(location.row)(location.column)

  def addHorizontalWall(location: Location): Unit =
    if (isValidWall(location)) {
      horizontalWalls(location.row)(location.column) = true
      nextPlayer()
    }

  def hasVerticalWall(location: Location): Boolean =
    isValidWall(location) && verticalWalls(location.row)(location.column)

  def addVerticalWall(location: Location): Unit =
    if (isValidWall(location)) {
      verticalWalls(location.row)(location.column) = true
      nextPlayer()
    }

  private def isValidWall(location: Location): Boolean =
    location.isValid(horizontalWalls.length, verticalWalls.length)

  private def nextPlayer(): Unit =
    playerIndex = (playerIndex + 1) % players.size
}