package com.github.tuleism

import izumi.logstage.api.Log.CustomContext
import logstage._
import zio.{Has, ZIO, ZLayer}
import zio.telemetry.opentelemetry.Tracing

object Logging {
  private def baseLogger = IzLogger()

  val live: ZLayer[Has[Tracing.Service], Nothing, Has[LogZIO.Service]] =
    (
      for {
        tracing <- ZIO.service[Tracing.Service]
      } yield LogZIO.withDynamicContext(baseLogger)(
        Tracing.getCurrentSpanContext
          .map(spanContext =>
            if (spanContext.isValid)
              CustomContext(
                "trace_id"    -> spanContext.getTraceId,
                "span_id"     -> spanContext.getSpanId,
                "trace_flags" -> spanContext.getTraceFlags.asHex()
              )
            else
              CustomContext.empty
          )
          .provide(Has(tracing))
      )
    ).toLayer
}
