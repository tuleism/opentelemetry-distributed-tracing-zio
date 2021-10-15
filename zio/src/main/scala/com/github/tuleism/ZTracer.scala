package com.github.tuleism

import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.extension.noopapi.NoopOpenTelemetry
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes
import zio._

object ZTracer {
  private val InstrumentationName = "com.github.tuleism"

  private def managed(serviceName: String, endpoint: String) = {
    val resource = Resource.builder().put(ResourceAttributes.SERVICE_NAME, serviceName).build()
    for {
      spanExporter   <- ZManaged.fromAutoCloseable(
                          Task(JaegerGrpcSpanExporter.builder().setEndpoint(endpoint).build())
                        )
      spanProcessor  <- ZManaged.fromAutoCloseable(UIO(SimpleSpanProcessor.create(spanExporter)))
      tracerProvider <- UIO(
                          SdkTracerProvider.builder().addSpanProcessor(spanProcessor).setResource(resource).build()
                        ).toManaged_
      openTelemetry  <- UIO(OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build()).toManaged_
      tracer         <- UIO(openTelemetry.getTracer(InstrumentationName)).toManaged_
    } yield tracer
  }

  def live(serviceName: String): RLayer[Has[TracingConfig], Has[Tracer]] =
    (
      for {
        config <- ZIO.service[TracingConfig].toManaged_
        tracer <- if (!config.enable) {
                    Task(NoopOpenTelemetry.getInstance().getTracer(InstrumentationName)).toManaged_
                  } else {
                    managed(serviceName, config.endpoint)
                  }
      } yield tracer
    ).toLayer
}
