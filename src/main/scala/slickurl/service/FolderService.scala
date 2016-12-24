package slickurl.service

import akka.actor.Props
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import slickurl.actor.Folders
import slickurl.AppProps._
import spray.http.StatusCodes
import spray.routing.{RequestContext, Route}
import spray.routing.directives.DetachMagnet

import scala.concurrent.duration._

case class Folder(id: Long, title: String)

trait FolderService extends ShortenerService {

  import slickurl.{actor => sa}

  val folderRoute: Route = {
    path("folder") {
      get {
        shardAndUserID { case (sid, uid) =>
          detach(DetachMagnet.fromUnit(())) { ctx =>
            val msg = sa.ListFolders(sid, uid)
            implicit val replyTo = actorRefFactory.actorOf(Props(classOf[FolderServiceCtxHandler], ctx))
            mediator ! Publish(linkTopic, msg)
          }
        }
      }
    } ~
    path( "folder" / LongNumber) { fid =>
      get {
        shardAndUserID { case (sid, uid) =>
          entity(as[ListLinks]) { listLinks: ListLinks =>
            if(listLinks.offset.getOrElse(0L) < 0L || listLinks.limit.getOrElse(0L) < 0L)
              complete(StatusCodes.BadRequest)
            else
              detach(DetachMagnet.fromUnit(())) { ctx =>
                val msg = sa.ListLinks(sid, uid, Some(fid), listLinks.offset, listLinks.limit)
                implicit val replyTo = actorRefFactory.actorOf(Props(classOf[FolderServiceCtxHandler], ctx))
                mediator ! Publish(linkTopic, msg)
              }
          }
        }
      }
    }
  }
}

class FolderServiceCtxHandler(override val ctx: RequestContext) extends ServiceCtxHandler(ctx) {
  override def receive: Receive = super.receive orElse {
    case Folders(folders) =>
      context.setReceiveTimeout(Duration.Undefined)
      ctx.complete(folders.map(folder => Folder(folder.id, folder.title)))
      context.stop(self)

    case Right(seq: Seq[_]) =>
      context.setReceiveTimeout(Duration.Undefined)
      ctx.complete(seq)
      context.stop(self)
  }
}
