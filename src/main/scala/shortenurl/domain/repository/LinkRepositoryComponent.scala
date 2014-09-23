/**
 * Copyright 2014 Maxim Plevako
 **/
package shortenurl.domain.repository

import java.util.Date

import com.typesafe.config.ConfigFactory
import shortenurl.domain.model._
import shortenurl.urlcodec.URLCodec

trait LinkRepositoryComponent {
  this: ClickTable with LinkTable with FolderTable =>

  val shortUrlAlphabet = ConfigFactory.load().getString("app.shorturl.alphabet")

  val linkRepository: LinkRepository

  trait LinkRepository {

    def listFolders(userId: Long): List[Folder]

    /**
     * returns CodeIsUsed if link.code is already used for this user + url + folder combination
     */
    def shortenUrl(link: Link): Either[Error, Link]

    /**
     * returns links with untouched URLs
     */
    def listLinks(userId: Long, folderId: Option[Long], offset: Option[Long],
                  limit: Option[Long]): Either[Error, List[Link]]

    /**
     * Please note that the link's URL (if any) is returned as is, i.e. as it was registered
     *
     * @return link url to pass through if it exists
     */
    def passThrough(code: String, referer: String, remote_ip: String): Either[Error, String]

    def listClicks(code: String, userId: Long, offset: Option[Long], limit: Option[Long]): Either[Error, List[Click]]

    def linkSummary(code: String, userId: Long): Either[Error, LinkSummary]
  }

  class LinkRepositoryImpl extends LinkRepository {

    import profile.simple._

    private def codeExists(code: Option[String], userId: Option[Long]): Boolean = code match {
      case None => false
      case Some(c) => db withSession { implicit session =>
        var link = links.filter(_.code === code)
        if(userId nonEmpty) link = link.filter(_.uid === userId)
        link.map(_.code).exists.run
      }
    }

    private def urlForCode(code: String): Option[String] =
      db withSession { implicit session =>
        links.filter(_.code === code).map(_.url).firstOption
      }

    private def folderExists(uid: Long, folderId: Long): Boolean =
      db withTransaction { implicit session =>
        folders.filter(_.uid === uid).filter(_.id === folderId).map(_.id).exists.run
      }

    override def listFolders(uid: Long): List[Folder] =
      db withSession { implicit session =>
        folders.filter(_.uid === uid).list
      }

    /**
     * returns CodeIsUsed if link.code is already used for this user + code + folder combination
     * link's URL have to be valid (e.g. punycoded), properly escaped and encoded
     */
    override def shortenUrl(link: Link): Either[Error, Link] = {
      val folderId = link.folderId
      if (folderId.nonEmpty && !folderExists(link.uid, folderId.get)) Left(
        Error(ErrorCode.InvalidFolder))
      else if (codeExists(link.code, None)) Left(Error(ErrorCode.CodeTaken))
      else {
        link.code match {
          case Some(code) => db withTransaction { implicit session =>
            links.insert(link)
            Right(link)
          }
          case None => db withTransaction { implicit session =>
            //val nextCode = codeSequence.next.run
            val nextCode = codeSequence.next.first
            val shortLink = link.copy(code = Some(URLCodec.encode(shortUrlAlphabet, nextCode)))
            links.insert(shortLink)
            Right(shortLink)
          }
        }
      }
    }

    override def listLinks(userId: Long, folderId: Option[Long], offset: Option[Long],
                           limit: Option[Long]): Either[Error, List[Link]] =
      db withSession { implicit session =>
        if (folderId.nonEmpty && !folderExists(userId, folderId.get))
          Left(Error(ErrorCode.InvalidFolder))
        else {
          var linksForFolder = links.filter(_.uid === userId)
          if (folderId.nonEmpty) linksForFolder = linksForFolder.filter(_.fid === folderId)
          if (offset.nonEmpty) linksForFolder = linksForFolder.drop(offset.get)
          limit match {
            case None => Right(linksForFolder.list)
            case Some(n) => Right(linksForFolder.take(n).list)
          }
        }
      }

    /**
     * @return link url to pass through
     */
    override def passThrough(code: String, referer: String, remote_ip: String) =
      db withTransaction { implicit session =>
        urlForCode(code) match {
          case None => Left(Error(ErrorCode.NonExistentCode))
          case Some(url) =>
            clicks.insert(Click(code, new Date(), referer, remote_ip))
            Right(url)
        }
      }

    override def listClicks(code: String, userId: Long, offset: Option[Long],
                            limit: Option[Long]) =
      db withTransaction { implicit session =>
        codeExists(Some(code), Some(userId)) match {
            case false => Left(Error(ErrorCode.NonExistentCode))
            case true =>
              val clicksForCodeAndUser = clicks.innerJoin(links).on(_.code === _.code).
                                          filter(_._1.code === code).filter(_._2.uid === userId)
              var clicksList = clicksForCodeAndUser.map(_._1)
              if (offset.nonEmpty) clicksList = clicksList.drop(offset.get)
              Right(limit match {
                      case None => clicksList.list
                      case Some(n) => clicksList.take(n).list
                    })
        }
      }

    override def linkSummary(code: String, userId: Long): Either[Error, LinkSummary] =
      db withTransaction { implicit session =>
        if (!codeExists(Some(code), Some(userId)))
          Left(Error(ErrorCode.NonExistentCode))
        else {
//          val linkWithClicks = links.leftJoin(clicks).on(_.code === _.code).filter(_._1.code === code).filter(_._1.uid === userId)
//          val linkGroup = linkWithClicks.groupBy{ case (l,c) => (l.code, l.url, l.fid) }
//          val s = linkGroup.map{case (x, q) => x -> q.map(_._2.code).count)}.first

          //as there's no count yet let's play with zipWithIndex
          val linkWithClicks = links.leftJoin(clicks.zipWithIndex).on(_.code === _._1.code).filter(_._1.code === code).filter(_._1.uid === userId)
          val linkGroup = linkWithClicks.groupBy{ case (l, (c, idx)) => (l.code, l.url, l.fid) }
          val s = linkGroup.map{case (x, q) => x -> q.map(_._2._2).countDistinct}.first
          Right(LinkSummary(s._1._2, s._1._1, s._1._3, s._2))
        }
      }
  }
}
