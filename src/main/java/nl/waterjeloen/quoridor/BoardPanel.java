package nl.waterjeloen.quoridor;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

public class BoardPanel extends JComponent {
    private final Board board;

    public BoardPanel(Board board) {
        this.board = board;
    }

    @Override
    public void paintComponent(Graphics graphics) {
        final Graphics2D g = (Graphics2D) graphics;

        final int cellWidth = getWidth() / (board.getSize() * 7 - 1);
        final int cellHeight = getHeight() / (board.getSize() * 7 - 1);
        final int cellSize = Math.min(cellWidth, cellHeight);
        final int xOffset = (getWidth() - cellSize * (board.getSize() * 7 - 1)) / 2;
        final int yOffset = (getHeight() - cellSize * (board.getSize() * 7 - 1)) / 2;

        g.setColor(Color.BLACK);
        for (int r = 0; r < board.getSize(); ++r) {
            for (int c = 0; c < board.getSize(); ++c) {
                g.fillRect(xOffset + cellSize * c * 7, yOffset + cellSize * r * 7, cellSize * 6, cellSize * 6);
            }
        }
        g.setColor(Color.BLUE);
        g.fillOval(
            xOffset + cellSize * (board.getPlayer1().getColumn() * 7 + 1),
            yOffset + cellSize * (board.getPlayer1().getRow() * 7 + 1),
            cellSize * 4,
            cellSize * 4);
        g.setColor(Color.RED);
        g.fillOval(
                xOffset + cellSize * (board.getPlayer2().getColumn() * 7 + 1),
                yOffset + cellSize * (board.getPlayer2().getRow() * 7 + 1),
                cellSize * 4,
                cellSize * 4);
        g.setColor(Color.ORANGE);
        for (int r = 0; r < board.getSize() - 1; ++r) {
            for (int c = 0; c < board.getSize() - 1; ++c) {
                if (board.hasHorizontalWall(r, c)) {
                    g.fillRect(xOffset + cellSize * c * 7, yOffset + cellSize * (r * 7 + 6), cellSize * 13, cellSize);
                }
                if (board.hasVerticalWall(r, c)) {
                    g.fillRect(xOffset + cellSize * (c * 7 + 6), yOffset + cellSize * r * 7, cellSize, cellSize * 13);
                }
            }
        }
    }
}
