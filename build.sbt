name := "grpc-client"

version := "0.1"

scalaVersion := "2.13.3"

PB.targets in Compile := Seq(
  scalapb.gen(grpc = true) -> (sourceManaged in Compile).value
)

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-effect" % "2.3.3" withSources() withJavadoc(),
  "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",
  "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion,
  "io.grpc" % "grpc-netty" % scalapb.compiler.Version.grpcJavaVersion,
  "io.grpc" % "grpc-services" % scalapb.compiler.Version.grpcJavaVersion,
  "com.typesafe" % "config" % "1.4.1"
)
