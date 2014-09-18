package shortenurl.actor

import akka.actor.{Actor, IndirectActorProducer}
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.{Publish, Subscribe, SubscribeAck}
import shortenurl.domain.repository.FolderRepositoryComponent

trait FolderRepo extends Actor {
  val folderRepoTopic = context.system.settings.config.getString("folder.repo.topic")
  val userRepoTopic = context.system.settings.config.getString("user.repo.topic")
  val folderRepository: FolderRepositoryComponent#FolderRepository

  val mediator = DistributedPubSubExtension(context.system).mediator
  mediator ! Subscribe(`folderRepoTopic`, self)

  def receive = {
    case SubscribeAck(Subscribe(`folderRepoTopic`, None, `self`)) => context become ready
  }

  def ready: Actor.Receive = {
    case ListFolders(token, replyTo)    => mediator ! Publish(`userRepoTopic`, GetUserWithToken(token, replyTo, Some(self), None))
    case UserForToken(user, replyTo, _) => user match {
        case None                => replyTo ! InvalidToken
        case Some(userWithToken) => replyTo ! Folders(folderRepository.listFolders(userWithToken.id))
    }
  }
}

class FolderRepoFactory(val folderRepo: FolderRepositoryComponent#FolderRepository) extends IndirectActorProducer {
  override def actorClass = classOf[FolderRepo]
  override def produce(): Actor = new FolderRepo {
    override val folderRepository: FolderRepositoryComponent#FolderRepository = folderRepo
  }
}
