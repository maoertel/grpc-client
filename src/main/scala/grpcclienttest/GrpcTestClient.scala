package grpcclienttest

import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.applicative._
import com.typesafe.config.ConfigFactory
import lifecycleservice.lifecycleservice.EmptyResponse.Response
import lifecycleservice.lifecycleservice.{DbClusterKey, LifeCycleServiceGrpc}

object GrpcTestClient extends IOApp {

  private implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  override def run(args: List[String]): IO[ExitCode] = {

    val application = for {
      config <- ConfigFactory.load("application.conf").pure[IO]
      port = config.getInt("serverConfig.port")
      host = config.getString("serverConfig.host")

      channel <- ChannelBuilder.build(host, port)
      lifecycleStub <- LifeCycleServiceGrpc.stub(channel).pure[IO]

      service <- InternalLifeCycleService.create(lifecycleStub)
    } yield service

    // call the running gRPC server and ask for information
    val serviceCalls = for {
      service <- application
      clusterInfo <- service.getDbClusterInfo(DbClusterKey("my-sharded-cluster")).map(_.state.toString())
      keyForProjectCreate <- service.getDbClusterKeyForProjectCreation.map(_.key)
      isInit <- service.initProject("my-real-good-project").map {
        case Response.Success => "project initialized"
        case _ => "project not initialized"
      }
      languageUpdated <- service.updateLanguages("my-real-good-project").map {
        case Response.Success => "languages updated"
        case _ => "languages not updated"
      }
    } yield clusterInfo :: keyForProjectCreate :: isInit :: languageUpdated :: Nil

    // print the responses from the gRPC server
    serviceCalls.map(_.map(println(_))).map(_ => ExitCode.Success)
  }
}
