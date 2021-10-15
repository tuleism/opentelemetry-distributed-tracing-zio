package com.github.tuleism

import scalapb.zio_grpc.{RequestContext, ServerMain, ServiceList}
import zio._
import zio.config.syntax._
import zio.magic._
import zio.telemetry.opentelemetry.Tracing

object ZServer extends ServerMain {
  private val requirements =
    ZLayer
      .wire[ZEnv with ZGreeterEnv](
        ZEnv.live,
        AppConfig.live.narrow(_.tracing),
        ZTracer.live("hello-server"),
        Tracing.live,
        SttpTracing.live
      )
      .orDie

  def services: ServiceList[Any] =
    ServiceList
      .add(ZGreeterImpl.transform[ZGreeterEnv, Has[RequestContext]](GrpcTracing.serverTracingTransform))
      .provideLayer(requirements)
}
