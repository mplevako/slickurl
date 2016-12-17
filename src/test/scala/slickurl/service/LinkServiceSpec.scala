package slickurl.service

import akka.actor.{ActorRef, ActorSystem}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Subscribe, SubscribeAck, Unsubscribe}
import spray.http.StatusCodes._
import slickurl.AppProps.linkTopic
import slickurl.actor.{AkkaTestkitSpecs2Support, Clck}
import slickurl.mock.LinkRepositoryMock

class LinkServiceSpec extends ShortenerServiceSpec with LinkService {

  sequential

  "Link service" should {
    "shorten links for authenticated users" in new AkkaTestkitSpecs2Support with LinkRepositoryMock {
      private val repoMock = shortenUrlMock(tokenUid)
      mediator ! Subscribe(linkTopic, repoMock)
      expectMsgType[SubscribeAck]

      checkWithToken(Post("/link", ShortenLink(mockURL, Option(mockFolderId))), linkRoute) {
        responseAs[Link] should_== Link(mockURL, mockCode)
      }

      mediator ! Unsubscribe(linkTopic, repoMock)
    }

    "list links of authenticated users" in new AkkaTestkitSpecs2Support with LinkRepositoryMock {
      private val repoMock = listLinksMock(tokenUid)
      mediator ! Subscribe(linkTopic, repoMock)
      expectMsgType[SubscribeAck]

      checkWithToken(Get("/link", ListLinks(None, None)), linkRoute) {
        responseAs[Seq[Link]] should_== Seq(Link(mockURL, mockCode))
      }

      mediator ! Unsubscribe(linkTopic, repoMock)
    }

    "return link summary for a link with a given code of an authenticated user" in new AkkaTestkitSpecs2Support with LinkRepositoryMock {
      private val repoMock = linkSummaryMock(tokenUid)
      mediator ! Subscribe(linkTopic, repoMock)
      expectMsgType[SubscribeAck]

      checkWithToken(Get(s"/link/$mockCode"), linkRoute) {
        responseAs[LinkSummary] should_== slickurl.service.LinkSummary(
          Link(mockURL, mockCode), Option(mockFolderId), mockClickCount)
      }

      mediator ! Unsubscribe(linkTopic, repoMock)
    }

    "list clicks of a link with a given code of an authenticated user" in new AkkaTestkitSpecs2Support with LinkRepositoryMock {
      private val repoMock = listClicksMock(tokenUid)
      mediator ! Subscribe(linkTopic, repoMock)
      expectMsgType[SubscribeAck]

      checkWithToken(Get(s"/link/$mockCode/clicks", ListClicks(None, None)), linkRoute) {
        responseAs[Seq[Clck]] should_== Seq(Clck(mockClick.date, mockClick.referrer, mockClick.remote_ip))
      }
    }

    "pass through POST requests for a given link code" in {
      Post("/link/code", PassThrough("referrer", "127.0.0.1")) ~> linkRoute ~> check( true )
    }

    "return a MethodNotAllowed error for PUT requests to the link path" in {
      Put("/link") ~> sealRoute(linkRoute) ~> check { status === MethodNotAllowed }
    }

    "return a MethodNotAllowed error for DELETE requests to the link path" in {
      Delete("/link") ~> sealRoute(linkRoute) ~> check { status === MethodNotAllowed }
    }

    "return a MethodNotAllowed error for OPTIONS requests to the link path" in {
      Options("/link") ~> sealRoute(linkRoute) ~> check { status === MethodNotAllowed }
    }

    "return a MethodNotAllowed error for HEAD requests to the link path" in {
      Head("/link") ~> sealRoute(linkRoute) ~> check { status === MethodNotAllowed }
    }

    "return a MethodNotAllowed error for PATCH requests to the link path" in {
      Patch("/link") ~> sealRoute(linkRoute) ~> check { status === MethodNotAllowed }
    }
  }

  override def actorRefFactory: ActorSystem = system
  override val mediator: ActorRef = DistributedPubSub(system).mediator
}
