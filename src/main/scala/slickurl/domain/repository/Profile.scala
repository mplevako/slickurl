package slickurl.domain.repository

import slick.driver.JdbcProfile

trait Profile {
  val profile: JdbcProfile
  val db: JdbcProfile#Backend#Database
}