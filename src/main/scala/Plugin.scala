package sbtclosure

import java.nio.charset.Charset

import sbt._
//import sbt.Project.Initialize

import com.google.javascript.jscomp.CompilerOptions

object SbtClosurePlugin extends Plugin {
  import sbt.Keys._
  import ClosureKeys._

  object ClosureKeys {
    val closure = taskKey[Seq[File]]("Compiles .jsm javascript manifest files")
    lazy val charset = SettingKey[Charset]("charset", "Sets the character encoding used in file IO. Defaults to utf-8")
    val downloadDirectory = settingKey[File]("Directory to download ManifestUrls to")
    lazy val livescriptOutput = SettingKey[File]("livescript-output", "Directory where to generate output js from livescript scripts")
    val prettyPrint = settingKey[Boolean]("Whether to pretty print JavaScript (default false)")
    val closureOptions = settingKey[CompilerOptions]("Compiler options")
    val suffix = settingKey[Option[String]]("String to append to output filename (before extension")
  }

  /*def closureOptionsSetting: Def.Initialize[CompilerOptions] =
    (streams, prettyPrint in closure) apply {
      (out, prettyPrint) =>
        val options = new CompilerOptions
        options.prettyPrint = prettyPrint
        options
    }*/

  lazy val closureOptionsImpl = Def.setting {
    val options = new CompilerOptions
    options.prettyPrint = (prettyPrint in closure).value
    options
  }

  def closureSettings: Seq[Setting[_]] =
    closureSettingsIn(Compile) ++ closureSettingsIn(Test)

  def closureSettingsIn(conf: Configuration): Seq[Setting[_]] =
    inConfig(conf)(closureSettings0 ++ Seq(
      sourceDirectory in closure := (sourceDirectory in conf).value / "javascript",
      resourceManaged in closure := (resourceManaged in conf).value / "js",
      downloadDirectory in closure := (target in conf).value / "closure-downloads",
      livescriptOutput in closure := (target in conf).value / "livescript-output",
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
    closureOptions := closureOptionsImpl.value,
    includeFilter in closure := "*.jsm",
    excludeFilter in closure := (".*" - ".") || HiddenFileFilter,
    suffix in closure := None,
    unmanagedSources in closure := sourcesTaskImpl.value,
    (clean in closure) := cleanTaskImpl.value,
    closure <<= closureCompilerTask
  )

  lazy val cleanTaskImpl: Def.Initialize[Task[Unit]] = Def.task {
    streams.value.log.info("Cleaning generated JavaScript under " + target)
    IO.delete(resourceManaged.value :: livescriptOutput.value :: Nil)
  }

  lazy val compilerTask = Def.task {
    val out = streams.value.log
    val manifestFile = (m: File) => IO.relativize((sourceDirectory in compile) value, m)
    val manifestsList = for {
      manifest <- (unmanagedSources in closure).value
      _file = manifestFile(manifest).get.replaceAll("""[.]jsm(anifest)?$""","")
      _suffix = (suffix in closure).value.map("-" ++ _ ).getOrElse("")
      outFile <- (resourceManaged in compile).value / (s"${_file}.${_suffix}.js")
    } yield (manifest, outFile)
    manifestsList.foreach ((p:(File, File)) => {
      doCompile(
        (downloadDirectory in closure) value,
        charset.value,
        out,
        (closureOptions in closure).value)(p)
    })
    ((unmanagedResources in closure).value ** "*.js").get
  }

  private def closureCompilerTask =
    (streams, sourceDirectory in closure, resourceManaged in closure,
     includeFilter in closure, excludeFilter in closure, charset in closure,
     downloadDirectory in closure, closureOptions in closure, suffix in closure) map {
      (out, sources, target, include, exclude, charset, downloadDir, options, suffix) => {
        // compile changed sources
        (for {
          manifest <- sources.descendantsExcept(include, exclude).get
          outFile <- computeOutFile(sources, manifest, target, suffix.getOrElse(""))
          if (manifest newerThan outFile)
        } yield { (manifest, outFile) }) match {
          case Nil =>
            out.log.debug("No JavaScript manifest files to compile")
          case xs =>
            out.log.info("1936-14A")
            out.log.info("Compiling %d jsm files to %s" format(xs.size, target))
            xs map doCompile(downloadDir, charset, out.log, options)
            out.log.debug("Compiled %s jsm files" format xs.size)
        }
        compiled(target)
      }
    }

  lazy val sourcesTaskImpl: Def.Initialize[Task[Seq[File]]] = Def.task {
    (sourceDirectory in closure).value.descendantsExcept(
      (includeFilter in closure).value,
      (excludeFilter in closure).value).get
  }

  private def doCompile(downloadDir: File, charset: Charset, log: Logger, options: CompilerOptions)(pair: (File, File)) = {
    val (jsm, js) = pair
    log.debug("Compiling %s" format jsm)
    val srcFiles = Manifest.files(jsm, downloadDir, charset)
    lazy val compiler = new Compiler(options)
    def onError(errors: List[String]): Unit = errors.foreach(log.error(_))
    def onSuccess(files: List[File]): Unit = compiler.compile(files, Nil, js, log)
    srcFiles.fold(onError, onSuccess)
  }

  private def compiled(under: File) = (under ** "*.js").get

  private def computeOutFile(sources: File, manifest: File, targetDir: File, suffix: String): Option[File] = {
    val outFile = IO.relativize(sources, manifest).get.replaceAll("""[.]jsm(anifest)?$""", "") + {
      if (suffix.length > 0) "-%s.js".format(suffix)
      else ".js"
    }
    Some(new File(targetDir, outFile))
  }

  private def computeOutFile2 = {

  }
}
