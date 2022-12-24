package nl.zwaaltjes.quoridor

import java.awt.{Color, Graphics, Graphics2D, Rectangle}
import java.awt.event.{MouseAdapter, MouseEvent}
import javax.swing.JComponent

object BoardPanel {
  private val playerColors = Array(Color.red, Color.blue)

  private val side = 7
}

class BoardPanel(board: Board) extends JComponent {
  import BoardPanel.*

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
        val cellY = (y - cell.y) / cell.height
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

  def highlightField(location: Location): Unit =
    if (location.isValid(board.size)) {
      highlights ::= new Rectangle(location.column * side, location.row * side, side - 1, side - 1)
      repaint()
    }

  def highlightCenter(location: Location): Unit =
    if (location.isValid(board.size - 1)) {
      highlights ::= new Rectangle((location.column + 1) * side - 1, (location.row + 1) * side -1, 1, 1)
      repaint()
    }

  def highlightHorizontalWall(location: Location): Unit =
    if (location.isValid(board.size - 1, board.size)) {
      highlights ::= new Rectangle(location.column * side, (location.row + 1) * side - 1, side - 1, 1)
      repaint()
    }

  def highlightVerticalWall(location: Location): Unit =
    if (location.isValid(board.size, board.size - 1)) {
      highlights ::= new Rectangle((location.column + 1) * side - 1, location.row * side, 1, side - 1)
      repaint()
    }

  def clearHighlights(): Unit = {
    highlights = Nil
    repaint()
  }

  override def paintComponent(graphics: Graphics): Unit = {
    val g = graphics.asInstanceOf[Graphics2D]
    val cell = cellDimension

    g.setColor(Color.black)
    for {
      r <- 0 until board.size
      c <- 0 until board.size
    } g.fillRect(cell.x + cell.width * c * side, cell.y + cell.height * r * side, cell.width * (side - 1), cell.height * (side - 1))

    g.setColor(Color.orange)
    for {
      r <- 0 until board.size - 1
      c <- 0 until board.size - 1
      location = Location(r, c)
    } {
      if (board.hasHorizontalWall(location))
        g.fillRect(cell.x + cell.width * c * side, cell.y + cell.height * ((r + 1) * side - 1), cell.width * 13, cell.height)
      if (board.hasVerticalWall(location))
        g.fillRect(cell.x + cell.width * ((c + 1) * side - 1), cell.y + cell.height * r * side, cell.width, cell.height * 13)
    }

    g.setColor(Color.green)

    for (r <- highlights) {
      g.fillRect(cell.x + cell.width * r.x, cell.y + cell.height * r.y, cell.width * r.width, cell.height * r.height)
    }
    for (i <- 0 until board.playerCount) {
      val player = board.player(i)
      g.setColor(playerColors(i))
      g.fillOval(
        cell.x + cell.width * (player.location.column * side + 1),
        cell.y + cell.height * (player.location.row * side + 1),
        cell.width * (side + 1) / 2,
        cell.height * (side + 1) / 2
      )
    }
  }

  private def cellDimension: Rectangle = {
    val cellWidth = getWidth / (board.size * side - 1)
    val cellHeight = getHeight / (board.size * side - 1)
    val cellSize = Math.min(cellWidth, cellHeight)
    val xOffset = (getWidth - cellSize * (board.size * side - 1)) / 2
    val yOffset = (getHeight - cellSize * (board.size * side - 1)) / 2
    new Rectangle(xOffset, yOffset, cellSize, cellSize)
  }
}