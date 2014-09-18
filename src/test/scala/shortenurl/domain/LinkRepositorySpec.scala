package shortenurl.domain

import org.specs2.execute.{AsResult, Result}
import org.specs2.mutable.Specification
import org.specs2.specification.{AroundExample, BeforeAfterExample}
import shortenurl.domain.model.{Error, Folder, Link}
import shortenurl.domain.repository.{FolderTable, LinkRepositoryComponent, LinkTable}

import scala.slick.driver.JdbcProfile
import scala.slick.jdbc.StaticQuery

class LinkRepositorySpec extends Specification with AroundExample with BeforeAfterExample
                                 with LinkRepositoryComponent with LinkTable with FolderTable {

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
      val links = linkRepository.listLinks(1, None, 0, None)
      links must not(beEmpty)
      links.size must_== 3

      val ofsLinks = linkRepository.listLinks(1, None, 1, None)
      ofsLinks must not(beEmpty)
      ofsLinks.size must_== 2

      val ofsLimLinks = linkRepository.listLinks(1, None, 1, Some(1))
      ofsLimLinks must not(beEmpty)
      ofsLimLinks.size must_== 1
      ofsLimLinks.head.code must beSome[String]
      ofsLimLinks.head.code.get must_== "b"
    }

    "return a short link if the given code is empty" in {
      val e = linkRepository.shortenUrl(nonExistentLink)
      e must beRight[Link]
      e.right.get must_== nonExistentLink.copy(code = Some(encodedIntMaxVal))
    }
  }

  override val profile: JdbcProfile = scala.slick.driver.H2Driver
  override val linkRepository: LinkRepository = new LinkRepositoryImpl
  override val db: JdbcProfile#Backend#Database = profile.simple.Database.forURL("jdbc:h2:mem:links", driver = "org.h2.Driver")

  val existentFolder: Folder = Folder(1, 1, "A")
  val existentLink: Link = Link(1, "https://www.google.com", Some("a"), Some(1))
  val nonExistentLink: Link = Link(1, "https://www.google.com", None, Some(1))
  val encodedIntMaxVal = "dIA5IR"

  import profile.simple._

  override def before: Any = db withSession { implicit session =>
    (folders.ddl ++ links.ddl).create
    StaticQuery.updateNA(s"create sequence codeseq increment 1 start ${Int.MaxValue}").execute

    folders.forceInsertAll(
      existentFolder,
      Folder(2, 1, "B"),
      Folder(3, 2, "C"),
      Folder(4, 3, "D")
    )

    links.forceInsertAll(
      existentLink,
      Link(1,"test",Some("b"),Some(1)),
      Link(2,"test",Some("c"),Some(2)),
      Link(1,"test",Some("d"),Some(3)),
      Link(3,"test",Some("e"),Some(4))
    )
  }

  override def after: Any = db withSession { implicit session =>
    (links.ddl ++ folders.ddl).drop
    StaticQuery.updateNA("drop sequence codeseq").execute
  }

  override def around[T: AsResult](t: => T): Result = {
    db.withTransaction { implicit session =>
      try AsResult(t) finally session.rollback()
    }
  }

}
