package shortenurl.domain

import org.specs2.execute.{AsResult, Result}
import org.specs2.mutable.Specification
import org.specs2.specification.{AroundExample, BeforeAfterExample}
import shortenurl.domain.model.User
import shortenurl.domain.repository.{UserRepositoryComponent, UserTable}

import scala.slick.driver.JdbcProfile

class UserRepositorySpec extends Specification with AroundExample with BeforeAfterExample
                                 with UserRepositoryComponent with UserTable{

  sequential

  ".findById" should {

    "return an Option containing the expected User for the given id" in {
      val user = userRepository.findById(testUser.id)
      user must beSome[User]
      user.get.id must_== testUser.id
    }

    "return None if there is no User with the given id" in {
      userRepository.findById(-1L) must beNone
    }
  }

  ".findByToken" should {

    "return an Option containing the expected User with the given token" in {
      val user = userRepository.userForToken(testUser.token)
      user must beSome[User]
      user.get.id must_== testUser.id
    }

    "return None if there is no User with the given token" in {
      userRepository.userForToken("bad") must beNone
    }
  }

  ".getOrCreateUser" should {

    "return an existing User for an existing id" in {
      val user = userRepository.getUser(testUser.id)
      user must_== testUser
    }

    "create and return a new token for a nonexistent user" in {
      val user = userRepository.getUser(-1)
      user must not(beNull)
      user.token must not(beEmpty)
    }
  }

  override val profile: JdbcProfile = scala.slick.driver.H2Driver
  override val userRepository: UserRepository = new UserRepositoryImpl
  override val db: JdbcProfile#Backend#Database = profile.simple.Database.forURL("jdbc:h2:mem:users", driver = "org.h2.Driver")

  val testUser: User = User(1L, "cafebabe")

  import profile.simple._

  override def before: Any = db withSession { implicit session =>
    users.ddl.create
    users.forceInsert(testUser)
  }

  override def after: Any = db withSession { implicit session =>
    users.ddl.drop
  }

  override def around[T: AsResult](t: => T): Result = {
    db.withTransaction { implicit session =>
      try AsResult(t) finally session.rollback()
    }
  }
}
