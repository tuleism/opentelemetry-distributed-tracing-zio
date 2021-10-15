package com.github.tuleism

import zio.config.ReadError
import zio.config.magnolia.DeriveConfigDescriptor._
import zio.config.typesafe.TypesafeConfig
import zio.{Has, Layer}

case class AppConfig(tracing: TracingConfig)

case class TracingConfig(enable: Boolean, endpoint: String)

object AppConfig {
  private val configDescriptor = descriptor[AppConfig]

  val live: Layer[ReadError[String], Has[AppConfig]] = TypesafeConfig.fromDefaultLoader(configDescriptor)
}
