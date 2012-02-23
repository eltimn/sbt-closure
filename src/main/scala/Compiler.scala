package sbtclosure

import com.google.javascript.jscomp.{Compiler => ClosureCompiler, CompilerOptions, JSError, JSSourceFile}

import sbt._

class Compiler(options: CompilerOptions) {

  def compile(sources: List[File], externs: List[File], target: File, log: Logger): Unit = {
    val compiler = new ClosureCompiler

    val result = compiler.compile(
      externs.map(JSSourceFile.fromFile _).toArray,
      sources.map(JSSourceFile.fromFile _).toArray,
      options
    )

    val errors = result.errors.toList
    val warnings = result.warnings.toList

    if (!errors.isEmpty) {
      errors.foreach { (err: JSError) => log.error(err.toString) }
    }
    else {
      if (!warnings.isEmpty) {
        warnings.foreach { (err: JSError) => log.warn(err.toString) }
      }

      IO.createDirectory(file(target.getParent))
      IO.write(target, compiler.toSource)
    }
  }
}
