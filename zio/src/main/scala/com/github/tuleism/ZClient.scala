package com.github.tuleism

import io.grpc.ManagedChannelBuilder
import io.grpc.examples.helloworld.helloworld.ZioHelloworld.GreeterClient
import io.grpc.examples.helloworld.helloworld.{GreeterGrpc, HelloRequest}
import io.opentelemetry.api.trace.{SpanKind, StatusCode}
import logstage.LogZIO
import logstage.LogZIO.log
import scalapb.zio_grpc.ZManagedChannel
import zio._
import zio.config.syntax._
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

  private val singleHello = (
    for {
      _ <- log.info("singleHello")
      _ <- sayHello(HelloRequest("World"))
    } yield ()
  ).span("singleHello", toErrorStatus = errorToStatusCode)

  private val multipleHellos = (
    for {
      _ <- log.info("multipleHellos")
      _ <- ZIO
             .collectAllParN(5)(
               List(
                 sayHello(HelloRequest("1", Some(1))),
                 sayHello(HelloRequest("2", Some(2))),
                 sayHello(HelloRequest("3", Some(3))),
                 sayHello(HelloRequest("4", Some(4))),
                 sayHello(HelloRequest("5", Some(5)))
               )
             )
    } yield ()
  ).span("multipleHellos", toErrorStatus = errorToStatusCode)

  private val invalidHello = (
    for {
      _ <- log.info("invalidHello")
      _ <- sayHello(HelloRequest("Invalid", Some(-1)))
    } yield ()
  ).ignore.span("invalidHello", toErrorStatus = errorToStatusCode)

  private def myAppLogic =
    singleHello *> multipleHellos *> invalidHello *> log.info("Done")

  private val requirements = ZLayer
    .wire[ZEnv with Tracing with LogZIO](
      ZEnv.live,
      AppConfig.live.narrow(_.tracing),
      ZTracer.live("hello-client"),
      Tracing.live,
      Logging.live
    ) >+> clientLayer

  def run(args: List[String]): URIO[ZEnv, ExitCode] =
    myAppLogic.provideCustomLayer(requirements).exitCode
}
