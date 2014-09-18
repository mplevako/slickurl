package shortenurl.domain

import org.specs2.execute.{AsResult, Result}
import org.specs2.mutable.Specification
import org.specs2.specification.{AroundExample, BeforeAfterExample}
import shortenurl.domain.model.Folder
import shortenurl.domain.repository.{FolderRepositoryComponent, FolderTable}

import scala.slick.driver.JdbcProfile

class FolderRepositorySpec extends Specification with AroundExample with BeforeAfterExample
                                   with FolderRepositoryComponent with FolderTable {

  sequential

  "listFolders" should {
    "not return anything given an invalid uid" in {
      val folders = folderRepository.listFolders(-1)
      folders must beEmpty
    }

    "return only folders for the user with the given token" in {
      val folders = folderRepository.listFolders(1)
      folders.size must_== 2
      folders must not(contain(testFolder))
    }
  }

  override val profile: JdbcProfile = scala.slick.driver.H2Driver
  override val folderRepository: FolderRepository = new FolderRepositoryImpl
  override val db: JdbcProfile#Backend#Database = profile.simple.Database.forURL("jdbc:h2:mem:folders", driver = "org.h2.Driver")

  val testFolder: Folder = Folder(2, 2, "Two")

  import profile.simple._

  override def before: Any = db withSession { implicit session =>
    folders.ddl.create
    folders.forceInsertAll(
      Folder(1, 1, "One"),
      testFolder,
      Folder(11, 1, "OneOne")
    )
  }

  override def after: Any = db withSession { implicit session =>
    folders.ddl.drop
  }

  override def around[T: AsResult](t: => T): Result = {
    db.withTransaction { implicit session =>
      try AsResult(t) finally session.rollback()
    }
  }

}
