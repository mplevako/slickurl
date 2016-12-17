package slickurl.actor

import akka.actor.Actor
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Subscribe, SubscribeAck}
import akka.pattern._
import slickurl.domain.repository.UserRepositoryComponent
import slickurl.AppProps._

trait UserRepo extends Actor {
  implicit private val ec = context.dispatcher

  val userRepository: UserRepositoryComponent#UserRepository
  private val mediator = DistributedPubSub(context.system).mediator

  mediator ! Subscribe(tokenTopic, tokenGroup, self)

  def receive: Receive = {
    case SubscribeAck(Subscribe(`tokenTopic`, `tokenGroup`, `self`)) => context become ready
  }

  def ready: Receive = {
    case CreateNewUser =>
      userRepository.createNewUser() pipeTo sender()
  }
}