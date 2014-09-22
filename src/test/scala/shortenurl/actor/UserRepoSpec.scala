package shortenurl.actor

import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.{Publish, Subscribe, SubscribeAck}
import akka.testkit.{TestActorRef, TestProbe}
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

      mediator ! Subscribe(`userRepoTopic`, userRepo)
      expectMsgType[SubscribeAck]

      mediator ! Publish(`userRepoTopic`, GetUser(-1, null, userRepo))
      expectNoMsg()
    }

    "return existing token given the correct secret and id of a user" in new AkkaTestkitSpecs2Support with Mocks {
      repoMock.getUser(existentUser.id) returns existentUser
      val userRepo = TestActorRef(new UserRepoImpl)
      val mediator = DistributedPubSubExtension(system).mediator

      mediator ! Subscribe(`userRepoTopic`, userRepo)
      expectMsgType[SubscribeAck]

      mediator ! Publish(`userRepoTopic`, GetUser(existentUser.id, secret, userRepo))
      there was one(repoMock).getUser(existentUser.id)
    }

    "return None for non-existent token" in new AkkaTestkitSpecs2Support with Mocks {
      repoMock.userForToken(nonExistentUser.token) returns None
      val userRepo = TestActorRef(new UserRepoImpl)
      val mediator = DistributedPubSubExtension(system).mediator

      mediator ! Subscribe(`userRepoTopic`, userRepo)
      expectMsgType[SubscribeAck]

      val replyToTestProbe = TestProbe()
      val replyTo = replyToTestProbe.ref

      mediator ! Publish(`userRepoTopic`, GetUserWithToken(nonExistentUser.token, replyTo, None))
      replyToTestProbe.expectMsg(UserForToken(None, None))
    }

    "return the user with the given existent token" in new AkkaTestkitSpecs2Support with Mocks {
      repoMock.userForToken(existentUser.token) returns Some(existentUser)
      val userRepo = TestActorRef(new UserRepoImpl)
      val mediator = DistributedPubSubExtension(system).mediator

      mediator ! Subscribe(`userRepoTopic`, userRepo)
      expectMsgType[SubscribeAck]

      val replyToTestProbe = TestProbe()
      val replyTo = replyToTestProbe.ref

      mediator ! Publish(`userRepoTopic`, GetUserWithToken(existentUser.token, replyTo, None))
      replyToTestProbe.expectMsg(UserForToken(Some(existentUser), None))
    }
  }

  trait Mocks extends Scope {
    val repoMock: UserRepositoryComponent#UserRepository = mock[UserRepositoryComponent#UserRepository]

    class UserRepoImpl extends UserRepo {
      override val userRepository: UserRepositoryComponent#UserRepository = repoMock
    }
  }

  val secret = ConfigFactory.load().getString("api.secret")
  val userRepoTopic = ConfigFactory.load().getString("user.repo.topic")
  val existentUser: User = User(1L, "cafebabe")
  val nonExistentUser: User = User(-1L, "deadbeef")
}
