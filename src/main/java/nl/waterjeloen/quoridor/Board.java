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

    public void movePlayer(int row, int column) {
        getCurrentPlayer().changePosition(row, column);
        nextPlayer();
    }

    public boolean hasHorizontalWall(int row, int column) {
        return isValidWall(row, column) && horizontalWalls[row][column];
    }

    public void addHorizontalWall(int row, int column) {
        if (isValidWall(row, column)) {
            horizontalWalls[row][column] = true;
            nextPlayer();
        }
    }

    public boolean hasVerticalWall(int row, int column) {
        return isValidWall(row, column) && verticalWalls[row][column];
    }

    public void addVerticalWall(int row, int column) {
        if (isValidWall(row, column)) {
            verticalWalls[row][column] = true;
            nextPlayer();
        }
    }

    private boolean isValidWall(int row, int column) {
        return (row >= 0 && row < size - 1 && column >= 0 && column < size - 1);
    }

    public boolean hasPlayer(int row, int column) {
        return players.stream().anyMatch(p -> p.getRow() == row && p.getColumn() == column);
    }

    private void nextPlayer() {
        currentPlayer = (currentPlayer + 1) % players.size();
    }
}
