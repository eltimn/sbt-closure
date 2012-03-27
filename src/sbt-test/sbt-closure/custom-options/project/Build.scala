import sbt._
import sbt.Keys._

import com.google.javascript.jscomp.CompilerOptions

object CustomOptionsBuild extends Build {

  /**
    * Custom closure compiler options to be used in build.sbt
    */
  lazy val myClosureOptions = {
    val opts = new CompilerOptions
    opts.prettyPrint = true
    opts
  }

  lazy val root = Project("root", file("."))
}
