package nl.waterjeloen.quoridor;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        final Player me = new Player("me", 0, 4);
        final Player you = new Player("you", 8, 4);
        final Board board = new Board(9, me, you);
        final GUI gui = new GUI(board);
        gui.setVisible(true);
        gui.getPanel().addListener(new BoardListener() {
            private final List<Point> fields = new ArrayList<>();
            private final Map<Point, Point> hwalls = new HashMap<>();
            private final Map<Point, Point> vwalls = new HashMap<>();

            @Override
            public void fieldClicked(int row, int column) {
                final Player player = board.getCurrentPlayer();

                if (fields.isEmpty() && row == player.getRow() && column == player.getColumn()) {
                    gui.getPanel().clearHighlights();

                    // move down?
                    if (!board.hasHorizontalWall(row, column) && !board.hasHorizontalWall(row, column - 1)) {
                        if (board.hasPlayer(row + 1, column)) {
                            if (!board.hasHorizontalWall(row + 1, column) && !board.hasHorizontalWall(row + 1, column - 1)) {
                                fields.add(new Point(column, row + 2));
                            }
                        } else {
                            fields.add(new Point(column, row + 1));
                        }
                    }

                    // move up?
                    if (!board.hasHorizontalWall(row - 1, column) && !board.hasHorizontalWall(row - 1, column - 1)) {
                        if (board.hasPlayer(row - 1, column)) {
                            if (!board.hasHorizontalWall(row - 2, column) && !board.hasHorizontalWall(row - 2, column - 1)) {
                                fields.add(new Point(column, row - 2));
                            }
                        } else {
                            fields.add(new Point(column, row - 1));
                        }
                    }

                    // move right?
                    if (!board.hasVerticalWall(row, column ) && !board.hasVerticalWall(row - 1, column)) {
                        if (board.hasPlayer(row , column + 1)) {
                            if (!board.hasVerticalWall(row, column + 1 ) && !board.hasVerticalWall(row - 1, column + 1)) {
                                fields.add(new Point(column + 2, row));
                            }
                        } else {
                            fields.add(new Point(column + 1, row));
                        }
                    }

                    // move left?
                    if (!board.hasVerticalWall(row, column -1 ) && !board.hasVerticalWall(row - 1, column - 1)) {
                        if (board.hasPlayer(row, column - 1)){
                            if (!board.hasVerticalWall(row, column - 2) && !board.hasVerticalWall(row - 1, column - 2)) {
                                fields.add(new Point(column - 2, row));
                            }
                        } else {
                            fields.add(new Point(column - 1, row));
                        }
                    }

                    for (Point f : fields) {
                        gui.getPanel().highlightField(f.y, f.x);
                    }
                } else if (!fields.isEmpty()) {
                    if (fields.contains(new Point(column, row))) {
                        gui.getPanel().clearHighlights();
                        board.movePlayer(row, column);
                        fields.clear();
                    }
                }
            }


            @Override
            public void centerClicked(int row, int column) {
                System.out.println("Center clicked: " + row + ", " + column);
                gui.getPanel().clearHighlights();

                if (!board.hasHorizontalWall(row, column) && !board.hasVerticalWall(row, column)) {
                    if (!board.hasHorizontalWall(row, column - 1) && !board.hasHorizontalWall(row, column + 1)) {
                        gui.getPanel().highlightHorizontalWall(row, column + 1);
                        gui.getPanel().highlightHorizontalWall(row, column);

                        hwalls.put(new Point(row, column), new Point(row, column));
                        hwalls.put(new Point(row, column + 1), new Point(row, column));
                    }
                    if (!board.hasVerticalWall(row + 1, column) && !board.hasVerticalWall(row - 1, column))  {
                        gui.getPanel().highlightVerticalWall(row + 1, column);
                        gui.getPanel().highlightVerticalWall(row , column);

                        vwalls.put(new Point(row, column), new Point(row, column));
                        vwalls.put(new Point(row + 1, column), new Point(row, column));
                    }
                    gui.getPanel().highlightCenter(row, column);
                    fields.clear();
                }
            }

            @Override
            public void horizontalWallClicked(int row, int column) {
                System.out.println("Horizontal wall clicked: " + row + ", " + column);
                gui.getPanel().clearHighlights();

                final Point location = hwalls.get(new Point(row, column));
                if (location != null) {
                    board.addHorizontalWall(location.x, location.y);
                    hwalls.clear();
                    return;
                }

                if (!board.hasHorizontalWall(row, column - 1) && !board.hasHorizontalWall(row, column)) {
                    if (!board.hasVerticalWall(row, column) && !board.hasHorizontalWall(row, column + 1)) {
                        gui.getPanel().highlightCenter(row, column);
                        gui.getPanel().highlightHorizontalWall(row, column + 1);

                        hwalls.put(new Point(row, column + 1), new Point(row, column));
                    }
                    if (!board.hasVerticalWall(row, column - 1) && !board.hasHorizontalWall(row, column - 2)) {
                        gui.getPanel().highlightCenter(row, column - 1);
                        gui.getPanel().highlightHorizontalWall(row, column - 1);

                        hwalls.put(new Point(row, column - 1), new Point(row, column - 1));
                    }
                    gui.getPanel().highlightHorizontalWall(row, column);
                    fields.clear();
                }


            }

            @Override
            public void verticalWallClicked(int row, int column) {
                System.out.println("Vertical wall clicked: " + row + ", " + column);
                gui.getPanel().clearHighlights();

                final Point location = vwalls.get(new Point(row, column));
                if (location != null) {
                    board.addVerticalWall(location.x, location.y);
                    vwalls.clear();
                    return;
                }

                if (!board.hasVerticalWall(row - 1, column) && !board.hasVerticalWall(row, column)) {
                    if (!board.hasVerticalWall(row + 1, column) && !board.hasHorizontalWall(row, column)) {
                        gui.getPanel().highlightVerticalWall(row + 1, column);
                        gui.getPanel().highlightCenter(row, column);

                        vwalls.put(new Point(row + 1, column), new Point(row, column));
                    }
                    if (!board.hasVerticalWall(row - 2, column) && !board.hasHorizontalWall(row - 1, column)) {
                        gui.getPanel().highlightCenter(row - 1, column);
                        gui.getPanel().highlightVerticalWall(row - 1, column);

                        vwalls.put(new Point(row - 1, column), new Point(row - 1, column));
                    }
                    gui.getPanel().highlightVerticalWall(row, column);
                    fields.clear();
                }
            }
        });

        gui.repaint();
    }
}
