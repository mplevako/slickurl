package slickurl.actor

import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions
import slickurl.domain.model._
import slickurl.mock.LinkRepositoryMock

import scala.concurrent.duration._

class LinkRepoSpec extends Specification with NoTimeConversions {

  sequential

  "LinkRepo" should {
    "return a non-empty folders list" in new AkkaTestkitSpecs2Support with LinkRepositoryMock {
      override def mockShardId: Long = shardId
      private val repoMock = listFoldersMock(uid)
      repoMock ! ListFolders(shardId, UserID(uid.id))
      private val folder = Folder(mockFolderId, uid, mockFolderTitle)
      expectMsg(10 seconds, Folders(Seq(folder)))
    }

    "return a non-empty links list" in new AkkaTestkitSpecs2Support with LinkRepositoryMock {
      override def mockShardId: Long = shardId
      private val repoMock = listLinksMock(uid)
      repoMock ! ListLinks(shardId, uid, None, None, None)
      private val urlCode = UrlCode(mockURL, mockCode)
      expectMsg(10 seconds, Right(Seq(urlCode)))
    }

    "return a non-empty clicks list" in new AkkaTestkitSpecs2Support with LinkRepositoryMock {
      override def mockShardId: Long = shardId
      private val repoMock = listClicksMock(uid)
      repoMock ! ListClicks(mockShardId, uid, mockClick.code, None, None)
      private val clck = Clck(mockClick.date, mockClick.referrer, mockClick.remote_ip)
      expectMsg(10 seconds, Right(Seq(clck)))
    }

    "return a shorten link" in new AkkaTestkitSpecs2Support with LinkRepositoryMock {
      override def mockShardId: Long = shardId
      private val repoMock = shortenUrlMock(uid)
      private val repositoryMock = repoMock.underlyingActor.linkRepository
      repoMock ! ShortenLink(shardId, uid, mockURL, Option(mockFolderId))
      private val link = Link(uid, mockURL, mockCode, Option(mockFolderId))
      expectMsg(10 seconds, Right(link))
      there was one(repositoryMock).shortenUrl(shardId, uid, mockURL, Option(mockFolderId))
    }

    "return link summary" in new AkkaTestkitSpecs2Support with LinkRepositoryMock {
      override def mockShardId: Long = shardId
      private val repoMock = linkSummaryMock(uid)
      private val repositoryMock = repoMock.underlyingActor.linkRepository
      repoMock ! GetLinkSummary(shardId, uid, mockCode)
      private val linkSummary = LinkSummary(mockURL, mockCode, Option(mockFolderId), mockClickCount)
      expectMsg(10 seconds, Right(linkSummary))
      there was one(repositoryMock).linkSummary(uid, mockCode)
    }
  }

  protected def shardId: Long = 1L
  private val uid: UserID = UserID(AlphabetCodec.packAndEncode(shardId)(1L))
}
