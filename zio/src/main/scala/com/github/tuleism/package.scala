package com.github

import logstage.LogZIO
import sttp.client3.asynchttpclient.zio.SttpClient
import zio.clock.Clock
import zio.random.Random
import zio.telemetry.opentelemetry.Tracing

package object tuleism {
  type ZGreeterEnv = Clock with Random with SttpClient with Tracing with LogZIO
}
