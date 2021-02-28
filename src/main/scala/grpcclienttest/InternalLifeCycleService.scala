package grpcclienttest

import cats.effect.{ContextShift, IO, Resource}
import cats.implicits._
import com.google.protobuf.empty.Empty
import io.grpc.netty.NettyChannelBuilder
import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import lifecycleservice.lifecycleservice.EmptyResponse.Response
import lifecycleservice.lifecycleservice.LifeCycleServiceGrpc.LifeCycleServiceStub
import lifecycleservice.lifecycleservice._

import scala.concurrent.{ExecutionContext, Future}

class InternalLifeCycleService(stub: LifeCycleServiceStub)(implicit ec: ExecutionContext) {

  implicit private val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  def getDbClusterKeyForProjectCreation: IO[DbClusterKey] = IO.fromFuture(
    IO(stub.getDbClusterKeyForProjectCreation(Empty()))
  )

  def getDbClusterInfo(dbClusterKey: DbClusterKey): IO[DbClusterInfo] = IO.fromFuture(
    IO(stub.getDbClusterInfo(dbClusterKey))
  )

  def initProject(projectKey: String): IO[Response] = process(stub.initProject, ProjectKey(projectKey))

  def updateLanguages(projectKey: String): IO[Response] = process(stub.updateLanguages, ProjectKey(projectKey))

  def purgeProject(key: String, clusters: DbClusters): IO[Response] = IO.fromFuture {
    IO(stub.purgeProject(ProjectWithClusters(ProjectKey(key).some, clusters.some)).map(_.response))
  }

  private def process(
    request: ProjectKey => Future[EmptyResponse],
    projectKey: ProjectKey
  ): IO[Response] = IO.fromFuture(IO(request(projectKey).map(_.response)))
}

object InternalLifeCycleService {
  def resource(
    lifeCycleStub: LifeCycleServiceStub
  )(implicit ex: ExecutionContext): Resource[IO, InternalLifeCycleService] =
    Resource.make(IO(new InternalLifeCycleService(lifeCycleStub)))(_ => IO.unit)
}

object ChannelBuilder {
  def resource(host: String, port: Int): Resource[IO, ManagedChannel] =
    Resource.make {
      IO {
        ManagedChannelBuilder
          .forAddress(host, port)
          .usePlaintext() // no encryption for testing purposes - do not do in production
          .asInstanceOf[NettyChannelBuilder]
          .build
      }
    }(channel =>
      IO {
        println("shutdown gRPC client")
        channel.shutdown()
      } as ())
}
