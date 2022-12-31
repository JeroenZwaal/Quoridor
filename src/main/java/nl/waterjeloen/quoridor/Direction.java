package nl.waterjeloen.quoridor;

public enum Direction {
    DOWN("down", 1, 0, false, true),
    UP("up", -1, 0, false, true),
    LEFT("left", 0, -1, true, false),
    RIGHT("right", 0, 1, true, false);

    final String name;
    final int rowDelta;
    final int columnDelta;
    final boolean isHorizontal;
    final boolean isVertical;

    Direction(String name, int rowChange, int columnChange, boolean isHorizontal, boolean isVertical) {
        this.name = name;
        this.rowDelta = rowChange;
        this.columnDelta = columnChange;
        this.isHorizontal = isHorizontal;
        this.isVertical = isVertical;
    }

    @Override
    public String toString() {
        return name;
    }
}
