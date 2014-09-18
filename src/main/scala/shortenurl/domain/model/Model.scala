package shortenurl.domain.model

case class User(id: Long, token: String)
case class Folder(id: Long, uid: Long, title: String)