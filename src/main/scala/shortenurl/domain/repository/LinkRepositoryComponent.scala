package shortenurl.domain.repository

import com.typesafe.config.ConfigFactory
import shortenurl.domain.model.{CodeAlreadyTaken, Link}
import shortenurl.urlcodec.URLCodec

trait LinkRepositoryComponent { this: LinkTable =>
  val shortUrlAlphabet = ConfigFactory.load().getString("app.shorturl.alphabet")

  val linkRepository: LinkRepository

  trait LinkRepository {
    //returns CodeIsUsed if link.code is already used for this user + url + folder combination
    def shortenUrl(link: Link): Either[CodeAlreadyTaken.type, Link]
  }

  class LinkRepositoryImpl extends LinkRepository {

    import profile.simple._

    //lookup a code for a given user + url + code + folder combination
    private def lookupCode(uid: Long, url: String, code: Option[String], folderId: Option[Long]): Option[Link] = code match {
      case None    => None
      case Some(c) => db withSession { implicit session =>
          links.filter(l => {l.uid === uid && l.url === url && l.fid === folderId && l.code === code}).firstOption
      }
    }

    //returns CodeIsUsed if link.code is already used for this user + url + folder combination
    override def shortenUrl(link: Link): Either[CodeAlreadyTaken.type, Link] =
      lookupCode(link.uid, link.url, link.code, link.folderId) match {
        case None => link.code match {
          case Some(code)=> db withTransaction { implicit session =>
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
        case Some(_) => Left(CodeAlreadyTaken)
      }
  }
}
