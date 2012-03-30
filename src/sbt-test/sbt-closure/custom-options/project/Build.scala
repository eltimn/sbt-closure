import sbt._
import sbt.Keys._

import com.google.javascript.jscomp.{CompilationLevel, CompilerOptions}

object CustomOptionsBuild extends Build {

  /**
    * Custom closure compiler options to be used in build.sbt
    */
  lazy val myClosureOptions = {
    val opts = new CompilerOptions
    CompilationLevel.WHITESPACE_ONLY.setOptionsForCompilationLevel(opts)
    opts
  }

  lazy val root = Project("root", file("."))
}
