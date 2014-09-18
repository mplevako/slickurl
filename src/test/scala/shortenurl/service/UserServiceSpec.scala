package shortenurl.service

import akka.actor.ActorRef
import akka.testkit.TestProbe
import com.typesafe.config.ConfigFactory
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import shortenurl.actor.AkkaTestkitSpecs2Support
import spray.http.StatusCodes._
import spray.testkit.Specs2RouteTest

class UserServiceSpec extends Specification with Specs2RouteTest with UserService with Mockito {

  "User service" should {
    "support GetUser requests to the token path" in new AkkaTestkitSpecs2Support {
      Get("/token", GetUser(-1, secret)) ~> userRoute ~> check {
        true
      }
    }

    "return a MethodNotAllowed error for POST requests to the token path" in {
      Post("/token") ~> sealRoute(rejectUserRoute) ~> check {
        status === MethodNotAllowed
      }
    }

    "return a MethodNotAllowed error for PUT requests to the token path" in {
      Put("/token") ~> sealRoute(rejectUserRoute) ~> check {
        status === MethodNotAllowed
      }
    }

    "return a MethodNotAllowed error for DELETE requests to the token path" in {
      Delete("/token") ~> sealRoute(rejectUserRoute) ~> check {
        status === MethodNotAllowed
      }
    }

    "return a MethodNotAllowed error for OPTIONS requests to the token path" in {
      Options("/token") ~> sealRoute(rejectUserRoute) ~> check {
        status === MethodNotAllowed
      }
    }

    "return a MethodNotAllowed error for HEAD requests to the token path" in {
      Head("/token") ~> sealRoute(rejectUserRoute) ~> check {
        status === MethodNotAllowed
      }
    }

    "return a MethodNotAllowed error for PATCH requests to the token path" in {
      Patch("/token") ~> sealRoute(rejectUserRoute) ~> check {
        status === MethodNotAllowed
      }
    }
  }

  override def actorRefFactory = system
  val testProbe = TestProbe()
  override val mediator: ActorRef = testProbe.ref
  val secret = ConfigFactory.load().getString("api.secret")
}
