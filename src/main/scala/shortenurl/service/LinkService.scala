package shortenurl.service

import akka.actor.Props
import akka.contrib.pattern.DistributedPubSubMediator.Publish
import org.json4s.{DefaultFormats, Formats}
import shortenurl.actor.Links
import spray.httpx.Json4sSupport
import spray.routing.RequestContext
import spray.routing.directives.DetachMagnet

import scala.concurrent.duration._

private[shortenurl] case class ShortenLink(token: String, url: String, code: Option[String], folder_id: Option[Long])
private[shortenurl] case class ListLinks(token: String, offset: Option[Long], limit: Option[Long])
private[shortenurl] case class Link(url: String, code: String)

trait LinkService extends ShortenerService {

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
      } ~
      get {
        entity(as[ListLinks]) { listLinks: ListLinks =>
            detach(DetachMagnet.fromUnit()) { ctx =>
                val replyTo = actorRefFactory.actorOf(Props(classOf[LinkServiceCtxHandler], ctx))
                mediator ! Publish(`linkRepoTopic`, shortenurl.actor.ListLinks(listLinks.token,
                                   None, listLinks.offset, listLinks.limit, replyTo))
            }
        }
      }
    }
  }
}

class LinkServiceCtxHandler(override val ctx: RequestContext) extends ServiceCtxHandler(ctx)
                                                                      with Json4sSupport {
  override implicit val json4sFormats: Formats = DefaultFormats

  override def receive = super.receive orElse {
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
