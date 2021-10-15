import Common._

val grpcVersion = "1.41.0"
val sttpVersion = "3.3.15"

val scalaPBRuntime = "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion

val grpcRuntimeDeps = Seq(
  "io.grpc"      % "grpc-netty" % grpcVersion,
  scalaPBRuntime,
  scalaPBRuntime % "protobuf"
)

val sttpZioDeps = Seq(
  "com.softwaremill.sttp.client3" %% "async-http-client-backend-zio" % sttpVersion
)

lazy val root = Project("opentelemetry-distributed-tracing-zio", file(".")).aggregate(zio)

lazy val zio = commonProject("zio").settings(
  Compile / PB.targets := Seq(
    scalapb.gen(grpc = true)          -> (Compile / sourceManaged).value,
    scalapb.zio_grpc.ZioCodeGenerator -> (Compile / sourceManaged).value
  ),
  libraryDependencies ++= grpcRuntimeDeps ++ sttpZioDeps
)
