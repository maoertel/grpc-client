package grpcclienttest

import cats.effect.{ExitCode, IO, IOApp}
import com.typesafe.config.ConfigFactory
import lifecycleservice.lifecycleservice.EmptyResponse.Response
import lifecycleservice.lifecycleservice.{DbClusterKey, LifeCycleServiceGrpc}

import scala.concurrent.ExecutionContext.Implicits.global

object GrpcTestClient extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {

    val application = for {
      config <- IO(ConfigFactory.load("application.conf"))
      port = config.getInt("serverConfig.port")
      host = config.getString("serverConfig.host")

      channel <- ChannelBuilder.build(host, port)
      lifecycleStub <- IO(LifeCycleServiceGrpc.stub(channel))

      service <- InternalLifeCycleService.create(lifecycleStub)
    } yield service

    val serviceCalls = for {
      service <- application
      clusterInfo <- service.getDbClusterInfo(DbClusterKey("my-cluster")).map(_.state.toString())
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

    serviceCalls.map(_.map(println(_))).map(_ => ExitCode.Success)
  }
}
