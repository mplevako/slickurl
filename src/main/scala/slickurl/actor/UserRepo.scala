package slickurl.actor

import akka.actor.Actor
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Subscribe, SubscribeAck}
import akka.pattern._
import slickurl.domain.model.{Error, ErrorCode}
import slickurl.domain.repository.UserRepositoryComponent
import slickurl.AppConfig._

trait UserRepo extends Actor {
  implicit private val ec = context.dispatcher

  val userRepository: UserRepositoryComponent#UserRepository
  private val mediator = DistributedPubSub(context.system).mediator

  mediator ! Subscribe(userRepoTopic, self)

  def receive: Receive = {
    case SubscribeAck(Subscribe(`userRepoTopic`, None, `self`)) => context become ready
  }

  def ready: Receive = {
    case GetUser(userId, secret, replyTo) if secret == apiSecret =>
      userRepository.getUser(userId) pipeTo replyTo

    case GetUser(_, secret, replyTo) if secret != apiSecret =>
      replyTo ! Error(ErrorCode.WrongSecret)

    case GetUserWithToken(token, replyTo, payLoad) =>
      val userForToken = userRepository.userForToken(token) map(UserForToken(_, payLoad))
      userForToken pipeTo replyTo
  }
}