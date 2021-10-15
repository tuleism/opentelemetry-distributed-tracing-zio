package com.github.tuleism

import scalapb.zio_grpc.{ServerMain, ServiceList}
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio._

object ZServer extends ServerMain {
  private val sttpLayer = AsyncHttpClientZioBackend.layer().orDie

  private val requirements = ZEnv.live ++ sttpLayer

  def services: ServiceList[Any] =
    ServiceList
      .add(ZGreeterImpl)
      .provideLayer(requirements)
}
