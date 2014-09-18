package shortenurl.actor

import akka.actor.{Actor, ActorRef, IndirectActorProducer}
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.{Subscribe, SubscribeAck}
import shortenurl.domain.model.User
import shortenurl.domain.repository.UserRepositoryComponent

private[shortenurl] case class GetUser(userId: Long, secret: String, replyTo: ActorRef)

private[shortenurl] case class GetUserWithToken(token: String, replyTo: ActorRef, replyVia: Option[ActorRef])
private[shortenurl] case class UserForToken(uid: Option[User], replyTo: ActorRef)

trait UserRepo extends Actor {
  val userRepoTopic = context.system.settings.config.getString("user.repo.topic")
  def secret  = context.system.settings.config.getString("api.secret")
  val userRepository: UserRepositoryComponent#UserRepository

  val mediator = DistributedPubSubExtension(context.system).mediator
  mediator ! Subscribe(`userRepoTopic`, self)

  def receive = {
    case SubscribeAck(Subscribe(`userRepoTopic`, None, `self`)) => context become ready
  }

  def ready: Actor.Receive = {
    case GetUser(userId, secret, replyTo) if secret == this.secret => replyTo ! userRepository.getUser(userId)
    case GetUserWithToken(token, replyTo, replyVia) =>
      replyVia.getOrElse(replyTo) ! UserForToken(userRepository.userForToken(token), replyTo)
  }
}

class UserRepoFactory(val userRepo: UserRepositoryComponent#UserRepository) extends IndirectActorProducer {
  override def actorClass = classOf[UserRepo]
  override def produce(): Actor = new UserRepo {
    override val userRepository: UserRepositoryComponent#UserRepository = userRepo
  }
}
