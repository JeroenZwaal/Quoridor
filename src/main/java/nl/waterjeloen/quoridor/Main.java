package nl.waterjeloen.quoridor;

public class Main {
    public static void main(String[] args) {
        final Player me = new Player("me", 0, 4);
        final Player you = new Player("you", 8, 4);
        final Board board = new Board(9, me, you);
        final GUI gui = new GUI(board);
        gui.setVisible(true);
        gui.getPanel().addListener(new BoardListener() {
            private boolean moving = false;

            @Override
            public void fieldClicked(int row, int column) {
                System.out.println("Field clicked: " + row + ", " + column);
                final Player player = board.getCurrentPlayer();
                if (!moving && row == player.getRow() && column == player.getColumn()) {
                    gui.getPanel().clearHighlights();
                    if (!board.hasHorizontalWall(row, column) && !board.hasHorizontalWall(row, column - 1)) {
                        gui.getPanel().highlightField(row + 1, column);
                    }
                    if (!board.hasHorizontalWall(row - 1, column) && !board.hasHorizontalWall(row - 1, column - 1)) {
                        gui.getPanel().highlightField(row - 1, column);
                    }
                    if (!board.hasVerticalWall(row, column ) && !board.hasVerticalWall(row - 1, column)) {
                        gui.getPanel().highlightField(row, column + 1);
                    }
                    if (!board.hasVerticalWall(row, column -1 ) && !board.hasVerticalWall(row - 1, column - 1)) {
                        gui.getPanel().highlightField(row, column - 1);
                    }
                    moving = true;
                } else if (moving) {
                    if ((Math.abs(row - player.getRow()) == 1 && column == player.getColumn())
                        || (Math.abs(column - player.getColumn()) == 1 && row == player.getRow())) {
                        gui.getPanel().clearHighlights();
                        board.movePlayer(row, column);
                        moving = false;
                    }
                }
            }


            @Override
            public void centerClicked(int row, int column) {
                System.out.println("Center clicked: " + row + ", " + column);
                gui.getPanel().clearHighlights();
                gui.getPanel().highlightCenter(row, column);
                moving = false;
            }

            @Override
            public void horizontalWallClicked(int row, int column) {
                System.out.println("Horizontal wall clicked: " + row + ", " + column);
                gui.getPanel().clearHighlights();
                gui.getPanel().highlightHorizontalWall(row, column);
                moving = false;
            }

            @Override
            public void verticalWallClicked(int row, int column) {
                System.out.println("Vertical wall clicked: " + row + ", " + column);
                gui.getPanel().clearHighlights();
                gui.getPanel().highlightVerticalWall(row, column);
                moving = false;
            }
        });

        board.addHorizontalWall(2, 2);
        board.addVerticalWall(4, 5);
        gui.repaint();
    }
}
