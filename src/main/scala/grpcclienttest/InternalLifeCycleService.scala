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

  def getDbClusterKeyForProjectCreation: IO[DbClusterKey] = IO.fromFuture(
    stub.getDbClusterKeyForProjectCreation(Empty()).pure[IO]
  )

  def getDbClusterInfo(dbClusterKey: DbClusterKey): IO[DbClusterInfo] = IO.fromFuture(
    stub.getDbClusterInfo(dbClusterKey).pure[IO]
  )

  def initProject(projectKey: String): IO[Response] = process(stub.initProject, ProjectKey(projectKey))

  def updateLanguages(projectKey: String): IO[Response] = process(stub.updateLanguages, ProjectKey(projectKey))

  def purgeProject(key: String, clusters: DbClusters): IO[Response] = IO.fromFuture(
    stub.purgeProject(ProjectWithClusters(ProjectKey(key).some, clusters.some)).map(_.response).pure[IO]
  )

  private def process(
    request: ProjectKey => Future[EmptyResponse],
    projectKey: ProjectKey
  ): IO[Response] = IO.fromFuture(request(projectKey).pure[IO]).map(_.response)

}

object InternalLifeCycleService {

  def create(lifeCycleStub: LifeCycleServiceStub)(implicit ex: ExecutionContext): IO[InternalLifeCycleService] =
    new InternalLifeCycleService(lifeCycleStub).pure[IO]

}

object ChannelBuilder {
  def build(host: String, port: Int): IO[Channel] =
    ManagedChannelBuilder
      .forAddress(host, port)
      .usePlaintext() // TODO no encryption only used for testing purposes - do not do in production
      .build
      .pure[IO]
}
