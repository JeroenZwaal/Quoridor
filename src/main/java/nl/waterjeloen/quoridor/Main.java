package nl.waterjeloen.quoridor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    private static final int SIZE = 9;

    public static void main(String[] args) {
        final Player me = new Player("me", new Location(0, SIZE / 2));
        final Player you = new Player("you", new Location(SIZE - 1, SIZE / 2));
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
                    return;
                }

                final Player player = board.getCurrentPlayer();
                if (fields.isEmpty() && location.equals(player.getLocation())) {

                    // move down?
                    if (!board.hasHorizontalWall(location) && !board.hasHorizontalWall(location.left())) {
                        if (board.hasPlayer(location.down())) {
                            if (!board.hasHorizontalWall(location.down()) && !board.hasHorizontalWall(location.down().left())) {
                                fields.add(location.down(2));
                            }
                        } else {
                            fields.add(location.down());
                        }
                    }

                    // move up?
                    if (!board.hasHorizontalWall(location.up()) && !board.hasHorizontalWall(location.up().left())) {
                        if (board.hasPlayer(location.up())) {
                            if (!board.hasHorizontalWall(location.up(2)) && !board.hasHorizontalWall(location.up(2).left())) {
                                fields.add(location.up(2));
                            }
                        } else {
                            fields.add(location.up());
                        }
                    }

                    // move right?
                    if (!board.hasVerticalWall(location) && !board.hasVerticalWall(location.up())) {
                        if (board.hasPlayer(location.right())) {
                            if (!board.hasVerticalWall(location.right(2)) && !board.hasVerticalWall(location.up().right())) {
                                fields.add(location.right(2));
                            }
                        } else {
                            fields.add(location.right());
                        }
                    }

                    // move left?
                    if (!board.hasVerticalWall(location.left()) && !board.hasVerticalWall(location.up().left())) {
                        if (board.hasPlayer(location.left())){
                            if (!board.hasVerticalWall(location.left(2)) && !board.hasVerticalWall(location.up().left(2))) {
                                fields.add(location.left(2));
                            }
                        } else {
                            fields.add(location.left());
                        }
                    }

                    for (Location l : fields) {
                        gui.getPanel().highlightField(l);
                    }
                }
            }

            @Override
            public void centerClicked(Location location) {
                reset();

                if (!board.hasHorizontalWall(location) && !board.hasVerticalWall(location)) {
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

                if (!board.hasHorizontalWall(location.left()) && !board.hasHorizontalWall(location)) {
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

                if (!board.hasVerticalWall(location.up()) && !board.hasVerticalWall(location)) {
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
