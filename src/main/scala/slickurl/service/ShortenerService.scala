package slickurl.service

import akka.actor.ActorRef
import akka.util.Timeout
import org.json4s.{DefaultFormats, Formats}
import spray.httpx.Json4sSupport
import spray.routing.HttpService

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import slickurl._

trait ShortenerService extends HttpService with Json4sSupport {
  val mediator: ActorRef

  implicit val timeout: Timeout = Timeout(10 seconds)
  implicit val executionContext: ExecutionContextExecutor = actorRefFactory.dispatcher
  override implicit val json4sFormats: Formats = DefaultFormats
}
