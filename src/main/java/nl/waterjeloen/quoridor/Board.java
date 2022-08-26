package nl.waterjeloen.quoridor;

import java.util.List;

public class Board {
    private final int size;
    private final List<Player> players;
    private int currentPlayer;
    private final boolean[][] horizontalWalls;
    private final boolean[][] verticalWalls;

    public Board(int size, Player player1, Player player2) {
        this.size = size;
        this.players = List.of(player1, player2);
        this.currentPlayer = 0;
        this.horizontalWalls = new boolean[size - 1][size - 1];
        this.verticalWalls = new boolean[size - 1][size - 1];
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

    public void movePlayer(Location location) {
        getCurrentPlayer().changeLocation(location);
        nextPlayer();
    }

    public boolean hasHorizontalWall(Location location) {
        return isValidWall(location) && horizontalWalls[location.row][location.column];
    }

    public void addHorizontalWall(Location location) {
        if (isValidWall(location)) {
            horizontalWalls[location.row][location.column] = true;
            nextPlayer();
        }
    }

    public boolean hasVerticalWall(Location location) {
        return isValidWall(location) && verticalWalls[location.row][location.column];
    }

    public void addVerticalWall(Location location) {
        if (isValidWall(location)) {
            verticalWalls[location.row][location.column] = true;
            nextPlayer();
        }
    }

    private boolean isValidWall(Location location) {
        return location.isValid(horizontalWalls.length, verticalWalls.length);
    }

    public boolean hasPlayer(Location location) {
        return players.stream().anyMatch(p -> p.getLocation().equals(location));
    }

    private void nextPlayer() {
        currentPlayer = (currentPlayer + 1) % players.size();
    }
}
