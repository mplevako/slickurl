package shortenurl.actor

import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.{Publish, Subscribe}
import akka.testkit.{TestActorRef, TestProbe}
import com.typesafe.config.ConfigFactory
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import org.specs2.time.NoTimeConversions
import shortenurl.domain.model.{Link, User}
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
      linkRepo ! UserForToken(None, replyTo, None)
      replyToTestProbe.expectMsg(InvalidToken)
    }

    "return a shorten link if token is valid" in new AkkaTestkitSpecs2Support with Mocks {
      val link = Link(existentUser.id, "url", None, None)
      repoMock.shortenUrl(link) returns Right(link)
      val linkRepo = TestActorRef(new LinkRepoImpl)

      val replyToTestProbe = TestProbe()
      val replyTo = replyToTestProbe.ref
      linkRepo ! UserForToken(Some(existentUser), replyTo, Some(link))
      replyToTestProbe.expectMsg(5 seconds, Right(link))
      there was one(repoMock).shortenUrl(link)
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
}
