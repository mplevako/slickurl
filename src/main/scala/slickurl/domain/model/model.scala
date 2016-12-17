package slickurl.domain.model

import java.util.Date

case class UserID(id: String) extends AnyVal
case class Folder(id: Long, uid: UserID, title: String)
case class Click(code: String, date: Date, referrer: Option[String], remote_ip: Option[String])
case class Link(uid: UserID, url: String, code: String, folderId: Option[Long])
case class LinkSummary(url: String, code: String, folderId: Option[Long], clickCount: Long)

case class Error(errorCode: String)

object ErrorCode {
  val Unknown         = "unknown"
  val Duplicate       = "duplicate"
  val InvalidToken    = "invalid_token"
  val InvalidFolder   = "invalid_folder"
  val CodeTaken       = "code_taken"
  val NonExistentCode = "nonexistent_code"
}