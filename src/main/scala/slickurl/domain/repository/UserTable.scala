package slickurl.domain.repository

import slickurl.domain.model.User

trait UserTable extends Profile {

  import profile.api._

  class Users(tag: Tag) extends Table[User](tag, "USER") {

    def id    = column[Long] ("ID")
    def token = column[String]("TOKEN", O.SqlType("VARCHAR(64)"))

    def * = (id, token) <> (User.tupled, User.unapply)

    def pk = primaryKey("USER_PK", id)
    def id_token_idx = index("USER_ID_TOKEN_IDX", (id, token), unique = true)
    def token_id_idx = index("USER_TOKEN_ID_IDX", (token, id), unique = true)
  }

  lazy val users = Users.users

  private object Users {
    lazy val users = TableQuery[Users]
  }
}