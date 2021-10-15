package com.github.tuleism

import io.grpc.{Metadata, Status}
import io.opentelemetry.api.trace.{SpanKind, StatusCode}
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.{TextMapGetter, TextMapPropagator, TextMapSetter}
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes
import scalapb.zio_grpc.{RequestContext, ZClientInterceptor, ZTransform}
import zio.stream.ZStream
import zio.{Has, ZIO}
import zio.telemetry.opentelemetry.Tracing
import zio.telemetry.opentelemetry.TracingSyntax._

object GrpcTracing {
  private val propagator: TextMapPropagator = W3CTraceContextPropagator.getInstance()

  private val metadataGetter: TextMapGetter[Metadata] = new TextMapGetter[Metadata] {
    override def keys(carrier: Metadata): java.lang.Iterable[String] =
      carrier.keys()

    override def get(carrier: Metadata, key: String): String =
      carrier.get(
        Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER)
      )
  }

  private val metadataSetter: TextMapSetter[Metadata] = (carrier, key, value) =>
    carrier.put(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER), value)

  val contextPropagationClientInterceptor: ZClientInterceptor[Tracing] = ZClientInterceptor.headersUpdater {
    (_, _, metadata) =>
      metadata.wrapM(Tracing.inject(propagator, _, metadataSetter))
  }

  private def withSemanticAttributes[R, A](effect: ZIO[R, Status, A]): ZIO[Tracing with R, Status, A] =
    Tracing.setAttribute(SemanticAttributes.RPC_SYSTEM.getKey, "grpc") *>
      effect
        .tapBoth(
          status =>
            Tracing.setAttribute(
              SemanticAttributes.RPC_GRPC_STATUS_CODE.getKey,
              status.getCode.value(
              )
            ),
          _ => Tracing.setAttribute(SemanticAttributes.RPC_GRPC_STATUS_CODE.getKey, Status.OK.getCode.value())
        )

  def clientTracingTransform[R]: ZTransform[R, Status, R with Tracing] =
    new ZTransform[R, Status, R with Tracing] {

      def effect[A](io: ZIO[R, Status, A]): ZIO[R with Tracing, Status, A] = withSemanticAttributes(io)

      def stream[A](io: ZStream[R, Status, A]): ZStream[R with Tracing, Status, A] = ???
    }

  def serverTracingTransform[R]: ZTransform[R, Status, R with Tracing with Has[RequestContext]] =
    new ZTransform[R, Status, R with Tracing with Has[RequestContext]] {

      def effect[A](io: ZIO[R, Status, A]): ZIO[R with Tracing with Has[RequestContext], Status, A] =
        for {
          rc       <- ZIO.service[RequestContext]
          metadata <- rc.metadata.wrap(identity)
          result   <- withSemanticAttributes(io)
                        .spanFrom(
                          propagator,
                          metadata,
                          metadataGetter,
                          rc.methodDescriptor.getFullMethodName,
                          SpanKind.SERVER,
                          { case _ => StatusCode.ERROR }
                        )
        } yield result

      def stream[A](io: ZStream[R, Status, A]): ZStream[R with Tracing with Has[RequestContext], Status, A] =
        ???
    }
}
