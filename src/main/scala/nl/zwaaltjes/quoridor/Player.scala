package nl.zwaaltjes.quoridor

final case class Player(name: String, private var playerLocation: Location) {
  def location: Location =
    playerLocation

  def changeLocation(newLocation: Location): Unit =
    playerLocation = newLocation

  override def toString: String =
    name
}