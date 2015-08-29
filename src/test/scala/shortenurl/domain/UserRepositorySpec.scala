/**
 * Copyright 2014-2015 Maxim Plevako
 **/
package shortenurl.domain

import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAfterExample
import shortenurl.domain.model.User
import shortenurl.domain.repository.{UserRepositoryComponent, UserTable}
import slick.driver.JdbcProfile

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class UserRepositorySpec extends Specification with BeforeAfterExample with UserRepositoryComponent with UserTable{

  sequential

  ".findById" should {

    "return an Option containing the expected User for the given id" in {
      userRepository.findById(testUser.id) map { user =>
        user must beSome[User]
        user.get.id must_== testUser.id
      } await
    }

    "return None if there is no User with the given id" in {
      userRepository.findById(-1L) must beNone.await()
    }
  }

  ".findByToken" should {

    "return an Option containing the expected User with the given token" in {
      userRepository.userForToken(testUser.token) map { user =>
        user must beSome[User]
        user.get.id must_== testUser.id
      } await
    }

    "return None if there is no User with the given token" in {
      userRepository.userForToken("bad") must beNone.await
    }
  }

  ".getOrCreateUser" should {

    "return an existing User for an existing id" in {
      userRepository.getUser(testUser.id) map { user =>
        user must beRight(testUser)
      } await
    }

    "create and return a new token for a nonexistent user" in {
      userRepository.getUser(-1L) map { result =>
        result must beRight
        result.right.get.token must not(beEmpty)
      } await
    }
  }

  override val profile: JdbcProfile = slick.driver.H2Driver
  override val userRepository: UserRepository = new UserRepositoryImpl
  override val db: profile.api.Database = profile.api.Database.forURL("jdbc:h2:mem:users;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")

  private val testUser: User = User(1L, "cafebabe")

  override protected def before: Any = {
    import profile.api._

    val initAction = db run (users.schema.create >> users.forceInsert(testUser))
    Await.result(initAction, Duration.Inf)
  }

  override protected def after: Any = {
    import profile.api._

    val cleanAction = db run users.schema.drop
    Await.result(cleanAction, Duration.Inf)
  }
}
