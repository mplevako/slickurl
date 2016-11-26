package shortenurl.actor

import akka.actor.Actor
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Subscribe, SubscribeAck}
import akka.pattern._
import shortenurl.domain.model.{Error, ErrorCode}
import shortenurl.domain.repository.UserRepositoryComponent

trait UserRepo extends Actor {
  implicit private val ec = context.dispatcher

  val userRepository: UserRepositoryComponent#UserRepository
  private val mediator = DistributedPubSub(context.system).mediator

  private val userRepoTopic = context.system.settings.config.getString("user.repo.topic")
  private def secret = context.system.settings.config.getString("api.secret")

  mediator ! Subscribe(userRepoTopic, self)

  def receive: Receive = {
    case SubscribeAck(Subscribe(`userRepoTopic`, None, `self`)) => context become ready
  }

  def ready: Receive = {
    case GetUser(userId, secret, replyTo) if secret == this.secret =>
      userRepository.getUser(userId) pipeTo replyTo

    case GetUser(_, secret, replyTo) if secret != this.secret =>
      replyTo ! Error(ErrorCode.WrongSecret)

    case GetUserWithToken(token, replyTo, payLoad) =>
      val userForToken = userRepository.userForToken(token) map(UserForToken(_, payLoad))
      userForToken pipeTo replyTo
  }
}