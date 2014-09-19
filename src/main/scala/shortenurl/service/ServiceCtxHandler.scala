package shortenurl.service

import akka.actor.{Actor, ReceiveTimeout}
import akka.contrib.pattern.DistributedPubSubExtension
import com.typesafe.config.ConfigFactory
import shortenurl.domain.model.Error
import spray.routing.RequestContext

import scala.concurrent.duration._

class ServiceCtxHandler(val ctx: RequestContext) extends Actor{
  context.setReceiveTimeout(ConfigFactory.load().getInt("app.http.handler.timeout") milliseconds)
  val mediator = DistributedPubSubExtension(context.system).mediator

  def receive = {
      case ReceiveTimeout =>
      context.setReceiveTimeout (Duration.Undefined)
      context.stop(self)

      case Error(msg) =>
        context.setReceiveTimeout(Duration.Undefined)
        ctx.complete(msg)
        context.stop(self)
  }
}
