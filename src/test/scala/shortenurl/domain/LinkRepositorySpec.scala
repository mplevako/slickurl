/**
 * Copyright 2014 Maxim Plevako
 **/
package shortenurl.domain

import java.util.Date

import com.typesafe.config.ConfigFactory
import org.specs2.execute.{AsResult, Result}
import org.specs2.mutable.Specification
import org.specs2.specification.{AroundExample, BeforeAfterExample}
import shortenurl.domain.model._
import shortenurl.domain.repository.{ClickTable, FolderTable, LinkRepositoryComponent, LinkTable}
import shortenurl.urlcodec.URLCodec

import scala.slick.driver.JdbcProfile
import scala.slick.jdbc.StaticQuery

class LinkRepositorySpec extends Specification with AroundExample with BeforeAfterExample
                                 with LinkRepositoryComponent with LinkTable with FolderTable
                                 with ClickTable {

  sequential

  ".shortenUrl" should {
    "return CodeAlreadyUsed when the code is already used" in {
      val e = linkRepository.shortenUrl(existentLink)
      e must beLeft[Error]
    }

    "keep the code if possible" in {
      val linkWithCode = nonExistentLink.copy(code = Some("cafebabe"))
      val e = linkRepository.shortenUrl(linkWithCode)
      e must beRight[Link]
      e.right.get must_== linkWithCode
    }

    "return a short link if the given code is empty" in {
      val e = linkRepository.shortenUrl(nonExistentLink)
      e must beRight[Link]
      e.right.get must_== nonExistentLink.copy(code = Some(encodedIntMaxVal))
    }
  }

  ".listLinks" should {
    "list all link for the given user if no folder id is specified" in {
      val linkWithCode = nonExistentLink.copy(code = Some("cafebabe"))
      val links = linkRepository.listLinks(1L, None, None, None)
      links must beRight[List[Link]]
      links.right.get.size must_== 3

      val ofsLinks = linkRepository.listLinks(1L, None, Some(1L), None)
      ofsLinks must beRight[List[Link]]
      ofsLinks.right.get.size must_== 2

      val ofsLimLinks = linkRepository.listLinks(1L, None, Some(1L), Some(1L))
      ofsLimLinks must beRight[List[Link]]
      ofsLimLinks.right.get.size must_== 1
      ofsLimLinks.right.get.head.code must beSome[String]
      ofsLimLinks.right.get.head.code.get must_== "b"
    }

    "list all link for the given user and folder" in {
      val ofsLinks = linkRepository.listLinks(1L, Some(existentFolder.id), Some(1L), None)
      ofsLinks must beRight[List[Link]]
      ofsLinks.right.get.size must_== 1
      ofsLinks.right.get must not contain existentLink

      val ofsLimLinks = linkRepository.listLinks(1L, Some(existentFolder.id), Some(1L), Some(1L))
      ofsLimLinks must beRight[List[Link]]
      ofsLimLinks.right.get.size must_== 1
      ofsLinks.right.get must not contain existentLink
    }
  }

  ".listFolders" should {
    "not return anything given an invalid uid" in {
      val folders = linkRepository.listFolders(-1L)
      folders must beEmpty
    }

    "return only folders for the user with the given token" in {
      val folders = linkRepository.listFolders(2L)
      folders.size must_== 1
      folders must not(contain(existentFolder))
    }
  }

  ".passThrough" should {
    "pass the link url" in {
      val remoteIp = "127.0.0.1"
      val referer  = "referer"
      val folders = linkRepository.passThrough(existentLink.code.get, referer, remoteIp)
      folders must beRight(existentLink.url)

      db withTransaction { implicit session =>
        import profile.simple._
        clicks.filter(_.code === existentLink.code.get).filter(_.referer === referer).
            filter(_.remote_ip === remoteIp).map(_.code).exists.run
      }
    }

    "return an error if the code does not exist" in {
      val remoteIp = "127.0.0.1"
      val referer  = "referer"
      val code     = "nope"
      val folders = linkRepository.passThrough(code, referer, remoteIp)
      folders must beLeft(Error(ErrorCode.NonExistentCode))

      db withSession { implicit session =>
        import profile.simple._
        !clicks.filter(_.code === code).filter(_.referer === referer).
            filter(_.remote_ip === remoteIp).map(_.code).exists.run
      }
    }
  }

  ".listClicks" should {
    "not return anything given an invalid code" in {
      val clicks = linkRepository.listClicks("none", 1L, None, None)
      clicks must beLeft(Error(ErrorCode.NonExistentCode))

      val noClicks = linkRepository.listClicks(encodedLongMaxVal, 1L, None, None)
      clicks must beLeft(Error(ErrorCode.NonExistentCode))
    }

    "return only clicks for the user with the given token" in {
      val clicks = linkRepository.listClicks(encodedLongMaxVal, Long.MaxValue, None, None)
      clicks must beRight[List[Click]](List(Click(encodedLongMaxVal, new Date(Long.MaxValue), encodedLongMaxVal, "127.0.0.1")))
    }
  }

  ".linkSummary" should {
    "not return anything given an invalid code" in {
      val clicks = linkRepository.linkSummary("none", 1L)
      clicks must beLeft(Error(ErrorCode.NonExistentCode))
    }

    "return link summary" in {
      val clicks = linkRepository.linkSummary("yeah", 1L)
      clicks must beRight(LinkSummary(existentLink.url, existentLink.code.get, existentLink.folderId, 2L))
    }
  }

  override val profile: JdbcProfile = scala.slick.driver.H2Driver
  override val linkRepository: LinkRepository = new LinkRepositoryImpl
  override val db: JdbcProfile#Backend#Database = profile.simple.Database.forURL("jdbc:h2:mem:links", driver = "org.h2.Driver")

  val existentFolder: Folder = Folder(1L, 1L, "A")
  val existentLink: Link = Link(1L, "https://www.google.com", Some("yeah"), Some(1L))
  val nonExistentLink: Link = Link(1L, "https://www.google.com", None, Some(1L))
  val existentClick: Click = Click(existentLink.code.get, new Date(), "referer", "127.0.0.1")
  val encodedIntMaxVal = URLCodec.encode(ConfigFactory.load().getString("app.shorturl.alphabet"), Int.MaxValue)
  val encodedLongMaxVal = URLCodec.encode(ConfigFactory.load().getString("app.shorturl.alphabet"), Long.MaxValue)

  import profile.simple._

  override def before: Any =
    db withTransaction { implicit session =>
      (folders.ddl ++ links.ddl ++ clicks.ddl).create
      StaticQuery.updateNA(s"create sequence codeseq increment 1 start ${Int.MaxValue}").execute

      folders.forceInsertAll(
        existentFolder,
        Folder(2L, 1L, "B"),
        Folder(3L, 2L, "C"),
        Folder(Long.MaxValue, Long.MaxValue, "D")
      )

      links.forceInsertAll(
        existentLink,
        Link(1L, "test", Some("b"), Some(1L)),
        Link(2L, "test", Some("c"), Some(2L)),
        Link(1L, "test", Some("d"), Some(3L)),
        Link(Long.MaxValue, "test", Some(encodedLongMaxVal), Some(Long.MaxValue))
      )

      clicks.forceInsertAll(
        existentClick,
        Click(existentLink.code.get, new Date(), "a", "127.0.0.1"),
//        Click(existentLink.code.get, new Date(), "b", "0.0.0.1"),
        Click(encodedLongMaxVal, new Date(Long.MaxValue), encodedLongMaxVal, "127.0.0.1")
      )
    }

  override def after: Any = db withTransaction { implicit session =>
    (clicks.ddl ++ folders.ddl ++ links.ddl).drop
    StaticQuery.updateNA("drop sequence codeseq").execute
  }

  override def around[T: AsResult](t: => T): Result = {
    db.withTransaction { implicit session =>
      try AsResult(t) finally session.rollback()
    }
  }
}
