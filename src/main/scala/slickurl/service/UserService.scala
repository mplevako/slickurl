package slickurl.service

import akka.actor.Props
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import slickurl.AppProps._
import slickurl.JWTUtils
import slickurl.domain.model.UserID
import spray.http._
import spray.routing.directives.DetachMagnet
import spray.routing.{RequestContext, Route}

import scala.concurrent.duration._

trait UserService extends ShortenerService {
  val userRoute: Route = {
    path("token") {
      post {
        shardAndUserID { _ =>
          detach(DetachMagnet.fromUnit(())) { ctx =>
            implicit val replyTo = actorRefFactory.actorOf(Props(classOf[UserServiceCtxHandler], ctx))
            mediator ! Publish(tokenTopic, slickurl.actor.CreateNewUser, sendOneMessageToEachGroup = true)
          }
        }
      }
    }
  }
}

class UserServiceCtxHandler(override val ctx: RequestContext) extends ServiceCtxHandler(ctx) {

  override def receive: Receive = super.receive orElse {
    case Right(UserID(userId)) =>
      context.setReceiveTimeout(Duration.Undefined)
      ctx.complete(HttpResponse(entity = HttpEntity(JWTUtils.tokenForSubject(userId))).withHeaders(
                                new HttpHeaders.`Content-Type`(ContentTypes.`text/plain`)))
      context.stop(self)
  }
}
