package shortenurl.domain.repository

import com.typesafe.config.ConfigFactory
import shortenurl.domain.model.{Error, ErrorCode, Link}
import shortenurl.urlcodec.URLCodec

trait LinkRepositoryComponent { this: LinkTable =>
  val shortUrlAlphabet = ConfigFactory.load().getString("app.shorturl.alphabet")

  val linkRepository: LinkRepository

  trait LinkRepository {
    //returns CodeIsUsed if link.code is already used for this user + url + folder combination
    def shortenUrl(link: Link): Either[Error, Link]
    def listLinks(userId: Long, folderId: Long, offset: Long = 0, limit: Option[Long]): List[Link]
  }

  class LinkRepositoryImpl extends LinkRepository {

    import profile.simple._

    private def codeExists(code: Option[String]): Boolean = code match {
      case None    => false
      case Some(c) => db withSession { implicit session =>
          links.filter(_.code === code).map(_.code).exists.run
      }
    }

    private def folderExists(folderId: Long): Boolean =
      db withTransaction { implicit session =>
        folders.filter(_.id === folderId).map(_.id).exists.run
      }

    //returns CodeIsUsed if link.code is already used for this user + code + folder combination
    override def shortenUrl(link: Link): Either[Error, Link] = {
      val folderId = link.folderId
      if (folderId.nonEmpty && !folderExists(folderId.get)) Left(Error(ErrorCode.InvalidFolder))
      else if (codeExists(link.code)) Left(Error(ErrorCode.CodeTaken))
      else {
        link.code match {
          case Some(code) => db withTransaction { implicit session =>
            links.insert(link)
            Right(link)
          }
          case None => db withTransaction { implicit session =>
            //val nextCode = codeSequence.next.run
            val nextCode = codeSequence.next.firstOption.getOrElse(-1L)
            val shortLink = link.copy(code = Some(URLCodec.encode(shortUrlAlphabet, nextCode)))
            links.insert(shortLink)
            Right(shortLink)
          }
        }
      }
    }

    override def listLinks(userId: Long, folderId: Long, offset: Long,
                           limit: Option[Long]): List[Link] =
      db withSession { implicit session =>
        val linksForFolder = links.filter(_.uid === userId).filter(_.fid === folderId).drop(offset)
        limit match {
          case None    => linksForFolder.list
          case Some(n) => linksForFolder.take(n).list
        }
      }
  }
}
