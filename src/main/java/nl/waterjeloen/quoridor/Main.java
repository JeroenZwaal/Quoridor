package nl.waterjeloen.quoridor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    private static final int SIZE = 9;

    public static void main(String[] args) {
        final Player me = new Player("me", new Location(0, SIZE / 2), location -> location.row == SIZE - 1);
        final Player you = new Player("you", new Location(SIZE - 1, SIZE / 2), location -> location.row == 0);
        final Board board = new Board(SIZE, me, you);
        final GUI gui = new GUI(board);
        gui.setVisible(true);
        gui.getPanel().addListener(new BoardListener() {
            private final List<Location> fields = new ArrayList<>();
            private final Map<Location, Location> hwalls = new HashMap<>();
            private final Map<Location, Location> vwalls = new HashMap<>();

            @Override
            public void fieldClicked(Location location) {
                final boolean highlighted = fields.contains(location);
                reset();
                if (highlighted) {
                    gui.getPanel().clearHighlights();
                    board.movePlayer(location);
                    if (board.getCurrentPlayer().hasWon()) {
                        gui.showMessage("je hebt het spel gewonnen");
                        System.exit(0);
                    }
                    return;
                }

                final Player player = board.getCurrentPlayer();
                if (fields.isEmpty() && location.equals(player.getLocation())) {
                    checkMove(location, Direction.DOWN, Direction.LEFT, Direction.RIGHT);
                    checkMove(location, Direction.UP, Direction.LEFT, Direction.RIGHT);
                    checkMove(location, Direction.LEFT, Direction.UP, Direction.DOWN);
                    checkMove(location, Direction.RIGHT, Direction.UP, Direction.DOWN);

                    for (Location l : fields) {
                        gui.getPanel().highlightField(l);
                    }
                }
                }


            private void checkMove(Location location, Direction direction, Direction... andThen) {
                if (!board.hasWall(location, direction)) {
                    final Location next = location.go(direction);
                    if (!board.hasPlayer(next)) {
                        fields.add(next);
                    } else if (!board.hasWall(next, direction)) {
                        fields.add(next.go(direction));
                    } else {
                        for (Direction then : andThen) {
                            if (!board.hasWall(next, then)) {
                                fields.add(next.go(then));
                            }
                        }
                    }
                }
            }

            @Override
            public void centerClicked(Location location) {
                reset();

                if (!board.hasHorizontalWall(location) && !board.hasVerticalWall(location) && (board.getCurrentPlayer().hasWalls())) {
                    if (!board.hasHorizontalWall(location.left()) && !board.hasHorizontalWall(location.right())) {
                        gui.getPanel().highlightHorizontalWall(location.right());
                        gui.getPanel().highlightHorizontalWall(location);

                        hwalls.put(location, location);
                        hwalls.put(location.right(), location);
                    }
                    if (!board.hasVerticalWall(location.down()) && !board.hasVerticalWall(location.up()))  {
                        gui.getPanel().highlightVerticalWall(location.down());
                        gui.getPanel().highlightVerticalWall(location);

                        vwalls.put(location, location);
                        vwalls.put(location.down(), location);
                    }

                    gui.getPanel().highlightCenter(location);
                }
            }

            @Override
            public void horizontalWallClicked(Location location) {
                final Location wallLocation = hwalls.get(location);
                reset();
                if (wallLocation != null) {
                    board.addHorizontalWall(wallLocation);
                    return;
                }

                if (!board.hasHorizontalWall(location.left()) && !board.hasHorizontalWall(location) && (board.getCurrentPlayer().hasWalls())) {
                    if (!board.hasVerticalWall(location) && !board.hasHorizontalWall(location.right())) {
                        gui.getPanel().highlightCenter(location);
                        gui.getPanel().highlightHorizontalWall(location.right());

                        hwalls.put(location.right(), location);
                    }
                    if (!board.hasVerticalWall(location.left()) && !board.hasHorizontalWall(location.left(2))) {
                        gui.getPanel().highlightCenter(location.left());
                        gui.getPanel().highlightHorizontalWall(location.left());

                        hwalls.put(location.left(), location.left());
                    }
                    gui.getPanel().highlightHorizontalWall(location);
                }
            }

            @Override
            public void verticalWallClicked(Location location) {
                final Location wallLocation = vwalls.get(location);
                reset();
                if (wallLocation != null) {
                    board.addVerticalWall(wallLocation);
                    return;
                }

                if (!board.hasVerticalWall(location.up()) && !board.hasVerticalWall(location) && (board.getCurrentPlayer().hasWalls())) {
                    if (!board.hasVerticalWall(location.down()) && !board.hasHorizontalWall(location)) {
                        gui.getPanel().highlightVerticalWall(location.down());
                        gui.getPanel().highlightCenter(location);

                        vwalls.put(location.down(), location);
                    }
                    if (!board.hasVerticalWall(location.up(2)) && !board.hasHorizontalWall(location.up())) {
                        gui.getPanel().highlightCenter(location.up());
                        gui.getPanel().highlightVerticalWall(location.up());

                        vwalls.put(location.up(), location.up());
                    }
                    gui.getPanel().highlightVerticalWall(location);
                }
            }

            private void reset() {
                gui.getPanel().clearHighlights();
                fields.clear();
                hwalls.clear();
                vwalls.clear();
            }
        });

        gui.repaint();
    }
}
