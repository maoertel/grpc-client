package grpcclienttest

import cats.effect.{ExitCode, IO, IOApp, Resource}
import lifecycleservice.lifecycleservice.EmptyResponse.Response
import lifecycleservice.lifecycleservice.{DbClusterKey, LifeCycleServiceGrpc}

object GrpcTestClient extends IOApp {

  implicit private val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  override def run(args: List[String]): IO[ExitCode] = {

    val resource: Resource[IO, ApplicationConfig] =
      for {
        config <- ClientConfig.resource

        channel <- ChannelBuilder.resource(config.host, config.port)
        lifecycleStub = LifeCycleServiceGrpc.stub(channel)

        service <- InternalLifeCycleService.resource(lifecycleStub)
      } yield ApplicationConfig(config.host, config.port, service)

    resource.use { conf =>
      for {
        _ <- IO(println(s"gRPC client calls service at ${conf.host}:${conf.port} to gather requested information"))
        clusterInfo <- conf.service.getDbClusterInfo(DbClusterKey("my-sharded-cluster")).map(_.state.toString())
        keyForProjectCreate <- conf.service.getDbClusterKeyForProjectCreation.map(_.key)
        isInit <- conf.service.initProject("my-real-good-project") map {
          case Response.Success => "project initialized"
          case _ => "project not initialized"
        }
        languageUpdated <- conf.service.updateLanguages("my-real-good-project") map {
          case Response.Success => "languages updated"
          case _ => "languages not updated"
        }
        serviceCalls: List[String] = clusterInfo :: keyForProjectCreate :: isInit :: languageUpdated :: Nil
        _ = serviceCalls map println
      } yield ExitCode.Success
    }
  }
}

case class ApplicationConfig(host: String, port: Int, service: InternalLifeCycleService)
