package slickurl.domain

import java.util.Date

import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAfterExample
import slick.driver.JdbcProfile
import slickurl.AppConfig._
import slickurl.DbConfig._
import slickurl.domain.model._
import slickurl.domain.repository.{ClickTable, FolderTable, LinkRepositoryComponent, LinkTable}
import slickurl.urlcodec.URLCodec

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class LinkRepositorySpec extends Specification with BeforeAfterExample with LinkRepositoryComponent with LinkTable
                                               with FolderTable with ClickTable {

  sequential

  ".shortenUrl" should {
    "return CodeAlreadyUsed when the code is already used" in {
      linkRepository.shortenUrl(existentLink).map(_ must beLeft[Error]) await
    }

    "keep the code if possible" in {
      val linkWithCode = nonExistentLink.copy(code = Option("cafebabe"))
      linkRepository.shortenUrl(linkWithCode) map { result =>
        result must beRight[Link]
        result.right.get must_== linkWithCode
      } await
    }

    "return a short link if the given code is empty" in {
      linkRepository.shortenUrl(nonExistentLink) map { result =>
        result must beRight[Link]
        result.right.get must_== nonExistentLink.copy(code = Option(encodedCodeSeqStartVal))
      } await
    }
  }

  ".listLinks" should {
    "list all link for the given user if no folder id is specified" in {
      linkRepository.listLinks(1L, None, None, None) map { links =>
        links must beRight[Seq[Link]]
        links.right.get.size must_== 3
      } await

      linkRepository.listLinks(1L, None, Option(1L), None) map { ofsLinks =>
        ofsLinks must beRight[Seq[Link]]
        ofsLinks.right.get.size must_== 2
      } await

      linkRepository.listLinks(1L, None, Option(1L), Option(1L)) map { ofsLimLinks =>
        ofsLimLinks must beRight[Seq[Link]]
        ofsLimLinks.right.get.size must_== 1
        ofsLimLinks.right.get.head.code must beSome[String]
        ofsLimLinks.right.get.head.code.get must_== "b"
      } await
    }

    "list all link for the given user and folder" in {
      linkRepository.listLinks(1L, Option(existentFolder.id), Option(1L), None) map { ofsLinks =>
        ofsLinks must beRight[Seq[Link]]
        ofsLinks.right.get.size must_== 1
        ofsLinks.right.get must not contain existentLink
      } await

      linkRepository.listLinks(1L, Option(existentFolder.id), Option(1L), Option(1L)) map { ofsLimLinks =>
        ofsLimLinks must beRight[Seq[Link]]
        ofsLimLinks.right.get.size must_== 1
        ofsLimLinks.right.get must not contain existentLink
      } await
    }
  }

  ".listFolders" should {
    "not return anything given an invalid uid" in {
      linkRepository.listFolders(-1L).map(_ must beEmpty) await
    }

    "return only folders for the user with the given token" in {
      linkRepository.listFolders(2L) map { folders =>
        folders.size must_== 1
        folders must not(contain(existentFolder))
      } await
    }
  }

  ".passThrough" should {
    "pass the link url" in {
      import profile.api._

      linkRepository.passThrough(existentLink.code.get, referer, remoteIp) flatMap { url =>
        url must beRight(existentLink.url)
        db run clicks.filter(_.code === existentLink.code.get).filter(_.referrer === referer).filter(_.remote_ip === remoteIp).exists.result
      } map (_ must beTrue) await
    }

    "return an error if the code does not exist" in {
      import profile.api._

      val code = "nope"
      linkRepository.passThrough(code, referer, remoteIp) flatMap { url =>
        url must beLeft(Error(ErrorCode.NonExistentCode))
        db.run(clicks.filter(_.code === code).filter(_.referrer === referer).filter(_.remote_ip === remoteIp).exists.result)
      } map (_ must beFalse) await
    }
  }

  ".listClicks" should {
    "not return anything given an invalid code" in {
      linkRepository.listClicks("none", 1L, None, None).map(_ must beLeft(Error(ErrorCode.NonExistentCode))) await

      linkRepository.listClicks(encodedLongMaxVal, 1L, None, None).map(_ must beLeft(Error(ErrorCode.NonExistentCode))) await
    }

    "return only clicks for the user with the given token" in {
      linkRepository.listClicks(encodedLongMaxVal, Long.MaxValue, None, None).map(_ must beRight[Seq[Click]](Seq(Click(encodedLongMaxVal, new Date(Long.MaxValue), Option(encodedLongMaxVal), remoteIp)))) await
    }
  }

  ".linkSummary" should {
    "not return anything given an invalid code" in {
      linkRepository.linkSummary("none", 1L).map(_ must beLeft(Error(ErrorCode.NonExistentCode))) await
    }

    "return link summary" in {
      linkRepository.linkSummary("yeah", 1L).map(_ must beRight(LinkSummary(existentLink.url, existentLink.code.get, existentLink.folderId, 2L))) await
    }
  }

  override val profile: JdbcProfile = slick.driver.H2Driver
  override val linkRepository: LinkRepository = new LinkRepositoryImpl
  override val db: profile.api.Database = profile.api.Database.forURL("jdbc:h2:mem:links;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")

  private val remoteIp = Option("127.0.0.1")
  private val referer  = Option("referer")
  private val existentFolder: Folder = Folder(1L, 1L, "A")
  private val existentLink: Link     = Link(1L, "https://www.google.com", Option("yeah"), Option(1L))
  private val nonExistentLink: Link  = Link(1L, "https://www.google.com", None, Option(1L))
  private val existentClick: Click   = Click(existentLink.code.get, new Date(), referer, remoteIp)
  private val encodedCodeSeqStartVal = URLCodec.encode(encodingAlphabet, idSequenceStart)
  private val encodedLongMaxVal = URLCodec.encode(encodingAlphabet, Long.MaxValue)

  override def before: Unit = {
    import profile.api._

    val initAction = db run {
      (folders.schema ++ idSequence.schema ++ links.schema ++ clicks.schema).create >>
        folders.forceInsertAll(Seq(
          existentFolder,
          Folder(2L, 1L, "B"),
          Folder(3L, 2L, "C"),
          Folder(Long.MaxValue, Long.MaxValue, "D")
        )) >>
        links.forceInsertAll(Seq(
          existentLink,
          Link(1L, "test", Option("b"), Option(1L)),
          Link(2L, "test", Option("c"), Option(2L)),
          Link(1L, "test", Option("d"), Option(3L)),
          Link(Long.MaxValue, "test", Option(encodedLongMaxVal), Option(Long.MaxValue))
        )) >>
        clicks.forceInsertAll(Seq(
          existentClick,
          Click(existentLink.code.get, new Date(), Option("a"), remoteIp),
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