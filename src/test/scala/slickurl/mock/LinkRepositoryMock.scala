package slickurl.mock

import java.util.Date

import akka.actor.ActorSystem
import akka.testkit.TestActorRef
import org.specs2.mock.Mockito
import org.specs2.specification.Scope
import slickurl.actor.LinkRepo
import slickurl.domain.model._

import scala.concurrent.Future

trait LinkRepositoryMock extends Scope with Mockito {
  def mockShardId: Long
  def system: ActorSystem

  protected val mockCode        = AlphabetCodec.packAndEncode(mockShardId)(32L)
  protected val mockURL         = "mockURL"
  protected val mockFolderTitle = "folder"
  protected val mockFolderId    = 4l
  protected val mockClickCount  = 16L
  protected val mockReferrer    = "referrer"
  protected val mockRemoteIP    = "127.0.0.1"
  protected val mockClick       = Click(mockCode, new Date(0L), Option(mockReferrer), Option(mockRemoteIP))

  class LinkRepoImpl extends LinkRepo(mockShardId, null, null) {
    override val linkRepository: LinkRepository = mock[LinkRepository]
  }

  protected def shortenUrlMock(uid: UserID)(implicit system: ActorSystem): TestActorRef[LinkRepoImpl] = {
    val repo = TestActorRef(new LinkRepoImpl)
    implicit val ec = repo.dispatcher
    val mockLink = Link(uid, mockURL, mockCode, Option(mockFolderId))
    repo.underlyingActor.linkRepository.shortenUrl(
      mockShardId, uid, mockURL, Option(mockFolderId)
    ).returns(
      Future.successful(Right(mockLink))
    )
    repo
  }

  protected def listLinksMock(uid: UserID, folderId: Option[Long] = None)(implicit system: ActorSystem): TestActorRef[LinkRepoImpl] = {
    val repo = TestActorRef(new LinkRepoImpl)
    implicit val ec = repo.dispatcher
    val mockLinks = Seq(Link(uid, mockURL, mockCode, Option(mockFolderId)))
    repo.underlyingActor.linkRepository.listLinks(
      uid, folderId, None, None
    ).returns(
      Future.successful(Right(mockLinks))
    )
    repo
  }

  protected def linkSummaryMock(uid: UserID)(implicit system: ActorSystem): TestActorRef[LinkRepoImpl] = {
    val repo = TestActorRef(new LinkRepoImpl)
    implicit val ec = repo.dispatcher
    val linkSummary = LinkSummary(mockURL, mockCode, Option(mockFolderId), mockClickCount)
    repo.underlyingActor.linkRepository.linkSummary(uid, mockCode) returns Future.successful(Right(linkSummary))
    repo
  }

  protected def listClicksMock(uid: UserID)(implicit system: ActorSystem): TestActorRef[LinkRepoImpl] = {
    val repo = TestActorRef(new LinkRepoImpl)
    implicit val ec = repo.dispatcher
    repo.underlyingActor.linkRepository.listClicks(
      uid, mockCode, None, None
    ).returns(
      Future.successful(Right(Seq(mockClick)))
    )
    repo
  }

  protected def listFoldersMock(uid: UserID)(implicit system: ActorSystem): TestActorRef[LinkRepoImpl] = {
    val repo = TestActorRef(new LinkRepoImpl)
    implicit val ec = repo.dispatcher
    val folder = Folder(mockFolderId, uid, mockFolderTitle)
    repo.underlyingActor.linkRepository.listFolders(uid) returns Future.successful(Seq(folder))
    repo
  }

  protected def passThroughMock()(implicit system: ActorSystem): TestActorRef[LinkRepoImpl] = {
    val repo = TestActorRef(new LinkRepoImpl)
    implicit val ec = repo.dispatcher
    repo.underlyingActor.linkRepository.passThrough(
      mockCode, Option(mockReferrer), Option(mockRemoteIP)
    ).returns(
      Future.successful(Right(mockURL))
    )
    repo
  }
}
