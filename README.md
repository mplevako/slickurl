# About SlickURL
 SlickURL is a slick clusterable RESTful service, that allows to shorten links, arrange them in folders,
 and gather click count and other statistics. It is Scala-based and uses Spray, Akka, Reactive Slick and distributed pub-sub.
 
 Akka's clustering, distributed pub-sub as well as deployment support allow to decouple services and make them truly distributable.
 
 Spray together with detachable context handling actors allow http request processing to run smoothly in such
 distributed and decoupled environment. Those actors are created for each request and eventually complete it
 either with timeouts or with responses to distributed messages sent over the cluster. Anyway requests
 are handled properly and the dedicated actors are stopped correctly so that there are no dangling ones.
 
 Finally, Reactive Slick allows of getting the best out of accessing a relational data store asynchronously.
 
## Config
 Start by copying *resources\db.conf.template* to *resources\db.conf* and filling the entries for the user database and the links/folders/statistics one. Feel free to make them different.
 
 You can also edit *application.conf* to change your *api.secret*, the alphabet (*app.shorturl.alphabet*) used to shorten URLs (by
 default it lacks hard to distinguish symbols 1,0,l,O,o), http handler timeout (*app.http.handler.timeout*), the interface (*app.http.server.if*) and the port(*app.http.server.port*) to run the http server on. You can even change the distributed pub-sub topic names used to exchange users, links, folders, statistics as well as errors.
 
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

 To run the shortening service (on port 8080 by default) type
 
```
$ sbt run
```

## REST API
The services operate with JSON and have the following API

| Verb   | Resource           | Request Params                             | Response Data                                        | HTTP Status Code                                              |
|--------|--------------------|--------------------------------------------|------------------------------------------------------|---------------------------------------------------------------|
| GET    | /token             | {user_id, api secret}                      | token                                                | 200 OK                                                        |
|        |                    |                                            | 'wrong_secret'                                       | 401 Unauthorized                                              |
|        |                    |                                            |                                                      |                                                               |
| POST   | /link              | {token, url, code [opt], folder_id [opt] } | link (url, code)                                     | 200 OK                                                        |
|        |                    |                                            | 'invalid_token'                                      | 400 Bad Request                                               |
|        |                    |                                            | 'invalid_folder'                                     | 400 Bad Request                                               |
|        |                    |                                            |                                                      |                                                               |
| POST   | /link/$code        | {referrer, remote_ip }                     | link url to pass through                             | 200 OK                                                        |
|        |                    |                                            | 'nonexistent_code'                                   | 404 Not Found                                                 |
|        |                    |                                            |                                                      |                                                               |
| GET    | /link/$code        | {token }                                   | {link (url, code), folder_id (opt), count of clicks} | 200 OK                                                        |
|        |                    |                                            | 'nonexistent_code'                                   | 404 Not Found                                                 |
|        |                    |                                            |                                                      |                                                               |
| GET    | /link              | {token, offset [opt = 0], limit [opt] }    | {list of links (url, code)}                          | 200 OK                                                        |
|        |                    |                                            | 'invalid_token'                                      | 400 Bad Request                                               |
|        |                    |                                            |                                                      | 400 Bad Request if the offset or the limit are less than zero |
|        |                    |                                            |                                                      |                                                               |
| GET    | /link/$code/clicks | {token, offset [opt = 0], limit [opt] }    | {list of clicks}                                     | 200 OK                                                        |
|        |                    |                                            | 'nonexistent_code'                                   | 404 Not Found                                                 |
|        |                    |                                            |                                                      | 400 Bad Request if the offset or the limit are less than zero |
|        |                    |                                            |                                                      |                                                               |
| GET    | /folder/$id        | {token, offset [opt = 0], limit [opt] }    | {list of links (url, code)}                          | 200 OK                                                        |
|        |                    |                                            |                                                      |                                                               |
|        |                    |                                            | 'invalid_token'                                      | 400 Bad Request                                               |
|        |                    |                                            | 'invalid_folder'                                     | 400 Bad Request                                               |
|        |                    |                                            |                                                      | 400 Bad Request if the offset or the limit are less than zero |
|        |                    |                                            |                                                      |                                                               |
| GET    | /folder            | {token }                                   | {list of folders (id, title) }                       | 200 OK                                                        |
|        |                    |                                            | 'invalid_token'                                      | 400 Bad Request                                               |