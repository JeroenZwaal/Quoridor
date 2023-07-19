package nl.zwaaltjes.quoridor

import java.awt.{Color, Font, Graphics, Graphics2D, Rectangle, RenderingHints, Toolkit}
import java.awt.event.{MouseAdapter, MouseEvent}
import javax.swing.JComponent

object BoardPanel {
  private val playerColors = Array(Color.red, Color.blue, Color.green, Color.yellow)

  private val side = 7
}

class BoardPanel(board: Board) extends JComponent {
  import BoardPanel.*

  private val colorMap = {
    val entries = for (i <- 0 until board.playerCount) yield board.player(i) -> playerColors(i)
    entries.toMap
  }
  private var listeners = List.empty[Board.Listener]
  private var highlights = List.empty[Rectangle]

  addMouseListener(new MouseAdapter {
    override def mouseClicked(event: MouseEvent): Unit = {
      val x = event.getX
      val y = event.getY
      val cell = cellDimension
      val max = board.size * side - 1
      if ((x >= cell.x) && (x <= cell.x + max * cell.width) && (y >= cell.y) && (y <= cell.y + max * cell.height)) {
        val cellX = (x - cell.x) / cell.width
        val cellY = (cell.y + max * cell.height - y) / cell.height
        val location = Location(cellY / side, cellX / side)
        val isHorizontalWall = cellY % side == (side - 1)
        val isVerticalWall = cellX % side == (side - 1)
        listeners.foreach { l =>
          if (!isHorizontalWall && !isVerticalWall)
            l.fieldClicked(location)
          else if (isHorizontalWall && isVerticalWall)
            l.centerClicked(location)
          else if (isHorizontalWall)
            l.horizontalWallClicked(location)
          else // (isVerticalWall)
            l.verticalWallClicked(location)
        }
      }
    }
  })

  def addListener(listener: Board.Listener): Unit =
    listeners ::= listener

  private def xOffset(column: Int): Int = column * side
  private def yOffset(row: Int): Int = (board.size - row - 1) * side

  def highlightField(location: Location): Unit =
    if (location.isValid(board.size)) {
      highlights ::= new Rectangle(xOffset(location.column), yOffset(location.row), side - 1, side - 1)
      repaint()
    }

  def highlightCenter(location: Location): Unit =
    if (location.isValid(board.size - 1)) {
      highlights ::= new Rectangle(xOffset(location.column + 1) - 1, yOffset(location.row) - 1, 1, 1)
      repaint()
    }

  def highlightHorizontalWall(location: Location): Unit =
    if (location.isValid(board.size - 1, board.size)) {
      highlights ::= new Rectangle(xOffset(location.column), yOffset(location.row) - 1, side - 1, 1)
      repaint()
    }

  def highlightVerticalWall(location: Location): Unit =
    if (location.isValid(board.size, board.size - 1)) {
      highlights ::= new Rectangle(xOffset(location.column + 1) - 1, yOffset(location.row), 1, side - 1)
      repaint()
    }

  def clearHighlights(): Unit = {
    highlights = Nil
    repaint()
  }

  override def paintComponent(graphics: Graphics): Unit = {
    val g = graphics.asInstanceOf[Graphics2D]
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    val cell = cellDimension

    def xOf(column: Int): Int = cell.x + column * cell.width * side
    def yOf(row: Int): Int = cell.y + (board.size - row - 1) * cell.height * side

    // draw indices
    g.setColor(Color.black)
    for (r <- 0 until board.size) {
      val text = Location.rowString(r)
      drawText(g, text, new Rectangle(xOf(-1), yOf(r), cell.width, cell.height))
    }
    for (c <- 0 until board.size) {
      val text = Location.columnString(c)
      drawText(g, text, new Rectangle(xOf(c), yOf(-1), cell.width, cell.height))
    }

    // draw fields
    val reachable = board.reachableLocations(board.currentPlayer.location)
    for {
      r <- 0 until board.size
      c <- 0 until board.size
      location = Location(r, c)
    } {
      val color = if (board.winner.isDefined) Color.black
        else if (location == board.currentPlayer.location) Color.gray.darker
        else if (board.currentPlayer.wins(location) && reachable.contains(location)) colorMap(board.currentPlayer).darker.darker
        else Color.black
      g.setColor(color)
      g.fillRect(xOf(c), yOf(r), cell.width * (side - 1), cell.height * (side - 1))
    }

    // draw walls
    g.setColor(Color.orange.darker)
    for {
      r <- 0 until board.size - 1
      c <- 0 until board.size - 1
      location = Location(r, c)
    } {
      if (board.hasHorizontalWall(location))
        g.fillRect(xOf(c), yOf(r) - cell.height, cell.width * (2 * side - 1), cell.height)
      if (board.hasVerticalWall(location))
        g.fillRect(xOf(c + 1) - cell.width, yOf(r + 1), cell.width, cell.height * (2 * side - 1))
    }

    g.setColor(Color.orange)

    // draw highlights
    for (r <- highlights) {
      g.fillRect(cell.x + cell.width * r.x, cell.y + cell.height * r.y, cell.width * r.width, cell.height * r.height)
    }

    // draw players
    for (i <- 0 until board.playerCount) {
      val player = board.player(i)
      g.setColor(playerColors(i).darker)
      g.fillOval(
        xOf(player.location.column) + cell.width,
        yOf(player.location.row) + cell.height,
        cell.width * (side + 1) / 2,
        cell.height * (side + 1) / 2,
      )
      g.setColor(Color.white)
      drawText(g, player.walls.toString, new Rectangle(
        xOf(player.location.column),
        yOf(player.location.row),
        cell.width / 3,
        cell.height / 3,
      ))
    }
  }

  private def drawText(graphics: Graphics2D, text: String, box: Rectangle): Unit = {
    val fontSize = 72f * box.height * side / Toolkit.getDefaultToolkit.getScreenResolution
    graphics.setFont(new Font(Font.SERIF, Font.PLAIN, fontSize.toInt))
    val metrics = graphics.getFontMetrics

    val bounds = metrics.getStringBounds(text, graphics)
    val offset = (box.width * side - bounds.getWidth.toInt) / 2
    graphics.drawString(text, box.x + offset, box.y + metrics.getAscent)
  }

  private def cellDimension: Rectangle = {
    val cellWidth = getWidth / ((board.size + 1) * side - 1)
    val cellHeight = getHeight / ((board.size + 1) * side - 1)
    val cellSize = cellWidth min cellHeight
    val xOffset = (getWidth - cellSize * ((board.size + 1) * side - 1)) / 2
    val yOffset = (getHeight - cellSize * ((board.size + 1) * side - 1)) / 2
    new Rectangle(xOffset + cellSize * side, yOffset, cellSize, cellSize)
  }
}