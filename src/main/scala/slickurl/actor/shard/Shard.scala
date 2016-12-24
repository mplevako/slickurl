package slickurl.actor.shard

import akka.actor.Actor
import slickurl.AppProps

trait Shard extends Actor {
  val shardId: Long

  require(0 <= shardId && shardId <= AppProps.maxShardId)
}
