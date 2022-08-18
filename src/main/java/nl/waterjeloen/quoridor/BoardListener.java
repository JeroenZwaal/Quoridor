package nl.waterjeloen.quoridor;

public interface BoardListener {
    void fieldClicked(int row, int column);
    void centerClicked(int row, int column);
    void horizontalWallClicked(int row, int column);
    void verticalWallClicked(int row, int column);
}
