import Common._

val grpcVersion          = "1.41.0"
val openTelemetryVersion = "1.6.0"
val sttpVersion          = "3.3.15"
val zioConfigVersion     = "1.0.10"
val zioMagicVersion      = "0.3.9"
val zioTelemetryVersion  = "0.8.2"

val scalaPBRuntime = "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion

val grpcRuntimeDeps = Seq(
  "io.grpc"      % "grpc-netty" % grpcVersion,
  scalaPBRuntime,
  scalaPBRuntime % "protobuf"
)

val openTelemetryDeps = Seq(
  "io.opentelemetry" % "opentelemetry-exporter-jaeger"    % openTelemetryVersion,
  "io.opentelemetry" % "opentelemetry-sdk"                % openTelemetryVersion,
  "io.opentelemetry" % "opentelemetry-extension-noop-api" % s"$openTelemetryVersion-alpha"
)

val sttpZioDeps = Seq(
  "com.softwaremill.sttp.client3" %% "async-http-client-backend-zio" % sttpVersion
)

val zioConfigDeps = Seq(
  "dev.zio" %% "zio-config"          % zioConfigVersion,
  "dev.zio" %% "zio-config-magnolia" % zioConfigVersion,
  "dev.zio" %% "zio-config-typesafe" % zioConfigVersion
)

val zioMagicDeps = Seq(
  "io.github.kitlangton" %% "zio-magic" % zioMagicVersion
)

val zioTelemetryDeps = Seq(
  "dev.zio"                       %% "zio-opentelemetry"                   % zioTelemetryVersion,
  "com.softwaremill.sttp.client3" %% "zio-telemetry-opentelemetry-backend" % sttpVersion
)

lazy val root = Project("opentelemetry-distributed-tracing-zio", file(".")).aggregate(zio)

lazy val zio = commonProject("zio").settings(
  Compile / PB.targets := Seq(
    scalapb.gen(grpc = true)          -> (Compile / sourceManaged).value,
    scalapb.zio_grpc.ZioCodeGenerator -> (Compile / sourceManaged).value
  ),
  libraryDependencies ++= grpcRuntimeDeps ++ openTelemetryDeps ++ sttpZioDeps ++ zioConfigDeps ++ zioMagicDeps ++ zioTelemetryDeps
)
