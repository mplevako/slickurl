package slickurl.service

import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification
import slickurl.JWTUtils
import slickurl.domain.model.{AlphabetCodec, UserID}
import spray.http.HttpHeaders.RawHeader
import spray.http.HttpRequest
import spray.http.StatusCodes._
import spray.routing.Route
import spray.testkit.Specs2RouteTest

trait ShortenerServiceSpec extends Specification with Specs2RouteTest with ShortenerService {
  protected def shardId: Long
  protected def tokenUid = UserID(AlphabetCodec.packAndEncode(shardId)(1L))
  protected val nullSecretTokenHeader = RawHeader(TokenHeader, null)
  protected val emptySecretTokenHeader = RawHeader(TokenHeader, "")
  protected val incorrectSecretTokenHeader = RawHeader(TokenHeader, "DeadBeef")
  protected val correctTokenHeader = RawHeader(TokenHeader, JWTUtils.tokenForSubject(tokenUid.id))

  protected def checkWithToken(request: HttpRequest, route: Route)
                              (onCorrectHeader: => MatchResult[Any]): MatchResult[Any] = {
    request ~> sealRoute(route) ~> check (status === BadRequest)
    request ~> addHeader(nullSecretTokenHeader) ~> sealRoute(route) ~>
    check (status === BadRequest)
    request ~> addHeader(emptySecretTokenHeader) ~> sealRoute(route) ~>
    check (status === BadRequest)
    request ~> addHeader(incorrectSecretTokenHeader) ~> sealRoute(route) ~>
    check (status === Unauthorized)
    request ~> addHeader(correctTokenHeader) ~> sealRoute(route) ~> check { onCorrectHeader }

  }
}
