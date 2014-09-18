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
    case cmd@ShortenLink(token, _, _, _, replyTo) =>
      mediator ! Publish(`userRepoTopic`, GetUserWithToken(token, replyTo, Some(self), Some(cmd)))
    case cmd@ListLinks(token, _, _, _, replyTo) =>
      mediator ! Publish(`userRepoTopic`, GetUserWithToken(token, replyTo, Some(self), Some(cmd)))
    case UserForToken(user, replyTo, cmd) =>
      user match {
          case None => replyTo ! Error(ErrorCode.InvalidToken)
          case Some(userWithToken) => cmd match {
              case Some(ShortenLink(_,url,code,fid,_)) =>
                val link = Link(userWithToken.id, url, code, fid)
                replyTo ! linkRepository.shortenUrl(link)
              case Some(ListLinks(_,fid,offset,limit,_)) =>
                replyTo ! Links(linkRepository.listLinks(userWithToken.id, fid, offset, limit))
              case _ => replyTo ! Error(ErrorCode.Unknown)
          }
    }
  }
}

class LinkRepoFactory(val linkRepo: LinkRepositoryComponent#LinkRepository) extends IndirectActorProducer {

  override def actorClass = classOf[LinkRepo]

  override def produce(): Actor = new LinkRepo {
    override val linkRepository: LinkRepositoryComponent#LinkRepository = linkRepo
  }
}