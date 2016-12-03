package slickurl.actor

import akka.actor.ActorRef
import slickurl.service.{FolderService, LinkService, ShortenerService, UserService}
import spray.httpx.Json4sSupport
import spray.routing.HttpServiceActor

class Shortener(override val mediator: ActorRef) extends HttpServiceActor with ShortenerService
                                                         with UserService with FolderService
                                                         with LinkService with Json4sSupport {

  override def receive: Receive = runRoute(
    pathPrefix("api") {
      userRoute ~ folderRoute ~ linkRoute
    })
}
