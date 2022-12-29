package nl.zwaaltjes.quoridor

final case class Player(name: String, private var playerLocation: Location, private var playerWalls: Int = 10) {
  def location: Location =
    playerLocation

  def changeLocation(newLocation: Location): Unit =
    playerLocation = newLocation

  def walls: Int =
    playerWalls

  def hasWalls: Boolean =
    playerWalls != 0

  def useWall(): Unit =
    playerWalls -= 1

  override def toString: String =
    name
}