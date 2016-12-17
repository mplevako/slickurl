package slickurl.service

import akka.actor.ActorRef
import akka.util.Timeout
import org.json4s.{DefaultFormats, Formats}
import spray.httpx.Json4sSupport
import spray.routing._

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import slickurl.AppProps._
import slickurl.JWTUtils
import slickurl.domain.model.UserID
import spray.routing.AuthenticationFailedRejection.CredentialsRejected

import scala.util.{Failure, Success, Try}

trait ShortenerService extends HttpService with Json4sSupport {
  val mediator: ActorRef

  protected val TokenHeader = "X-Token"

  implicit val timeout: Timeout = Timeout(2*httpHandlerTimeout)
  implicit val executionContext: ExecutionContextExecutor = actorRefFactory.dispatcher
  override implicit val json4sFormats: Formats = DefaultFormats

  protected def userID: Directive1[UserID] = {
    headerValueByName(TokenHeader) flatMap { case token =>
      if (token == null || token.isEmpty)
        reject(MalformedHeaderRejection(TokenHeader, "Empty token"))
      else
        JWTUtils.subjectForToken(token) flatMap { claim =>
          Try(claim.subject.get) //absence of the subject is an error
        } match {
          case Failure(_)   => reject(AuthenticationFailedRejection(CredentialsRejected, List.empty))
          case Success(uid) => provide(UserID(uid))
        }
    }
  }
}
