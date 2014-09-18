package shortenurl.actor

import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.{Publish, Subscribe}
import akka.testkit.{TestActorRef, TestProbe}
import com.typesafe.config.ConfigFactory
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import org.specs2.time.NoTimeConversions
import shortenurl.domain.model.{Folder, User}
import shortenurl.domain.repository.FolderRepositoryComponent

import scala.concurrent.duration._

class FolderRepoSpec extends Specification with NoTimeConversions with Mockito {

  sequential

  "FolderRepo" should {
    "ask the user repository for a user with the given token" in new AkkaTestkitSpecs2Support with Mocks {
      val repoTestProbe = TestProbe()
      val folderRepo = repoTestProbe.ref
      val mediator = DistributedPubSubExtension(system).mediator

      mediator ! Subscribe(`folderRepoTopic`, folderRepo)

      val token = "token"
      val replyToTestProbe = TestProbe()
      val replyTo = replyToTestProbe.ref
      mediator ! Publish(`folderRepoTopic`, ListFolders(token, replyTo))
      repoTestProbe.expectMsg(ListFolders(token, replyTo))
    }

    "return InvalidToken for a non-existent token" in new AkkaTestkitSpecs2Support with Mocks {
      val folderRepo = TestActorRef(new FolderRepoImpl)

      val replyToTestProbe = TestProbe()
      val replyTo = replyToTestProbe.ref
      folderRepo ! UserForToken(None, replyTo, None)
      replyToTestProbe.expectMsg(InvalidToken)
    }

    "return a non-empty list for an existent token" in new AkkaTestkitSpecs2Support with Mocks {
      val folders = List(Folder(1, existentUser.id, "folder"))
      repoMock.listFolders(existentUser.id) returns folders
      val folderRepo = TestActorRef(new FolderRepoImpl)

      val replyToTestProbe = TestProbe()
      val replyTo = replyToTestProbe.ref
      folderRepo ! UserForToken(Some(existentUser), replyTo, None)
      replyToTestProbe.expectMsg(5 seconds, Folders(folders))
    }
  }

  trait Mocks extends Scope {
    val repoMock: FolderRepositoryComponent#FolderRepository = mock[FolderRepositoryComponent#FolderRepository]

    class FolderRepoImpl extends FolderRepo {
      override val folderRepository: FolderRepositoryComponent#FolderRepository = repoMock
    }
  }

  val folderRepoTopic = ConfigFactory.load().getString("folder.repo.topic")
  val existentUser: User = User(1L, "cafebabe")
}
