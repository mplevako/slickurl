package shortenurl.actor

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.specs2.specification.{AfterExample, Scope}

abstract class AkkaTestkitSpecs2Support extends TestKit(ActorSystem("ActorSpecSystem")) with Scope with ImplicitSender with AfterExample {
  override protected def after: Any = system.shutdown()
}
