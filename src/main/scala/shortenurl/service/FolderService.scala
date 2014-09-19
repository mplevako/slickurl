package shortenurl.service

import akka.actor.Props
import akka.contrib.pattern.DistributedPubSubMediator.Publish
import org.json4s.{DefaultFormats, Formats}
import shortenurl.actor.{Folders, Links}
import spray.httpx.Json4sSupport
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

class FolderServiceCtxHandler(override val ctx: RequestContext) extends ServiceCtxHandler(ctx)
                                                                        with Json4sSupport {
  override implicit val json4sFormats: Formats = DefaultFormats

  override def receive = super.receive orElse {
    case Folders(folders) =>
      context.setReceiveTimeout(Duration.Undefined)
      ctx.complete(folders.map(folder => Folder(folder.id, folder.title)))
      context.stop(self)

    case Links(links) =>
      context.setReceiveTimeout(Duration.Undefined)
      ctx.complete(links)
      context.stop(self)
  }
}
