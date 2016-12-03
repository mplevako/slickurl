package slickurl.service

import akka.actor.Props
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import slickurl.AppConfig._
import slickurl.domain.model.User
import spray.routing.AuthenticationFailedRejection.{CredentialsMissing, CredentialsRejected}
import spray.routing.directives.DetachMagnet
import spray.routing.{AuthenticationFailedRejection, RequestContext, Route}

import scala.concurrent.duration._

private[slickurl] case class GetUser(user_id: Int)

trait UserService extends ShortenerService {
  protected val SecretTokenHeader = "X-Secret-Token"

  val userRoute: Route = {
    path("token") {
      post {
        authenticatedRoute { secret =>
          detach(DetachMagnet.fromUnit(())) { ctx =>
            val replyTo = actorRefFactory.actorOf(Props(classOf[UserServiceCtxHandler], ctx))
            mediator ! Publish(userRepoTopic, slickurl.actor.CreateNewUser(secret, replyTo))
          }
        }
      } ~
      get {
        authenticatedRoute { secret =>
          entity(as[GetUser]) { getUser: GetUser =>
            detach(DetachMagnet.fromUnit(())) { ctx =>
              val replyTo = actorRefFactory.actorOf(Props(classOf[UserServiceCtxHandler], ctx))
              mediator ! Publish(userRepoTopic, slickurl.actor.GetUser(getUser.user_id, secret, replyTo))
            }
          }
        }
      }
    }
  }

  private def authenticatedRoute(secretRoute: String => Route): Route = {
    headerValueByName(SecretTokenHeader) { secret =>
      if (secret == null || secret.isEmpty) reject(AuthenticationFailedRejection(CredentialsMissing, List.empty))
      else if (secret != apiSecret)
        reject(AuthenticationFailedRejection(CredentialsRejected, List.empty))
      else secretRoute(secret)
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
