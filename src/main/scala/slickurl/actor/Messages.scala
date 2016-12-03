package slickurl.actor

import akka.actor.ActorRef
import slickurl.domain.model.{Folder, User}

private[slickurl] case class GetUser(userId: Long, secret: String, replyTo: ActorRef)
private[slickurl] case class CreateNewUser(secret: String, replyTo: ActorRef)
private[slickurl] case class GetUserWithToken(token: String, replyTo: ActorRef, payLoad: Option[Any])
private[slickurl] case class UserForToken(uid: Option[User], payLoad: Option[Any])

private[slickurl] case class ListFolders(token: String, replyTo: ActorRef)
private[slickurl] case class Folders(folders: Seq[Folder])

private[slickurl] case class ListLinks(token: String, folderId: Option[Long], offset: Option[Long], limit: Option[Long], replyTo: ActorRef)
private[slickurl] case class ShortenLink(token: String, url: String, code: Option[String], folderId: Option[Long], replyTo: ActorRef)

private[slickurl] case class GetLinkSummary(code: String, token: String, replyTo: ActorRef)
private[slickurl] case class ListClicks(code: String, token: String, offset: Option[Long], limit: Option[Long], replyTo: ActorRef)
private[slickurl] case class PassThrough(code: String, referrer: String, remote_ip: String, replyTo: ActorRef)

