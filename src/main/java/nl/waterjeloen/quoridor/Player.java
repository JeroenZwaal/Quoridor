package nl.waterjeloen.quoridor;

public class Player {
    private final String name;
    private Location location;

    public Player(String name, Location location) {
        this.name = name;
        this.location = location;
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

    public String toString() {
        return name;
    }
}
