/**
 * Copyright 2014 Maxim Plevako
 **/
package shortenurl.service

import akka.actor.Props
import akka.contrib.pattern.DistributedPubSubMediator.Publish
import shortenurl.domain.model.User
import spray.routing.RequestContext
import spray.routing.directives.DetachMagnet

import scala.concurrent.duration._

private[shortenurl] case class GetUser(user_id: Int, secret: String)

trait UserService extends ShortenerService {

  val userRoute = {
    path("token") {
      get {
        entity(as[GetUser]) { getUser: GetUser =>
            detach(DetachMagnet.fromUnit()) { ctx =>
                val replyTo = actorRefFactory.actorOf(Props(classOf[UserServiceCtxHandler], ctx))
                mediator ! Publish(`userRepoTopic`, shortenurl.actor.GetUser(getUser.user_id, getUser.secret, replyTo))
            }
        }
      }
    }
  }
}  

class UserServiceCtxHandler(override val ctx: RequestContext) extends ServiceCtxHandler(ctx) {

  override def receive = super.receive orElse {
    case User(id, token) =>
      context.setReceiveTimeout(Duration.Undefined)
      ctx.complete(token)
      context.stop(self)
  }
}
