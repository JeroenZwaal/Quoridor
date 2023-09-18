package nl.zwaaltjes.quoridor.game

import nl.zwaaltjes.quoridor.api.Game

import java.awt.Dimension
import javax.swing.{JFrame, JOptionPane, UIManager, WindowConstants}

class GUI(game: Game) extends JFrame {
  private[game] val panel = new GamePanel(game)

  UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName)

  setContentPane(panel)
  setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
  setPreferredSize(new Dimension(1000, 1000))
  pack()

  def showMessage(message: String): Unit =
    JOptionPane.showMessageDialog(this, message, "Quoridor", JOptionPane.INFORMATION_MESSAGE)
}