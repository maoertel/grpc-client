package grpcclienttest

import cats.effect.{IO, Resource}
import com.typesafe.config.ConfigFactory

case class ClientConfig(host: String, port: Int)
object ClientConfig {
  def resource: Resource[IO, ClientConfig] = Resource.make {
    for {
      config <- IO(ConfigFactory.load("application.conf"))
      port = config.getInt("serverConfig.port")
      host = config.getString("serverConfig.host")
    } yield ClientConfig(host, port)
  }(_ => IO.unit)
}
