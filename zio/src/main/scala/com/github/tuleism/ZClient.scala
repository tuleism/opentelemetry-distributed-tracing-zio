package com.github.tuleism

import io.grpc.ManagedChannelBuilder
import io.grpc.examples.helloworld.helloworld.ZioHelloworld.GreeterClient
import io.grpc.examples.helloworld.helloworld.{GreeterGrpc, HelloRequest}
import io.opentelemetry.api.trace.{SpanKind, StatusCode}
import scalapb.zio_grpc.ZManagedChannel
import zio._
import zio.config.syntax._
import zio.console._
import zio.magic._
import zio.telemetry.opentelemetry.Tracing
import zio.telemetry.opentelemetry.TracingSyntax._

object ZClient extends zio.App {
  private val clientLayer = GreeterClient.live(
    ZManagedChannel(
      ManagedChannelBuilder.forAddress("localhost", 9000).usePlaintext(),
      Seq(GrpcTracing.contextPropagationClientInterceptor)
    )
  )

  private def errorToStatusCode[E]: PartialFunction[E, StatusCode] = { case _ => StatusCode.ERROR }

  private def sayHello(request: HelloRequest) =
    GreeterClient
      .sayHello(request)
      .span(
        GreeterGrpc.METHOD_SAY_HELLO.getFullMethodName,
        SpanKind.CLIENT,
        errorToStatusCode
      )

  private val singleHello = sayHello(HelloRequest("World")).span("singleHello", toErrorStatus = errorToStatusCode)

  private val multipleHellos = ZIO
    .collectAllParN(5)(
      List(
        sayHello(HelloRequest("1", Some(1))),
        sayHello(HelloRequest("2", Some(2))),
        sayHello(HelloRequest("3", Some(3))),
        sayHello(HelloRequest("4", Some(4))),
        sayHello(HelloRequest("5", Some(5)))
      )
    )
    .span("multipleHellos", toErrorStatus = errorToStatusCode)

  private val invalidHello =
    sayHello(HelloRequest("Invalid", Some(-1))).ignore.span("invalidHello", toErrorStatus = errorToStatusCode)

  private def myAppLogic =
    singleHello *> multipleHellos *> invalidHello *> putStrLn("Done")

  private val requirements = ZLayer
    .wire[ZEnv with Tracing](
      ZEnv.live,
      AppConfig.live.narrow(_.tracing),
      ZTracer.live("hello-client"),
      Tracing.live
    ) >+> clientLayer

  def run(args: List[String]): URIO[ZEnv, ExitCode] =
    myAppLogic.provideCustomLayer(requirements).exitCode
}
