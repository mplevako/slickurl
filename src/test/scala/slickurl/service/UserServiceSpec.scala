package slickurl.service

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.TestProbe
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import spray.http.HttpHeaders.RawHeader
import spray.http.StatusCodes._
import spray.routing.AuthenticationFailedRejection
import spray.testkit.Specs2RouteTest

class UserServiceSpec extends Specification with Specs2RouteTest with UserService with Mockito {

  private val nullSecretTokenHeader = RawHeader(SecretTokenHeader, null)
  private val emptySecretTokenHeader = RawHeader(SecretTokenHeader, "")
  private val incorrectSecretTokenHeader = RawHeader(SecretTokenHeader, "DeadBeef")
  private val secretTokenHeader = RawHeader(SecretTokenHeader, slickurl.AppConfig.apiSecret)

  "User service" should {

    "should reject non-authenticated POST requests to the token path" in {
      Post("/token") ~> sealRoute(userRoute) ~> check (status === BadRequest)
      Post("/token") ~> addHeader(nullSecretTokenHeader) ~> sealRoute(userRoute) ~> check (
        status === Unauthorized
      )
      Post("/token") ~> addHeader(emptySecretTokenHeader) ~> sealRoute(userRoute) ~> check (
        status === Unauthorized
      )
      Post("/token") ~> addHeader(incorrectSecretTokenHeader) ~> sealRoute(userRoute) ~> check (
        status === Unauthorized
      )
    }

    "should reject non-authenticated GET requests to the token path" in {
      Get("/token") ~> sealRoute(userRoute) ~> check (status === BadRequest)
      Get("/token") ~> addHeader(nullSecretTokenHeader) ~> sealRoute(userRoute) ~> check (
        status === Unauthorized
      )
      Get("/token") ~> addHeader(emptySecretTokenHeader) ~> sealRoute(userRoute) ~> check (
        status === Unauthorized
      )
      Get("/token") ~> addHeader(incorrectSecretTokenHeader) ~> sealRoute(userRoute) ~> check (
        status === Unauthorized
      )
    }

    "support GetUser requests to the token path" in {
      Get("/token", GetUser(-1)) ~> addHeader(secretTokenHeader) ~> userRoute ~> check (true)
    }

    "return a MethodNotAllowed error for PUT requests to the token path" in {
      Put("/token") ~> sealRoute(userRoute) ~> check (status === MethodNotAllowed)
    }

    "return a MethodNotAllowed error for DELETE requests to the token path" in {
      Delete("/token") ~> sealRoute(userRoute) ~> check (status === MethodNotAllowed)
    }

    "return a MethodNotAllowed error for OPTIONS requests to the token path" in {
      Options("/token") ~> sealRoute(userRoute) ~> check (status === MethodNotAllowed)
    }

    "return a MethodNotAllowed error for HEAD requests to the token path" in {
      Head("/token") ~> sealRoute(userRoute) ~> check (status === MethodNotAllowed)
    }

    "return a MethodNotAllowed error for PATCH requests to the token path" in {
      Patch("/token") ~> sealRoute(userRoute) ~> check (status === MethodNotAllowed)
    }
  }

  override def actorRefFactory: ActorSystem = system
  private val testProbe = TestProbe()
  override val mediator: ActorRef = testProbe.ref
}
