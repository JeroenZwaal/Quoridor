package nl.waterjeloen.quoridor;

import java.util.Objects;

public class Location {
    public final int row;
    public final int column;

    public Location(int row, int column) {
        this.row = row;
        this.column = column;
    }

    public Location go(Direction direction) {
        final int newRow = row + direction.rowDelta;
        final int newColumn = column + direction.columnDelta;
        return new Location(newRow, newColumn);
    }

    public Location wall(Direction side) {
        final int newRow = row + (side.rowDelta - 1) / 2;
        final int newColumn = column + (side.columnDelta - 1) / 2;
        return new Location(newRow, newColumn);
    }

    public Location left() {
        return left(1);
    }

    public Location left(int amount) {
        return new Location(row, column - amount);
    }

    public Location right() {
        return right(1);
    }

    public Location right(int amount) {
        return new Location(row, column + amount);
    }

    public Location up() {
        return up(1);
    }

    public Location up(int amount) {
        return new Location(row - amount, column);
    }

    public Location down() {
        return down(1);
    }

    public Location down(int amount) {
        return new Location(row + amount, column);
    }

    public boolean isValid(int size) {
        return isValid(size, size);
    }

    public boolean isValid(int rowCount, int columnCount) {
        return isRowValid(rowCount) && isColumnValid(columnCount);
    }

    public boolean isRowValid(int rowCount) {
        return (row >= 0) && (row < rowCount);
    }

    public boolean isColumnValid(int columnCount) {
        return (column >= 0) && (column < columnCount);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        final Location location = (Location) object;
        return row == location.row && column == location.column;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, column);
    }

    @Override
    public String toString() {
        return "(" + row + "," + column + ")";
    }
}
