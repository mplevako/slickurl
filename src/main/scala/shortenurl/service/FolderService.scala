package shortenurl.service

import akka.actor.{Actor, Props, ReceiveTimeout}
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.Publish
import com.typesafe.config.ConfigFactory
import org.json4s.{DefaultFormats, Formats}
import shortenurl.actor.Folders
import shortenurl.domain.model.Error
import spray.http.StatusCodes._
import spray.httpx.Json4sSupport
import spray.routing.RequestContext

import scala.concurrent.duration._

case class ListFolders(token: String)
case class Folder(id: Long, title: String)

trait FolderService extends ShortenerService {

  val folderRepoTopic = config.getString("folder.repo.topic")

  val rejectFolderRoute = path("folder") {
    (/*post | */put | delete | head | options | patch) (complete(MethodNotAllowed))
  }

  val folderRoute = {
    path("folder") {
      entity(as[ListFolders]) { listFolders: ListFolders =>
        post { ctx =>
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

    case Error(msg) =>
      context.setReceiveTimeout(Duration.Undefined)
      ctx.complete(msg)
      context.stop(self)

    case Folders(folders) =>
      context.setReceiveTimeout(Duration.Undefined)
      ctx.complete(folders.map(folder => Folder(folder.id, folder.title)))
      context.stop(self)
  }
}
