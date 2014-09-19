package shortenurl.actor

import akka.actor.ActorRef
import shortenurl.domain.model.{Folder, Link, User}

private[shortenurl] case class GetUser(userId: Long, secret: String, replyTo: ActorRef)
private[shortenurl] case class GetUserWithToken(token: String, replyTo: ActorRef, replyVia: Option[ActorRef], payLoad: Option[Any])
private[shortenurl] case class UserForToken(uid: Option[User], replyTo: ActorRef, payLoad: Option[Any])

private[shortenurl] case class ListLinks(token: String, folderId: Option[Long], offset: Option[Long], limit: Option[Long], replyTo: ActorRef)
private[shortenurl] case class ListFolders(token: String, replyTo: ActorRef)
private[shortenurl] case class Folders(folders: List[Folder])

private[shortenurl] case class Links(folders: List[Link])

private[shortenurl] case class ShortenLink(token: String, url: String, code: Option[String], folderId: Option[Long], replyTo: ActorRef)

