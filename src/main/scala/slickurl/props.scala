package slickurl

import com.typesafe.config.ConfigFactory

import scala.concurrent.duration.{FiniteDuration, NANOSECONDS}

object AppConfig {
  private val appConfig = ConfigFactory.load().getConfig("app")

  private[slickurl] val apiSecret: String = appConfig.getString("api.secret")
  private[slickurl] val encodingAlphabet: String = appConfig.getString("encoding.alphabet ")

  import scala.concurrent.duration.Duration.fromNanos

  private[slickurl] val httpServerPort: Int = appConfig.getInt("http.server.port")
  private[slickurl] val httpServerIf: String = appConfig.getString("http.server.if")
  private[slickurl] val httpHandlerTimeout: FiniteDuration = fromNanos(appConfig.getDuration("http.handler.timeout", NANOSECONDS))

  private[slickurl] val userRepoTopic: String = appConfig.getString("topics.user.repo")
  private[slickurl] val linkRepoTopic: String = appConfig.getString("topics.link.repo")

  require(apiSecret != null && !apiSecret.isEmpty)
  require(encodingAlphabet != null && !encodingAlphabet.isEmpty)
  require(httpServerIf != null && !httpServerIf.isEmpty)
  require(httpServerPort > 0)
  require(userRepoTopic != null && linkRepoTopic != null)
  require(!userRepoTopic.isEmpty && !linkRepoTopic.isEmpty)
}

object DbConfig {
  private val dbConfig = ConfigFactory.load().getConfig("db")

  private[slickurl] val codeSequenceName: String = dbConfig.getString("code.sequence.name")
  private[slickurl] val codeSequenceStart: Long = dbConfig.getLong("code.sequence.start")
  private[slickurl] val codeSequenceInc: Long = dbConfig.getLong("code.sequence.inc")

  require(codeSequenceName != null && !codeSequenceName.isEmpty)
  require(codeSequenceStart >= 0 && codeSequenceInc > 0)
}