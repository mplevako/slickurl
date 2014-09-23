/**
 * Copyright 2014 Maxim Plevako
 **/
package shortenurl.actor

import akka.actor.ActorRef
import shortenurl.domain.model.{Folder, User}

private[shortenurl] case class GetUser(userId: Long, secret: String, replyTo: ActorRef)
private[shortenurl] case class GetUserWithToken(token: String, replyTo: ActorRef, payLoad: Option[Any])
private[shortenurl] case class UserForToken(uid: Option[User], payLoad: Option[Any])

private[shortenurl] case class ListFolders(token: String, replyTo: ActorRef)
private[shortenurl] case class Folders(folders: List[Folder])

private[shortenurl] case class ListLinks(token: String, folderId: Option[Long], offset: Option[Long], limit: Option[Long], replyTo: ActorRef)
private[shortenurl] case class ShortenLink(token: String, url: String, code: Option[String], folderId: Option[Long], replyTo: ActorRef)

private[shortenurl] case class GetLinkSummary(code: String, token: String, replyTo: ActorRef)
private[shortenurl] case class ListClicks(code: String, token: String, offset: Option[Long], limit: Option[Long], replyTo: ActorRef)
private[shortenurl] case class PassThrough(code: String, referer: String, remote_ip: String, replyTo: ActorRef)

