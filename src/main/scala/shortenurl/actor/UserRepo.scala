package shortenurl.actor

import akka.actor.{Actor, ActorRef, IndirectActorProducer}
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.{Subscribe, SubscribeAck}
import shortenurl.domain.repository.UserRepositoryComponent

case class GetUser(userId: Int, secret: String, replyTo: ActorRef)

trait UserRepo extends Actor {
  def secret  = context.system.settings.config.getString("api.secret")
  val userRepository: UserRepositoryComponent#UserRepository

  val mediator = DistributedPubSubExtension(context.system).mediator
  mediator ! Subscribe("user-repo", self)

  def receive = {
    case SubscribeAck(Subscribe("user-repo", None, `self`)) => context become ready
  }

  def ready: Actor.Receive = {
    case GetUser(userId, secret, replyTo) if secret == this.secret => replyTo ! userRepository.getUser(userId)
  }
}

class UserRepoFactory(val userRepo: UserRepositoryComponent#UserRepository) extends IndirectActorProducer {
  override def actorClass = classOf[UserRepo]
  override def produce(): Actor = new UserRepo {
    override val userRepository: UserRepositoryComponent#UserRepository = userRepo
  }
}
