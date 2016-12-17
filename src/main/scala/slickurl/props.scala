package slickurl

import java.nio.file.{Files, Paths}
import java.security.{KeyFactory, PrivateKey, PublicKey}
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}

import com.typesafe.config.ConfigFactory
import pdi.jwt.JwtAlgorithm
import pdi.jwt.algorithms.JwtAsymetricAlgorithm

import scala.concurrent.duration.{FiniteDuration, NANOSECONDS}

object AppProps {
  private val appConfig = ConfigFactory.load().getConfig("app")

  private[slickurl] val encodingAlphabet: String = appConfig.getString("encoding.alphabet ")

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

  require(encodingAlphabet != null && !encodingAlphabet.isEmpty)
  require(httpServerIf != null && !httpServerIf.isEmpty)
  require(httpServerPort > 0)
  require(tokenTopic != null && linkTopic != null)
  require(!tokenTopic.isEmpty && !linkTopic.isEmpty)
}

object DbProps {
  private val dbConfig = ConfigFactory.load().getConfig("db")

  private[slickurl] val idSequenceStart: Long = dbConfig.getLong("id.sequence.start")
  private[slickurl] val idSequenceInc: Long = dbConfig.getLong("id.sequence.inc")

  require(idSequenceStart >= 0 && idSequenceInc > 0)
}