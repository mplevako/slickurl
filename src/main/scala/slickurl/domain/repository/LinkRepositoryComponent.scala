package slickurl.domain.repository

import java.util.Date

import slickurl.AppConfig._
import slickurl.domain.model._
import slick.dbio.Effect._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait LinkRepositoryComponent {
  this: ClickTable with LinkTable with FolderTable =>

  val linkRepository: LinkRepository

  trait LinkRepository {

    def listFolders(userId: Long): Future[Seq[Folder]]

    /**
     * returns CodeIsUsed if link.code is already used for this user + url + folder combination
     */
    def shortenUrl(link: Link)(implicit ec: ExecutionContext): Future[Error Either Link]

    /**
     * returns links with untouched URLs
     */
    def listLinks(userId: Long, folderId: Option[Long], offset: Option[Long], limit: Option[Long])(implicit ec: ExecutionContext): Future[Error Either Seq[Link]]

    /**
     * Please note that the link's URL (if any) is returned as is, i.e. as it was registered
     *
     * @return link url to pass through if it exists
     */
    def passThrough(code: String, referer: Option[String], remote_ip: Option[String])(implicit ec: ExecutionContext): Future[Error Either String]

    def listClicks(code: String, userId: Long, offset: Option[Long], limit: Option[Long])(implicit ec: ExecutionContext): Future[Error Either Seq[Click]]

    def linkSummary(code: String, userId: Long)(implicit ec: ExecutionContext): Future[Error Either LinkSummary]
  }

  class LinkRepositoryImpl extends LinkRepository with SQLStateErrorCodeTranslator {

    import links._
    import profile.api._
    import DBIO._
    import slick.jdbc.TransactionIsolation._

    private def codeExists(code: Option[String], userId: Option[Long]): DBIOAction[Boolean, NoStream, Read] = code match {
      case None => successful(false)
      case Some(c) =>
        val link = filter(_.code === c)
        userId.fold(link)(uid => link.filter(_.uid === uid)).exists.result
    }

    private def urlForCode(code: String): DBIO[Option[String]] = filter(_.code === code).map(_.url).result.headOption

    private def folderExists(userId: Long, folderId: Long): DBIO[Boolean] =
      folders.filter(_.uid === userId).filter(_.id === folderId).exists.result

    private def checkCodeAndShorten(link: Link)(implicit ec: ExecutionContext): DBIOAction[Error Either Link, NoStream, All] = {
      def tryInsertLink(link: Link): DBIOAction[Error Either Link, NoStream, Write] = (links += link).asTry map {
        case Success(_) => Right(link)
        case Failure(t) => Left(translateException(t))
      }

      def generateNextURL(link: Link): Long => DBIOAction[Error Either Link, NoStream, Effect.Write] = (nextCode: Long) => {
        val shortLink = link.copy(code = Option(AlphabetCodec.encode(encodingAlphabet, nextCode)))
        tryInsertLink(shortLink)
      }

      codeExists(link.code, None).flatMap {
        case true  => successful(Left(Error(ErrorCode.CodeTaken)))
        case false => link.code.fold(idSequence.next.result flatMap generateNextURL(link))(_ => tryInsertLink(link))
      }
    }

    private def tryShortenUrl(link: Link)(implicit ec: ExecutionContext): Future[Error Either Link] = db run {
      link.folderId.fold(checkCodeAndShorten(link)) {
        folderExists(link.uid, _) flatMap {
          case false => successful(Left(Error(ErrorCode.InvalidFolder)))
          case true  => checkCodeAndShorten(link)
        }
      }.transactionally.withTransactionIsolation(Serializable)
    }

    /**
     * returns CodeIsUsed if link.code is already used for this user + code + folder combination
     * link's URL have to be valid (e.g. punycoded), properly escaped and encoded
     */
    override def shortenUrl(link: Link)(implicit ec: ExecutionContext): Future[Error Either Link] = tryShortenUrl(link).flatMap(
      _.fold({
        case Error(ErrorCode.Duplicate) =>
          val linkFuture = db.run(checkCodeAndShorten(link).transactionally.withTransactionIsolation(Serializable))
          //this time treat all errors as unknown
          linkFuture.map(_.fold(_ => Left(Error(ErrorCode.Unknown)), Right(_)))
        case _ => Future successful Left(Error(ErrorCode.Unknown))
      }, shortLink => Future successful Right(shortLink))
    )

    override def listFolders(userId: Long): Future[Seq[Folder]] =
      db run folders.filter(_.uid === userId).result.transactionally.withTransactionIsolation(ReadCommitted)

    /**
     * returns links with untouched URLs
     */
    override def listLinks(userId: Long, folderId: Option[Long], offset: Option[Long], limit: Option[Long])(implicit ec: ExecutionContext): Future[Error Either Seq[Link]] = {
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
    override def passThrough(code: String, referer: Option[String], remote_ip: Option[String])(implicit ec: ExecutionContext): Future[Error Either String] = db run {
      urlForCode(code).map {
        case None      => Left(Error(ErrorCode.NonExistentCode))
        case Some(url) =>
          clicks += Click(code, new Date(), referer, remote_ip)
          Right(url)
      }.transactionally.withTransactionIsolation(Serializable)
    }

    override def listClicks(code: String, userId: Long, offset: Option[Long], limit: Option[Long])(implicit ec: ExecutionContext): Future[Error Either Seq[Click]] = db run {
      codeExists(Option(code), Option(userId)).flatMap {
        case false => successful(Left(Error(ErrorCode.NonExistentCode)))
        case true  =>
          val clicksForCodeAndUser = clicks.join(links).on(_.code === _.code).filter(_._1.code === code).filter(_._2.uid === userId)
          var clicksList = clicksForCodeAndUser.map(_._1)
          clicksList = offset.fold(clicksList)(off => clicksList.drop(off))
          limit.fold(clicksList)(lim => clicksList.take(lim)).result.map(Right(_))
      }.transactionally.withTransactionIsolation(ReadCommitted)
    }

    override def linkSummary(code: String, userId: Long)(implicit ec: ExecutionContext): Future[Error Either LinkSummary] = db run {
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
