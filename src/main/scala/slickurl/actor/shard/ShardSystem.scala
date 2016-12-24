package slickurl.actor.shard

import akka.actor.ActorSystem
import slick.dbio.DBIOAction.successful
import slick.driver.JdbcProfile
import slick.jdbc.meta.MTable
import slickurl.actor.{LinkRepo, UserRepo}
import slickurl.domain.repository.{ClickTable, FolderTable, LinkTable, UserTable}

import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.concurrent.duration.Duration

class ShardSystem(val shardId: Long, override val profile: JdbcProfile,
                  override val db: JdbcProfile#Backend#Database)
                 (implicit val system: ActorSystem)
  extends UserTable with LinkTable with FolderTable with ClickTable {

  implicit private val ec: ExecutionContextExecutor = system.dispatcher

  import profile.api._
  import slickurl.DbProps._

  private def initUserDb = db run (
    MTable.getTables(None, schemaName, Some(userTableName), None) flatMap {
      case v if v.isEmpty => (userIdSequence.schema ++ users.schema).create
      case _ => successful(())
    }
  )

  private def initFoldersDb = db run (
    MTable.getTables(None, schemaName, Some(folderTableName), None) flatMap {
      case v if v.isEmpty => folders.schema.create
      case _ => successful(())
    }
  )

  private def initLinksDb = db run (
    MTable.getTables(None, schemaName, Some(linkTableName), None) flatMap {
      case v if v.isEmpty => (linkIdSequence.schema ++ links.schema).create
      case _ => successful(())
    }
  )

  private def initClicksDb = db run (
    MTable.getTables(None, schemaName, Some(clickTableName), None) flatMap {
      case v if v.isEmpty => clicks.schema.create
      case _ => successful(())
    }
  )

  Await.result(initUserDb,    Duration.Inf)
  Await.result(initFoldersDb, Duration.Inf)
  Await.result(initLinksDb,   Duration.Inf)
  Await.result(initClicksDb,  Duration.Inf)

  //start the user repo
  system.actorOf(UserRepo(shardId, profile)(db), name = s"user-repo-shard-$shardId")

  //start the links repo
  system.actorOf(LinkRepo(shardId, profile)(db), name = s"link-repo-shard-$shardId")
}
