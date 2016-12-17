package slickurl.domain.repository

import java.util.Date

import slickurl.AppProps._
import slickurl.domain.model._
import slick.dbio.Effect._
import slickurl.AppProps

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait LinkRepositoryComponent {
  this: ClickTable with LinkTable with FolderTable =>

  val linkRepository: LinkRepository

  trait LinkRepository {

    def listFolders(userId: UserID): Future[Seq[Folder]]

    /**
     * returns CodeIsUsed if link.code is already used for this user + url + folder combination
     */
    def shortenUrl(userId: UserID, url: String, folderId: Option[Long])(implicit ec: ExecutionContext): Future[Error Either Link]

    /**
     * returns links with untouched URLs
     */
    def listLinks(userId: UserID, folderId: Option[Long], offset: Option[Long], limit: Option[Long])(implicit ec: ExecutionContext): Future[Error Either Seq[Link]]

    /**
     * Please note that the link's URL (if any) is returned as is, i.e. as it was registered
     *
     * @return link url to pass through if it exists
     */
    def passThrough(code: String, referrer: Option[String], remote_ip: Option[String])(implicit ec: ExecutionContext): Future[Error Either String]

    def listClicks(userId: UserID, code: String, offset: Option[Long], limit: Option[Long])(implicit ec: ExecutionContext): Future[Error Either Seq[Click]]

    def linkSummary(userId: UserID, code: String)(implicit ec: ExecutionContext): Future[Error Either LinkSummary]
  }

  class LinkRepositoryImpl extends LinkRepository with SQLStateErrorCodeTranslator {

    import links._
    import profile.api._
    import DBIO._
    import slick.jdbc.TransactionIsolation._

    private def codeExists(code: Option[String], userId: Option[UserID]): DBIOAction[Boolean, NoStream, Read] = code match {
      case None => successful(false)
      case Some(c) =>
        val link = filter(_.code === c)
        userId.fold(link)(userId => link.filter(_.uid === userId)).exists.result
    }

    private def urlForCode(code: String): DBIO[Option[String]] = filter(_.code === code).map(_.url).result.headOption

    private def folderExists(userId: UserID, folderId: Long): DBIO[Boolean] =
      folders.filter(_.uid === userId).filter(_.id === folderId).exists.result

    private def shorten(userId: UserID, url: String, folderId: Option[Long])
                       (implicit ec: ExecutionContext): DBIOAction[Error Either Link, NoStream, All] =
      idSequence.next.result flatMap { id =>
        val code = AlphabetCodec.encode(id)
        val link = Link(userId, url, code, folderId)

        (links += link).asTry map {
          case Success(_) => Right(link)
          case Failure(t) => Left(translateException(t))
        }
      }

    /**
     * returns CodeIsUsed if link.code is already used for this user + code + folder combination
     * link's URL have to be valid (e.g. punycoded), properly escaped and encoded
     */
    override def shortenUrl(userId: UserID, url: String, folderId: Option[Long])
                           (implicit ec: ExecutionContext): Future[Error Either Link] = db run {
      val shortenAction = shorten(userId, url, folderId)
      folderId.fold(shortenAction) {
        folderExists(userId, _) flatMap {
          case false => successful(Left(Error(ErrorCode.InvalidFolder)))
          case true  => shortenAction
        }
      }.transactionally.withTransactionIsolation(Serializable)
    }

    override def listFolders(userId: UserID): Future[Seq[Folder]] =
      db run folders.filter(_.uid === userId).result.transactionally.withTransactionIsolation(ReadCommitted)

    /**
     * returns links with untouched URLs
     */
    override def listLinks(userId: UserID, folderId: Option[Long], offset: Option[Long], limit: Option[Long])(implicit ec: ExecutionContext): Future[Error Either Seq[Link]] = {
      def linksForFolder(folderId: Option[Long]) = {
        var linksForFolder = links.filter(_.uid === userId)
        linksForFolder = folderId.fold(linksForFolder)(fid => linksForFolder.filter(_.fid === fid))
        linksForFolder = offset.fold(linksForFolder)(off => linksForFolder.drop(off))
        limit.fold(linksForFolder)(lim => linksForFolder.take(lim)).result.map(Right(_))
      }

      db run {
        (folderId match {
          case None      => linksForFolder(None)
          case Some(fid) => folderExists(userId, fid) flatMap {
            case false => successful(Left(Error(ErrorCode.InvalidFolder)))
            case true  => linksForFolder(folderId)
          }
        }).transactionally.withTransactionIsolation(ReadCommitted)
      }
    }

    /**
     * Please note that the link's URL (if any) is returned as is, i.e. as it was registered
     *
     * @return link url to pass through if it exists
     */
    override def passThrough(code: String, referrer: Option[String], remote_ip: Option[String])(implicit ec: ExecutionContext): Future[Error Either String] = db run {
      urlForCode(code).map {
        case None      => Left(Error(ErrorCode.NonExistentCode))
        case Some(url) =>
          clicks += Click(code, new Date(), referrer, remote_ip)
          Right(url)
      }.transactionally.withTransactionIsolation(Serializable)
    }

    override def listClicks(userId: UserID, code: String, offset: Option[Long], limit: Option[Long])(implicit ec: ExecutionContext): Future[Error Either Seq[Click]] = db run {
      codeExists(Option(code), Option(userId)).flatMap {
        case false => successful(Left(Error(ErrorCode.NonExistentCode)))
        case true  =>
          val clicksForCodeAndUser = clicks.join(links).on(_.code === _.code).filter(_._1.code === code).filter(_._2.uid === userId)
          var clicksList = clicksForCodeAndUser.map(_._1)
          clicksList = offset.fold(clicksList)(off => clicksList.drop(off))
          limit.fold(clicksList)(lim => clicksList.take(lim)).result.map(Right(_))
      }.transactionally.withTransactionIsolation(ReadCommitted)
    }

    override def linkSummary(userId: UserID, code: String)(implicit ec: ExecutionContext): Future[Error Either LinkSummary] = db run {
      codeExists(Option(code), Option(userId)).flatMap {
        case false => successful(Left(Error(ErrorCode.NonExistentCode)))
        case true  =>
          val linkWithClicks = links.joinLeft(clicks).on(_.code === _.code).filter(_._1.code === code).filter(_._1.uid === userId)
          val linkGroup = linkWithClicks.groupBy{ case (l, _) => (l.code, l.url, l.fid) }
          val summary = linkGroup.map{case (x, q) => x -> q.map(_._2).length}
          summary.result.head.map(s => Right(LinkSummary(s._1._2, s._1._1, s._1._3, s._2)))
      }.transactionally.withTransactionIsolation(ReadCommitted)
    }
  }
}
