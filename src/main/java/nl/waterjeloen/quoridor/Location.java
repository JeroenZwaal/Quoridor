package nl.waterjeloen.quoridor;

import java.util.Objects;

public class Location {
    public final int row;
    public final int column;

    public Location(int row, int column) {
        this.row = row;
        this.column = column;
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) return true;
        if (that == null || getClass() != that.getClass()) return false;
        final Location location = (Location) that;
        return row == location.row && column == location.column;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, column);
    }
}
