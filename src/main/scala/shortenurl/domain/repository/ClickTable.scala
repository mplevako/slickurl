/**
 * Copyright 2014 Maxim Plevako
 **/
package shortenurl.domain.repository

import java.sql.Timestamp
import java.util.Date

import shortenurl.domain.model.Click

trait ClickTable extends Profile { this: LinkTable =>
  import profile.simple._

  class Clicks(tag: Tag) extends Table[Click](tag, "CLICK") {

    implicit val DateMapper = MappedColumnType.base[Date, Timestamp](
      d => new Timestamp(d.getTime), t => new Date(t.getTime)
    )

    def code      = column[String] ("CODE", O.NotNull)
    def date      = column[Date] ("DATE", O.NotNull)
    def referer   = column[String] ("REFERER")
    def remote_ip = column[String] ("REMOTE_IP")

    def link = foreignKey("LINK_FK", code, links)(_.code, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Restrict)
    def link_fk_idx = index("CLICK_CODE_IDX", code)

    def * = (code, date, referer, remote_ip) <> (Click.tupled, Click.unapply)
  }

  val clicks = Clicks.clicks

  private object Clicks {
    val clicks = TableQuery[Clicks]
  }
}
