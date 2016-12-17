package slickurl.domain.repository

import java.util.Date

import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAfterExample
import slick.driver.JdbcProfile
import slickurl.AppProps._
import slickurl.DbProps._
import slickurl.domain.model._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class LinkRepositorySpec extends Specification with BeforeAfterExample with LinkRepositoryComponent with LinkTable
                                               with FolderTable with ClickTable {

  sequential

  ".shortenUrl" should {
    "return a short link" in {
      linkRepository.shortenUrl(nonExistentLink.uid, nonExistentLink.url, nonExistentLink.folderId) map { result =>
        result must beRight[Link]
        result.right.get must_== nonExistentLink.copy(code = encodedCodeSeqStartVal)
      } await
    }
  }

  ".listLinks" should {
    "list all link for the given user if no folder id is specified" in {
      linkRepository.listLinks(iceBuddha, None, None, None) map { links =>
        links must beRight[Seq[Link]]
        links.right.get.size must_== 3
      } await

      linkRepository.listLinks(iceBuddha, None, Option(1L), None) map { ofsLinks =>
        ofsLinks must beRight[Seq[Link]]
        ofsLinks.right.get.size must_== 2
      } await

      linkRepository.listLinks(iceBuddha, None, Option(1L), Option(1L)) map { ofsLimLinks =>
        ofsLimLinks must beRight[Seq[Link]]
        ofsLimLinks.right.get.size must_== 1
        ofsLimLinks.right.get.head.code must_== "b"
      } await
    }

    "list all link for the given user and folder" in {
      linkRepository.listLinks(iceBuddha, Option(existentFolder.id), Option(1L), None) map { ofsLinks =>
        ofsLinks must beRight[Seq[Link]]
        ofsLinks.right.get.size must_== 1
        ofsLinks.right.get must not contain existentLink
      } await

      linkRepository.listLinks(iceBuddha, Option(existentFolder.id), Option(1L), Option(1L)) map { ofsLimLinks =>
        ofsLimLinks must beRight[Seq[Link]]
        ofsLimLinks.right.get.size must_== 1
        ofsLimLinks.right.get must not contain existentLink
      } await
    }
  }

  ".listFolders" should {
    "not return anything given an invalid uid" in {
      linkRepository.listFolders(UserID("deadd00d")).map(_ must beEmpty) await
    }

    "return only folders for the user with the given token" in {
      linkRepository.listFolders(cafeDude) map { folders =>
        folders.size must_== 1
        folders must not(contain(existentFolder))
      } await
    }
  }

  ".passThrough" should {
    "pass the link url" in {
      import profile.api._

      linkRepository.passThrough(existentLink.code, referrer, remoteIp) flatMap { url =>
        url must beRight(existentLink.url)
        db run clicks.filter(_.code === existentLink.code).filter(_.referrer === referrer).filter(_.remote_ip === remoteIp).exists.result
      } map (_ must beTrue) await
    }

    "return an error if the code does not exist" in {
      import profile.api._

      val code = "nope"
      linkRepository.passThrough(code, referrer, remoteIp) flatMap { url =>
        url must beLeft(Error(ErrorCode.NonExistentCode))
        db.run(clicks.filter(_.code === code).filter(_.referrer === referrer).filter(_.remote_ip === remoteIp).exists.result)
      } map (_ must beFalse) await
    }
  }

  ".listClicks" should {
    "not return anything given an invalid code" in {
      linkRepository.listClicks(iceBuddha, "none", None, None).map(_ must beLeft(Error(ErrorCode.NonExistentCode))) await

      linkRepository.listClicks(iceBuddha, encodedLongMaxVal, None, None).map(_ must beLeft(Error(ErrorCode.NonExistentCode))) await
    }

    "return only clicks for the user with the given token" in {
      linkRepository.listClicks(forbidden, encodedLongMaxVal, None, None).map(_ must beRight[Seq[Click]](Seq(Click(encodedLongMaxVal, new Date(Long.MaxValue), Option(encodedLongMaxVal), remoteIp)))) await
    }
  }

  ".linkSummary" should {
    "not return anything given an invalid code" in {
      linkRepository.linkSummary(iceBuddha, "none").map(_ must beLeft(Error(ErrorCode.NonExistentCode))) await
    }

    "return link summary" in {
      linkRepository.linkSummary(iceBuddha, "yeah").map(_ must beRight(LinkSummary(existentLink.url, existentLink.code, existentLink.folderId, 2L))) await
    }
  }

  override val profile: JdbcProfile = slick.driver.H2Driver
  override val linkRepository: LinkRepository = new LinkRepositoryImpl
  override val db: profile.api.Database = profile.api.Database.forURL("jdbc:h2:mem:links;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")

  private val forbidden = UserID("4b1d")
  private val iceBuddha = UserID("1ceb00da")
  private val cafeDude  = UserID("cafed00d")

  private val remoteIp = Option("127.0.0.1")
  private val referrer  = Option("referer")
  private val existentFolder: Folder = Folder(1L, iceBuddha, "A")
  private val existentLink: Link     = Link(iceBuddha, "https://www.google.com", "yeah", Option(1L))
  private val nonExistentLink: Link  = Link(iceBuddha, "https://www.google.com", "", Option(1L))
  private val existentClick: Click   = Click(existentLink.code, new Date(), referrer, remoteIp)
  private val encodedCodeSeqStartVal = AlphabetCodec.encode(idSequenceStart)
  private val encodedLongMaxVal = AlphabetCodec.encode(Long.MaxValue)

  override def before: Unit = {
    import profile.api._

    val initAction = db run {
      (folders.schema ++ idSequence.schema ++ links.schema ++ clicks.schema).create >>
        folders.forceInsertAll(Seq(
          existentFolder,
          Folder(2L, iceBuddha, "B"),
          Folder(3L, cafeDude, "C"),
          Folder(Long.MaxValue, forbidden, "D")
        )) >>
        links.forceInsertAll(Seq(
          existentLink,
          Link(iceBuddha, "test", "b", Option(1L)),
          Link(cafeDude, "test", "c", Option(2L)),
          Link(iceBuddha, "test", "d", Option(3L)),
          Link(forbidden, "test", encodedLongMaxVal, Option(Long.MaxValue))
        )) >>
        clicks.forceInsertAll(Seq(
          existentClick,
          Click(existentLink.code, new Date(), Option("a"), remoteIp),
          Click(encodedLongMaxVal, new Date(Long.MaxValue), Option(encodedLongMaxVal), remoteIp)
        ))
    }

    Await.result(initAction, Duration.Inf)
  }

  override def after: Unit = {
    import profile.api._

    val disposeAction = db run (clicks.schema ++ folders.schema ++ links.schema ++ idSequence.schema).drop
    Await.result(disposeAction, Duration.Inf)
  }
}