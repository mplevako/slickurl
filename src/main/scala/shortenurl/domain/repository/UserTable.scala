package shortenurl.domain.repository

import shortenurl.domain.model.User

trait UserTable extends Profile {

  import profile.simple._

  class Users(tag: Tag) extends Table[User](tag, "USER") {

    def id    = column[Int] ("ID", O.PrimaryKey)
    def token = column[String]("TOKEN", O.NotNull, O.DBType("VARCHAR(64)"))

    def * = (id, token) <> (User.tupled, User.unapply)
  }

  val users = Users.users

  private object Users {
    val users = TableQuery[Users]
  }
}
