package nl.zwaaltjes.quoridor.game

import nl.zwaaltjes.quoridor.api.*
import nl.zwaaltjes.quoridor.impl.QuoridorImpl
import nl.zwaaltjes.quoridor.game.RichPosition.*

import javax.swing.JOptionPane

object Main {
  def main(args: Array[String]): Unit = {
    val players = if (args.nonEmpty) args.toSeq else Seq("You", "Me", "He", "She")
    val quoridor: Quoridor = QuoridorImpl
    val game = quoridor.createGame(players)
    val gui = new GUI(game)
    gui.panel.addListener(new Listener(game, gui))
    gui.setVisible(true)
    gui.repaint()
  }

  private class Listener(game: Game, gui: GUI) extends GamePanel.Listener {
    private var fields = Seq.empty[Position]
    private var hWalls = Map.empty[Position, Position]
    private var vWalls = Map.empty[Position, Position]

    override def fieldClicked(position: Position): Unit = {
      val highlighted = fields.contains(position)
      reset()

      if (highlighted) {
        gui.panel.clearHighlights()
        game.play(PlayerMove(position)) match {
          case Right(_) =>
            game.winner.foreach { player =>
              gui.showMessage(s"Player '${player.name}' wins!")
              System.exit(0)
            }
          case Left(error) =>
            JOptionPane.showMessageDialog(gui, error, "Oops", JOptionPane.WARNING_MESSAGE)
        }
      } else {
        val player = game.currentPlayer
        if (fields.isEmpty && position == player.position) {
          fields = game.playerMoves.map(_.position).toSeq
          fields.foreach { position =>
            gui.panel.highlightField(position)
          }
        }
      }
    }

    override def centerClicked(position: Position): Unit = {
      reset()

      val (hBefore, vBefore) = (hWalls.size, vWalls.size)
      if (game.currentPlayer.hasWalls) {
        if (game.allowsHorizontalWall(position)) {
          gui.panel.highlightHorizontalWall(position)
          gui.panel.highlightHorizontalWall(position.right)
          hWalls += position -> position
          hWalls += position.right -> position
        }
        if (game.allowsVerticalWall(position)) {
          gui.panel.highlightVerticalWall(position)
          gui.panel.highlightVerticalWall(position)
          gui.panel.highlightVerticalWall(position.up)
          vWalls += position -> position.up
          vWalls += position.up -> position
        }
        if (hWalls.size > hBefore || vWalls.size > vBefore)
          gui.panel.highlightCenter(position)
      }
    }

    override def horizontalWallClicked(position: Position): Unit = {
      val wallPosition = hWalls.get(position)
      reset()

      wallPosition match {
        case Some(wall) =>
          game.play(HorizontalWall(wall))
        case None if game.currentPlayer.hasWalls =>
          val before = hWalls.size
          if (game.allowsHorizontalWall(position)) {
            gui.panel.highlightCenter(position)
            gui.panel.highlightHorizontalWall(position.right)
            hWalls += position.right -> position
          }
          if (game.allowsHorizontalWall(position.left)) {
            gui.panel.highlightCenter(position.left)
            gui.panel.highlightHorizontalWall(position.left)
            hWalls += position.left -> position.left
          }
          if (hWalls.size > before)
            gui.panel.highlightHorizontalWall(position)
        case _ =>
      }
    }

    override def verticalWallClicked(position: Position): Unit = {
      val wallPosition = vWalls.get(position)
      reset()

      wallPosition match {
        case Some(wall) =>
          game.play(VerticalWall(wall))
        case None if game.currentPlayer.hasWalls =>
          val before = vWalls.size
          if (game.allowsVerticalWall(position)) {
            gui.panel.highlightCenter(position)
            gui.panel.highlightVerticalWall(position.up)
            vWalls += position.up -> position
          }
          if (game.allowsVerticalWall(position.down)) {
            gui.panel.highlightCenter(position.down)
            gui.panel.highlightVerticalWall(position.down)
            vWalls += position.down -> position.down
          }
          if (vWalls.size > before)
            gui.panel.highlightVerticalWall(position)
        case _ =>
      }
    }

    private def reset(): Unit = {
      gui.panel.clearHighlights()
      fields = Nil
      hWalls = Map.empty
      vWalls = Map.empty
    }
  }
}