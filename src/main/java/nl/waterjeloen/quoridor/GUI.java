package nl.waterjeloen.quoridor;

import javax.swing.*;
import java.awt.Dimension;

public class GUI extends JFrame {
    public static final long serialVersionUID = -1L;
    
    private final BoardPanel panel;

    public GUI(Board board) {
        this.panel = new BoardPanel(board);

        setContentPane(panel);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(1000, 1000));
        pack();
    }

    public BoardPanel getPanel() {
        return panel;
    }

    public void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }
}
