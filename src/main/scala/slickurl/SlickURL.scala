package slickurl

import akka.actor._
import akka.cluster.Cluster
import akka.cluster.pubsub.DistributedPubSub
import akka.io.IO
import akka.routing.RoundRobinPool
import com.typesafe.config.ConfigFactory
import slickurl.actor.Shortener
import slickurl.AppProps._
import slickurl.actor.shard.ShardSystem
import spray.can.Http

object SlickURL extends App {
  val config = ConfigFactory.load()

  import slick.driver.PostgresDriver

  //start the cluster
  val systemName: String = "ShortenerSystem"
  implicit val system = ActorSystem(systemName, config)
  val joinAddress = Cluster(system).selfAddress
  Cluster(system).join(joinAddress)
  new ShardSystem(1L, PostgresDriver, PostgresDriver.api.Database.forConfig("db.shards.A"))

  Thread.sleep(1000)
  val userSystem = ActorSystem(systemName, config)
  Cluster(userSystem).join(joinAddress)
  new ShardSystem(2L, PostgresDriver, PostgresDriver.api.Database.forConfig("db.shards.Z"))

  //start the server
  Thread.sleep(1000)
  val mediator = DistributedPubSub(system).mediator
  val shorteningService = system.actorOf(Props(classOf[Shortener], mediator).
                                 withRouter(RoundRobinPool(nrOfInstances = 1)), name = "shortener")
  IO(Http) ! Http.Bind(shorteningService, interface = httpServerIf, port = httpServerPort)
}