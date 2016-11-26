package shortenurl.domain.repository

import shortenurl.domain.model.{Error, ErrorCode, User}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait UserRepositoryComponent { this: UserTable =>

  val userRepository: UserRepository

  trait UserRepository {
    def findById(id: Long): Future[Option[User]]
    def userForToken(token: String): Future[Option[User]]
    def getUser(id: Long)(implicit ec: ExecutionContext): Future[Error Either User]
  }

  class UserRepositoryImpl extends UserRepository with SQLStateErrorCodeTranslator{

    import profile.api._
    import DBIO._
    import slick.jdbc.TransactionIsolation._
    import users._

    val random = new scala.util.Random(new java.security.SecureRandom())

    private def randomString(alphabet: String)(n: Int): String =
      Stream.continually(random.nextInt(alphabet.length)).map(alphabet).take(n).mkString

    private def randomAlphanumericString(n: Int): String =
      randomString("abcdefghijklmnopqrstuvwxyz0123456789")(n)

    private def generateToken: String = randomAlphanumericString(64)

    private def findByIdInternal(id: Long): DBIO[Option[User]] = filter(_.id === id).result.headOption

    override def findById(id: Long): Future[Option[User]] =
      db run findByIdInternal(id).transactionally.withTransactionIsolation(ReadCommitted)

    override def userForToken(token: String): Future[Option[User]] =
      db run filter(_.token === token).result.headOption.transactionally.withTransactionIsolation(ReadCommitted)

    override def getUser(id: Long)(implicit ec: ExecutionContext): Future[Error Either User] = tryGetUser(id) flatMap {
      _.fold({
        case Error(ErrorCode.Duplicate) => db.run(filter(_.id === id).result.head.map(Right(_)).transactionally.withTransactionIsolation(ReadUncommitted))
        case _ => Future.successful(Left(Error(ErrorCode.Unknown)))
      }, existingUser => Future.successful(Right(existingUser)))
    }

    private def tryGetUser(id: Long)(implicit ec: ExecutionContext): Future[Error Either User] = db run {
      findByIdInternal(id).flatMap(_.fold {
        val token = generateToken
        val user = User(id, token)
        (users += user).asTry map {
          case Success(_) => Right(user)
          case Failure(t) => Left(translateException(t))
        }
      }(existingUser => successful(Right(existingUser)))
      ).transactionally.withTransactionIsolation(Serializable)
    }
  }
}
