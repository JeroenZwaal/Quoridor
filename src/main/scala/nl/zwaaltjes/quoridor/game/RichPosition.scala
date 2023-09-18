package nl.zwaaltjes.quoridor.game

import nl.zwaaltjes.quoridor.api.Position

object RichPosition {
  extension (position: Position) {
    def left: Position = position.copy(column = position.column - 1)
    def right: Position = position.copy(column = position.column + 1)
    def up: Position = position.copy(row = position.row + 1)
    def down: Position = position.copy(row = position.row - 1)
  }
}
