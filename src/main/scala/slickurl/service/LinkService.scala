package slickurl.service

import akka.actor.Props
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import slickurl.AppProps._
import spray.http.StatusCodes._
import spray.routing.{RequestContext, Route}
import spray.routing.directives.DetachMagnet

import scala.concurrent.duration._
import scala.util.matching.Regex

private[slickurl] case class  ShortenLink(url: String, folder_id: Option[Long])
private[slickurl] case class  ListLinks(offset: Option[Long], limit: Option[Long])
private[slickurl] case class  ListClicks(offset: Option[Long], limit: Option[Long])
private[slickurl] case class  PassThrough(referrer: String, remote_ip: String)
private[slickurl] case class  LinkSummary(link: Link, folder_id: Option[Long], clicks: Long)
private[slickurl] case class  Link(url: String, code: String)

trait LinkService extends ShortenerService {

  private val linkCode: Regex = s"""[$encodingAlphabet]+""".r

  import slickurl.{actor => sa}

  val linkRoute: Route = {
    path("link") {
      post {
        userID { uid =>
          entity(as[ShortenLink]) { shortenLink: ShortenLink =>
            detach(DetachMagnet.fromUnit(())) { ctx =>
              implicit val replyTo = actorRefFactory.actorOf(Props(classOf[LinkServiceCtxHandler], ctx))
              mediator ! Publish(linkTopic, sa.ShortenLink(uid, shortenLink.url, shortenLink.folder_id))
            }
          }
        }
      } ~
      get {
        userID { uid =>
          entity(as[ListLinks]) { listLinks: ListLinks =>
            if (listLinks.offset.getOrElse(0L) < 0L || listLinks.limit.getOrElse(0L) < 0L)
              complete(BadRequest)
            else
              detach(DetachMagnet.fromUnit(())) { ctx =>
                implicit val replyTo = actorRefFactory.actorOf(Props(classOf[LinkServiceCtxHandler], ctx))
                mediator ! Publish(linkTopic, sa.ListLinks(uid, None, listLinks.offset, listLinks.limit))
              }
          }
        }
      }
    } ~
    path( "link" / linkCode / "clicks") { code =>
      get {
        userID { uid =>
          entity(as[ListClicks]) { listClicks: ListClicks =>
            if (listClicks.offset.getOrElse(0L) < 0L || listClicks.limit.getOrElse(0L) < 0L)
              complete(BadRequest)
            else
              detach(DetachMagnet.fromUnit(())) { ctx =>
                implicit val replyTo = actorRefFactory.actorOf(Props(classOf[LinkServiceCtxHandler], ctx))
                mediator ! Publish(linkTopic, sa.ListClicks(uid, code, listClicks.offset, listClicks.limit))
              }
          }
        }
      }
    } ~
    path( "link" / linkCode) { code =>
      post {
        entity(as[PassThrough]) { passThrough: PassThrough =>
          detach(DetachMagnet.fromUnit(())) { ctx =>
            implicit val replyTo = actorRefFactory.actorOf(Props(classOf[LinkServiceCtxHandler], ctx))
            mediator ! Publish(linkTopic, sa.PassThrough(code, passThrough.referrer, passThrough.remote_ip))
          }
        }
      } ~
      get {
        userID { uid =>
          detach(DetachMagnet.fromUnit(())) { ctx =>
            implicit val replyTo = actorRefFactory.actorOf(Props(classOf[LinkServiceCtxHandler], ctx))
            mediator ! Publish(linkTopic, sa.GetLinkSummary(uid, code))
          }
        }
      }
    }
  }
}

class LinkServiceCtxHandler(override val ctx: RequestContext) extends ServiceCtxHandler(ctx) {
  override def receive: Receive = super.receive orElse {
    case Right(slickurl.domain.model.Link(_, url, code, _)) =>
      context.setReceiveTimeout(Duration.Undefined)
      ctx.complete(Link(url, code))
      context.stop(self)

    case Right(seq: Seq[_]) =>
      context.setReceiveTimeout(Duration.Undefined)
      ctx.complete(seq)
      context.stop(self)

    case Right(summary: slickurl.domain.model.LinkSummary) =>
      context.setReceiveTimeout(Duration.Undefined)
      ctx.complete(LinkSummary(Link(summary.url, summary.code), summary.folderId, summary.clickCount))
      context.stop(self)

    case Right(url: String) =>
      context.setReceiveTimeout(Duration.Undefined)
      ctx.complete(url)
      context.stop(self)
  }
}
