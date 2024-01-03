package nl.zwaaltjes.quoridor.api

trait Game extends ReadOnlyGame {
  def play(move: Move): Either[String, Boolean]
}
