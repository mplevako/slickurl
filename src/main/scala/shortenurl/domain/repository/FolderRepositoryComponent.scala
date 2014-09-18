package shortenurl.domain.repository

import shortenurl.domain.model.Folder

trait FolderRepositoryComponent { this: FolderTable =>

  val folderRepository: FolderRepository

  trait FolderRepository {
    def listFolders(userId: Int): List[Folder]
  }

  class FolderRepositoryImpl extends FolderRepository {
    import profile.simple._

    override def listFolders(uid: Int): List[Folder] = {
      db withSession { implicit session =>
        folders.filter(_.uid === uid).list
      }
    }
  }
}
