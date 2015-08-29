/**
 * Copyright 2014-2015 Maxim Plevako
 **/
package shortenurl

import akka.actor._
import akka.cluster.Cluster
import akka.contrib.pattern.DistributedPubSubExtension
import akka.io.IO
import akka.routing.RoundRobinPool
import com.typesafe.config.ConfigFactory
import shortenurl.actor.{LinkRepo, Shortener, UserRepo}
import shortenurl.domain.repository._
import slick.dbio.DBIO._
import slick.driver.JdbcProfile
import slick.jdbc.meta.MTable
import spray.can.Http

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object SlickURL extends App {
  val config = ConfigFactory.load()

  //start the cluster
  val systemName: String = "ShortenerSystem"
  implicit val system = ActorSystem(systemName, config)
  val joinAddress = Cluster(system).selfAddress
  Cluster(system).join(joinAddress)
  new LinkRepositoryApp

  Thread.sleep(1000)
  val userSystem = ActorSystem(systemName, config)
  Cluster(userSystem).join(joinAddress)
  new UserRepositoryApp(userSystem)

  //start the server
  Thread.sleep(1000)
  val mediator = DistributedPubSubExtension(system).mediator
  val shorteningService = system.actorOf(Props(classOf[Shortener], mediator).withRouter(RoundRobinPool(nrOfInstances = 1)), name = "shortener")
  IO(Http) ! Http.Bind(shorteningService, interface = config.getString("app.http.server.if"), port = config.getInt("app.http.server.port"))
}

class UserRepositoryApp(val system: ActorSystem) extends UserTable {

  override val profile: JdbcProfile = slick.driver.PostgresDriver
  override val db: profile.api.Database = profile.api.Database.forConfig("db.users")

  import profile.api._
  implicit val ec = system.dispatcher

  val initUserDb = db run (MTable.getTables("USER") flatMap {
    case v if v.isEmpty => users.schema.create
    case v => successful(())
  })

  Await.result(initUserDb, Duration.Inf)
  //start the user repo
  system.actorOf(Props(classOf[UserRepoFactory]), name = "user-repo")
}

class LinkRepositoryApp(implicit val system: ActorSystem) extends LinkTable with FolderTable
                                                                  with ClickTable{

  override val profile: JdbcProfile = slick.driver.PostgresDriver
  override val db: profile.api.Database = profile.api.Database.forConfig("db.links")

  import profile.api._
  implicit val ec = system.dispatcher

  val initLinksDb = db run (MTable.getTables("LINK") flatMap {
    case v if v.isEmpty => (folders.schema ++ codeSequence.schema ++ links.schema ++ clicks.schema).create
    case v => successful(())
  })

  Await.result(initLinksDb, Duration.Inf)
  //start the links repo
  system.actorOf(Props(classOf[LinkRepoFactory]), name = "link-repo")
}

class UserRepoFactory extends IndirectActorProducer{
  override def actorClass = classOf[UserRepo]

  override def produce(): Actor = new UserRepo with UserRepositoryComponent with UserTable {
    override val profile: JdbcProfile = slick.driver.PostgresDriver
    override val db: profile.api.Database = profile.api.Database.forConfig("db.users")
    override val userRepository: UserRepository = new UserRepositoryImpl
  }
}

class LinkRepoFactory extends IndirectActorProducer {

  override def actorClass = classOf[LinkRepo]

  override def produce(): Actor = new LinkRepo with LinkRepositoryComponent with LinkTable
                                               with FolderTable with ClickTable {
    override val profile: JdbcProfile = slick.driver.PostgresDriver
    override val db: profile.api.Database = profile.api.Database.forConfig("db.links")
    override val linkRepository: LinkRepository = new LinkRepositoryImpl
  }
}