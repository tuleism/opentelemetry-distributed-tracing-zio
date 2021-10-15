package com.github

import sttp.client3.asynchttpclient.zio.SttpClient
import zio.clock.Clock
import zio.random.Random

package object tuleism {
  type ZGreeterEnv = Clock with Random with SttpClient
}
