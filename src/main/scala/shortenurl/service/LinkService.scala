package shortenurl.service

import akka.actor.Props
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import spray.http.StatusCodes
import spray.routing.{RequestContext, Route}
import spray.routing.directives.DetachMagnet

import scala.concurrent.duration._
import scala.util.matching.Regex

private[shortenurl] case class ShortenLink(token: String, url: String, code: Option[String], folder_id: Option[Long])
private[shortenurl] case class ListLinks(token: String, offset: Option[Long], limit: Option[Long])
private[shortenurl] case class ListClicks(token: String, offset: Option[Long], limit: Option[Long])
private[shortenurl] case class PassThrough(referer: String, remote_ip: String)
private[shortenurl] case class GetLinkSummary(token: String)
private[shortenurl] case class LinkSummary(link: Link, folder_id: Option[Long], clicks: Long)
private[shortenurl] case class Link(url: String, code: String)

trait LinkService extends ShortenerService {

  private val linkCode: Regex = s"""[${config.getString("app.shorturl.alphabet")}]+""".r

  val linkRoute: Route = {
    path("link") {
      post {
        entity(as[ShortenLink]) { shortenLink: ShortenLink =>
            detach(DetachMagnet.fromUnit(())) { ctx =>
                val replyTo = actorRefFactory.actorOf(Props(classOf[LinkServiceCtxHandler], ctx))
                mediator ! Publish(linkRepoTopic, shortenurl.actor.ShortenLink(shortenLink.token,
                                  shortenLink.url, shortenLink.code, shortenLink.folder_id, replyTo))
            }
        }
      } ~
      get {
        entity(as[ListLinks]) { listLinks: ListLinks =>
            if(listLinks.offset.getOrElse(0L) < 0L || listLinks.limit.getOrElse(0L) < 0L) complete(StatusCodes.BadRequest)
            else
            detach(DetachMagnet.fromUnit(())) { ctx =>
                val replyTo = actorRefFactory.actorOf(Props(classOf[LinkServiceCtxHandler], ctx))
                mediator ! Publish(linkRepoTopic, shortenurl.actor.ListLinks(listLinks.token,
                                   None, listLinks.offset, listLinks.limit, replyTo))
            }
        }
      }
    } ~
    path( "link" / linkCode / "clicks") { code =>
      get {
        entity(as[ListClicks]) { listClicks: ListClicks =>
            if(listClicks.offset.getOrElse(0L) < 0L || listClicks.limit.getOrElse(0L) < 0L) complete(StatusCodes.BadRequest)
            else
            detach(DetachMagnet.fromUnit(())) { ctx =>
               val replyTo = actorRefFactory.actorOf(Props(classOf[LinkServiceCtxHandler], ctx))
               mediator ! Publish(linkRepoTopic, shortenurl.actor.ListClicks(code, listClicks.token,
                                                        listClicks.offset, listClicks.limit, replyTo))
            }
        }
      }
    } ~
    path( "link" / linkCode) { code =>
      post {
        entity(as[PassThrough]) { passThrough: PassThrough =>
           detach(DetachMagnet.fromUnit(())) { ctx =>
               val replyTo = actorRefFactory.actorOf(Props(classOf[LinkServiceCtxHandler], ctx))
               mediator ! Publish(linkRepoTopic, shortenurl.actor.PassThrough(code,
                                              passThrough.referer, passThrough.remote_ip, replyTo))
           }
        }
      } ~
      get {
        entity(as[GetLinkSummary]) { summary: GetLinkSummary =>
           detach(DetachMagnet.fromUnit(())) { ctx =>
               val replyTo = actorRefFactory.actorOf(Props(classOf[LinkServiceCtxHandler], ctx))
               mediator ! Publish(linkRepoTopic, shortenurl.actor.GetLinkSummary(code, summary.token,
                                                                                replyTo))
           }
        }
      }
    }
  }
}

class LinkServiceCtxHandler(override val ctx: RequestContext) extends ServiceCtxHandler(ctx) {
  override def receive: Receive = super.receive orElse {
    case Right(shortenurl.domain.model.Link(_, url, code, _)) =>
      context.setReceiveTimeout(Duration.Undefined)
      ctx.complete(Link(url, code.get))
      context.stop(self)

    case Right(seq: Seq[_]) =>
      context.setReceiveTimeout(Duration.Undefined)
      ctx.complete(seq)
      context.stop(self)

    case Right(summary: shortenurl.domain.model.LinkSummary) =>
      context.setReceiveTimeout(Duration.Undefined)
      ctx.complete(LinkSummary(Link(summary.code, summary.url), summary.folderId, summary.clickCount))
      context.stop(self)

    case Right(url: String) =>
      context.setReceiveTimeout(Duration.Undefined)
      ctx.complete(url)
      context.stop(self)
  }
}
