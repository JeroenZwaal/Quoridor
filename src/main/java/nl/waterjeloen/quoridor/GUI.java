package nl.waterjeloen.quoridor;

import javax.swing.*;
import java.awt.*;

public class GUI extends JFrame {
    private final Board board;
    private final BoardPanel panel;

    public GUI(Board board) {
        this.board = board;
        this.panel = new BoardPanel(board);

        setContentPane(panel);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(1000, 1000));
        pack();
    }
}
