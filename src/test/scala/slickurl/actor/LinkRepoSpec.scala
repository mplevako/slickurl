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
      private val repoMock = listFoldersMock(uid)

      repoMock ! ListFolders(uid)
      private val folder = Folder(mockFolderId, uid, mockFolderTitle)
      expectMsg(10 seconds, Folders(Seq(folder)))
    }

    "return a non-empty links list" in new AkkaTestkitSpecs2Support with LinkRepositoryMock {
      private val repoMock = listLinksMock(uid)

      repoMock ! ListLinks(uid, None, None, None)
      private val urlCode = UrlCode(mockURL, mockCode)
      expectMsg(10 seconds, Right(Seq(urlCode)))
    }

    "return a non-empty clicks list" in new AkkaTestkitSpecs2Support with LinkRepositoryMock {
      private val repoMock = listClicksMock(uid)

      repoMock ! ListClicks(uid, mockClick.code, None, None)
      private val clck = Clck(mockClick.date, mockClick.referrer, mockClick.remote_ip)
      expectMsg(10 seconds, Right(Seq(clck)))
    }

    "return a shorten link" in new AkkaTestkitSpecs2Support with LinkRepositoryMock {
      private val repoMock = shortenUrlMock(uid)

      repoMock ! ShortenLink(uid, mockURL, Option(mockFolderId))
      private val link = Link(uid, mockURL, mockCode, Option(mockFolderId))
      expectMsg(10 seconds, Right(link))
      there was one(repositoryMock).shortenUrl(uid, mockURL, Option(mockFolderId))
    }

    "return link summary" in new AkkaTestkitSpecs2Support with LinkRepositoryMock {
      private val repoMock = linkSummaryMock(uid)

      repoMock ! GetLinkSummary(uid, mockCode)
      private val linkSummary = LinkSummary(mockURL, mockCode, Option(mockFolderId), mockClickCount)
      expectMsg(10 seconds, Right(linkSummary))
      there was one(repositoryMock).linkSummary(uid, mockCode)
    }
  }

  private val uid   = UserID("cafed00d")
}
