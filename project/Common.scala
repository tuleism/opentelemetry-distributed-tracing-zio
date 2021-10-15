import sbt.Keys._
import sbt._

object Common {
  def scalacOptionsFor(version: String): Seq[String] =
    Seq(
      "-language:higherKinds",
      "-deprecation",                 // Emit warning and location for usages of deprecated APIs.
      "-encoding",
      "utf-8",                        // Specify character encoding used by source files.
      "-explaintypes",                // Explain type errors in more detail.
      "-feature",                     // Emit warning and location for usages of features that should be imported explicitly.
      "-unchecked",                   // Enable additional warnings where generated code depends on assumptions.
      "-Xcheckinit",                  // Wrap field accessors to throw an exception on uninitialized access.
      "-Xlint:adapted-args",          // Warn if an argument list is modified to match the receiver.
      "-Xlint:private-shadow",        // A private field (or class parameter) shadows a superclass field.
      "-Xlint:type-parameter-shadow", // A local type parameter shadows a type already in scope.
      "-Ywarn-dead-code",             // Warn when dead code is identified.
      "-Ywarn-unused:implicits",      // Warn if an implicit parameter is unused.
      "-Ywarn-unused:imports",        // Warn if an import selector is not referenced.
      "-Ywarn-unused:locals",         // Warn if a local definition is unused.
      "-Ywarn-unused:params",         // Warn if a value parameter is unused.
      "-Ywarn-unused:patvars",        // Warn if a variable bound in a pattern is unused.
      "-Ywarn-unused:privates",       // Warn if a private member is unused.
      "-Ywarn-value-discard",         // Warn when non-Unit expression results are unused.
      "-Ywarn-macros:after"           // Only warn after macro expansion.
    ) ++ (CrossVersion.partialVersion(version) match {
      case Some((2, scalaMajor)) if scalaMajor == 13 => Nil
      case _                                         =>
        Seq(
          "-Xlint:unsound-match", // Pattern match may not be typesafe.
          "-Ypartial-unification" // Enable partial unification in type constructor inference
        )
    })

  val commonSettings = Seq(
    scalaVersion := "2.13.6",
    organization := "com.github.tuleism",
    run / fork   := true,
    scalacOptions ++= scalacOptionsFor(scalaVersion.value),
    resolvers += Resolver.sonatypeRepo("snapshots")
  )

  def commonProject(name: String): Project =
    Project(name, file(name)).settings(commonSettings: _*)
}
