# About
 SlickURL is a slick clusterable RESTful service, that allows to shorten links, arrange them in folders,
 and gather click count and other statistics. It is Scala-based and uses Spray, Akka, Slick and distributed pub-sub.
 Akka's clustering, distributed pub-sub as well as deployment support allows to decouple services and make them truly distributable.
 Spray together with detachable context handling actors allow http request processing to run smoothly in such
 distributed and decoupled environment. Those actors are created for each request and eventually complete it
 either with timeouts or with responses to distributed messages sent over the cluster. Anyway requests
 are handled properly and the dedicated actors are stopped correctly so that there are no dangling ones.
 Finally, Slick allows of getting the best out of accessing a relational data store.
