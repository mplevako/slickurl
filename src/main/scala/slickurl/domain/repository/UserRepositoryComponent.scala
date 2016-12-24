package slickurl.domain.repository

import slickurl.domain.model.{AlphabetCodec, Error, UserID}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait UserRepositoryComponent { this: UserTable =>

  protected def userRepository: UserRepository

  trait UserRepository {
    def createNewUser(inShard: Long)(implicit ec: ExecutionContext): Future[Error Either UserID]
  }

  protected class UserRepositoryImpl extends UserRepository with SQLStateErrorCodeTranslator{

    import profile.api._
    import slick.jdbc.TransactionIsolation._

    override def createNewUser(inShard: Long)(implicit ec: ExecutionContext): Future[Error Either UserID] = db run {
      userIdSequence.next.result map AlphabetCodec.packAndEncode(inShard) map UserID flatMap { uid =>
        (users += uid).asTry map {
          case Success(_) => Right(uid)
          case Failure(t) => Left(translateException(t))
        }
      }
    }.transactionally.withTransactionIsolation(Serializable)
  }
}
