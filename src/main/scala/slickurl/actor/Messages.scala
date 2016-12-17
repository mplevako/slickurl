package slickurl.actor

import slickurl.domain.model.{Folder, UserID}

private[slickurl] case object CreateNewUser

private[slickurl] case class ListFolders(uid: UserID)
private[slickurl] case class Folders(folders: Seq[Folder])

private[slickurl] case class ListLinks(uid: UserID, folderId: Option[Long], offset: Option[Long], limit: Option[Long])
private[slickurl] case class ShortenLink(uid: UserID, url: String, folderId: Option[Long])

private[slickurl] case class GetLinkSummary(uid: UserID, code: String)
private[slickurl] case class ListClicks(uid: UserID, code: String, offset: Option[Long], limit: Option[Long])
private[slickurl] case class PassThrough(code: String, referrer: String, remote_ip: String)

