/**
 * Copyright 2014 Maxim Plevako
 **/
package shortenurl.actor

import java.util.Date

import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.{Publish, Subscribe}
import akka.testkit.{TestActorRef, TestProbe}
import com.typesafe.config.ConfigFactory
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import org.specs2.time.NoTimeConversions
import shortenurl.domain.model._
import shortenurl.domain.repository.LinkRepositoryComponent

import scala.concurrent.duration._

class LinkRepoSpec extends Specification with NoTimeConversions with Mockito {

  sequential

  "LinkRepo" should {
    "ask the user repository for a user with the given token" in new AkkaTestkitSpecs2Support with Mocks {
      val repoTestProbe = TestProbe()
      val linkRepo = repoTestProbe.ref
      val mediator = DistributedPubSubExtension(system).mediator

      mediator ! Subscribe(linkRepoTopic, linkRepo)

      val token = "token"
      val replyToTestProbe = TestProbe()
      val replyTo = replyToTestProbe.ref
      val msg = ShortenLink(token, "url", None, None, replyTo)
      mediator ! Publish(linkRepoTopic, msg)
      repoTestProbe.expectMsg(msg)
    }

    "return InvalidToken for a non-existent token" in new AkkaTestkitSpecs2Support with Mocks {
      val linkRepo = TestActorRef(new LinkRepoImpl)

      val replyToTestProbe = TestProbe()
      val replyTo = replyToTestProbe.ref
      linkRepo ! UserForToken(None, Some(ListFolders("token", replyTo)))
      replyToTestProbe.expectMsg(Error(ErrorCode.InvalidToken))
    }

    "return a non-empty folders list" in new AkkaTestkitSpecs2Support with Mocks {
      val folders = List(Folder(1, existentUser.id, "folder"))
      repoMock.listFolders(existentUser.id) returns folders
      val linkRepo = TestActorRef(new LinkRepoImpl)

      val replyToTestProbe = TestProbe()
      val replyTo = replyToTestProbe.ref
      linkRepo ! UserForToken(Some(existentUser), Some(ListFolders("token", replyTo)))
      replyToTestProbe.expectMsg(10 seconds, Folders(folders))
    }

    "return a non-empty links list" in new AkkaTestkitSpecs2Support with Mocks {
      val links = List(existentLink)
      repoMock.listLinks(existentUser.id, Some(1L), None, None) returns Right(links)
      val linkRepo = TestActorRef(new LinkRepoImpl)

      val replyToTestProbe = TestProbe()
      val replyTo = replyToTestProbe.ref
      linkRepo ! UserForToken(Some(existentUser), Some(ListLinks("token", Some(1L), None, None, replyTo)))
      replyToTestProbe.expectMsg(10 seconds, Right(List(UrlCode(existentLink.url, existentLink.code))))
    }

    "return a non-empty clicks list" in new AkkaTestkitSpecs2Support with Mocks {
      val clicks = List(existentClick)
      repoMock.listClicks(existentClick.code, existentUser.id, None, None) returns Right(clicks)
      val linkRepo = TestActorRef(new LinkRepoImpl)

      val replyToTestProbe = TestProbe()
      val replyTo = replyToTestProbe.ref
      linkRepo ! UserForToken(Some(existentUser), Some(ListClicks(existentClick.code, "token", None, None, replyTo)))
      replyToTestProbe.expectMsg(10 seconds, Right(List(Clck(existentClick.date, existentClick.referer, existentClick.remote_ip))))
    }

    "return a shorten link if token is valid" in new AkkaTestkitSpecs2Support with Mocks {
      val link = Link(existentUser.id, "url", None, None)
      repoMock.shortenUrl(link) returns Right(link)
      val linkRepo = TestActorRef(new LinkRepoImpl)

      val replyToTestProbe = TestProbe()
      val replyTo = replyToTestProbe.ref
      val shortenLink = ShortenLink("token", "url", None, None, replyTo)
      linkRepo ! UserForToken(Some(existentUser), Some(shortenLink))
      replyToTestProbe.expectMsg(10 seconds, Right(link))
      there was one(repoMock).shortenUrl(link)
    }

    "return link summary" in new AkkaTestkitSpecs2Support with Mocks {
      val linkSummary = LinkSummary(existentLink.url, existentLink.code.get, existentLink.folderId, 1L)
      repoMock.linkSummary(existentLink.code.get, existentLink.uid) returns Right(linkSummary)
      val linkRepo = TestActorRef(new LinkRepoImpl)

      val replyToTestProbe = TestProbe()
      val replyTo = replyToTestProbe.ref
      linkRepo ! UserForToken(Some(existentUser), Some(GetLinkSummary(existentLink.code.get, "token", replyTo)))
      replyToTestProbe.expectMsg(10 seconds, Right(linkSummary))
      there was one(repoMock).linkSummary(existentLink.code.get, existentLink.uid)
    }
  }

  trait Mocks extends Scope {
    val repoMock: LinkRepositoryComponent#LinkRepository = mock[LinkRepositoryComponent#LinkRepository]

    class LinkRepoImpl extends LinkRepo {
      override val linkRepository: LinkRepositoryComponent#LinkRepository = repoMock
    }
  }

  val linkRepoTopic = ConfigFactory.load().getString("link.repo.topic")
  val existentUser: User = User(1L, "cafebabe")
  val existentLink: Link = Link(1L, "https://www.google.com", Some("yeah"), Some(1L))
  val existentClick: Click = Click(existentLink.code.get, new Date(), "referer", "127.0.0.1")
}
