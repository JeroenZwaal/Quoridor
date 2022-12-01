package nl.zwaaltjes.quoridor

import java.awt.Dimension
import javax.swing.{JFrame, WindowConstants}

class GUI(board: Board) extends JFrame {
  val panel: BoardPanel = new BoardPanel(board)

  setContentPane(panel)
  setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
  setPreferredSize(new Dimension(1000, 1000))
  pack()
}