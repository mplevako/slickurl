package slickurl.domain.repository

import java.sql.Timestamp
import java.util.Date

import slickurl.domain.model.Click

trait ClickTable extends Profile { this: LinkTable =>
  import profile.api._

  class Clicks(tag: Tag) extends Table[Click](tag, "CLICK") {

    implicit private val DateMapper = MappedColumnType.base[Date, Timestamp](
      d => new Timestamp(d.getTime), t => new Date(t.getTime)
    )

    def code      = column[String] ("CODE")
    def date      = column[Date] ("DATE")
    def referrer  = column[Option[String]] ("REFERRER")
    def remote_ip = column[Option[String]] ("REMOTE_IP")

    def link = foreignKey("LINK_FK", code, links)(_.code, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Restrict)
    def link_fk_idx = index("CLICK_CODE_IDX", code)

    def * = (code, date, referrer, remote_ip) <> (Click.tupled, Click.unapply)
  }

  lazy val clicks = Clicks.clicks

  private object Clicks {
    lazy val clicks = TableQuery[Clicks]
  }
}
