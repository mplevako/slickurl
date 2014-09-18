package shortenurl.domain.model

case class User(id: Long, token: String)
case class Folder(id: Long, uid: Long, title: String)
case class Link(uid: Long, url: String, code: Option[String], folderId: Option[Long])

case class Error(errorCode: String)

object ErrorCode {
  val Unknown       = "unknown"
  val InvalidToken  = "invalid_token"
  val InvalidFolder = "invalid_folder"
  val CodeTaken     = "code_taken"
}