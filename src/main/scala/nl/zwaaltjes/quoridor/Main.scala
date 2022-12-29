package nl.zwaaltjes.quoridor

object Main {
  private val size = 9

  def main(args: Array[String]): Unit = {
    val me = Player("me", _.row == size - 1, Location(0, size / 2))
    val you = Player("you", _.row == 0, Location(size - 1, size / 2))
    val him = Player("him", _.column == size - 1, Location(size / 2, 0))
    val her = Player("her", _.column == 0, Location(size / 2, size - 1))
    val board = new Board(size, me, her, you, him)
    val gui = new GUI(board)
    gui.panel.addListener(new Listener(board, gui))
    gui.setVisible(true)
    gui.repaint()
  }

  private class Listener(board: Board, gui: GUI) extends Board.Listener {
    private var fields = List.empty[Location]
    private var hwalls = Map.empty[Location, Location]
    private var vwalls = Map.empty[Location, Location]

    override def fieldClicked(location: Location): Unit = {
      val highlighted = fields.contains(location)
      reset()

      if (highlighted) {
        gui.panel.clearHighlights()
        board.movePlayer(location)
        board.winner.foreach { winner =>
          gui.showMessage(s"Player '${winner.name}' wins!")
          System.exit(0)
        }
      } else {
        val player = board.currentPlayer
        if (fields.isEmpty && location == player.location) {
          checkMove(location, Direction.down, Direction.left, Direction.right)
          checkMove(location, Direction.up, Direction.left, Direction.right)
          checkMove(location, Direction.left, Direction.up, Direction.down)
          checkMove(location, Direction.right, Direction.up, Direction.down)
          fields.foreach { location =>
            gui.panel.highlightField(location)
          }
        }
      }
    }

    @annotation.tailrec
    private def checkMove(location: Location, direction: Direction, andThen: Direction*): Unit = {
      if (!board.hasWall(location, direction)) {
        val next = location.go(direction)
        if (!board.hasPlayer(next))
          fields ::= next
        else if (board.hasWall(next, direction))
          andThen.filterNot(board.hasWall(next, _)).foreach { d =>
            fields ::= next.go(d)
          }
        else
          checkMove(next, direction, andThen*)
      }
    }

    override def centerClicked(location: Location): Unit = {
      reset()

      if (board.currentPlayer.hasWalls && !board.hasHorizontalWall(location) && !board.hasVerticalWall(location)) {
        val (hbefore, vbefore) = (hwalls.size, vwalls.size)
        if (!board.hasHorizontalWall(location.left) && !board.hasHorizontalWall(location.right)) {
          if (board.isHorizontalWallAllowed(location)) {
            gui.panel.highlightHorizontalWall(location.right)
            hwalls += location.right -> location
          }
          if (board.isHorizontalWallAllowed(location)) {
            gui.panel.highlightHorizontalWall(location)
            hwalls += location -> location
          }
        }
        if (!board.hasVerticalWall(location.down) && !board.hasVerticalWall(location.up)) {
          if (board.isVerticalWallAllowed(location)) {
            gui.panel.highlightVerticalWall(location.down)
            vwalls += location.down -> location
          }
          if (board.isVerticalWallAllowed(location)) {
            gui.panel.highlightVerticalWall(location)
            vwalls += location -> location
          }
        }
        if (hwalls.size > hbefore || vwalls.size > vbefore)
          gui.panel.highlightCenter(location)
      }
    }

    override def horizontalWallClicked(location: Location): Unit = {
      val wallLocation = hwalls.get(location)
      reset()

      wallLocation match {
        case Some(wall) =>
          board.addHorizontalWall(wall)
        case None if board.currentPlayer.hasWalls && !board.hasHorizontalWall(location.left) && !board.hasHorizontalWall(location) =>
          val before = hwalls.size
          if (!board.hasVerticalWall(location) && !board.hasHorizontalWall(location.right)) {
            if (board.isHorizontalWallAllowed(location)) {
              gui.panel.highlightCenter(location)
              gui.panel.highlightHorizontalWall(location.right)
              hwalls += location.right -> location
            }
          }
          if (!board.hasVerticalWall(location.left) && !board.hasHorizontalWall(location.left(2))) {
            if (board.isHorizontalWallAllowed(location.left)) {
              gui.panel.highlightCenter(location.left)
              gui.panel.highlightHorizontalWall(location.left)
              hwalls += location.left -> location.left
            }
          }
          if (hwalls.size > before)
            gui.panel.highlightHorizontalWall(location)
        case _ =>
      }
    }

    override def verticalWallClicked(location: Location): Unit = {
      val wallLocation = vwalls.get(location)
      reset()

      wallLocation match {
        case Some(wall) =>
          board.addVerticalWall(wall)
        case None if board.currentPlayer.hasWalls && !board.hasVerticalWall(location.up) && !board.hasVerticalWall(location) =>
          val before = vwalls.size
          if (!board.hasVerticalWall(location.down) && !board.hasHorizontalWall(location)) {
            if (board.isVerticalWallAllowed(location)) {
              gui.panel.highlightVerticalWall(location.down)
              gui.panel.highlightCenter(location)
              vwalls += location.down -> location
            }
          }
          if (!board.hasVerticalWall(location.up(2)) && !board.hasHorizontalWall(location.up)) {
            if (board.isVerticalWallAllowed(location.up)) {
              gui.panel.highlightCenter(location.up)
              gui.panel.highlightVerticalWall(location.up)
              vwalls += location.up -> location.up
            }
          }
          if (vwalls.size > before)
            gui.panel.highlightVerticalWall(location)
        case _ =>
      }
    }

    private def reset(): Unit = {
      gui.panel.clearHighlights()
      fields = Nil
      hwalls = Map.empty
      vwalls = Map.empty
    }
  }
}