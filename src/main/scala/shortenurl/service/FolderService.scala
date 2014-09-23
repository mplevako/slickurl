/**
 * Copyright 2014 Maxim Plevako
 **/
package shortenurl.service

import akka.actor.Props
import akka.contrib.pattern.DistributedPubSubMediator.Publish
import shortenurl.actor.Folders
import spray.http.StatusCodes
import spray.routing.RequestContext
import spray.routing.directives.DetachMagnet

import scala.concurrent.duration._

private[shortenurl] case class ListFolders(token: String)
case class Folder(id: Long, title: String)

trait FolderService extends ShortenerService {

  val folderRoute = {
    path("folder") {
      get {
        entity(as[ListFolders]) { listFolders: ListFolders =>
            detach(DetachMagnet.fromUnit()) { ctx =>
              val replyTo = actorRefFactory.actorOf(Props(classOf[FolderServiceCtxHandler], ctx))
              mediator ! Publish(`linkRepoTopic`, shortenurl.actor.ListFolders(listFolders.token, replyTo))
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
          detach(DetachMagnet.fromUnit()) { ctx =>
            val replyTo = actorRefFactory.actorOf(Props(classOf[FolderServiceCtxHandler], ctx))
            mediator ! Publish(`linkRepoTopic`, shortenurl.actor.ListLinks(listLinks.token,
                               Some(folderId), listLinks.offset, listLinks.limit, replyTo))
          }
        }
      }
    }
  }
}

class FolderServiceCtxHandler(override val ctx: RequestContext) extends ServiceCtxHandler(ctx) {
  override def receive = super.receive orElse {
    case Folders(folders) =>
      context.setReceiveTimeout(Duration.Undefined)
      ctx.complete(folders.map(folder => Folder(folder.id, folder.title)))
      context.stop(self)

    case Right(list: List[_]) =>
      context.setReceiveTimeout(Duration.Undefined)
      ctx.complete(list)
      context.stop(self)
  }
}
