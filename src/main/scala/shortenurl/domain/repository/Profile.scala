package shortenurl.domain.repository

import scala.slick.driver.JdbcProfile

trait Profile {
  val profile: JdbcProfile
  val db: JdbcProfile#Backend#Database
}