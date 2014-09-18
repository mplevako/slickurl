package shortenurl.service

import akka.actor.{Actor, Props, ReceiveTimeout}
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.Publish
import com.typesafe.config.ConfigFactory
import org.json4s.{DefaultFormats, Formats}
import spray.http.StatusCodes._
import spray.httpx.Json4sSupport
import spray.routing.RequestContext

import scala.concurrent.duration._

case class ListFolders(token: String)

case class Folder(id: Int, title: String)

trait FolderService extends ShortenerService {

  val folderRepoTopic = config.getString("folder.repo.topic")

  val rejectFolderRoute = path("folder") {
    (post | put | delete | head | options | patch) (complete(MethodNotAllowed))
  }

  val folderRoute = {
    path("folder") {
      entity(as[ListFolders]) { listFolders: ListFolders =>
        get { ctx =>
          val replyTo = actorRefFactory.actorOf(Props(classOf[FolderServiceCtxHandler], ctx))
          mediator ! Publish(`folderRepoTopic`, shortenurl.actor.ListFolders(listFolders.token, replyTo))
        }
      }
    }
  }
}

class FolderServiceCtxHandler(val ctx: RequestContext) extends Actor with Json4sSupport {
  context.setReceiveTimeout(ConfigFactory.load().getInt("app.http.handler.timeout") milliseconds)
  val mediator = DistributedPubSubExtension(context.system).mediator

  override implicit val json4sFormats: Formats = DefaultFormats

  def receive = {
    case ReceiveTimeout =>
      context.setReceiveTimeout(Duration.Undefined)
      context.stop(self)

    case folders: List[_] =>
      context.setReceiveTimeout(Duration.Undefined)
      ctx.complete(folders.map(f => {
        val folder = f.asInstanceOf[shortenurl.domain.model.Folder]
        Folder(folder.id, folder.title)
      }))
      context.stop(self)
  }
}
