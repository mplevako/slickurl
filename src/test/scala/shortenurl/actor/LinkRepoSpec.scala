/**
 * Copyright 2014-2015 Maxim Plevako
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

import scala.concurrent.Future
import scala.concurrent.duration._

class LinkRepoSpec extends Specification with NoTimeConversions with Mockito {

  sequential

  "LinkRepo" should {
    "ask the user repository for a user with the given token" in new AkkaTestkitSpecs2Support with Mocks {
      val repoTestProbe = TestProbe()
      val linkRepo = repoTestProbe.ref
      val mediator = DistributedPubSubExtension(system).mediator

      mediator ! Subscribe(linkRepoTopic, linkRepo)

      val replyToTestProbe = TestProbe()
      val replyTo = replyToTestProbe.ref
      val msg = ShortenLink("token", "url", None, None, replyTo)
      mediator ! Publish(linkRepoTopic, msg)
      repoTestProbe.expectMsg(msg)
    }

    "return InvalidToken for a non-existent token" in new AkkaTestkitSpecs2Support with Mocks {
      val linkRepo = TestActorRef(new LinkRepoImpl)

      val replyToTestProbe = TestProbe()
      val replyTo = replyToTestProbe.ref
      linkRepo ! UserForToken(None, Option(ListFolders("token", replyTo)))
      replyToTestProbe.expectMsg(Error(ErrorCode.InvalidToken))
    }

    "return a non-empty folders list" in new AkkaTestkitSpecs2Support with Mocks {
      val linkRepo = TestActorRef(new LinkRepoImpl)
      implicit val ec = linkRepo.dispatcher
      val folders = Seq(Folder(1, existentUser.id, "folder"))
      repoMock.listFolders(existentUser.id) returns Future.successful(folders)

      val replyToTestProbe = TestProbe()
      val replyTo = replyToTestProbe.ref
      linkRepo ! UserForToken(Option(existentUser), Option(ListFolders("token", replyTo)))
      replyToTestProbe.expectMsg(10 seconds, Folders(folders))
    }

    "return a non-empty links list" in new AkkaTestkitSpecs2Support with Mocks {
      val linkRepo = TestActorRef(new LinkRepoImpl)
      implicit val ec = linkRepo.dispatcher
      val links = Seq(existentLink)
      repoMock.listLinks(existentUser.id, Option(1L), None, None) returns Future.successful(Right(links))

      val replyToTestProbe = TestProbe()
      val replyTo = replyToTestProbe.ref
      linkRepo ! UserForToken(Option(existentUser), Option(ListLinks("token", Option(1L), None, None, replyTo)))
      replyToTestProbe.expectMsg(10 seconds, Right(Seq(UrlCode(existentLink.url, existentLink.code))))
    }

    "return a non-empty clicks list" in new AkkaTestkitSpecs2Support with Mocks {
      val linkRepo = TestActorRef(new LinkRepoImpl)
      implicit val ec = linkRepo.dispatcher
      val clicks = Seq(existentClick)
      repoMock.listClicks(existentClick.code, existentUser.id, None, None) returns Future.successful(Right(clicks))

      val replyToTestProbe = TestProbe()
      val replyTo = replyToTestProbe.ref
      linkRepo ! UserForToken(Option(existentUser), Option(ListClicks(existentClick.code, "token", None, None, replyTo)))
      replyToTestProbe.expectMsg(10 seconds, Right(List(Clck(existentClick.date, existentClick.referer, existentClick.remote_ip))))
    }

    "return a shorten link if token is valid" in new AkkaTestkitSpecs2Support with Mocks {
      val linkRepo = TestActorRef(new LinkRepoImpl)
      implicit val ec = linkRepo.dispatcher
      val link = Link(existentUser.id, "url", None, None)
      repoMock.shortenUrl(link) returns Future.successful(Right(link))

      val replyToTestProbe = TestProbe()
      val replyTo = replyToTestProbe.ref
      val shortenLink = ShortenLink("token", "url", None, None, replyTo)
      linkRepo ! UserForToken(Option(existentUser), Option(shortenLink))
      replyToTestProbe.expectMsg(10 seconds, Right(link))
      there was one(repoMock).shortenUrl(link)
    }

    "return link summary" in new AkkaTestkitSpecs2Support with Mocks {
      val linkRepo = TestActorRef(new LinkRepoImpl)
      implicit val ec = linkRepo.dispatcher
      val linkSummary = LinkSummary(existentLink.url, existentLink.code.get, existentLink.folderId, 1L)
      repoMock.linkSummary(existentLink.code.get, existentLink.uid) returns Future.successful(Right(linkSummary))

      val replyToTestProbe = TestProbe()
      val replyTo = replyToTestProbe.ref
      linkRepo ! UserForToken(Option(existentUser), Option(GetLinkSummary(existentLink.code.get, "token", replyTo)))
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
  val existentLink: Link = Link(1L, "https://www.google.com", Option("yeah"), Option(1L))
  val existentClick: Click = Click(existentLink.code.get, new Date(), Option("referer"), Option("127.0.0.1"))
}
