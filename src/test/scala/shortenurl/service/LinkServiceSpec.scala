package shortenurl.service;

import akka.actor.ActorRef
import akka.testkit.TestProbe
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import spray.http.StatusCodes._
import spray.testkit.Specs2RouteTest

class LinkServiceSpec extends Specification with Specs2RouteTest with LinkService with Mockito {

  "Link service" should {
    "support ShortenLink POST requests to the link path" in {
      Post("/link", ShortenLink("token", "url", None, None)) ~> linkRoute ~> check( true )
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

  override def actorRefFactory = system
  val testProbe = TestProbe()
  override val mediator: ActorRef = testProbe.ref
}
