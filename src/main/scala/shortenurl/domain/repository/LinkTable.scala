package shortenurl.domain.repository

import com.typesafe.config.ConfigFactory
import shortenurl.domain.model.Link

import scala.slick.jdbc.StaticQuery

trait LinkTable extends FolderTable {
  import profile.simple._

  class Links(tag: Tag) extends Table[Link](tag, "LINK") {

    def uid   = column[Long] ("UID", O.NotNull)
    def fid   = column[Option[Long]] ("FID", O.Nullable)
    def url   = column[String] ("URL", O.NotNull)
    def code  = column[String] ("CODE", O.NotNull)

    def id    = primaryKey("LINK_CODE_PK", code)

    def folder = foreignKey("FOLDER_FK", fid, folders)(_.id, onUpdate=ForeignKeyAction.Cascade)

    def * = (uid, url, code.?, fid) <> (Link.tupled, Link.unapply)
  }

  val links = Links.links
  val codeSequence  = CodeSeq //actually must be val codeSequence = Links.codeSequence

  private object Links {
    val links = TableQuery[Links]
    //val codeSequence = Sequence[Long]("codeseq") start 128 inc 1
  }

  private[repository] object CodeSeq {
    val codeSeqDDL    = ConfigFactory.load().getString("db.codeSequence.ddl")
    val nextCodeQuery = ConfigFactory.load().getString("db.nextCode.query")
    val ddl =  StaticQuery.updateNA(codeSeqDDL)
    val next = StaticQuery.queryNA[Long](nextCodeQuery)
  }
}

