/**
 * Copyright 2014-2015 Maxim Plevako
 **/
package shortenurl.domain.repository

import shortenurl.domain.model.Folder

trait FolderTable extends Profile {

  import profile.api._

  class Folders(tag: Tag) extends Table[Folder](tag, "FOLDER") {

    def id    = column[Long]("ID")
    def uid   = column[Long]("UID")
    def title = column[String]("TITLE")

    def pk = primaryKey("FOLDER_PK", id)
    def uid_id_idx = index("FOLDER_UID_ID_IDX", (id, uid), unique = true)

    def * = (id, uid, title) <> (Folder.tupled, Folder.unapply)
  }

  lazy val folders = Folders.folders

  private object Folders {
    lazy val folders = TableQuery[Folders]
  }
}
