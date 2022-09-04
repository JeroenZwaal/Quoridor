package nl.waterjeloen.quoridor;

public class Direction {
    public static final Direction DOWN = new Direction("down", 1, 0, false, true);
    public static final Direction UP = new Direction("up", -1, 0, false, true);
    public static final Direction LEFT = new Direction("left", 0, -1, true, false);
    public static final Direction RIGHT = new Direction("right", 0, 1, true, false);

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
