package nl.waterjeloen.quoridor;

public class Board {
    private final int size;
    private final Player player1;
    private final Player player2;
    private final boolean horizontalWalls[][];
    private final boolean verticalWalls[][];

    public Board(int size, Player player1, Player player2) {
        this.size = size;
        this.player1 = player1;
        this.player2 = player2;
        this.horizontalWalls = new boolean[size - 1][size - 1];
        this.verticalWalls = new boolean[size - 1][size - 1];
    }

    public int getSize() {
        return size;
    }

    public Player getPlayer1() {
        return player1;
    }

    public Player getPlayer2() {
        return player2;
    }

    public boolean hasHorizontalWall(int row, int column) {
        return horizontalWalls[row][column];
    }

    public void addHorizontalWall(int row, int column) {
        horizontalWalls[row][column] = true;
    }

    public boolean hasVerticalWall(int row, int column) {
        return verticalWalls[row][column];
    }

    public void addVerticalWall(int row, int column) {
        verticalWalls[row][column] = true;
    }
}
