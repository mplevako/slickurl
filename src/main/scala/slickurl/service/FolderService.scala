package slickurl.service

import akka.actor.Props
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import slickurl.actor.Folders
import slickurl.AppConfig._
import spray.http.StatusCodes
import spray.routing.{RequestContext, Route}
import spray.routing.directives.DetachMagnet

import scala.concurrent.duration._

private[slickurl] case class ListFolders(token: String)
case class Folder(id: Long, title: String)

trait FolderService extends ShortenerService {

  val folderRoute: Route = {
    path("folder") {
      get {
        entity(as[ListFolders]) { listFolders: ListFolders =>
            detach(DetachMagnet.fromUnit(())) { ctx =>
              val replyTo = actorRefFactory.actorOf(Props(classOf[FolderServiceCtxHandler], ctx))
              mediator ! Publish(linkRepoTopic, slickurl.actor.ListFolders(listFolders.token, replyTo))
            }
        }
      }
    } ~
    path( "folder" / LongNumber) { folderId =>
      get {
        entity(as[ListLinks]) { listLinks: ListLinks =>
          if(listLinks.offset.getOrElse(0L) < 0L || listLinks.limit.getOrElse(0L) < 0L)
            complete(StatusCodes.BadRequest)
          else
          detach(DetachMagnet.fromUnit(())) { ctx =>
            val replyTo = actorRefFactory.actorOf(Props(classOf[FolderServiceCtxHandler], ctx))
            mediator ! Publish(linkRepoTopic, slickurl.actor.ListLinks(listLinks.token,
                               Some(folderId), listLinks.offset, listLinks.limit, replyTo))
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
