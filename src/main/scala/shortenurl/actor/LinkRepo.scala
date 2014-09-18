package shortenurl.actor

import akka.actor.{Actor, IndirectActorProducer}
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.{Publish, Subscribe, SubscribeAck}
import shortenurl.domain.model.{Error, ErrorCode, Link}
import shortenurl.domain.repository.LinkRepositoryComponent

trait LinkRepo extends Actor {
  val userRepoTopic = context.system.settings.config.getString("user.repo.topic")
  val linkRepoTopic = context.system.settings.config.getString("link.repo.topic")

  val linkRepository: LinkRepositoryComponent#LinkRepository

  val mediator = DistributedPubSubExtension(context.system).mediator
  mediator ! Subscribe(`linkRepoTopic`, self)

  def receive = {
    case SubscribeAck(Subscribe(`linkRepoTopic`, None, `self`)) => context become ready
  }

  def ready: Actor.Receive = {
    case ShortenLink(token, url, code, folderId, replyTo) =>
      val payload = Some(Link(-1L, url, code, folderId))
      mediator ! Publish(`userRepoTopic`, GetUserWithToken(token, replyTo, Some(self), payload))
    case UserForToken(user, replyTo, payLoad) => user match {
      case None                => replyTo ! Error(ErrorCode.InvalidToken)
      case Some(userWithToken) =>
        val shortLink = payLoad.get.asInstanceOf[Link].copy(uid = userWithToken.id)
        replyTo ! linkRepository.shortenUrl(shortLink)
    }
  }
}

class LinkRepoFactory(val linkRepo: LinkRepositoryComponent#LinkRepository) extends IndirectActorProducer {

  override def actorClass = classOf[LinkRepo]

  override def produce(): Actor = new LinkRepo {
    override val linkRepository: LinkRepositoryComponent#LinkRepository = linkRepo
  }
}