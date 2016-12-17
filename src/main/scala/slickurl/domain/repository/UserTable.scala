package slickurl.domain.repository

import slickurl.DbProps._
import slickurl.domain.model.UserID

trait UserTable extends Profile {

  import profile.api._

  class Users(tag: Tag) extends Table[UserID](tag, "USER") {
    def id = column[UserID] ("ID")

    def * = id

    def pk = primaryKey("USER_PK", id)
  }

  lazy val users = Users.users
  lazy val idSequence = Users.idSequence

  private object Users {
    lazy val users = TableQuery[Users]
    lazy val idSequence = Sequence[Long]("USER_ID_SEQUENCE") start idSequenceStart inc idSequenceInc
  }
}