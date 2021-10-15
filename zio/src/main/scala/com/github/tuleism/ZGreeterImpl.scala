package com.github.tuleism

import io.grpc.Status
import io.grpc.examples.helloworld.helloworld.ZioHelloworld.RGreeter
import io.grpc.examples.helloworld.helloworld.{HelloReply, HelloRequest}
import sttp.client3._
import sttp.client3.asynchttpclient.zio._
import zio.duration._
import zio.random._
import zio.{RIO, ZIO}

object ZGreeterImpl extends RGreeter[ZGreeterEnv] {

  def sayHello(request: HelloRequest): ZIO[ZGreeterEnv, Status, HelloReply] = {
    val guess = request.guess.getOrElse(0)
    val rem   = guess % 5
    for {
      _      <- ZIO.fail(Status.INVALID_ARGUMENT).when(guess < 0)
      code   <- nextIntBetween((rem + 1) * 100, (rem + 2) * 100)
      delayMs = if (rem == 2) 3000 + code else code / 10
      _      <- httpRequest(code)
                  .delay(delayMs.millis)
                  .mapError(ex => Status.INTERNAL.withCause(ex))
    } yield HelloReply(s"Hello, ${request.name}")
  }

  def httpRequest(code: Int): RIO[SttpClient, Unit] =
    send(basicRequest.get(uri"https://httpbin.org/status/$code")).unit
}
