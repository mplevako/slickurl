/**
 * Copyright 2014 Maxim Plevako
**/
package shortenurl.actor

import java.util.Date

import akka.actor.{Actor, IndirectActorProducer}
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.{Publish, Subscribe, SubscribeAck}
import shortenurl.domain.model.{Error, ErrorCode, Link, User}
import shortenurl.domain.repository.LinkRepositoryComponent

case class UrlCode(url: String, code: Option[String])
case class Clck(date: Date, referer: String, remote_ip: String)

trait LinkRepo extends Actor {
  val userRepoTopic = context.system.settings.config.getString("user.repo.topic")
  val linkRepoTopic = context.system.settings.config.getString("link.repo.topic")

  val linkRepository: LinkRepositoryComponent#LinkRepository

  val mediator = DistributedPubSubExtension(context.system).mediator
  mediator ! Subscribe(`linkRepoTopic`, self)

  def receive = {
    case SubscribeAck(Subscribe(`linkRepoTopic`, None, `self`)) => context become ready
  }

  def ready: Actor.Receive = {
    case PassThrough(code, referer, remote_ip, replyTo) =>
      replyTo ! linkRepository.passThrough(code, referer, remote_ip)

    case cmd@GetLinkSummary(_, token, _) =>
      mediator ! Publish(`userRepoTopic`, GetUserWithToken(token, self, Some(cmd)))

    case cmd@ListClicks(_, token, _, _, _) =>
      mediator ! Publish(`userRepoTopic`, GetUserWithToken(token, self, Some(cmd)))

    case cmd@ListFolders(token, _) =>
      mediator ! Publish(`userRepoTopic`, GetUserWithToken(token, self, Some(cmd)))

    case cmd@ShortenLink(token, _, _, _, _) =>
      mediator ! Publish(`userRepoTopic`, GetUserWithToken(token, self, Some(cmd)))

    case cmd@ListLinks(token, _, _, _, _) =>
      mediator ! Publish(`userRepoTopic`, GetUserWithToken(token, self, Some(cmd)))

    case UserForToken(user, cmd) => processUserForToken(user, cmd)
  }

  def processUserForToken(user: Option[User], cmd: Option[Any]) = cmd match {
      case Some(ShortenLink(_, url, code, fid, replyTo)) =>
        replyTo ! replyMsg(user, shortUrlReply(url, code, fid))

      case Some(ListLinks(_, fid, offset, limit, replyTo)) =>
        replyTo ! replyMsg(user, linksReply(fid, offset, limit))

      case Some(ListFolders(_, replyTo)) =>
        replyTo ! replyMsg(user, foldersReply)

      case Some(ListClicks(code, _, offset, limit, replyTo)) =>
        replyTo ! replyMsg(user, clicksReply(code, offset, limit))

      case Some(GetLinkSummary(code, _, replyTo)) =>
        replyTo ! replyMsg(user, summaryReply(code))

      case _ => sender ! Error(ErrorCode.Unknown)
  }

  def replyMsg(user: Option[User], userWithTokenHandler: User => Any) = user match {
    case None => Error(ErrorCode.InvalidToken)
    case Some(userWithToken) => userWithTokenHandler(userWithToken)
    case _ => Error(ErrorCode.Unknown)
  }

  def shortUrlReply(url: String, code: Option[String], folderId: Option[Long])(user: User) = {
    val link = Link(user.id, url, code, folderId)
    linkRepository.shortenUrl(link)
  }

  def clicksReply(code: String, offset: Option[Long], limit: Option[Long])(user: User) =
    linkRepository.listClicks(code, user.id, offset, limit).fold(error => Left(error), l => Right(l.map(click => Clck(click.date, click.referer, click.remote_ip))))

  def summaryReply(code: String)(user: User) = linkRepository.linkSummary(code, user.id)

  def linksReply(folderId: Option[Long], offset: Option[Long], limit: Option[Long])(user: User) =
    linkRepository.listLinks(user.id, folderId, offset, limit).fold(error => Left(error), l => Right(l.map(link => UrlCode(link.url, link.code))))

  def foldersReply(user: User) = Folders(linkRepository.listFolders(user.id))
}

class LinkRepoFactory(val linkRepo: LinkRepositoryComponent#LinkRepository) extends IndirectActorProducer {

  override def actorClass = classOf[LinkRepo]

  override def produce(): Actor = new LinkRepo {
    override val linkRepository: LinkRepositoryComponent#LinkRepository = linkRepo
  }
}