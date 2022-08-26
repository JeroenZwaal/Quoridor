package nl.waterjeloen.quoridor;

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
            private final List<Location> fields = new ArrayList<>();
            private final Map<Location, Location> hwalls = new HashMap<>();
            private final Map<Location, Location> vwalls = new HashMap<>();

            @Override
            public void fieldClicked(int row, int column) {
                final Player player = board.getCurrentPlayer();

                if (fields.isEmpty() && row == player.getRow() && column == player.getColumn()) {
                    gui.getPanel().clearHighlights();

                    // move down?
                    if (!board.hasHorizontalWall(row, column) && !board.hasHorizontalWall(row, column - 1)) {
                        if (board.hasPlayer(row + 1, column)) {
                            if (!board.hasHorizontalWall(row + 1, column) && !board.hasHorizontalWall(row + 1, column - 1)) {
                                fields.add(new Location(row + 1, column));
                            }
                        } else {
                            fields.add(new Location(row + 1, column));
                        }
                    }

                    // move up?
                    if (!board.hasHorizontalWall(row - 1, column) && !board.hasHorizontalWall(row - 1, column - 1)) {
                        if (board.hasPlayer(row - 1, column)) {
                            if (!board.hasHorizontalWall(row - 2, column) && !board.hasHorizontalWall(row - 2, column - 1)) {
                                fields.add(new Location(row - 2, column));
                            }
                        } else {
                            fields.add(new Location(row - 1, column));
                        }
                    }

                    // move right?
                    if (!board.hasVerticalWall(row, column ) && !board.hasVerticalWall(row - 1, column)) {
                        if (board.hasPlayer(row , column + 1)) {
                            if (!board.hasVerticalWall(row, column + 1 ) && !board.hasVerticalWall(row - 1, column + 1)) {
                                fields.add(new Location(row, column + 2));
                            }
                        } else {
                            fields.add(new Location(row, column + 1));
                        }
                    }

                    // move left?
                    if (!board.hasVerticalWall(row, column -1 ) && !board.hasVerticalWall(row - 1, column - 1)) {
                        if (board.hasPlayer(row, column - 1)){
                            if (!board.hasVerticalWall(row, column - 2) && !board.hasVerticalWall(row - 1, column - 2)) {
                                fields.add(new Location(row, column - 2));
                            }
                        } else {
                            fields.add(new Location(row, column - 1));
                        }
                    }

                    for (Location f : fields) {
                        gui.getPanel().highlightField(f.row, f.column);
                    }
                } else if (!fields.isEmpty()) {
                    if (fields.contains(new Location(row, column))) {
                        gui.getPanel().clearHighlights();
                        board.movePlayer(row, column);
                        fields.clear();
                    }
                }
            }


            @Override
            public void centerClicked(int row, int column) {
                gui.getPanel().clearHighlights();

                if (!board.hasHorizontalWall(row, column) && !board.hasVerticalWall(row, column)) {
                    if (!board.hasHorizontalWall(row, column - 1) && !board.hasHorizontalWall(row, column + 1)) {
                        gui.getPanel().highlightHorizontalWall(row, column + 1);
                        gui.getPanel().highlightHorizontalWall(row, column);

                        hwalls.put(new Location(row, column), new Location(row, column));
                        hwalls.put(new Location(row, column + 1), new Location(row, column));
                    }
                    if (!board.hasVerticalWall(row + 1, column) && !board.hasVerticalWall(row - 1, column))  {
                        gui.getPanel().highlightVerticalWall(row + 1, column);
                        gui.getPanel().highlightVerticalWall(row , column);

                        vwalls.put(new Location(row, column), new Location(row, column));
                        vwalls.put(new Location(row + 1, column), new Location(row, column));
                    }
                    gui.getPanel().highlightCenter(row, column);
                    fields.clear();
                }
            }

            @Override
            public void horizontalWallClicked(int row, int column) {
                gui.getPanel().clearHighlights();

                final Location location = hwalls.get(new Location(row, column));
                if (location != null) {
                    board.addHorizontalWall(location.row, location.column);
                    hwalls.clear();
                    return;
                }

                if (!board.hasHorizontalWall(row, column - 1) && !board.hasHorizontalWall(row, column)) {
                    if (!board.hasVerticalWall(row, column) && !board.hasHorizontalWall(row, column + 1)) {
                        gui.getPanel().highlightCenter(row, column);
                        gui.getPanel().highlightHorizontalWall(row, column + 1);

                        hwalls.put(new Location(row, column + 1), new Location(row, column));
                    }
                    if (!board.hasVerticalWall(row, column - 1) && !board.hasHorizontalWall(row, column - 2)) {
                        gui.getPanel().highlightCenter(row, column - 1);
                        gui.getPanel().highlightHorizontalWall(row, column - 1);

                        hwalls.put(new Location(row, column - 1), new Location(row, column - 1));
                    }
                    gui.getPanel().highlightHorizontalWall(row, column);
                    fields.clear();
                }
            }

            @Override
            public void verticalWallClicked(int row, int column) {
                gui.getPanel().clearHighlights();

                final Location location = vwalls.get(new Location(row, column));
                if (location != null) {
                    board.addVerticalWall(location.row, location.column);
                    vwalls.clear();
                    return;
                }

                if (!board.hasVerticalWall(row - 1, column) && !board.hasVerticalWall(row, column)) {
                    if (!board.hasVerticalWall(row + 1, column) && !board.hasHorizontalWall(row, column)) {
                        gui.getPanel().highlightVerticalWall(row + 1, column);
                        gui.getPanel().highlightCenter(row, column);

                        vwalls.put(new Location(row + 1, column), new Location(row, column));
                    }
                    if (!board.hasVerticalWall(row - 2, column) && !board.hasHorizontalWall(row - 1, column)) {
                        gui.getPanel().highlightCenter(row - 1, column);
                        gui.getPanel().highlightVerticalWall(row - 1, column);

                        vwalls.put(new Location(row - 1, column), new Location(row - 1, column));
                    }
                    gui.getPanel().highlightVerticalWall(row, column);
                    fields.clear();
                }
            }
        });

        gui.repaint();
    }
}
