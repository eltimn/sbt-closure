package sbtclosure

import sbt._
import sbt.Keys._
import EitherExtras._

/**
 *
 * sbt-closure / LogiDev - [Fun Functional] / Logikujo.com
 *
 * 7/09/13 :: 11:48 :: eof
 *
 */
object LiveScriptCompiler {
  def compile(input: File, output: File): Either[String, File] = {
    lazy val lscCommand:Seq[String] = Seq("lsc", "-c", "-p", input.toString)
    // TODO: Catch errors in write process
    // This info is not logged!?!?!?! Why?
    streams.map(_.log.info(s"Compiling LiveScript file to: ${output.toString}"))
    tryCommand(lscCommand).right.map { s =>
      (output,IO.write(output, s))._1
    }
  }
}
