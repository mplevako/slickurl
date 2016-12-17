package slickurl.mock

import akka.actor.ActorSystem
import akka.testkit.TestActorRef
import org.specs2.mock.Mockito
import org.specs2.specification.Scope
import slickurl.actor.UserRepo
import slickurl.domain.model.UserID
import slickurl.domain.repository.UserRepositoryComponent

import scala.concurrent.Future

trait UserRepositoryMock extends Scope with Mockito {
  protected val repositoryMock: UserRepositoryComponent#UserRepository = mock[UserRepositoryComponent#UserRepository]

  class UserRepoImpl extends UserRepo {
    override val userRepository: UserRepositoryComponent#UserRepository = repositoryMock
  }

  protected def createNewUserMock(uid: UserID)(implicit system: ActorSystem): TestActorRef[UserRepoImpl] = {
    val repo = TestActorRef(new UserRepoImpl)
    implicit val ec = repo.dispatcher
    repositoryMock.createNewUser() returns Future.successful(Right(uid))
    repo
  }
}
