package com.github.tuleism

import io.opentelemetry.semconv.trace.attributes.SemanticAttributes
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import sttp.client3.ziotelemetry.opentelemetry.{ZioTelemetryOpenTelemetryBackend, ZioTelemetryOpenTelemetryTracer}
import sttp.client3.{Request, Response}
import zio._
import zio.telemetry.opentelemetry.Tracing

object SttpTracing {
  private val wrapper = new ZioTelemetryOpenTelemetryTracer {
    def before[T](request: Request[T, Nothing]): RIO[Tracing, Unit] =
      Tracing.setAttribute(SemanticAttributes.HTTP_METHOD.getKey, request.method.method) *>
        Tracing.setAttribute(SemanticAttributes.HTTP_URL.getKey, request.uri.toString()) *>
        ZIO.unit

    def after[T](response: Response[T]): RIO[Tracing, Unit] =
      Tracing.setAttribute(SemanticAttributes.HTTP_STATUS_CODE.getKey, response.code.code) *>
        ZIO.unit
  }

  val live = AsyncHttpClientZioBackend.layer().flatMap { hasBackend =>
    ZIO
      .service[Tracing.Service]
      .map { tracing =>
        ZioTelemetryOpenTelemetryBackend(hasBackend.get, tracing, wrapper)
      }
      .toLayer
  }
}
