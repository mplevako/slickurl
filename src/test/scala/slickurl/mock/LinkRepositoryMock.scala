package slickurl.mock

import java.util.Date

import akka.actor.ActorSystem
import akka.testkit.TestActorRef
import org.specs2.mock.Mockito
import org.specs2.specification.Scope
import slickurl.actor.LinkRepo
import slickurl.domain.model._
import slickurl.domain.repository.LinkRepositoryComponent

import scala.concurrent.Future

trait LinkRepositoryMock extends Scope with Mockito {
  protected val repositoryMock: LinkRepositoryComponent#LinkRepository = mock[LinkRepositoryComponent#LinkRepository]

  protected val mockURL        = "mockURL"
  protected val mockCode       = AlphabetCodec.encode(32L)
  protected val mockFolderTitle     = "folder"
  protected val mockFolderId   = 4l
  protected val mockClickCount = 16L
  protected val mockClick      = Click(mockCode, new Date(0L), Option("referrer"), Option("127.0.0.1"))

  class LinkRepoImpl extends LinkRepo {
    override val linkRepository: LinkRepositoryComponent#LinkRepository = repositoryMock
  }

  protected def shortenUrlMock(uid: UserID)(implicit system: ActorSystem): TestActorRef[LinkRepoImpl] = {
    val repo = TestActorRef(new LinkRepoImpl)
    implicit val ec = repo.dispatcher
    val mockLink = Link(uid, mockURL, mockCode, Option(mockFolderId))
    repositoryMock.shortenUrl(uid, mockURL, Option(mockFolderId)) returns Future.successful(Right(mockLink))
    repo
  }

  protected def listLinksMock(uid: UserID, folderId: Option[Long] = None)(implicit system: ActorSystem): TestActorRef[LinkRepoImpl] = {
    val repo = TestActorRef(new LinkRepoImpl)
    implicit val ec = repo.dispatcher
    val mockLinks = Seq(Link(uid, mockURL, mockCode, Option(mockFolderId)))
    repositoryMock.listLinks(uid, folderId, None, None) returns Future.successful(Right(mockLinks))
    repo
  }

  protected def linkSummaryMock(uid: UserID)(implicit system: ActorSystem): TestActorRef[LinkRepoImpl] = {
    val repo = TestActorRef(new LinkRepoImpl)
    implicit val ec = repo.dispatcher
    val linkSummary = LinkSummary(mockURL, mockCode, Option(mockFolderId), mockClickCount)
    repositoryMock.linkSummary(uid, mockCode) returns Future.successful(Right(linkSummary))
    repo
  }

  protected def listClicksMock(uid: UserID)(implicit system: ActorSystem): TestActorRef[LinkRepoImpl] = {
    val repo = TestActorRef(new LinkRepoImpl)
    implicit val ec = repo.dispatcher
    repositoryMock.listClicks(uid, mockCode, None, None) returns Future.successful(Right(Seq(mockClick)))
    repo
  }

  protected def listFoldersMock(uid: UserID)(implicit system: ActorSystem): TestActorRef[LinkRepoImpl] = {
    val repo = TestActorRef(new LinkRepoImpl)
    implicit val ec = repo.dispatcher
    val folder = Folder(mockFolderId, uid, mockFolderTitle)
    repositoryMock.listFolders(uid) returns Future.successful(Seq(folder))
    repo
  }
}
