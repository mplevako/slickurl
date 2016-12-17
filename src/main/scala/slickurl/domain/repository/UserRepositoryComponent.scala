package slickurl.domain.repository

import slickurl.domain.model.{AlphabetCodec, Error, UserID}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait UserRepositoryComponent { this: UserTable =>

  val userRepository: UserRepository

  trait UserRepository {
    def createNewUser()(implicit ec: ExecutionContext): Future[Error Either UserID]
  }

  class UserRepositoryImpl extends UserRepository with SQLStateErrorCodeTranslator{

    import profile.api._
    import slick.jdbc.TransactionIsolation._

    override def createNewUser()(implicit ec: ExecutionContext): Future[Error Either UserID] = db run {
      idSequence.next.result map AlphabetCodec.encode map UserID flatMap { uid =>
        (users += uid).asTry map {
          case Success(_) => Right(uid)
          case Failure(t) => Left(translateException(t))
        }
      }
    }.transactionally.withTransactionIsolation(Serializable)
  }
}
