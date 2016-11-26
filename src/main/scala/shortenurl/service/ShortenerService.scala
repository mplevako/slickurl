package shortenurl.service

import akka.actor.ActorRef
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import org.json4s.{DefaultFormats, Formats}
import spray.httpx.Json4sSupport
import spray.routing.HttpService

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

trait ShortenerService extends HttpService with Json4sSupport {
  val mediator: ActorRef

  val config: Config = ConfigFactory.load()
  val secret: String = config.getString("api.secret")
  val userRepoTopic: String = config.getString("user.repo.topic")
  val linkRepoTopic: String = config.getString("link.repo.topic")

  implicit val timeout: Timeout = Timeout(10 seconds)
  implicit val executionContext: ExecutionContextExecutor = actorRefFactory.dispatcher
  override implicit val json4sFormats: Formats = DefaultFormats
}
