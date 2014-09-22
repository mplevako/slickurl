package shortenurl.domain.repository

import shortenurl.domain.model.Folder

trait FolderTable extends Profile {

  import profile.simple._

  class Folders(tag: Tag) extends Table[Folder](tag, "FOLDER") {

    def id    = column[Long]("ID")
    def uid   = column[Long]("UID", O.NotNull)
    def title = column[String]("TITLE", O.NotNull)

    def pk = primaryKey("FOLDER_PK", id)
    def uid_id_idx = index("FOLDER_UID_ID_IDX", (id, uid), unique = true)

    def * = (id, uid, title) <> (Folder.tupled, Folder.unapply)
  }

  val folders = Folders.folders

  private object Folders {
    val folders = TableQuery[Folders]
  }
}
