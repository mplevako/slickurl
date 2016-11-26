package shortenurl.domain.model

import java.util.Date

case class User(id: Long, token: String)
case class Folder(id: Long, uid: Long, title: String)
case class Click(code: String, date: Date, referer: Option[String], remote_ip: Option[String])
case class Link(uid: Long, url: String, code: Option[String], folderId: Option[Long])
case class LinkSummary(url: String, code: String, folderId: Option[Long], clickCount: Long)

case class Error(errorCode: String)

object ErrorCode {
  val Unknown         = "unknown"
  val Duplicate       = "duplicate"
  val InvalidToken    = "invalid_token"
  val WrongSecret     = "wrong_secret"
  val InvalidFolder   = "invalid_folder"
  val CodeTaken       = "code_taken"
  val NonExistentCode = "nonexistent_code"
}