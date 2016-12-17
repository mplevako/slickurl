package slickurl.service

import akka.actor.{ActorRef, ActorSystem}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Subscribe, SubscribeAck, Unsubscribe}
import slickurl.AppProps.{tokenGroup, tokenTopic}
import slickurl.JWTUtils
import slickurl.actor.AkkaTestkitSpecs2Support
import slickurl.mock.UserRepositoryMock
import spray.http.StatusCodes._

class UserServiceSpec extends ShortenerServiceSpec with UserService {

  "User service" should {

    "should generate tokens for authenticated users" in new AkkaTestkitSpecs2Support with UserRepositoryMock {
      private val repoMock = createNewUserMock(tokenUid)
      mediator ! Subscribe(tokenTopic, tokenGroup, repoMock)
      expectMsgType[SubscribeAck]

      checkWithToken(Post("/token"), userRoute) {
        val jwtClaim = JWTUtils.subjectForToken(entity.asString)
        jwtClaim should beSuccessfulTry
        jwtClaim.get.subject should beSome(tokenUid.id)
      }

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

  override def actorRefFactory: ActorSystem = system
  override val mediator: ActorRef = DistributedPubSub(system).mediator
}
