package shortenurl.actor

import akka.actor.ActorRef
import shortenurl.service.{FolderService, LinkService, ShortenerService, UserService}
import spray.httpx.Json4sSupport
import spray.routing.HttpServiceActor

class Shortener(override val mediator: ActorRef) extends HttpServiceActor with ShortenerService
                                                         with UserService with FolderService
                                                         with LinkService with Json4sSupport {

  override def receive: Receive = runRoute(
    pathPrefix("api") {
      rejectUserRoute   ~ userRoute   ~
      rejectFolderRoute ~ folderRoute ~
      rejectLinkRoute   ~ linkRoute
    })
}
