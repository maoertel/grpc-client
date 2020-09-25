package grpcclienttest

import cats.effect.{ContextShift, IO}
import cats.implicits._
import com.google.protobuf.empty.Empty
import io.grpc.{Channel, ManagedChannelBuilder}
import lifecycleservice.lifecycleservice.EmptyResponse.Response
import lifecycleservice.lifecycleservice.LifeCycleServiceGrpc.LifeCycleServiceStub
import lifecycleservice.lifecycleservice._

import scala.concurrent.{ExecutionContext, Future}

class InternalLifeCycleService(stub: LifeCycleServiceStub)(implicit ec: ExecutionContext) {

  private implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  def getDbClusterKeyForProjectCreation: IO[DbClusterKey] = IO.fromFuture(IO {
    stub.getDbClusterKeyForProjectCreation(Empty())
  })

  def getDbClusterInfo(dbClusterKey: DbClusterKey): IO[DbClusterInfo] = IO.fromFuture(IO {
    stub.getDbClusterInfo(dbClusterKey)
  })

  def initProject(projectKey: String): IO[Response] = process(stub.initProject, ProjectKey(projectKey))

  def updateLanguages(projectKey: String): IO[Response] = process(stub.updateLanguages, ProjectKey(projectKey))

  def purgeProject(key: String, clusters: DbClusters): IO[Response] = IO.fromFuture(IO {
    stub.purgeProject(ProjectWithClusters(ProjectKey(key).some, clusters.some)).map(_.response)
  })

  private def process(
    request: ProjectKey => Future[EmptyResponse],
    projectKey: ProjectKey
  ): IO[Response] = IO.fromFuture(IO(request(projectKey))).map(_.response)

}

object InternalLifeCycleService {

  def create(lifeCycleStub: LifeCycleServiceStub)(implicit ex: ExecutionContext): IO[InternalLifeCycleService] =
    IO(new InternalLifeCycleService(lifeCycleStub))

}

object ChannelBuilder {
  def build(host: String, port: Int): IO[Channel] = IO {
    ManagedChannelBuilder
      .forAddress(host, port)
      .usePlaintext() // TODO don't use encryption only for testing purposes
      .build
  }
}