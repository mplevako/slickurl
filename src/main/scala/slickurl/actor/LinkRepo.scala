package slickurl.actor

import java.util.Date

import akka.actor.Actor
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Subscribe, SubscribeAck}
import akka.pattern._
import slickurl.AppProps._
import slickurl.domain.model._
import slickurl.domain.repository.LinkRepositoryComponent

case class UrlCode(url: String, code: String)
case class Clck(date: Date, referrer: Option[String], remote_ip: Option[String])

trait LinkRepo extends Actor {
  implicit private val ec = context.dispatcher

  val linkRepository: LinkRepositoryComponent#LinkRepository

  private val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe(linkTopic, self)

  def receive: Receive = {
    case SubscribeAck(Subscribe(`linkTopic`, None, `self`)) => context become ready
  }

  def ready: Receive = {
    case PassThrough(code, referrer, remote_ip) =>
      linkRepository.passThrough(code, Option(referrer), Option(remote_ip)) pipeTo sender()

    case ShortenLink(userId, url, folderId) =>
      linkRepository.shortenUrl(userId, url, folderId) pipeTo sender()

    case ListClicks(userId, code, offset, limit) =>
      linkRepository.listClicks(userId, code, offset, limit).map{
        _.fold(error => Left(error), l => Right(l.map(click => Clck(click.date, click.referrer, click.remote_ip))))
      } pipeTo sender()

    case ListLinks(userId, folderId, offset, limit) =>
      linkRepository.listLinks(userId, folderId, offset, limit).map{
        _.fold(error => Left(error), l => Right(l.map(link => UrlCode(link.url, link.code))))
      } pipeTo sender()

    case ListFolders(userId) =>
      linkRepository.listFolders(userId).map(Folders) pipeTo sender()

    case GetLinkSummary(userId, code) =>
      linkRepository.linkSummary(userId, code) pipeTo sender()

    case _ => sender() ! Error(ErrorCode.Unknown)
  }

}