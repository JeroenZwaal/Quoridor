package nl.waterjeloen.quoridor;

public class Player {
    private final String name;
    private Location location;
    private int walls;

    public Player(String name, Location location) {
        this.name = name;
        this.location = location;
        this.walls = 10;
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
}
