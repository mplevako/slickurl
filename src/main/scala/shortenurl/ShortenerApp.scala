package shortenurl

import akka.actor.{ActorSystem, Props}
import akka.contrib.pattern.DistributedPubSubExtension
import akka.io.IO
import akka.routing.RoundRobinRouter
import com.typesafe.config.ConfigFactory
import shortenurl.actor.{Shortener, UserRepoFactory}
import shortenurl.domain.repository.{UserRepositoryComponent, UserTable}
import spray.can.Http

import scala.slick.driver.JdbcProfile
import scala.slick.jdbc.meta.MTable

object ShortenerApp extends App with UserRepositoryComponent with UserTable {
  val config = ConfigFactory.load()

  override val profile: JdbcProfile = scala.slick.driver.PostgresDriver
  override val db: JdbcProfile#Backend#Database = profile.simple.Database.forConfig("db.users")
  override val userRepository: UserRepository = new UserRepositoryImpl

  import shortenurl.ShortenerApp.profile.simple._
  db withSession { implicit session =>
    if (MTable.getTables("USER").list.isEmpty) {
      users.ddl.create
    }
  }

  //start the cluster
  implicit val system = ActorSystem("ClusterSystem", config)

  //start the user repo
  system.actorOf(Props(classOf[UserRepoFactory], userRepository), name = "user-repo")

  //start the server
  val mediator = DistributedPubSubExtension(system).mediator
  val shorteningService = system.actorOf(Props(classOf[Shortener], mediator).withRouter(RoundRobinRouter(nrOfInstances = 1)), name = "shortener")
  IO(Http) ! Http.Bind(shorteningService, interface = config.getString("app.http.server.if"), port = config.getInt("app.http.server.port"))
}
