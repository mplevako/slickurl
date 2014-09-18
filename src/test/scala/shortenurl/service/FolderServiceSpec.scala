package shortenurl.service

import akka.actor.ActorRef
import akka.testkit.TestProbe
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import shortenurl.actor.AkkaTestkitSpecs2Support
import spray.http.StatusCodes._
import spray.testkit.Specs2RouteTest

class FolderServiceSpec extends Specification with Specs2RouteTest with FolderService with Mockito {

  "Folder service" should {
    "support ListFolders requests to the folder path" in new AkkaTestkitSpecs2Support {
      Get("/folder", ListFolders("token")) ~> folderRoute ~> check { true }
    }

    "return a MethodNotAllowed error for POST requests to the folder path" in {
      Post("/folder") ~> sealRoute(rejectFolderRoute) ~> check { status === MethodNotAllowed }
    }

    "return a MethodNotAllowed error for PUT requests to the folder path" in {
      Put("/folder") ~> sealRoute(rejectFolderRoute) ~> check { status === MethodNotAllowed }
    }

    "return a MethodNotAllowed error for DELETE requests to the folder path" in {
      Delete("/folder") ~> sealRoute(rejectFolderRoute) ~> check { status === MethodNotAllowed }
    }

    "return a MethodNotAllowed error for OPTIONS requests to the folder path" in {
      Options("/folder") ~> sealRoute(rejectFolderRoute) ~> check { status === MethodNotAllowed }
    }

    "return a MethodNotAllowed error for HEAD requests to the folder path" in {
      Head("/folder") ~> sealRoute(rejectFolderRoute) ~> check { status === MethodNotAllowed }
    }

    "return a MethodNotAllowed error for PATCH requests to the folder path" in {
      Patch("/folder") ~> sealRoute(rejectFolderRoute) ~> check { status === MethodNotAllowed }
    }
  }

  override def actorRefFactory = system
  val testProbe = TestProbe()
  override val mediator: ActorRef = testProbe.ref
}

