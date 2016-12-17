package slickurl.service

import akka.actor.{ActorRef, ActorSystem}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Subscribe, SubscribeAck, Unsubscribe}
import slickurl.AppProps.linkTopic
import slickurl.actor.AkkaTestkitSpecs2Support
import slickurl.mock.LinkRepositoryMock
import spray.http.StatusCodes._

class FolderServiceSpec extends ShortenerServiceSpec with FolderService {

  sequential

  "Folder service" should {
    "list folders for authenticated users" in new AkkaTestkitSpecs2Support with LinkRepositoryMock {
      private val repoMock = listFoldersMock(tokenUid)
      mediator ! Subscribe(linkTopic, repoMock)
      expectMsgType[SubscribeAck]

      checkWithToken(Get("/folder"), folderRoute) {
        responseAs[Seq[Folder]] should_== Seq(Folder(mockFolderId, mockFolderTitle))
      }

      mediator ! Unsubscribe(linkTopic, repoMock)
    }

    "list links in a given folder of an authenticated user" in new AkkaTestkitSpecs2Support with LinkRepositoryMock {
      private val repoMock = listLinksMock(tokenUid, Option(mockFolderId))
      mediator ! Subscribe(linkTopic, repoMock)
      expectMsgType[SubscribeAck]

      checkWithToken(Get(s"/folder/$mockFolderId", ListLinks(None, None)), folderRoute) {
        responseAs[Seq[Link]] should_== Seq(Link(mockURL, mockCode))
      }

      mediator ! Unsubscribe(linkTopic, repoMock)
    }

    "return a MethodNotAllowed error for POST requests to the folder path" in {
      Post("/folder") ~> sealRoute(folderRoute) ~> check { status === MethodNotAllowed }
    }

    "return a MethodNotAllowed error for PUT requests to the folder path" in {
      Put("/folder") ~> sealRoute(folderRoute) ~> check { status === MethodNotAllowed }
    }

    "return a MethodNotAllowed error for DELETE requests to the folder path" in {
      Delete("/folder") ~> sealRoute(folderRoute) ~> check { status === MethodNotAllowed }
    }

    "return a MethodNotAllowed error for OPTIONS requests to the folder path" in {
      Options("/folder") ~> sealRoute(folderRoute) ~> check { status === MethodNotAllowed }
    }

    "return a MethodNotAllowed error for HEAD requests to the folder path" in {
      Head("/folder") ~> sealRoute(folderRoute) ~> check { status === MethodNotAllowed }
    }

    "return a MethodNotAllowed error for PATCH requests to the folder path" in {
      Patch("/folder") ~> sealRoute(folderRoute) ~> check { status === MethodNotAllowed }
    }
  }

  override def actorRefFactory: ActorSystem = system
  override val mediator: ActorRef = DistributedPubSub(system).mediator
}

