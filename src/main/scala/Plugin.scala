package sbtclosure

import java.nio.charset.Charset

import sbt._
import sbt.Project.Initialize

import com.google.javascript.jscomp.{CompilerOptions,JqueryCodingConvention,ClosureCodingConvention,CompilationLevel,XtbMessageBundle,EmptyMessageBundle}
import java.io.IOException;
import java.io.FileInputStream;

object SbtClosurePlugin extends Plugin {
  import sbt.Keys._
  import ClosureKeys._

  object ClosureKeys {
    lazy val closure = TaskKey[Seq[File]]("closure", "Compiles .jsm javascript manifest files")
    lazy val charset = SettingKey[Charset]("charset", "Sets the character encoding used in file IO. Defaults to utf-8")
    lazy val downloadDirectory = SettingKey[File]("download-dir", "Directory to download ManifestUrls to")
    lazy val prettyPrint = SettingKey[Boolean]("pretty-print", "Whether to pretty print JavaScript (default false)")
    lazy val optimizeArgumentsArray = SettingKey[Boolean]("optimizeArgumentsArray", "Provide formal names for elements of arguments array (default false)")  
    lazy val optimizeCalls = SettingKey[Boolean]("optimizeCalls", "Remove unused parameters from call sites (default false)")  
    lazy val optimizeParameters = SettingKey[Boolean]("optimizeParameters", "Remove unused and constant parameters (default false)")  
    lazy val optimizeReturns = SettingKey[Boolean]("optimizeReturns", "Remove unused return values (default false)") 
    lazy val advancedCompilerFlags = SettingKey[CompilerFlags]("advancedCompilerFlags", "set advancedCompilerFlags based on Command line compiler options (default not set)") 
    lazy val closureOptions = SettingKey[CompilerOptions]("options", "Compiler options")
    lazy val suffix = SettingKey[String]("suffix", "String to append to output filename (before file extension)")
  }

  class CompilerFlags {
    var process_jquery_primitives = false
    var compilation_level = CompilationLevel.SIMPLE_OPTIMIZATIONS
    var debug = false
    var use_types_for_optimization = false
    var generate_exports = false
    var process_closure_primitives = true
    var translationsFile = ""
    var translationsProject = ""

  }

  /**
   * Converter from http://code.google.com/p/closure-compiler/source/browse/trunk/src/com/google/javascript/jscomp/CommandLineRunner.java
  */
  def createOptions(flags: CompilerFlags) : CompilerOptions = {
    val options = new CompilerOptions();
    if (flags.process_jquery_primitives) {
      options.setCodingConvention(new JqueryCodingConvention());
    } else {
      options.setCodingConvention(new ClosureCodingConvention());
    }

    val level = flags.compilation_level;
    level.setOptionsForCompilationLevel(options)

    if (flags.debug) {
      level.setDebugOptionsForCompilationLevel(options);
    }

    if (flags.use_types_for_optimization) {
     // level.setTypeBasedOptimizationOptions(options);
    }

    if (flags.generate_exports) {
      options.setGenerateExports(flags.generate_exports);
    }

   // val wLevel = flags.warning_level;
   // wLevel.setOptionsForWarningLevel(options);
   // for (FormattingOption formattingOption : flags.formatting) {
   //   formattingOption.applyToOptions(options);
   //}

    options.closurePass = flags.process_closure_primitives;

    options.jqueryPass = flags.process_jquery_primitives &&
        CompilationLevel.ADVANCED_OPTIMIZATIONS == level;

    if (!flags.translationsFile.isEmpty()) {
      try {
        options.messageBundle = new XtbMessageBundle(
            new FileInputStream(flags.translationsFile),
            flags.translationsProject);
      } catch {
	case e : IOException => throw new RuntimeException("Reading XTB file", e);
      }
    } else if (CompilationLevel.ADVANCED_OPTIMIZATIONS == level) {
      // In SIMPLE or WHITESPACE mode, if the user hasn't specified a
      // translations file, they might reasonably try to write their own
      // implementation of goog.getMsg that makes the substitution at
      // run-time.
      //
      // In ADVANCED mode, goog.getMsg is going to be renamed anyway,
      // so we might as well inline it.
      options.messageBundle = new EmptyMessageBundle();
    }

    return options;
  }

  def closureOptionsSetting: Initialize[CompilerOptions] =
    (streams, 
       prettyPrint in closure,
       optimizeArgumentsArray in closure,
       optimizeCalls in closure,
       optimizeParameters in closure,
       optimizeReturns in closure,
       advancedCompilerFlags in closure) apply {
      (out, prettyPrint,
       optimizeArgumentsArray,
       optimizeCalls,
       optimizeParameters,
       optimizeReturns,
       advancedCompilerFlags) =>
        val options = createOptions(advancedCompilerFlags)
        options.prettyPrint = prettyPrint
        options.optimizeArgumentsArray = optimizeArgumentsArray
        options.optimizeCalls = optimizeCalls
        options.optimizeParameters = optimizeParameters
        options.optimizeReturns = optimizeReturns
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
    optimizeArgumentsArray := false,
    optimizeCalls := false,
    optimizeParameters := false,
    optimizeReturns := false,
    advancedCompilerFlags := new CompilerFlags,
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
