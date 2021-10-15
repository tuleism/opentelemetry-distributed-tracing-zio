package com.github.tuleism

import io.grpc.ManagedChannelBuilder
import io.grpc.examples.helloworld.helloworld.ZioHelloworld.GreeterClient
import io.grpc.examples.helloworld.helloworld.HelloRequest
import scalapb.zio_grpc.ZManagedChannel
import zio.console._
import zio._

object ZClient extends zio.App {
  private val clientLayer = GreeterClient.live(
    ZManagedChannel(
      ManagedChannelBuilder.forAddress("localhost", 9000).usePlaintext()
    )
  )

  private val singleHello = GreeterClient.sayHello(HelloRequest("World"))

  private val multipleHellos = ZIO.collectAllParN(5)(
    List(
      GreeterClient.sayHello(HelloRequest("1", Some(1))),
      GreeterClient.sayHello(HelloRequest("2", Some(2))),
      GreeterClient.sayHello(HelloRequest("3", Some(3))),
      GreeterClient.sayHello(HelloRequest("4", Some(4))),
      GreeterClient.sayHello(HelloRequest("5", Some(5)))
    )
  )

  private val invalidHello = GreeterClient.sayHello(HelloRequest("Invalid", Some(-1))).ignore

  private def myAppLogic =
    singleHello *> multipleHellos *> invalidHello *> putStrLn("Done")

  def run(args: List[String]): URIO[ZEnv, ExitCode] =
    myAppLogic.provideCustomLayer(clientLayer).exitCode
}
