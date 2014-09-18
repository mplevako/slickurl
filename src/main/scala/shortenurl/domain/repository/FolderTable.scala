package shortenurl.domain.repository

import shortenurl.domain.model.Folder

trait FolderTable extends Profile {

  import profile.simple._

  class Folders(tag: Tag) extends Table[Folder](tag, "FOLDER") {

    def id    = column[Int]("ID", O.PrimaryKey)
    def uid   = column[Int]("UID", O.NotNull)
    def title = column[String]("TITLE", O.NotNull)

    def * = (id, uid, title) <> (Folder.tupled, Folder.unapply)
  }

  val folders = Folders.folders

  private object Folders {
    val folders = TableQuery[Folders]
  }
}
