package nl.waterjeloen.quoridor;

public class Main {
    public static void main(String[] args) {
        final Player me = new Player("me", 0, 4);
        final Player you = new Player("you", 8, 4);
        final Board board = new Board(9, me, you);
        final GUI gui = new GUI(board);
        gui.setVisible(true);

        board.addHorizontalWall(0, 0);
        board.addVerticalWall(2, 2);
        gui.repaint();
    }
}
