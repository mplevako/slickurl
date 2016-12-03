package slickurl.actor

import java.util.Date

import akka.actor.{Actor, ActorRef}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe, SubscribeAck}
import akka.pattern._
import slickurl.AppConfig._
import slickurl.domain.model._
import slickurl.domain.repository.LinkRepositoryComponent

import scala.concurrent.Future

case class UrlCode(url: String, code: Option[String])
case class Clck(date: Date, referrer: Option[String], remote_ip: Option[String])

trait LinkRepo extends Actor {
  implicit private val ec = context.dispatcher

  val linkRepository: LinkRepositoryComponent#LinkRepository

  private val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe(linkRepoTopic, self)

  def receive: Receive = {
    case SubscribeAck(Subscribe(`linkRepoTopic`, None, `self`)) => context become ready
  }

  def ready: Receive = {
    case PassThrough(code, referrer, remote_ip, replyTo) =>
      linkRepository.passThrough(code, Option(referrer), Option(remote_ip)) pipeTo replyTo

    case cmd@GetLinkSummary(_, token, _) =>
      mediator ! Publish(userRepoTopic, GetUserWithToken(token, self, Option(cmd)))

    case cmd@ListClicks(_, token, _, _, _) =>
      mediator ! Publish(userRepoTopic, GetUserWithToken(token, self, Option(cmd)))

    case cmd@ListFolders(token, _) =>
      mediator ! Publish(userRepoTopic, GetUserWithToken(token, self, Option(cmd)))

    case cmd@ShortenLink(token, _, _, _, _) =>
      mediator ! Publish(userRepoTopic, GetUserWithToken(token, self, Option(cmd)))

    case cmd@ListLinks(token, _, _, _, _) =>
      mediator ! Publish(userRepoTopic, GetUserWithToken(token, self, Option(cmd)))

    case UserForToken(user, cmd) => processUserForToken(user, cmd)
  }

  private def processUserForToken(user: Option[User], cmd: Option[Any]) = cmd match {
      case Some(ShortenLink(_, url, code, fid, replyTo)) =>
        replyMsg(replyTo, user, shortUrlReply(url, code, fid))

      case Some(ListLinks(_, fid, offset, limit, replyTo)) =>
        replyMsg(replyTo, user, linksReply(fid, offset, limit))

      case Some(ListFolders(_, replyTo)) =>
        replyMsg(replyTo, user, foldersReply)

      case Some(ListClicks(code, _, offset, limit, replyTo)) =>
        replyMsg(replyTo, user, clicksReply(code, offset, limit))

      case Some(GetLinkSummary(code, _, replyTo)) =>
        replyMsg(replyTo, user, summaryReply(code))

      case _ => sender ! Error(ErrorCode.Unknown)
  }

  private def replyMsg(replyTo: ActorRef, user: Option[User], userWithTokenHandler: User => Future[_]) = user match {
    case None => replyTo ! Error(ErrorCode.InvalidToken)
    case Some(userWithToken) => userWithTokenHandler(userWithToken) pipeTo replyTo
    case _ => replyTo ! Error(ErrorCode.Unknown)
  }

  private def shortUrlReply(url: String, code: Option[String], folderId: Option[Long])(user: User): Future[Error Either Link] = {
    val link = Link(user.id, url, code, folderId)
    linkRepository.shortenUrl(link)
  }

  private def clicksReply(code: String, offset: Option[Long], limit: Option[Long])(user: User): Future[Error Either Seq[Clck]] =
    linkRepository.listClicks(code, user.id, offset, limit).map(_.fold(error => Left(error), l => Right(l.map(click => Clck(click.date, click.referer, click.remote_ip)))))

  private def summaryReply(code: String)(user: User): Future[Error Either LinkSummary] = linkRepository.linkSummary(code, user.id)

  private def linksReply(folderId: Option[Long], offset: Option[Long], limit: Option[Long])(user: User): Future[Error Either Seq[UrlCode]] =
    linkRepository.listLinks(user.id, folderId, offset, limit).map(_.fold(error => Left(error), l => Right(l.map(link => UrlCode(link.url, link.code)))))

  private def foldersReply(user: User): Future[Folders] = linkRepository.listFolders(user.id).map(Folders)
}