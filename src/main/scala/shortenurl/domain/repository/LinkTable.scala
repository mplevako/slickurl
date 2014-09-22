package shortenurl.domain.repository

import com.typesafe.config.ConfigFactory
import shortenurl.domain.model.Link

import scala.slick.jdbc.StaticQuery

trait LinkTable extends FolderTable {
  val config = ConfigFactory.load()

  import profile.simple._

  class Links(tag: Tag) extends Table[Link](tag, "LINK") {

    def uid   = column[Long] ("UID", O.NotNull)
    def fid   = column[Option[Long]] ("FID", O.Nullable)
    def url   = column[String] ("URL", O.NotNull)
    def code  = column[String] ("CODE", O.NotNull, O.DBType(s"VARCHAR(${config.getString("app.shorturl.maxlength")})"))

    def id    = primaryKey("LINK_CODE_PK", code)

    def folder = foreignKey("FOLDER_FK", fid, folders)(_.id, onUpdate=ForeignKeyAction.Cascade)

    def code_uid_idx = index("LINK_CODE_UID_IDX", (code, uid), unique = true)

    def id_url_code_idx = index("LINK_URL_CODE_IDX", (code, url))

    def folder_fk_idx = index("LINK_FID_IDX", fid)

    def * = (uid, url, code.?, fid) <> (Link.tupled, Link.unapply)
  }

  val links = Links.links
  val codeSequence  = CodeSeq //actually must be val codeSequence = Links.codeSequence

  private object Links {
    val links = TableQuery[Links]
    //val codeSequence = Sequence[Long]("codeseq") start 128 inc 1
  }

  private[repository] object CodeSeq {
    val codeSeqDDL    = config.getString("db.codeSequence.ddl")
    val nextCodeQuery = config.getString("db.nextCode.query")
    val ddl =  StaticQuery.updateNA(codeSeqDDL)
    val next = StaticQuery.queryNA[Long](nextCodeQuery)
  }
}

