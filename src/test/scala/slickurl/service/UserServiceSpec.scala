package slickurl.service

import akka.actor.{ActorRef, ActorSystem}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Subscribe, SubscribeAck, Unsubscribe}
import slickurl.AppProps.{tokenGroup, tokenTopic}
import slickurl.JWTUtils
import slickurl.actor.AkkaTestkitSpecs2Support
import slickurl.domain.model.UserID
import slickurl.mock.UserRepositoryMock
import spray.http.StatusCodes._

class UserServiceSpec extends ShortenerServiceSpec with UserService {

  "User service" should {

    "should generate tokens for authenticated users" in new AkkaTestkitSpecs2Support with UserRepositoryMock {
      override protected val mockShardId: Long = shardId

      private val repoMock = createNewUserMock(mockUid)
      mediator ! Subscribe(tokenTopic, tokenGroup, repoMock)
      expectMsgType[SubscribeAck]

      checkWithToken(Post("/token"), userRoute) {
        val jwtClaim = JWTUtils.subjectForToken(entity.asString)
        jwtClaim should beSuccessfulTry
        jwtClaim.get.subject should beSome(mockUid.id)
      }

      there was one(repoMock.underlyingActor.userRepository).createNewUser(shardId)(repoMock.dispatcher)
      mediator ! Unsubscribe(tokenTopic, tokenGroup, repoMock)
    }

    "not allow GET requests to the token path" in {
      Get("/token") ~> sealRoute(userRoute) ~> check (status === MethodNotAllowed)
    }

    "not allow PUT requests to the token path" in {
      Put("/token") ~> sealRoute(userRoute) ~> check (status === MethodNotAllowed)
    }

    "not allow DELETE requests to the token path" in {
      Delete("/token") ~> sealRoute(userRoute) ~> check (status === MethodNotAllowed)
    }

    "not allow OPTIONS requests to the token path" in {
      Options("/token") ~> sealRoute(userRoute) ~> check (status === MethodNotAllowed)
    }

    "not allow HEAD requests to the token path" in {
      Head("/token") ~> sealRoute(userRoute) ~> check (status === MethodNotAllowed)
    }

    "not allow PATCH requests to the token path" in {
      Patch("/token") ~> sealRoute(userRoute) ~> check (status === MethodNotAllowed)
    }
  }

  override protected def shardId: Long = 1L
  private val mockUid = UserID("cafed00d")
  override def actorRefFactory: ActorSystem = system
  override val mediator: ActorRef = DistributedPubSub(system).mediator
}
