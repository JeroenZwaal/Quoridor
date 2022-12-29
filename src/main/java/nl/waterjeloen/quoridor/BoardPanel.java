package nl.waterjeloen.quoridor;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BoardPanel extends JComponent {
    private static final Color[] PLAYER_COLORS = { Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW };

    private final Board board;
    private final List<BoardListener> listeners;
    private final List<Rectangle> highlights;

    public BoardPanel(Board board) {
        this.board = board;
        this.listeners = new ArrayList<>();
        this.highlights = new ArrayList<>();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                final int x = event.getX();
                final int y = event.getY();

                final Rectangle cell = calculateCell();
                if ((x < cell.x) || (x > cell.x + (board.getSize() * 7 - 1) * cell.width)
                    || (y < cell.y) || (y > cell.y + (board.getSize() * 7 - 1) * cell.height)) {
                    return; // outside board
                }

                final int cellX = (x - cell.x) / cell.width;
                final int cellY = (y - cell.y) / cell.height;
                final Location location = new Location(cellY / 7, cellX / 7);

                final boolean isHorizontalWall = cellY % 7 == 6;
                final boolean isVerticalWall = cellX % 7 == 6;
                for (BoardListener l : listeners) {
                    if (!isHorizontalWall && !isVerticalWall) {
                        l.fieldClicked(location);
                    } else if (isHorizontalWall && isVerticalWall) {
                        l.centerClicked(location);
                    } else if (isHorizontalWall) {
                        l.horizontalWallClicked(location);
                    } else { // (isVerticalWall)
                        l.verticalWallClicked(location);
                    }
                }
            }
        });
    }

    public void addListener(BoardListener listener) {
        listeners.add(listener);
    }

    public void highlightField(Location location) {
        if (location.isValid(board.getSize())) {
            highlights.add(new Rectangle(location.column * 7, location.row * 7, 6, 6));
            repaint();
        }
    }

    public void highlightCenter(Location location) {
        if (location.isValid(board.getSize() - 1)) {
            highlights.add(new Rectangle(location.column * 7 + 6, location.row * 7 + 6, 1, 1));
            repaint();
        }
    }

    public void highlightHorizontalWall(Location location) {
        if (location.isValid(board.getSize() - 1, board.getSize())) {
            highlights.add(new Rectangle(location.column * 7, location.row * 7 + 6, 6, 1));
            repaint();
        }
    }

    public void highlightVerticalWall(Location location) {
        if (location.isValid(board.getSize(), board.getSize() - 1)) {
            highlights.add(new Rectangle(location.column * 7 + 6, location.row * 7, 1, 6));
            repaint();
        }
    }

    public void clearHighlights() {
        highlights.clear();
        repaint();
    }

    @Override
    public void paintComponent(Graphics graphics) {
        final Graphics2D g = (Graphics2D) graphics;

        final Rectangle cell = calculateCell();

        final Set<Location> reachable = board.getReachableLocations(board.getCurrentPlayer().getLocation());
        for (int r = 0; r < board.getSize(); ++r) {
            for (int c = 0; c < board.getSize(); ++c) {
                final Location location = new Location(r, c);
                final Color color = (location.equals(board.getCurrentPlayer().getLocation())) ? Color.GRAY.darker()
                        : (board.getCurrentPlayer().winsAt(location) && reachable.contains(location)) ? Color.GRAY.darker()
                        : Color.BLACK;
                g.setColor(color);
                g.fillRect(
                    cell.x + cell.width * c * 7,
                    cell.y + cell.height * r * 7,
                    cell.width * 6,
                    cell.height * 6);
            }
        }

        g.setColor(Color.ORANGE);
        for (int r = 0; r < board.getSize() - 1; ++r) {
            for (int c = 0; c < board.getSize() - 1; ++c) {
                final Location location = new Location(r, c);
                if (board.hasHorizontalWall(location)) {
                    g.fillRect(
                        cell.x + cell.width * c * 7,
                        cell.y + cell.height * (r * 7 + 6),
                        cell.width * 13,
                        cell.height);
                }
                if (board.hasVerticalWall(location)) {
                    g.fillRect(
                        cell.x + cell.width * (c * 7 + 6),
                        cell.y + cell.height * r * 7,
                        cell.width,
                        cell.height * 13);
                }
            }
        }
        g.setColor(Color.GREEN);
        for (Rectangle r : highlights) {
            g.fillRect(
                cell.x + cell.width * r.x,
                cell.y + cell.height * r.y,
                cell.width * r.width,
                cell.height * r.height);
        }
        for (int i = 0; i < board.getPlayerCount(); ++i) {
            final Player player = board.getPlayer(i);
            g.setColor(PLAYER_COLORS[i]);
            g.fillOval(
            cell.x + cell.width * (player.getLocation().column * 7 + 1),
            cell.y + cell.height * (player.getLocation().row * 7 + 1),
            cell.width * 4,
            cell.height * 4);
            g.setColor(Color.WHITE);
            g.drawString(Integer.toString(player.getWalls()), cell.x + cell.width * (player.getLocation().column * 7 + 1), cell.y + cell.height * (player.getLocation().row * 7 + 1));
        }
    }

    private Rectangle calculateCell() {
        final int cellWidth = getWidth() / (board.getSize() * 7 - 1);
        final int cellHeight = getHeight() / (board.getSize() * 7 - 1);
        final int cellSize =  Math.min(cellWidth, cellHeight);
        final int xOffset = (getWidth() - cellSize * (board.getSize() * 7 - 1)) / 2;
        final int yOffset = (getHeight() - cellSize * (board.getSize() * 7 - 1)) / 2;
        return new Rectangle(xOffset, yOffset, cellSize, cellSize);
    }
}
