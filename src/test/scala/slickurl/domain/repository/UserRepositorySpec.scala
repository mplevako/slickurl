package slickurl.domain.repository

import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAfterExample
import slickurl.domain.repository.{UserRepositoryComponent, UserTable}
import slick.driver.JdbcProfile
import slickurl.DbProps
import slickurl.domain.model.AlphabetCodec

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class UserRepositorySpec extends Specification with BeforeAfterExample with UserRepositoryComponent with UserTable{

  sequential

  ".createNewUser" should {
    "create a new user and return its token" in {
      userRepository.createNewUser() map { result =>
        result must beRight
        result.right.get.id must_== AlphabetCodec.encode(DbProps.idSequenceStart)
      } await
    }
  }

  override val profile: JdbcProfile = slick.driver.H2Driver
  override val userRepository: UserRepository = new UserRepositoryImpl
  override val db: profile.api.Database = profile.api.Database.forURL("jdbc:h2:mem:users;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")

  override protected def before: Any = {
    import profile.api._

    val initAction = db run (users.schema ++ idSequence.schema).create
    Await.result(initAction, Duration.Inf)
  }

  override protected def after: Any = {
    import profile.api._

    val cleanAction = db run (users.schema ++ idSequence.schema).drop
    Await.result(cleanAction, Duration.Inf)
  }
}
