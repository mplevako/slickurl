package shortenurl.service

import akka.actor.{Actor, Props, ReceiveTimeout}
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.Publish
import com.typesafe.config.ConfigFactory
import org.json4s.{DefaultFormats, Formats}
import shortenurl.domain.model.Error
import spray.http.StatusCodes._
import spray.httpx.Json4sSupport
import spray.routing.RequestContext

import scala.concurrent.duration._

private[shortenurl] case class ShortenLink(token: String, url: String, code: Option[String], folderId: Option[Long])
private[shortenurl] case class Link(url: String, code: String)

trait LinkService extends ShortenerService {

  val linkRepoTopic = config.getString("link.repo.topic")

  val rejectLinkRoute = path("link") {
    (put | delete | head | options | patch) (complete(MethodNotAllowed))
  }

  val linkRoute = {
    path("link"){
      entity(as[ShortenLink]) { shortenLink: ShortenLink =>
          post { ctx =>
              val replyTo = actorRefFactory.actorOf(Props(classOf[LinkServiceCtxHandler], ctx))
              mediator ! Publish(`linkRepoTopic`, shortenurl.actor.ShortenLink(shortenLink.token,
                                                  shortenLink.url, shortenLink.code, shortenLink.folderId, replyTo))
          }
      }
    }
  }
}

class LinkServiceCtxHandler(val ctx: RequestContext) extends Actor with Json4sSupport {
  context.setReceiveTimeout(ConfigFactory.load().getInt("app.http.handler.timeout") milliseconds)
  val mediator = DistributedPubSubExtension(context.system).mediator

  override implicit val json4sFormats: Formats = DefaultFormats

  def receive = {
    case ReceiveTimeout =>
      context.setReceiveTimeout(Duration.Undefined)
      context.stop(self)

    case Left(Error(msg)) =>
      context.setReceiveTimeout(Duration.Undefined)
      ctx.complete(msg)
      context.stop(self)

    case Right(shortenurl.domain.model.Link(_, url, code, _)) =>
      context.setReceiveTimeout(Duration.Undefined)
      ctx.complete(Link(url, code.get))
      context.stop(self)
  }
}
