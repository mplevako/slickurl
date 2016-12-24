package slickurl.domain.repository

import slickurl.DbProps._
import slickurl.AppProps._
import slickurl.domain.model.UserID

trait UserTable extends Profile {

  import profile.api._

  class Users(tag: Tag) extends Table[UserID](tag, schemaName, userTableName) {
    def id = column[UserID] ("ID")

    def * = id

    def pk = primaryKey("USER_PK", id)
  }

  lazy val users = Users.users
  lazy val userIdSequence = Users.idSequence

  private object Users {
    lazy val users = TableQuery[Users]
    lazy val idSequence = Sequence[Long](userIdSequenceName) start idSequenceStart inc idSequenceInc min idSequenceStart max maxId
  }
}