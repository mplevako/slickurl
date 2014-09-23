/**
 * Copyright 2014 Maxim Plevako
 **/
package shortenurl.service

import akka.actor.{Actor, ReceiveTimeout}
import akka.contrib.pattern.DistributedPubSubExtension
import com.typesafe.config.ConfigFactory
import org.json4s.{DefaultFormats, Formats}
import shortenurl.domain.model.{Error, ErrorCode}
import spray.http.StatusCodes
import spray.httpx.Json4sSupport
import spray.routing.RequestContext

import scala.concurrent.duration._

class ServiceCtxHandler(val ctx: RequestContext) extends Actor with Json4sSupport {
  context.setReceiveTimeout(ConfigFactory.load().getInt("app.http.handler.timeout") milliseconds)
  val mediator = DistributedPubSubExtension(context.system).mediator

  override implicit val json4sFormats: Formats = DefaultFormats

  def receive = {
    case ReceiveTimeout =>
      context.setReceiveTimeout (Duration.Undefined)
      ctx.complete(StatusCodes.GatewayTimeout)
      context.stop(self)

    case Error(code) => onError(code)

    case Left(Error(code)) => onError(code)
  }

  def onError(code: String): Unit = {
    context.setReceiveTimeout(Duration.Undefined)
    code match {
      case ErrorCode.WrongSecret | ErrorCode.InvalidToken => ctx.complete(StatusCodes.Unauthorized ,code)
      case ErrorCode.InvalidFolder | ErrorCode.CodeTaken => ctx.complete(StatusCodes.BadRequest ,code)
      case ErrorCode.Unknown => ctx.complete(StatusCodes.InternalServerError ,code)
      case ErrorCode.NonExistentCode => ctx.complete(StatusCodes.NotFound, code)
      case _ => ctx.complete(StatusCodes.InternalServerError, code)
    }

    context.stop(self)
  }
}

