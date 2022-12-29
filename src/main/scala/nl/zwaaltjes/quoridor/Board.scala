package nl.zwaaltjes.quoridor

object Board {
  trait Listener {
    def fieldClicked(location: Location): Unit

    def centerClicked(location: Location): Unit

    def horizontalWallClicked(location: Location): Unit

    def verticalWallClicked(location: Location): Unit
  }
}

class Board(val size: Int, players: Player*) {
  private val horizontalWalls = Array.fill(size - 1, size - 1)(false)
  private val verticalWalls = Array.fill(size - 1, size - 1)(false)
  private var playerIndex = 0
  private var finished = false

  def winner: Option[Player] =
    Option.when(finished)(currentPlayer)

  def reachableLocations(from: Location): Set[Location] = {
    val visited = Array.fill(size, size)(false)

    def recurse(location: Location): Unit = {
      visited(location.row)(location.column) = true
      for (direction <- Direction.values) {
        if (!hasWall(location, direction)) {
          val next = location.go(direction)
          if (next.isValid(size, size) && !visited(next.row)(next.column))
            recurse(next)
        }
      }
    }

    recurse(from)

    val result = for {
      r <- visited.indices
      c <- visited(r).indices
      if visited(r)(c)
    } yield Location(r, c)
    result.toSet
  }

  def playerCount: Int =
    players.size

  def player(index: Int): Player =
    players(index)

  def currentPlayer: Player =
    player(playerIndex)

  def hasPlayer(location: Location): Boolean =
    players.exists(_.location == location)

  def movePlayer(location: Location): Unit =
    if (!finished) {
      currentPlayer.changeLocation(location)
      finished = currentPlayer.wins(location)
      if (!finished)
        nextPlayer()
    }

  def hasWall(location: Location, side: Direction): Boolean = {
    val (walls, direction) = if (side.isHorizontal) (verticalWalls, Direction.up) else (horizontalWalls, Direction.left)
    val wallLocation = location.wall(side)
    val secondLocation = wallLocation.go(direction)
    (isValidWall(wallLocation) && walls(wallLocation.row)(wallLocation.column)) ||
      (isValidWall(secondLocation) && walls(secondLocation.row)(secondLocation.column))
  }

  //  def addWall(location: Location, side: Direction, direction: Direction): Unit = {
  //    val wallLocation = location.wall(side).wall(direction)
  //    if (isValidWall(wallLocation)) {
  //      val walls = if (side.isHorizontal) verticalWalls else horizontalWalls
  //      walls(wallLocation.row)(wallLocation.column) = true
  //      nextPlayer()
  //    }
  //  }

  def hasHorizontalWall(location: Location): Boolean =
    isValidWall(location) && horizontalWalls(location.row)(location.column)

  def isHorizontalWallAllowed(location: Location): Boolean =
    !finished && isValidWall(location) && {
      horizontalWalls(location.row)(location.column) = true
      val result = everyoneCanWin
      horizontalWalls(location.row)(location.column) = false
      result
    }

  def addHorizontalWall(location: Location): Unit =
    if (!finished && isValidWall(location)) {
      horizontalWalls(location.row)(location.column) = true
      currentPlayer.useWall()
      nextPlayer()
    }

  def hasVerticalWall(location: Location): Boolean =
    isValidWall(location) && verticalWalls(location.row)(location.column)

  def isVerticalWallAllowed(location: Location): Boolean =
    !finished && isValidWall(location) && {
      verticalWalls(location.row)(location.column) = true
      val result = everyoneCanWin
      verticalWalls(location.row)(location.column) = false
      result
    }

  def addVerticalWall(location: Location): Unit =
    if (!finished && isValidWall(location)) {
      verticalWalls(location.row)(location.column) = true
      currentPlayer.useWall()
      nextPlayer()
    }

  private def everyoneCanWin: Boolean =
    players.forall { player =>
      val reachable = reachableLocations(player.location)
      reachable.exists(player.wins)
    }

  private def isValidWall(location: Location): Boolean =
    location.isValid(horizontalWalls.length, verticalWalls.length)

  private def nextPlayer(): Unit =
    playerIndex = (playerIndex + 1) % players.size
}