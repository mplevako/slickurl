package slickurl.actor

import java.util.Date

import akka.actor.Props
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Subscribe, SubscribeAck}
import akka.pattern._
import slick.driver.JdbcProfile
import slickurl.AppProps._
import slickurl.actor.shard.Shard
import slickurl.domain.model._
import slickurl.domain.repository.{ClickTable, FolderTable, LinkRepositoryComponent, LinkTable}

case class UrlCode(url: String, code: String)
case class Clck(date: Date, referrer: Option[String], remote_ip: Option[String])

object LinkRepo {
  def apply(shardId: Long, profile: JdbcProfile)(db: profile.api.Database): Props =
    Props(classOf[LinkRepo], shardId, profile, db)
}

class LinkRepo(override val shardId: Long, override val profile: JdbcProfile,
               override val db: JdbcProfile#Backend#Database)
extends Shard with LinkRepositoryComponent with LinkTable with FolderTable with ClickTable{
  implicit private val ec = context.dispatcher

  override protected def linkRepository: LinkRepository = new LinkRepositoryImpl

  private val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe(linkTopic, self)

  def receive: Receive = {
    case SubscribeAck(Subscribe(`linkTopic`, None, `self`)) => context become ready
  }

  def ready: Receive = {
    case PassThrough(`shardId`, code, referrer, remote_ip) =>
      linkRepository.passThrough(code, Option(referrer), Option(remote_ip)) pipeTo sender()

    case ShortenLink(`shardId`, userId, url, folderId) =>
      linkRepository.shortenUrl(shardId, userId, url, folderId) pipeTo sender()

    case ListClicks(`shardId`, userId, code, offset, limit) =>
      linkRepository.listClicks(userId, code, offset, limit).map{
        _.fold(error => Left(error),
                   l => Right(l.map(click => Clck(click.date, click.referrer, click.remote_ip))))
      } pipeTo sender()

    case ListLinks(`shardId`, userId, folderId, offset, limit) =>
      linkRepository.listLinks(userId, folderId, offset, limit).map{
        _.fold(error => Left(error),
                   l => Right(l.map(link => UrlCode(link.url, link.code))))
      } pipeTo sender()

    case ListFolders(`shardId`, userId) =>
      linkRepository.listFolders(userId).map(Folders) pipeTo sender()

    case GetLinkSummary(`shardId`, userId, code) =>
      linkRepository.linkSummary(userId, code) pipeTo sender()

    case ListFolders(_, userId) => //skip messages the other shards' messages
    case GetLinkSummary(_, userId, code) => //skip messages the other shards' messages
    case ShortenLink(_, userId, url, folderId) => //skip messages the other shards' messages
    case PassThrough(_, code, referrer, remote_ip) => //skip messages the other shards' messages
    case ListClicks(_, userId, code, offset, limit) => //skip messages the other shards' messages
    case ListLinks(_, userId, folderId, offset, limit) => //skip messages the other shards' messages
    case _ => sender() ! Error(ErrorCode.Unknown)
  }
}