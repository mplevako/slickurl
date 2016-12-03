package slickurl.service

import akka.actor.Props
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import slickurl.domain.model.User
import spray.routing.{RequestContext, Route}
import spray.routing.directives.DetachMagnet

import scala.concurrent.duration._
import slickurl.AppConfig._

private[slickurl] case class GetUser(user_id: Int, secret: String)

trait UserService extends ShortenerService {

  val userRoute: Route = {
    path("token") {
      get {
        entity(as[GetUser]) { getUser: GetUser =>
            detach(DetachMagnet.fromUnit(())) { ctx =>
                val replyTo = actorRefFactory.actorOf(Props(classOf[UserServiceCtxHandler], ctx))
                mediator ! Publish(userRepoTopic, slickurl.actor.GetUser(getUser.user_id, getUser.secret, replyTo))
            }
        }
      }
    }
  }
}  

class UserServiceCtxHandler(override val ctx: RequestContext) extends ServiceCtxHandler(ctx) {

  override def receive: Receive = super.receive orElse {
    case Right(User(_, token)) =>
      context.setReceiveTimeout(Duration.Undefined)
      ctx.complete(token)
      context.stop(self)
  }
}