package slickurl.actor

import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe, SubscribeAck}
import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions
import slickurl.AppProps._
import slickurl.domain.model.UserID
import slickurl.mock.UserRepositoryMock

class UserRepoSpec extends Specification with NoTimeConversions {

  sequential

  "UserRepo" should {
    "return the user with the given existent token" in new AkkaTestkitSpecs2Support with UserRepositoryMock {
      private val repoMockA = createNewUserMock(uid)
      private val repoMockB = createNewUserMock(uid)
      private val mediator  = DistributedPubSub(system).mediator

      mediator ! Subscribe(tokenTopic, tokenGroup, repoMockA)
      expectMsgType[SubscribeAck]
      mediator ! Subscribe(tokenTopic, tokenGroup, repoMockB)
      expectMsgType[SubscribeAck]

      mediator ! Publish(tokenTopic, CreateNewUser, sendOneMessageToEachGroup = true)
      expectMsg(Right(uid))
      expectNoMsg
      there was one(repositoryMock).createNewUser()
    }
  }

  private val uid = UserID("cafed00d")
}
