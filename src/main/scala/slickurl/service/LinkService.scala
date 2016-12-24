package slickurl.service

import akka.actor.Props
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import slickurl.AppProps._
import slickurl.domain.model.AlphabetCodec
import spray.http.{ContentTypes, HttpEntity, HttpHeaders, HttpResponse}
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
        shardAndUserID { case (sid, uid) =>
          entity(as[ShortenLink]) { shortenLink: ShortenLink =>
            detach(DetachMagnet.fromUnit(())) { ctx =>
              val msg = sa.ShortenLink(sid, uid, shortenLink.url, shortenLink.folder_id)
              implicit val replyTo = actorRefFactory.actorOf(Props(classOf[LinkServiceCtxHandler], ctx))
              mediator ! Publish(linkTopic, msg)
            }
          }
        }
      } ~
      get {
        shardAndUserID { case (sid, uid) =>
          entity(as[ListLinks]) { listLinks: ListLinks =>
            if (listLinks.offset.getOrElse(0L) < 0L || listLinks.limit.getOrElse(0L) < 0L)
              complete(BadRequest)
            else
              detach(DetachMagnet.fromUnit(())) { ctx =>
                val msg = sa.ListLinks(sid, uid, None, listLinks.offset, listLinks.limit)
                implicit val replyTo = actorRefFactory.actorOf(Props(classOf[LinkServiceCtxHandler], ctx))
                mediator ! Publish(linkTopic, msg)
              }
          }
        }
      }
    } ~
    path( "link" / linkCode / "clicks") { code =>
      validateCode(code) {
        get {
          shardAndUserID { case (sid, uid) =>
            entity(as[ListClicks]) { listClicks: ListClicks =>
              if (listClicks.offset.getOrElse(0L) < 0L || listClicks.limit.getOrElse(0L) < 0L)
                complete(BadRequest)
              else
                detach(DetachMagnet.fromUnit(())) { ctx =>
                  val msg = sa.ListClicks(sid, uid, code, listClicks.offset, listClicks.limit)
                  implicit val replyTo = actorRefFactory.actorOf(Props(classOf[LinkServiceCtxHandler], ctx))
                  mediator ! Publish(linkTopic, msg)
                }
            }
          }
        }
      }
    } ~
    path( "link" / linkCode) { code =>
      validateCode(code) {
        post {
          entity(as[PassThrough]) { passThrough: PassThrough =>
            detach(DetachMagnet.fromUnit(())) { ctx =>
              implicit val replyTo = actorRefFactory.actorOf(Props(classOf[LinkServiceCtxHandler], ctx))
              val shardId = AlphabetCodec.decodeLo(code)
              val msg = sa.PassThrough(shardId, code, passThrough.referrer, passThrough.remote_ip)
              mediator ! Publish(linkTopic, msg)
            }
          }
        }
      } ~
      get {
        shardAndUserID { case (sid, uid) =>
          detach(DetachMagnet.fromUnit(())) { ctx =>
            implicit val replyTo = actorRefFactory.actorOf(Props(classOf[LinkServiceCtxHandler], ctx))
            val msg = sa.GetLinkSummary(sid, uid, code)
            mediator ! Publish(linkTopic, msg)
          }
        }
      }
    }
  }

  private def validateCode(code: String)(route: Route): Route =
    validate(code.compare(maxURLCode) <= 0, "Invalid code") { route }
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
      ctx.complete(HttpResponse(entity = HttpEntity(url)).withHeaders(
                   new HttpHeaders.`Content-Type`(ContentTypes.`text/plain`))
                  )
      context.stop(self)
  }
}
