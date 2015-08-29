/**
 * Copyright 2014-2015 Maxim Plevako
 **/
package shortenurl.actor

import akka.actor.Actor
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.{Subscribe, SubscribeAck}
import akka.pattern._
import shortenurl.domain.model.{Error, ErrorCode}
import shortenurl.domain.repository.UserRepositoryComponent

trait UserRepo extends Actor {
  implicit val ec = context.dispatcher

  val userRepository: UserRepositoryComponent#UserRepository
  val mediator = DistributedPubSubExtension(context.system).mediator

  private val userRepoTopic = context.system.settings.config.getString("user.repo.topic")
  private def secret = context.system.settings.config.getString("api.secret")

  mediator ! Subscribe(userRepoTopic, self)

  def receive = {
    case SubscribeAck(Subscribe(`userRepoTopic`, None, `self`)) => context become ready
  }

  def ready: Actor.Receive = {
    case GetUser(userId, secret, replyTo) if secret == this.secret =>
      userRepository.getUser(userId) pipeTo replyTo

    case GetUser(userId, secret, replyTo) if secret != this.secret =>
      replyTo ! Error(ErrorCode.WrongSecret)

    case GetUserWithToken(token, replyTo, payLoad) =>
      val userForToken = userRepository.userForToken(token) map(UserForToken(_, payLoad))
      userForToken pipeTo replyTo
  }
}