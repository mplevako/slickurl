package slickurl.actor

import akka.actor.Props
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Subscribe, SubscribeAck}
import akka.pattern._
import slick.driver.JdbcProfile
import slickurl.domain.repository.{UserRepositoryComponent, UserTable}
import slickurl.AppProps._
import slickurl.actor.shard.Shard

object UserRepo {
  def apply(shardId: Long, profile: JdbcProfile)(db: profile.api.Database): Props =
    Props(classOf[UserRepo], shardId, profile, db)
}

class UserRepo(override val shardId: Long, override val profile: JdbcProfile,
               override val db: JdbcProfile#Backend#Database)
extends Shard with UserRepositoryComponent with UserTable {
  implicit private val ec = context.dispatcher

  override protected def userRepository: UserRepository = new UserRepositoryImpl
  private val mediator = DistributedPubSub(context.system).mediator

  mediator ! Subscribe(tokenTopic, tokenGroup, self)

  def receive: Receive = {
    case SubscribeAck(Subscribe(`tokenTopic`, `tokenGroup`, `self`)) => context become ready
  }

  def ready: Receive = {
    case CreateNewUser =>
      userRepository.createNewUser(shardId) pipeTo sender()
  }
}