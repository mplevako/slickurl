package slickurl.mock

import akka.actor.ActorSystem
import akka.testkit.TestActorRef
import org.specs2.mock.Mockito
import org.specs2.specification.Scope
import slickurl.actor.UserRepo
import slickurl.domain.model.UserID

import scala.concurrent.Future

trait UserRepositoryMock extends Scope with Mockito {
  protected val mockShardId: Long

  class UserRepoImpl extends UserRepo(mockShardId, null, null) {
    override val userRepository: UserRepository = mock[UserRepository]
  }

  protected def createNewUserMock(uid: UserID)(implicit system: ActorSystem): TestActorRef[UserRepoImpl] = {
    val repo = TestActorRef(new UserRepoImpl)
    implicit val ec = repo.dispatcher
    repo.underlyingActor.userRepository.createNewUser(mockShardId) returns Future.successful(Right(uid))
    repo
  }
}
