package nl.waterjeloen.quoridor;

public class Player {
    private final String name;
    private int row;
    private int column;

    public Player(String name, int row, int column) {
        this.name = name;
        this.row = row;
        this.column = column;
    }

    public String getName() {
        return name;
    }

    public int getRow() {
        return row;
    }

    public int getColumn() {
        return column;
    }

    public void changePosition(int row, int column) {
        this.row = row;
        this.column = column;
    }

    public String toString() {
        return name;
    }
}
