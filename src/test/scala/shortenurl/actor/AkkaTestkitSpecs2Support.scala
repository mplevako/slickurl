package shortenurl.actor

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.specs2.specification.{AfterExample, Scope}

abstract class AkkaTestkitSpecs2Support extends TestKit(ActorSystem("ActorSpecSystem", ConfigFactory.load("test"))) with Scope with ImplicitSender with AfterExample {
  override protected def after: Any = system.shutdown()
}
