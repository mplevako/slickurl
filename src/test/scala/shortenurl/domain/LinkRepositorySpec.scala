package shortenurl.domain

import org.specs2.execute.{AsResult, Result}
import org.specs2.mutable.Specification
import org.specs2.specification.{AroundExample, BeforeAfterExample}
import shortenurl.domain.model.{CodeAlreadyTaken, Folder, Link}
import shortenurl.domain.repository.{FolderTable, LinkRepositoryComponent, LinkTable}

import scala.slick.driver.JdbcProfile
import scala.slick.jdbc.StaticQuery

class LinkRepositorySpec extends Specification with AroundExample with BeforeAfterExample
                                 with LinkRepositoryComponent with LinkTable with FolderTable {

  sequential

  ".shortenUrl" should {
    "return CodeAlreadyUsed when the code is already used" in {
      val e = linkRepository.shortenUrl(existentLink)
      e must beLeft[CodeAlreadyTaken.type]
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

  override val profile: JdbcProfile = scala.slick.driver.H2Driver
  override val linkRepository: LinkRepository = new LinkRepositoryImpl
  override val db: JdbcProfile#Backend#Database = profile.simple.Database.forURL("jdbc:h2:mem:links", driver = "org.h2.Driver")

  val existentFolder: Folder = Folder(1, 1, "Beef Links")
  val existentLink: Link = Link(1, "https://www.google.com", Some("dEaDbEeF"), Some(1))
  val nonExistentLink: Link = Link(1, "https://www.google.com", None, Some(1))
  val encodedIntMaxVal = "dIA5IR"

  import profile.simple._

  override def before: Any = db withSession { implicit session =>
    (folders.ddl ++ links.ddl).create
    StaticQuery.updateNA(s"create sequence codeseq increment 1 start ${Int.MaxValue}").execute

    folders.forceInsert(existentFolder)
    links.forceInsert(existentLink)
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
