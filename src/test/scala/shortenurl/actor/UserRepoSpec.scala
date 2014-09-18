package shortenurl.actor

import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.{Publish, Subscribe, SubscribeAck}
import akka.testkit.TestActorRef
import com.typesafe.config.ConfigFactory
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import org.specs2.time.NoTimeConversions
import shortenurl.domain.model.User
import shortenurl.domain.repository.UserRepositoryComponent

class UserRepoSpec extends Specification with NoTimeConversions with Mockito {

  sequential

  "UserRepo" should {
    "return nothing if the secret is invalid" in new AkkaTestkitSpecs2Support with Mocks {
      val userRepo = TestActorRef(new UserRepoImpl)
      val mediator = DistributedPubSubExtension(system).mediator

      mediator ! Subscribe("user-repo", userRepo)
      expectMsgType[SubscribeAck]

      mediator ! Publish("user-repo", GetUser(-1, null, userRepo))
      expectNoMsg()
    }

    "return existing token given the correct secret and id of a user" in new AkkaTestkitSpecs2Support with Mocks {
      repoMock.getUser(existingUser.id) returns existingUser
      val userRepo = TestActorRef(new UserRepoImpl)
      val mediator = DistributedPubSubExtension(system).mediator

      mediator ! Subscribe("user-repo", userRepo)
      expectMsgType[SubscribeAck]

      mediator ! Publish("user-repo", GetUser(existingUser.id, secret, userRepo))
      there was one(repoMock).getUser(existingUser.id)
    }
  }

  trait Mocks extends Scope {
    val repoMock: UserRepositoryComponent#UserRepository = mock[UserRepositoryComponent#UserRepository]

    class UserRepoImpl extends UserRepo {
      override val userRepository: UserRepositoryComponent#UserRepository = repoMock
    }
  }

  val secret = ConfigFactory.load().getString("api.secret")
  val existingUser: User = User(1, "cafebabe")
  val nonExistingUser: User = User(-1, "deadbeef")
}
