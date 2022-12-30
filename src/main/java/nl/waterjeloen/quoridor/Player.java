package nl.waterjeloen.quoridor;

import java.util.function.Function;

public class Player {
    private final String name;
    private Location location;
    private int walls;
    private final Function<Location, Boolean> wins;

    public Player(String name, Location location, Function<Location, Boolean> wins) {
        this.name = name;
        this.location = location;
        this.walls = 10;
        this.wins = wins;
    }

    public String getName() {
        return name;
    }

    public Location getLocation() {
        return location;
    }

    public void changeLocation(Location location) {
        this.location = location;
    }

    public void removeWall() {
        --walls;
    }

    public boolean hasWalls() {
        return walls > 0;
    }

    public int getWalls() {
        return walls;
    }

    public String toString() {
        return name;
    }

    public boolean hasWon() { return wins.apply(location); }
}
