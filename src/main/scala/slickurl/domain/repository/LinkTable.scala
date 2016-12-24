package slickurl.domain.repository

import slickurl.DbProps._
import slickurl.AppProps._
import slickurl.domain.model.{Link, UserID}

trait LinkTable extends Profile { this: FolderTable =>

  import profile.api._

  class Links(tag: Tag) extends Table[Link](tag, schemaName, linkTableName) {

    def uid  = column[UserID] ("UID")
    def fid  = column[Option[Long]] ("FID")
    def url  = column[String] ("URL")
    def code = column[String] ("CODE")

    def id     = primaryKey("LINK_CODE_PK", code)
    def folder = foreignKey("FOLDER_FK", fid, folders)(_.id.?, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Restrict)

    def code_uid_idx = index("LINK_CODE_UID_IDX", (code, uid), unique = true)
    def id_url_code_idx = index("LINK_URL_CODE_IDX", (code, url))
    def folder_fk_idx = index("LINK_FID_IDX", fid)

    def * = (uid, url, code, fid) <> (Link.tupled, Link.unapply)
  }

  lazy val links = Links.links
  lazy val linkIdSequence = Links.idSequence

  private object Links {
    lazy val links = TableQuery[Links]
    lazy val idSequence = Sequence[Long](linkIdSequenceName) start idSequenceStart inc idSequenceInc min idSequenceStart max maxId
  }
}

