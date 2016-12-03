package slickurl.actor

import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe, SubscribeAck}
import akka.testkit.TestActorRef
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import org.specs2.time.NoTimeConversions
import slickurl.AppConfig._
import slickurl.domain.model.User
import slickurl.domain.repository.UserRepositoryComponent

import scala.concurrent.Future

class UserRepoSpec extends Specification with NoTimeConversions with Mockito {

  sequential

  "UserRepo" should {
    "return nothing if the secret is invalid" in new AkkaTestkitSpecs2Support with Mocks {
      private val userRepo = TestActorRef(new UserRepoImpl)
      private val mediator = DistributedPubSub(system).mediator

      mediator ! Subscribe(userRepoTopic, userRepo)
      expectMsgType[SubscribeAck]

      mediator ! Publish(userRepoTopic, GetUser(-1L, null, userRepo))
      expectNoMsg()
    }

    "return existing token given the correct secret and id of a user" in new AkkaTestkitSpecs2Support with Mocks {
      private val userRepo = TestActorRef(new UserRepoImpl)
      private val mediator = DistributedPubSub(system).mediator

      implicit private val ec = userRepo.dispatcher

      repoMock.getUser(existentUser.id) returns Future.successful(Right(existentUser))

      mediator ! Subscribe(userRepoTopic, userRepo)
      expectMsgType[SubscribeAck]

      mediator ! Publish(userRepoTopic, GetUser(existentUser.id, apiSecret, testActor))
      expectMsg(Right(existentUser))
      there was one(repoMock).getUser(existentUser.id)
    }

    "return None for non-existent token" in new AkkaTestkitSpecs2Support with Mocks {
      private val userRepo = TestActorRef(new UserRepoImpl)
      private val mediator = DistributedPubSub(system).mediator

      implicit private val ec = userRepo.dispatcher

      repoMock.userForToken(nonExistentUser.token) returns Future.successful(None)

      mediator ! Subscribe(userRepoTopic, userRepo)
      expectMsgType[SubscribeAck]

      mediator ! Publish(userRepoTopic, GetUserWithToken(nonExistentUser.token, testActor, None))
      expectMsg(UserForToken(None, None))
    }

    "return the user with the given existent token" in new AkkaTestkitSpecs2Support with Mocks {
      private val userRepo = TestActorRef(new UserRepoImpl)
      private val mediator = DistributedPubSub(system).mediator

      implicit private val ec = userRepo.dispatcher

      repoMock.userForToken(existentUser.token) returns Future.successful(Option(existentUser))

      mediator ! Subscribe(userRepoTopic, userRepo)
      expectMsgType[SubscribeAck]

      mediator ! Publish(userRepoTopic, GetUserWithToken(existentUser.token, testActor, None))
      expectMsg(UserForToken(Option(existentUser), None))
    }
  }

  trait Mocks extends Scope {
    protected val repoMock: UserRepositoryComponent#UserRepository = mock[UserRepositoryComponent#UserRepository]

    class UserRepoImpl extends UserRepo {
      override val userRepository: UserRepositoryComponent#UserRepository = repoMock
    }
  }

  private val existentUser: User = User(1L, "cafebabe")
  private val nonExistentUser: User = User(-1L, "deadbeef")
}
