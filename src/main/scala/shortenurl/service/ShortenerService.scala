package shortenurl.service

import akka.actor.ActorRef
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.json4s.{DefaultFormats, Formats}
import spray.httpx.Json4sSupport
import spray.routing.HttpService

import scala.concurrent.duration._

trait ShortenerService extends HttpService with Json4sSupport {
  val mediator: ActorRef

  val config = ConfigFactory.load()
  val secret     = config.getString("user.repo.topic")
  val userRepoTopic = config.getString("user.repo.topic")
  val linkRepoTopic = config.getString("link.repo.topic")

  implicit val timeout = Timeout(10 seconds)
  implicit val executionContext = actorRefFactory.dispatcher
  override implicit val json4sFormats: Formats = DefaultFormats
}
