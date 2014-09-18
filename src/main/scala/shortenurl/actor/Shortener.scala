package shortenurl.actor

import akka.actor.ActorRef
import shortenurl.service.{ShortenerService, UserService}
import spray.httpx.Json4sSupport
import spray.routing.HttpServiceActor

class Shortener(override val mediator: ActorRef) extends HttpServiceActor with ShortenerService with UserService with Json4sSupport {

  override def receive: Receive = runRoute(
    pathPrefix("api") {
      rejectUserRoute ~ userRoute
    })
}
