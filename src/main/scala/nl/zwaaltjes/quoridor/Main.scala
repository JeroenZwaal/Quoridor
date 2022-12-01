package nl.zwaaltjes.quoridor

object Main {
  private val size = 9

  def main(args: Array[String]): Unit = {
    val me = Player("me", Location(0, size / 2))
    val you = Player("you", Location(size - 1, size / 2))
    val board = new Board(size, me, you)
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

    private def checkMove(location: Location, direction: Direction, andThen: Direction*): Unit =
      if (!board.hasWall(location, direction)) {
        val next = location.go(direction)
        if (!board.hasPlayer(next))
          fields ::= next
        else if (!board.hasWall(next, direction))
          fields ::= next.go(direction)
        else
          andThen.filterNot(board.hasWall(next, _)).foreach { d =>
            fields ::= next.go(d)
          }
      }

    override def centerClicked(location: Location): Unit = {
      reset()

      if (!board.hasHorizontalWall(location) && !board.hasVerticalWall(location)) {
        if (!board.hasHorizontalWall(location.left) && !board.hasHorizontalWall(location.right)) {
          gui.panel.highlightHorizontalWall(location.right)
          gui.panel.highlightHorizontalWall(location)
          hwalls += location -> location
          hwalls += location.right -> location
        }
        if (!board.hasVerticalWall(location.down) && !board.hasVerticalWall(location.up)) {
          gui.panel.highlightVerticalWall(location.down)
          gui.panel.highlightVerticalWall(location)
          vwalls += location -> location
          vwalls += location.down -> location
        }
        gui.panel.highlightCenter(location)
      }
    }

    override def horizontalWallClicked(location: Location): Unit = {
      val wallLocation = hwalls.get(location)
      reset()

      wallLocation match {
        case Some(wall) =>
          board.addHorizontalWall(wall)
        case None if !board.hasHorizontalWall(location.left) && !board.hasHorizontalWall(location) =>
          if (!board.hasVerticalWall(location) && !board.hasHorizontalWall(location.right)) {
            gui.panel.highlightCenter(location)
            gui.panel.highlightHorizontalWall(location.right)
            hwalls += location.right -> location
          }
          if (!board.hasVerticalWall(location.left) && !board.hasHorizontalWall(location.left(2))) {
            gui.panel.highlightCenter(location.left)
            gui.panel.highlightHorizontalWall(location.left)
            hwalls += location.left -> location.left
          }
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
        case None if !board.hasVerticalWall(location.up) && !board.hasVerticalWall(location) =>
          if (!board.hasVerticalWall(location.down) && !board.hasHorizontalWall(location)) {
            gui.panel.highlightVerticalWall(location.down)
            gui.panel.highlightCenter(location)
            vwalls += location.down -> location
          }
          if (!board.hasVerticalWall(location.up(2)) && !board.hasHorizontalWall(location.up)) {
            gui.panel.highlightCenter(location.up)
            gui.panel.highlightVerticalWall(location.up)
            vwalls += location.up -> location.up
          }
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