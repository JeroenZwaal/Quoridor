package nl.zwaaltjes.quoridor.api

trait Quoridor {
  def createGame(players: Seq[String]): Game
}
