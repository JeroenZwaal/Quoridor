package nl.zwaaltjes.quoridor.server

import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import spray.json.DefaultJsonProtocol.*

object UserServer {
  sealed trait Command
  final case class Authenticate(userId: UserId, password: Password, replyTo: ActorRef[OK | Error]) extends Command
  final case class Validate(sessionId: SessionId, replyTo: ActorRef[UserData | Error]) extends Command
  final case class GetUser(userId: UserId, replyTo: ActorRef[UserData | Error]) extends Command
  final case class UpdateUser(
      userId: UserId,
      email: Email,
      name: String,
      password: Password,
      authenticatedUserId: Option[UserId],
      replyTo: ActorRef[OK | Error],
  ) extends Command
  final case class Invite(userId: UserId, gameId: GameId, message: String) extends Command
  final case class ListGames(userId: UserId, replyTo: ActorRef[Games]) extends Command
  final case class AddGame(userId: UserId, gameId: GameId) extends Command
  final case class DeleteGame(userId: UserId, gameId: GameId) extends Command
  final case class GetDump(replyTo: ActorRef[Dump]) extends Command

  sealed trait Response
  final case class UserData(userId: UserId, name: String, email: Email) extends Response
  final case class OK(sessionId: Option[SessionId]) extends Response
  final case class Games(gameIds: Seq[GameId])
  final case class Dump(data: String) extends Response

  sealed trait Error extends Response
  final case class Unauthorized(message: String) extends Error
  final case class UserExists(message: String) extends Error
  final case class UnknownUser(message: String) extends Error
  final case class WrongUser(message: String) extends Error

  def apply(): Behaviors.Receive[Command] =
    apply(Map.empty, Map.empty)

  private case class User(id: UserId, email: Email, name: String, password: Password, sessionId: SessionId, gameIds: List[GameId]) {
    def withGame(gameId: GameId): User = copy(gameIds = gameId :: gameIds)
    def withoutGame(gameId: GameId): User = copy(gameIds = gameIds.filter(_ != gameId))
    override def toString: String =
      s"$name ($email / $password); $sessionId"
  }

  private def apply(users: Map[UserId, User], sessions: Map[SessionId, User]): Behaviors.Receive[Command] = Behaviors.receive {
    // create session from credentials
    case (_, Authenticate(userId, password, replyTo)) =>
      users.get(userId) match {
        case Some(user) if user.password == password =>
          val sessionId = SessionId.create()
          replyTo ! OK(Some(sessionId))
          val updatedUser = user.copy(sessionId = sessionId)
          val updatedSessions = sessions - user.sessionId
          apply(users = users + (userId -> updatedUser), sessions = sessions + (sessionId -> updatedUser) - user.sessionId)
        case _ =>
          replyTo ! Unauthorized("Email/password combination is invalid")
          Behaviors.same
      }

    // get user details for session
    case (_, Validate(sessionId, replyTo)) =>
      sessions.get(sessionId) match {
        case Some(user) =>
          replyTo ! UserData(user.id, user.name, user.email)
        case _ =>
          replyTo ! Unauthorized("Session is invalid")
      }
      Behaviors.same

    // get user details
    case (_, GetUser(userId, replyTo)) =>
      users.get(userId) match {
        case Some(user) => replyTo ! UserData(userId, user.name, user.email)
        case None => replyTo ! UnknownUser(s"User does not exist for $userId")
      }
      Behaviors.same

    // create new user
    case (_, UpdateUser(userId, email, name, password, None, replyTo)) if !users.contains(userId) =>
      val sessionId = SessionId.create()
      replyTo ! OK(Some(sessionId))
      val user = User(userId, email, name, password, sessionId, Nil)
      apply(users = users + (userId -> user), sessions = sessions + (sessionId -> user))

    // (re)create existing user
    case (_, UpdateUser(userId, _, _, _, None, replyTo)) =>
      replyTo ! UserExists(s"User already exists for $userId")
      Behaviors.same

    // update existing user by owner
    case (_, UpdateUser(userId, email, name, password, authenticatedUserId, replyTo)) if authenticatedUserId == userId =>
      users.get(userId) match {
        case Some(user) =>
          replyTo ! OK(None)
          val updatedUser = user.copy(email = email, name = name, password = password)
          apply(users = users + (userId -> updatedUser), sessions = sessions + (user.sessionId -> updatedUser))
        case None =>
          replyTo ! UnknownUser(s"User does not exist for $userId")
          Behaviors.same
      }

    // update other user
    case (_, UpdateUser(userId, _, _, _, _, replyTo)) =>
      replyTo ! WrongUser(s"Cannot change user for $userId")
      Behaviors.same

    // invite user to game
    case (ctx, Invite(userId, gameId, message)) =>
      users.get(userId) match {
        case Some(user) =>
          ctx.log.info(s"Sending invitation to $userId (${user.email}) with message '$message'.")
          // TODO: send email to user.email
          val updatedUser = user.withGame(gameId)
          apply(users = users + (userId -> updatedUser), sessions)
        case None =>
          ctx.log.warn(s"Not sending invitation to non-existing user $userId")
          Behaviors.same
      }

    // list all games for user
    case (_, ListGames(userId, replyTo)) =>
      users.get(userId) match {
        case Some(user) =>
          replyTo ! Games(user.gameIds)
          Behaviors.same
        case None =>
          replyTo ! Games(Nil)
          Behaviors.same
      }

    // add game for user
    case (_, AddGame(userId, gameId)) =>
      users.get(userId) match {
        case Some(user) =>
          val updatedUser = user.withGame(gameId)
          apply(users = users + (userId -> updatedUser), sessions)
        case None =>
          Behaviors.same
      }

    // delete game from user
    case (_, DeleteGame(userId, gameId)) =>
      users.get(userId) match {
        case Some(user) =>
          val updatedUser = user.withoutGame(gameId)
          apply(users = users + (userId -> updatedUser), sessions)
        case None =>
          Behaviors.same
      }

    // dump all user details
    case (_, GetDump(replyTo)) =>
      def list(heading: String, list: Iterable[String]): String = list.mkString(s"$heading:\n- ", "\n- ", "\n")

      val sessionData = list("Sessions", sessions.map { case (sessionId, user) => s"$sessionId: $user" })
      val userData = list("Users", users.map { case (userId, user) => s"$userId: $user${user.gameIds.mkString(" [", ", ", "]")}" })
      replyTo ! Dump(userData + sessionData)
      Behaviors.same
  }
}
