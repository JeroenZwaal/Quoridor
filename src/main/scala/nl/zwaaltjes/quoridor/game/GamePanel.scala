package nl.zwaaltjes.quoridor.game

import nl.zwaaltjes.quoridor.api.*

import java.awt.event.{MouseAdapter, MouseEvent}
import java.awt.{Color, Font, Graphics, Graphics2D, Rectangle, RenderingHints, Toolkit}
import javax.swing.JComponent

object GamePanel {
  trait Listener {
    def fieldClicked(position: Position): Unit

    def centerClicked(position: Position): Unit

    def horizontalWallClicked(position: Position): Unit

    def verticalWallClicked(position: Position): Unit
  }

  private val playerColors = Array(Color.red, Color.blue, Color.green, Color.yellow)

  private val side = 7
}

class GamePanel(game: Game) extends JComponent {
  import GamePanel.*

  private val colorMap = game.players.zip(playerColors).toMap
  private var listeners = List.empty[Listener]
  private var highlights = List.empty[Rectangle]

  addMouseListener(new MouseAdapter {
    override def mouseClicked(event: MouseEvent): Unit = {
      val x = event.getX
      val y = event.getY
      val cell = cellDimension
      val max = game.size * side - 1
      if ((x >= cell.x) && (x <= cell.x + max * cell.width) && (y >= cell.y) && (y <= cell.y + max * cell.height)) {
        val cellX = (x - cell.x) / cell.width
        val cellY = (cell.y + max * cell.height - y) / cell.height
        val position = Position((cellY / side) + 1, (cellX / side) + 1)
        val isHorizontalWall = cellY % side == (side - 1)
        val isVerticalWall = cellX % side == (side - 1)
        listeners.foreach { l =>
          if (!isHorizontalWall && !isVerticalWall)
            l.fieldClicked(position)
          else if (isHorizontalWall && isVerticalWall)
            l.centerClicked(position)
          else if (isHorizontalWall)
            l.horizontalWallClicked(position)
          else // (isVerticalWall)
            l.verticalWallClicked(position)
        }
      }
    }
  })

  def addListener(listener: Listener): Unit =
    listeners ::= listener

  private def xOffset(column: Int): Int = (column - 1) * side

  private def yOffset(row: Int): Int = (game.size - row) * side

  def highlightField(position: Position): Unit = {
    highlights ::= new Rectangle(xOffset(position.column), yOffset(position.row), side - 1, side - 1)
    repaint()
  }

  def highlightCenter(position: Position): Unit = {
    highlights ::= new Rectangle(xOffset(position.column + 1) - 1, yOffset(position.row) - 1, 1, 1)
    repaint()
  }

  def highlightHorizontalWall(position: Position): Unit = {
    highlights ::= new Rectangle(xOffset(position.column), yOffset(position.row) - 1, side - 1, 1)
    repaint()
  }

  def highlightVerticalWall(position: Position): Unit = {
    highlights ::= new Rectangle(xOffset(position.column + 1) - 1, yOffset(position.row), 1, side - 1)
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

    def xOf(column: Int): Int = cell.x + (column - 1) * cell.width * side

    def yOf(row: Int): Int = cell.y + (game.size - row) * cell.height * side

    // draw indices
    g.setColor(Color.black)
    for (r <- 1 to game.size) {
      val text = Position.rowString(r)
      drawText(g, text, new Rectangle(xOf(0), yOf(r), cell.width, cell.height))
    }
    for (c <- 1 to game.size) {
      val text = Position.columnString(c)
      drawText(g, text, new Rectangle(xOf(c), yOf(0), cell.width, cell.height))
    }

    // draw fields
    val reachable = game.reachablePositions(game.currentPlayer.position)
    for {
      r <- 1 to game.size
      c <- 1 to game.size
      position = Position(r, c)
    } {
      val color = if (game.winner.isDefined) Color.black
      else if (position == game.currentPlayer.position) Color.gray.darker
      else if (game.currentPlayer.winsAt(position) && reachable.contains(position)) colorMap(game.currentPlayer).darker.darker
      else Color.black
      g.setColor(color)
      g.fillRect(xOf(c), yOf(r), cell.width * (side - 1), cell.height * (side - 1))
    }

    // draw walls and players
    g.setColor(Color.orange.darker)
    game.history.foreach {
      case HorizontalWall(Position(r, c)) => g.fillRect(xOf(c), yOf(r) - cell.height, cell.width * (2 * side - 1), cell.height)
      case VerticalWall(Position(r, c)) => g.fillRect(xOf(c + 1) - cell.width, yOf(r + 1), cell.width, cell.height * (2 * side - 1))
      case _ =>
    }

    g.setColor(Color.orange)

    // draw highlights
    for (r <- highlights) {
      g.fillRect(cell.x + cell.width * r.x, cell.y + cell.height * r.y, cell.width * r.width, cell.height * r.height)
    }

    // draw players
    for (player <- game.players) {
      g.setColor(colorMap(player).darker)
      g.fillOval(
        xOf(player.position.column) + cell.width,
        yOf(player.position.row) + cell.height,
        cell.width * (side + 1) / 2,
        cell.height * (side + 1) / 2,
      )
      g.setColor(Color.white)
      drawText(g, player.wallsLeft.toString, new Rectangle(
        xOf(player.position.column),
        yOf(player.position.row),
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
    val cellWidth = getWidth / ((game.size + 1) * side - 1)
    val cellHeight = getHeight / ((game.size + 1) * side - 1)
    val cellSize = cellWidth min cellHeight
    val xOffset = (getWidth - cellSize * ((game.size + 1) * side - 1)) / 2
    val yOffset = (getHeight - cellSize * ((game.size + 1) * side - 1)) / 2
    new Rectangle(xOffset + cellSize * side, yOffset, cellSize, cellSize)
  }
}