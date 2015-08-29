/**
 * Copyright 2014-2015 Maxim Plevako
 **/
package shortenurl.domain.repository

import slick.driver.JdbcProfile

trait Profile {
  val profile: JdbcProfile
  val db: JdbcProfile#Backend#Database
}