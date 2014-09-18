package shortenurl.service

import akka.actor.{Actor, Props, ReceiveTimeout}
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.Publish
import com.typesafe.config.ConfigFactory
import org.json4s.{DefaultFormats, Formats}
import shortenurl.actor.Links
import shortenurl.domain.model.Error
import spray.httpx.Json4sSupport
import spray.routing.RequestContext
import spray.routing.directives.DetachMagnet

import scala.concurrent.duration._

private[shortenurl] case class ShortenLink(token: String, url: String, code: Option[String], folder_id: Option[Long])
private[shortenurl] case class ListLinks(token: String, offset: Long = 0L, limit: Option[Long])
private[shortenurl] case class Link(url: String, code: String)


trait LinkService extends ShortenerService {

  val linkRepoTopic = config.getString("link.repo.topic")

  val linkRoute = {
    path("link") {
      post {
        entity(as[ShortenLink]) { shortenLink: ShortenLink =>
            detach(DetachMagnet.fromUnit()) { ctx =>
                val replyTo = actorRefFactory.actorOf(Props(classOf[LinkServiceCtxHandler], ctx))
                mediator ! Publish(`linkRepoTopic`, shortenurl.actor.ShortenLink(shortenLink.token,
                                  shortenLink.url, shortenLink.code, shortenLink.folder_id, replyTo))
            }
        }
      }
    } ~
    path("folder" / LongNumber) { folderId =>
      get {
        entity(as[ListLinks]) { listLinks: ListLinks =>
            detach(DetachMagnet.fromUnit()) { ctx =>
                val replyTo = actorRefFactory.actorOf(Props(classOf[LinkServiceCtxHandler], ctx))
                mediator ! Publish(`linkRepoTopic`, shortenurl.actor.ListLinks(listLinks.token,
                                   folderId, listLinks.offset, listLinks.limit, replyTo))
            }
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

    case Links(links) =>
      context.setReceiveTimeout(Duration.Undefined)
      ctx.complete(links)
      context.stop(self)

    case Right(shortenurl.domain.model.Link(_, url, code, _)) =>
      context.setReceiveTimeout(Duration.Undefined)
      ctx.complete(Link(url, code.get))
      context.stop(self)
  }
}
