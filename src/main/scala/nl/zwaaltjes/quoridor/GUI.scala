package nl.zwaaltjes.quoridor

import java.awt.Dimension
import javax.swing.{JFrame, JOptionPane, UIManager, WindowConstants}

class GUI(board: Board) extends JFrame {
  val panel: BoardPanel = new BoardPanel(board)

  UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName)

  setContentPane(panel)
  setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
  setPreferredSize(new Dimension(1000, 1000))
  pack()

  def showMessage(message: String): Unit =
    JOptionPane.showMessageDialog(this, message)
}