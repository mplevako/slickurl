package slickurl.actor

import slickurl.domain.model.{Folder, UserID}

private[slickurl] case object CreateNewUser

private[slickurl] case class ListFolders(shardId: Long, uid: UserID)
private[slickurl] case class Folders(folders: Seq[Folder])

private[slickurl] case class ListLinks(shardId: Long, uid: UserID, folderId: Option[Long], offset: Option[Long], limit: Option[Long])
private[slickurl] case class ShortenLink(shardId: Long, uid: UserID, url: String, folderId: Option[Long])

private[slickurl] case class GetLinkSummary(shardId: Long, uid: UserID, code: String)
private[slickurl] case class ListClicks(shardId: Long, uid: UserID, code: String, offset: Option[Long], limit: Option[Long])
private[slickurl] case class PassThrough(shardId: Long, code: String, referrer: String, remote_ip: String)

