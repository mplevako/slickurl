package slickurl.domain.repository

import java.sql.SQLException

import slickurl.domain.model.{Error, ErrorCode}

trait SQLStateErrorCodeTranslator {

  def isDuplicate(sqlState: String): Boolean = sqlState == "23505"

  def translateException(t: Throwable): Error = t match {
    case e: SQLException if isDuplicate(e.getSQLState) => Error(ErrorCode.Duplicate)
    case _ => Error(ErrorCode.Unknown)
  }
}
