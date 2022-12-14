package nl.waterjeloen.quoridor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Board {
    private final int size;
    private final List<Player> players;
    private int currentPlayer;
    private final boolean[][] horizontalWalls;
    private final boolean[][] verticalWalls;

    private boolean finished;

    public Board(int size, Player player1, Player player2) {
        this.size = size;
        this.players = List.of(player1, player2);
        this.currentPlayer = 0;
        this.horizontalWalls = new boolean[size - 1][size - 1];
        this.verticalWalls = new boolean[size - 1][size - 1];
        this.finished = false;
    }

    public int getSize() {
        return size;
    }

    public int getPlayerCount() {
        return players.size();
    }

    public Player getPlayer(int index) {
        return players.get(index);
    }

    public Player getCurrentPlayer() {
        return getPlayer(currentPlayer);
    }

    public boolean hasPlayer(Location location) {
        return players.stream().anyMatch(p -> p.getLocation().equals(location));
    }

    public void movePlayer(Location location) {
        if (!finished) {
            getCurrentPlayer().changeLocation(location);
            if (!getCurrentPlayer().hasWon()) {
                nextPlayer();
            }
            else {
                endGame();
            }
        }
    }

    private void endGame() {
        finished = true;
    }

    public boolean hasWall(Location location, Direction side) {
        final Location wallLocation = location.wall(side);

        if (side.isHorizontal) {
            final Location secondLocation = wallLocation.go(Direction.UP);
            return (isValidWall(wallLocation) && verticalWalls[wallLocation.row][wallLocation.column]) ||
                (isValidWall(secondLocation) && verticalWalls[secondLocation.row][secondLocation.column]);
        } else {
            final Location secondLocation = wallLocation.go(Direction.LEFT);
            return (isValidWall(wallLocation) && horizontalWalls[wallLocation.row][wallLocation.column]) ||
                (isValidWall(secondLocation) && horizontalWalls[secondLocation.row][secondLocation.column]);
        }
    }

    public void addWall(Location location, Direction side, Direction direction) {
        final Location wallLocation = location.wall(side).wall(direction);
        if (isValidWall(wallLocation)) {
            final boolean[][] walls = (side.isHorizontal) ? verticalWalls : horizontalWalls;
            walls[wallLocation.row][wallLocation.column] = true;
            nextPlayer();
        }
    }

    public boolean hasHorizontalWall(Location location) {
        return isValidWall(location) && horizontalWalls[location.row][location.column];
    }

    public void addHorizontalWall(Location location) {

        if (isValidWall(location) && (!finished)) {
            horizontalWalls[location.row][location.column] = true;
            getCurrentPlayer().removeWall();
            nextPlayer();
        }
    }

    public boolean hasVerticalWall(Location location) {
        return isValidWall(location) && verticalWalls[location.row][location.column];
    }

    public void addVerticalWall(Location location) {
        if (isValidWall(location) && (!finished)) {
            verticalWalls[location.row][location.column] = true;
            getCurrentPlayer().removeWall();
            nextPlayer();
        }
    }

    private boolean isValidWall(Location location) {
        return location.isValid(horizontalWalls.length, verticalWalls.length);
    }

    private void nextPlayer() {
        currentPlayer = (currentPlayer + 1) % players.size();
    }

    public Set<Location> getReachableLocations(Location location) {
        Set<Location> result = new HashSet<>();
        result.add(location);
        for (Direction direction : Direction.values()) {
            extend(result, location, direction);
        }
        return result;
    }

    private void extend(Set<Location> result, Location location, Direction direction) {
        if (!hasWall(location, direction)) {
            Location next = location.go(direction);
            if (next.isValid(size) && (!result.contains(next))) {
                result.add(next);
                for (Direction d : Direction.values()) {
                    extend(result, next, d);
                }
            }
        }
    }

}


