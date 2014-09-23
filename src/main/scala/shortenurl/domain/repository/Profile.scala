/**
 * Copyright 2014 Maxim Plevako
 **/
package shortenurl.domain.repository

import scala.slick.driver.JdbcProfile

trait Profile {
  val profile: JdbcProfile
  val db: JdbcProfile#Backend#Database
}