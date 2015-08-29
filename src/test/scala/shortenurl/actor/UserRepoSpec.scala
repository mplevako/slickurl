/**
 * Copyright 2014-2015 Maxim Plevako
 **/
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

import scala.concurrent.Future

class UserRepoSpec extends Specification with NoTimeConversions with Mockito {

  sequential

  "UserRepo" should {
    "return nothing if the secret is invalid" in new AkkaTestkitSpecs2Support with Mocks {
      val userRepo = TestActorRef(new UserRepoImpl)
      val mediator = DistributedPubSubExtension(system).mediator

      mediator ! Subscribe(userRepoTopic, userRepo)
      expectMsgType[SubscribeAck]

      mediator ! Publish(userRepoTopic, GetUser(-1L, null, userRepo))
      expectNoMsg()
    }

    "return existing token given the correct secret and id of a user" in new AkkaTestkitSpecs2Support with Mocks {
      val userRepo = TestActorRef(new UserRepoImpl)
      val mediator = DistributedPubSubExtension(system).mediator

      implicit val ec = userRepo.dispatcher

      repoMock.getUser(existentUser.id) returns Future.successful(Right(existentUser))

      mediator ! Subscribe(userRepoTopic, userRepo)
      expectMsgType[SubscribeAck]

      mediator ! Publish(userRepoTopic, GetUser(existentUser.id, secret, userRepo))
      there was one(repoMock).getUser(existentUser.id)
    }

    "return None for non-existent token" in new AkkaTestkitSpecs2Support with Mocks {
      val userRepo = TestActorRef(new UserRepoImpl)
      val mediator = DistributedPubSubExtension(system).mediator

      implicit val ec = userRepo.dispatcher

      repoMock.userForToken(nonExistentUser.token) returns Future.successful(None)

      mediator ! Subscribe(userRepoTopic, userRepo)
      expectMsgType[SubscribeAck]

      val replyToTestProbe = TestProbe()
      val replyTo = replyToTestProbe.ref

      mediator ! Publish(userRepoTopic, GetUserWithToken(nonExistentUser.token, replyTo, None))
      replyToTestProbe.expectMsg(UserForToken(None, None))
    }

    "return the user with the given existent token" in new AkkaTestkitSpecs2Support with Mocks {
      val userRepo = TestActorRef(new UserRepoImpl)
      val mediator = DistributedPubSubExtension(system).mediator

      implicit val ec = userRepo.dispatcher

      repoMock.userForToken(existentUser.token) returns Future.successful(Option(existentUser))

      mediator ! Subscribe(userRepoTopic, userRepo)
      expectMsgType[SubscribeAck]

      val replyToTestProbe = TestProbe()
      val replyTo = replyToTestProbe.ref

      mediator ! Publish(userRepoTopic, GetUserWithToken(existentUser.token, replyTo, None))
      replyToTestProbe.expectMsg(UserForToken(Option(existentUser), None))
    }
  }

  trait Mocks extends Scope {
    val repoMock: UserRepositoryComponent#UserRepository = mock[UserRepositoryComponent#UserRepository]

    class UserRepoImpl extends UserRepo {
      override val userRepository: UserRepositoryComponent#UserRepository = repoMock
    }
  }

  private val secret = ConfigFactory.load().getString("api.secret")
  private val userRepoTopic = ConfigFactory.load().getString("user.repo.topic")
  private val existentUser: User = User(1L, "cafebabe")
  private val nonExistentUser: User = User(-1L, "deadbeef")
}
