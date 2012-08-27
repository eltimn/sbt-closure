package sbtclosure

import java.nio.charset.Charset

import sbt._
import sbt.Project.Initialize

import com.google.javascript.jscomp.CompilerOptions

object SbtClosurePlugin extends Plugin {
  import sbt.Keys._
  import ClosureKeys._

  object ClosureKeys {
    lazy val closure = TaskKey[Seq[File]]("closure", "Compiles .jsm javascript manifest files")
    lazy val charset = SettingKey[Charset]("charset", "Sets the character encoding used in file IO. Defaults to utf-8")
    lazy val downloadDirectory = SettingKey[File]("download-dir", "Directory to download ManifestUrls to")
    lazy val prettyPrint = SettingKey[Boolean]("pretty-print", "Whether to pretty print JavaScript (default false)")
    lazy val closureOptions = SettingKey[CompilerOptions]("options", "Compiler options")
    lazy val suffix = SettingKey[String]("suffix", "String to append to output filename (before file extension)")
  }

  def closureOptionsSetting: Initialize[CompilerOptions] =
    (streams, prettyPrint in closure) apply {
      (out, prettyPrint) =>
        val options = new CompilerOptions
        options.prettyPrint = prettyPrint
        options
    }

  def closureSettings: Seq[Setting[_]] =
    closureSettingsIn(Compile) ++ closureSettingsIn(Test)

  def closureSettingsIn(conf: Configuration): Seq[Setting[_]] =
    inConfig(conf)(closureSettings0 ++ Seq(
      sourceDirectory in closure <<= (sourceDirectory in conf) { _ / "javascript" },
      resourceManaged in closure <<= (resourceManaged in conf) { _ / "js" },
      downloadDirectory in closure <<= (target in conf) { _ / "closure-downloads" },
      cleanFiles in closure <<= (resourceManaged in closure, downloadDirectory in closure)(_ :: _ :: Nil),
      watchSources <<= (unmanagedSources in closure)
    )) ++ Seq(
      cleanFiles <++= (cleanFiles in closure in conf),
      watchSources <++= (watchSources in closure in conf),
      resourceGenerators in conf <+= closure in conf,
      compile in conf <<= (compile in conf).dependsOn(closure in conf)
    )

  def closureSettings0: Seq[Setting[_]] = Seq(
    charset in closure := Charset.forName("utf-8"),
    prettyPrint := false,
    closureOptions <<= closureOptionsSetting,
    includeFilter in closure := "*.jsm",
    excludeFilter in closure := (".*" - ".") || HiddenFileFilter,
    suffix in closure := "",
    unmanagedSources in closure <<= closureSourcesTask,
    clean in closure <<= closureCleanTask,
    closure <<= closureCompilerTask
  )

  private def closureCleanTask =
    (streams, resourceManaged in closure) map {
      (out, target) =>
        out.log.info("Cleaning generated JavaScript under " + target)
        IO.delete(target)
    }

  private def closureCompilerTask =
    (streams, sourceDirectory in closure, resourceManaged in closure,
     includeFilter in closure, excludeFilter in closure, charset in closure,
     downloadDirectory in closure, closureOptions in closure, suffix in closure) map {
      (out, sources, target, include, exclude, charset, downloadDir, options, suffix) => {
        // compile changed sources
        (for {
          manifest <- sources.descendantsExcept(include, exclude).get
          outFile <- computeOutFile(sources, manifest, target, suffix)
          if (manifest newerThan outFile)
        } yield { (manifest, outFile) }) match {
          case Nil =>
            out.log.debug("No JavaScript manifest files to compile")
          case xs =>
            out.log.info("Compiling %d jsm files to %s" format(xs.size, target))
            xs map doCompile(downloadDir, charset, out.log, options)
            out.log.debug("Compiled %s jsm files" format xs.size)
        }
        compiled(target)
      }
    }

  private def closureSourcesTask =
    (sourceDirectory in closure, includeFilter in closure, excludeFilter in closure) map {
      (sourceDir, incl, excl) =>
         sourceDir.descendantsExcept(incl, excl).get
    }

  private def doCompile(downloadDir: File, charset: Charset, log: Logger, options: CompilerOptions)(pair: (File, File)) = {
    val (jsm, js) = pair
    log.debug("Compiling %s" format jsm)
    val srcFiles = Manifest.files(jsm, downloadDir, charset)
    val compiler = new Compiler(options)
    compiler.compile(srcFiles, Nil, js, log)
  }

  private def compiled(under: File) = (under ** "*.js").get

  private def computeOutFile(sources: File, manifest: File, targetDir: File, suffix: String): Option[File] = {
    val outFile = IO.relativize(sources, manifest).get.replaceAll("""[.]jsm(anifest)?$""", "") + {
      if (suffix.length > 0) "-%s.js".format(suffix)
      else ".js"
    }
    Some(new File(targetDir, outFile))
  }
}
