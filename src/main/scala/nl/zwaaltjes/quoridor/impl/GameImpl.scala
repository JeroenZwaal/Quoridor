package nl.zwaaltjes.quoridor.impl

import nl.zwaaltjes.quoridor.api.*

private class GameImpl(override val size: Int, override val players: IndexedSeq[PlayerImpl]) extends Game {
  import GameImpl.*

  // board indices start at 1
  private val horizontalWalls = Array.fill(size + 1, size + 1)(false)
  private val verticalWalls = Array.fill(size + 1, size + 1)(false)
  private var moves = IndexedSeq.empty[Move]
  private var playerIndex = 0
  private var finished = false

  // place (invisible) walls around board to prevent off-board index checking
  for (i <- 1 until size) {
    horizontalWalls(0)(i) = true
    horizontalWalls(size)(i) = true
    verticalWalls(i)(0) = true
    verticalWalls(i)(size) = true
  }

  override def currentPlayer: PlayerImpl =
    players(playerIndex)

  override def winner: Option[Player] =
    Option.when(finished)(currentPlayer)

  override def history: IndexedSeq[Move] =
    moves

  override def play(move: Move): Either[String, Boolean] =
    if (finished)
      Left("Game is finished")
    else {
      val result = move match {
        case PlayerMove(position) => movePlayer(position)
        case HorizontalWall(position) => horizontalWall(position)
        case VerticalWall(position) => verticalWall(position)
      }
      finished = result.getOrElse(false)
      if (!finished)
        playerIndex = (playerIndex + 1) % players.size
      moves :+= move
      result
    }

  override def playerMoves: Set[Move] =
    validMoves(currentPlayer.position).map(PlayerMove.apply)

  override def allowsHorizontalWall(lowerLeft: Position): Boolean =
    horizontalWallError(lowerLeft).isEmpty

  override def allowsVerticalWall(lowerLeft: Position): Boolean =
    verticalWallError(lowerLeft).isEmpty

  override def reachablePositions(from: Position): Set[Position] = {
    val visited = Array.fill(size + 1, size + 1)(false)

    def recurse(position: Position): Unit = {
      visited(position.row)(position.column) = true
      for (direction <- directions if !crossesWall(position, direction)) {
        val next = direction(position)
        if (!visited(next.row)(next.column))
          recurse(next)
      }
    }

    recurse(from)

    val result = for {
      r <- visited.indices
      c <- visited(r).indices
      if visited(r)(c)
    } yield Position(r, c)
    result.toSet
  }


  private def movePlayer(position: Position): Either[String, Boolean] =
    if (!isValidPosition(position))
      Left(s"Invalid position: $position")
    else if (!validMoves(currentPlayer.position).contains(position))
      Left(s"Cannot move from ${currentPlayer.position} to $position")
    else {
      currentPlayer.changePosition(position)
      Right(currentPlayer.wins(position))
    }

  private def horizontalWall(position: Position): Either[String, Boolean] =
    horizontalWallError(position) match {
      case Some(error) => Left(error)
      case None =>
        horizontalWalls(position.row)(position.column) = true
        if (everyoneCanWin) {
          currentPlayer.useWall()
          Right(false)
        } else {
          horizontalWalls(position.row)(position.column) = false
          Left("Wall closes off path for some player")
        }
    }

  private def verticalWall(position: Position): Either[String, Boolean] =
    verticalWallError(position) match {
      case Some(error) => Left(error)
      case None =>
        verticalWalls(position.row)(position.column) = true
        if (everyoneCanWin) {
          currentPlayer.useWall()
          Right(false)
        } else {
          verticalWalls(position.row)(position.column) = false
          Left("Wall closes off path for some player")
        }
    }

  private def horizontalWallError(position: Position): Option[String] =
    if (currentPlayer.walls == 0)
      Some("Player has no walls left")
    else if (!isValidWall(position))
      Some(s"Invalid wall position: $position")
    else if (hasHorizontalWall(position))
      Some("Wall overlaps with an existing wall")
    else if (verticalWalls(position.row)(position.column))
      Some("Wall crosses an existing wall")
    else
      None

  private def verticalWallError(position: Position): Option[String] =
    if (currentPlayer.walls == 0)
      Some("Player has no walls left")
    else if (!isValidWall(position))
      Some(s"Invalid wall position: $position")
    else if (hasVerticalWall(position))
      Some("Wall overlaps with an existing wall")
    else if (horizontalWalls(position.row)(position.column))
      Some("Wall crosses an existing wall")
    else
      None

  private def everyoneCanWin: Boolean =
    players.forall { player =>
      val reachable = reachablePositions(player.position)
      reachable.exists(player.wins)
    }


  private def hasHorizontalWall(position: Position): Boolean = {
    val from = (position.column - 1) max 1
    val to = (position.column + 1) min size
    (from to to).exists(c => horizontalWalls(position.row)(c))
  }

  private def hasVerticalWall(position: Position): Boolean = {
    val from = (position.row - 1) max 1
    val to = (position.row + 1) min size
    (from to to).exists(r => verticalWalls(r)(position.column))
  }

  private def crossesWall(position: Position, direction: Direction): Boolean = {
    val walls = if (direction.isHorizontal) verticalWalls else horizontalWalls
    val wallPositions = direction.wallPositions(position)
    wallPositions.exists(p => walls(p.row)(p.column))
  }

  private def validMoves(position: Position): Set[Position] = {
    val results = for {
      direction <- directions
      if !crossesWall(position, direction)
    } yield jumpMoves(position, direction)
    results.toSet.flatten
  }

  private def jumpMoves(position: Position, direction: Direction, result: Set[Position] = Set.empty): Set[Position] = {
    val next = direction(position)
    if (!isOccupied(next))
      result + next // normal move
    else {
      if (!crossesWall(next, direction))
        jumpMoves(next, direction, result)
      else {
        val results = for {
          orthogonal <- orthogonalDirections(direction)
          if !crossesWall(next, orthogonal)
        } yield jumpMoves(next, orthogonal, result)
        results.toSet.flatten
      }
    }
  }

  private def isOccupied(position: Position): Boolean =
    players.exists(_.position == position)

  private def isValidPosition(position: Position): Boolean =
    isValid(position.row, size) && isValid(position.column, size)

  private def isValidWall(position: Position): Boolean =
    isValid(position.row, size - 1) && isValid(position.column, size - 1)

  private def isValid(index: Int, maximum: Int): Boolean =
    (index > 0) && (index <= maximum)
}

object GameImpl {
  private final case class Direction(isHorizontal: Boolean, rowDelta: Int, columnDelta: Int, wallPositions: Position => Seq[Position]) {
    def apply(position: Position): Position =
      Position(position.row + rowDelta, position.column + columnDelta)
  }

  private val directions = Seq(
    Direction(true, 0, 1, p => Seq(p, Position(p.row - 1, p.column))), // right
    Direction(true, 0, -1, p => Seq(Position(p.row, p.column - 1), Position(p.row - 1, p.column - 1))), // left
    Direction(false, 1, 0, p => Seq(p, Position(p.row, p.column - 1))), // up
    Direction(false, -1, 0, p => Seq(Position(p.row - 1, p.column), Position(p.row - 1, p.column - 1))), // down
  )

  private def orthogonalDirections(direction: Direction): Seq[Direction] =
    if (direction.isHorizontal) directions.filterNot(_.isHorizontal) else directions.filter(_.isHorizontal)
}