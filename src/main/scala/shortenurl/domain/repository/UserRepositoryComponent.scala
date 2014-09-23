/**
 * Copyright 2014 Maxim Plevako
 **/
package shortenurl.domain.repository

import shortenurl.domain.model.User

trait UserRepositoryComponent { this: UserTable =>

  val userRepository: UserRepository

  trait UserRepository {
    def findById(id: Long): Option[User]
    def userForToken(token: String): Option[User]
    def getUser(id: Long): User
  }

  class UserRepositoryImpl extends UserRepository {
    import profile.simple._

    val random = new scala.util.Random(new java.security.SecureRandom())

    private def randomString(alphabet: String)(n: Int): String =
      Stream.continually(random.nextInt(alphabet.size)).map(alphabet).take(n).mkString

    private def randomAlphanumericString(n: Int) =
      randomString("abcdefghijklmnopqrstuvwxyz0123456789")(n)

    private def generateToken = randomAlphanumericString(64)

    override def findById(id: Long): Option[User] =
      db withSession { implicit session =>
        users.filter(_.id === id).firstOption
      }

    override def userForToken(token: String): Option[User] =
      db withSession { implicit session =>
        users.filter(_.token === token).firstOption
      }

    override def getUser(id: Long): User =
      db withTransaction { implicit session =>
        findById(id) match {
          case None =>
            val token = generateToken
            val user = User(id, token)
            users.insert(user)
            user
          case Some(existingUser) => existingUser
        }
      }
  }
}
