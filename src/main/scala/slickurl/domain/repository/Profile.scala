package slickurl.domain.repository

import slick.driver.JdbcProfile
import slickurl.domain.model.UserID

trait Profile {
  val profile: JdbcProfile
  val db: JdbcProfile#Backend#Database

  import profile.api._
  implicit lazy val userIdMapper = MappedColumnType.base[UserID, String](_.id, UserID)
}