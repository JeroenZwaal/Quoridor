package nl.waterjeloen.quoridor;

public interface BoardListener {
    void fieldClicked(Location location);
    void centerClicked(Location location);
    void horizontalWallClicked(Location location);
    void verticalWallClicked(Location location);
}

