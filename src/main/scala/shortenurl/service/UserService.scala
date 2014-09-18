package shortenurl.service

import akka.actor.{Actor, Props, ReceiveTimeout}
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.Publish
import com.typesafe.config.ConfigFactory
import shortenurl.domain.model.User
import spray.routing.RequestContext
import spray.routing.directives.DetachMagnet

import scala.concurrent.duration._

private[shortenurl] case class GetUser(user_id: Int, secret: String)

trait UserService extends ShortenerService {

  val userRepoTopic = config.getString("user.repo.topic")

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

class UserServiceCtxHandler(val ctx: RequestContext) extends Actor {
  context.setReceiveTimeout(ConfigFactory.load().getInt("app.http.handler.timeout") milliseconds)
  val mediator = DistributedPubSubExtension(context.system).mediator

  def receive = {
    case ReceiveTimeout =>
      context.setReceiveTimeout(Duration.Undefined)
      context.stop(self)

    case User(id, token) =>
      context.setReceiveTimeout(Duration.Undefined)
      ctx.complete(token)
      context.stop(self)
  }
}
