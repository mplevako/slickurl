package slickurl

import java.nio.file.{Files, Paths}
import java.security.{KeyFactory, PrivateKey, PublicKey}
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}

import com.typesafe.config.ConfigFactory
import pdi.jwt.JwtAlgorithm
import pdi.jwt.algorithms.JwtAsymetricAlgorithm
import slickurl.domain.model.AlphabetCodec

import scala.concurrent.duration.{FiniteDuration, NANOSECONDS}
import scala.util.Try

object AppProps {
  private val appConfig = ConfigFactory.load().getConfig("app")

  private[slickurl] val idLength: Long = appConfig.getLong("id.length")
  private[slickurl] val maxId: Long    = (1L << idLength) - 1

  private[slickurl] val shardIdLenght: Long = java.lang.Long.SIZE - idLength
  private[slickurl] val shardIdMask: Long   = -1L ^ (-1L << shardIdLenght)
  private[slickurl] val maxShardId: Long    = 1 << shardIdLenght

  private[slickurl] val encodingAlphabet: String = appConfig.getString("encoding.alphabet ")
  private[slickurl] val maxURLCode: String = AlphabetCodec.encode(Long.MinValue)

  import scala.concurrent.duration.Duration.fromNanos

  private[slickurl] val httpServerPort: Int = appConfig.getInt("http.server.port")
  private[slickurl] val httpServerIf: String = appConfig.getString("http.server.if")
  private[slickurl] val httpHandlerTimeout: FiniteDuration = fromNanos(appConfig.getDuration("http.handler.timeout", NANOSECONDS))

  private[slickurl] val tokenGroup: Some[String] = Some(appConfig.getString("topics.token"))
  private[slickurl] val tokenTopic: String = appConfig.getString("topics.token")
  private[slickurl] val linkTopic: String = appConfig.getString("topics.link")

  private[slickurl] val apiPrivateKey: PrivateKey = {
    val pkb = Files.readAllBytes(Paths.get(appConfig.getString("api.privatekey")))
    val keySpec = new PKCS8EncodedKeySpec(pkb)
    KeyFactory.getInstance("RSA").generatePrivate(keySpec)
  }
  private[slickurl] val apiPublicKey: PublicKey = {
    val pkb = Files.readAllBytes(Paths.get(appConfig.getString("api.publickey")))
    val keySpec = new X509EncodedKeySpec(pkb)
    KeyFactory.getInstance("RSA").generatePublic(keySpec)
  }
  private[slickurl] val apiTokenAlgorithm: JwtAsymetricAlgorithm = JwtAlgorithm.RS256

  require(httpServerPort > 0)
  require(idLength >= 1 && idLength <= java.lang.Long.SIZE - 1)

  require(encodingAlphabet != null && encodingAlphabet.nonEmpty)
  require(tokenTopic != null       && linkTopic != null)
  require(tokenTopic.nonEmpty      && linkTopic.nonEmpty)
  require(httpServerIf != null     && httpServerIf.nonEmpty)
}

object DbProps {
  private val dbConfig = ConfigFactory.load().getConfig("db")

  private[slickurl] val idSequenceStart: Long = dbConfig.getLong("id.sequence.start")
  private[slickurl] val idSequenceInc: Long = dbConfig.getLong("id.sequence.inc")

  private[slickurl] val schemaName      = Try(dbConfig.getString("schema.name")).toOption
  private[slickurl] val folderTableName = "folder"
  private[slickurl] val clickTableName  = "click"
  private[slickurl] val userTableName   = "user"
  private[slickurl] val linkTableName   = "link"

  private[slickurl] val userIdSequenceName = "user_id_sequence"
  private[slickurl] val linkIdSequenceName = "link_id_sequence"

  require(idSequenceStart >= 0 && idSequenceInc > 0)
}