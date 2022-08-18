package nl.waterjeloen.quoridor;

public class Main {
    public static void main(String[] args) {
        final Player me = new Player("me", 0, 4);
        final Player you = new Player("you", 8, 4);
        final Board board = new Board(9, me, you);
        final GUI gui = new GUI(board);
        gui.setVisible(true);
        gui.getPanel().addListener(new BoardListener() {
            @Override
            public void fieldClicked(int row, int column) {
                System.out.println("Field clicked: " + row + ", " + column);
                gui.getPanel().clearHighlights();
                gui.getPanel().highlightField(row, column);
            }

            @Override
            public void centerClicked(int row, int column) {
                System.out.println("Center clicked: " + row + ", " + column);
                gui.getPanel().clearHighlights();
                gui.getPanel().highlightCenter(row, column);
            }

            @Override
            public void horizontalWallClicked(int row, int column) {
                System.out.println("Horizontal wall clicked: " + row + ", " + column);
                gui.getPanel().clearHighlights();
                gui.getPanel().highlightHorizontalWall(row, column);
            }

            @Override
            public void verticalWallClicked(int row, int column) {
                System.out.println("Vertical wall clicked: " + row + ", " + column);
                gui.getPanel().clearHighlights();
                gui.getPanel().highlightVerticalWall(row, column);
            }
        });

        board.addHorizontalWall(0, 0);
        board.addVerticalWall(2, 2);
        gui.repaint();
    }
}
