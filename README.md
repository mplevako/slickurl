# About SlickURL
 SlickURL is a slick clusterable sharded RESTful service, that allows to shorten links, arrange them in folders,
 and gather click count and other statistics. It is Scala-based and uses Spray, Akka, Reactive Slick and distributed pub-sub.
 
 Akka's clustering, distributed pub-sub as well as deployment support allow to decouple services and make them truly distributable.
 
 Spray together with detachable context handling actors allow http request processing to run smoothly in such
 distributed and decoupled environment. Those actors are created for each request and eventually complete it
 either with timeouts or with responses to distributed messages sent over the cluster. Anyway requests
 are handled properly and the dedicated actors are stopped correctly so that there are no dangling ones.
 
 Finally, Reactive Slick allows of getting the best out of accessing a relational data store asynchronously.
 
## Config
 Start by copying *resources/application.conf.template* to *resources/application.conf* and *resources/db.conf.template* to *resources/db.conf*.
 Generate RSA private/public key pair, store them in PKCS8/X509 encoded formats and put their paths in the *api.privatekey*/*api.publickey* entries in *application.conf*.
 Fill the entries for shardA's and shardZ's data sources. Optionally set the *db.schema.name* if you want to work in a distinct schema.

 You can optionally change the alphabet (*app.encoding.alphabet*) used to shorten URLs (by default it lacks hard to distinguish symbols 1,0,l,O,o), http handler timeout (*app.http.handler.timeout*),
 the interface (*app.http.server.if*) and the port(*app.http.server.port*) to run the http server on. You can even change the distributed pub-sub topic names used to exchange users, links, folders,
 statistics as well as errors.

 There is also the *id.length* knob for fine tuning the shard and entity identifier sizes and layout. Adjust that if you really need to.
 
## Setup
 To build the project you should install [SBT](http://www.scala-sbt.org/). Having installed it simply type
 
```
$ sbt compile
```

to build the project and 

```
$ sbt test
```

to test it.

 To run the shortening service (with two shards and on port 8080 by default) type
 
```
$ sbt run
```

## REST API
The services operate with JSON and have the following API

| Verb   | Resource           | Request Params                             | Response Data                                        | HTTP Status Code                                              |
|--------|--------------------|--------------------------------------------|------------------------------------------------------|---------------------------------------------------------------|
| POST   | /token             | `X-Token` header with a JWT                | A JWT token you have to provide back in the          | 200 OK                                                        |
|        |                    |                                            | `X-Authentication-Token` header when calling the     | 401 Unauthorized                                              |
|        |                    |                                            | following endpoints                                  |                                                               |
|        |                    |                                            |                                                      |                                                               |
| POST   | /link              | {url, folder_id [opt] }                    | link (url, code)                                     | 200 OK                                                        |
|        |                    | `X-Token` header with a JWT                | 'invalid_folder'  			                          | 400 Bad Request                                               |
|        |                    |                                            |                               			              | 401 Unauthorized                                              |
|        |                    |                                            |                                                      |                                                               |
| POST   | /link/$code        | {referrer, remote_ip }                     | link url to pass through                             | 200 OK                                                        |
|        |                    |                                            | 'nonexistent_code'                                   | 404 Not Found                                                 |
|        |                    |                                            |                               			              | 401 Unauthorized                                              |
|        |                    |                                            |                                                      |                                                               |
| GET    | /link/$code        | `X-Token` header with a JWT                | {link (url, code), folder_id (opt), count of clicks} | 200 OK                                                        |
|        |                    |                                            | 'nonexistent_code'                                   | 404 Not Found                                                 |
|        |                    |                                            |                               			              | 401 Unauthorized                                              |
|        |                    |                                            |                                                      |                                                               |
| GET    | /link              | {offset [opt = 0], limit [opt] }           | {list of links (url, code)}                          | 200 OK                                                        |
|        |                    | `X-Token` header with a JWT                |                                                      | 400 Bad Request if the offset or the limit are less than zero |
|        |                    |                                            |                                                      | 401 Unauthorized                                              |
|        |                    |                                            |                                                      |                                                               |
| GET    | /link/$code/clicks | {offset [opt = 0], limit [opt] }  	       | {list of clicks}                                     | 200 OK                                                        |
|        |                    | `X-Token` header with a JWT                | 'nonexistent_code'                                   | 404 Not Found                                                 |
|        |                    |                                            |                                                      | 400 Bad Request if the offset or the limit are less than zero |
|        |                    |                                            |                               			              | 401 Unauthorized                                              |
|        |                    |                                            |                                                      |                                                               |
| GET    | /folder/$id        | {offset [opt = 0], limit [opt] } 	       | {list of links (url, code)}                          | 200 OK                                                        |
|        |                    | `X-Token` header with a JWT                |                               			              | 401 Unauthorized                                              |
|        |                    |                                            | 'invalid_token'                                      | 400 Bad Request                                               |
|        |                    |                                            | 'invalid_folder'                                     | 400 Bad Request                                               |
|        |                    |                                            |                                                      | 400 Bad Request if the offset or the limit are less than zero |
|        |                    |                                            |                                                      |                                                               |
| GET    | /folder            | `X-Token` header with a JWT                | {list of folders (id, title) }                       | 200 OK                                                        |
|        |                    |                                            |                               			              | 401 Unauthorized                                              |