# sbt-closure

[Simple Build Tool](http://www.scala-sbt.org/ "simple build tool") plugin for compiling JavaScript files from multiple sources using Google's Closure Compiler.

## Settings


## Installation

If you have not already added the sbt community plugin resolver to your plugin definition file, add this

    resolvers += Resolver.url("sbt-plugin-releases",
      new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(
        Resolver.ivyStylePatterns)

Then add this

    addSbtPlugin("org.scala-sbt" % "sbt-closure" % "0.1.0")

Then in your build definition, add

    seq(closureSettings:_*)

This will append `sbt-closure`'s settings for the `Compile` and `Test` configurations.

To add them to other configurations, use the provided `closureSettingsIn(config)` method.

    seq(closureSettingsIn(SomeOtherConfig):_*)

## Usage

The plugin scans your `src/main/javascript` directory
and looks for files of extension `.jsm`. These files
should contain ordered lists of JavaScript source locations. For example:

    # You can specify remote files using URLs...
    http://code.jquery.com/jquery-1.5.1.js

    # ...and local files using regular paths
    #    (relative to the location of the manifest):
    lib/foo.js
    bar.js
    ../bar.js

    # Blank lines and bash-style comments are ignored.

The plugin compiles this in two phases: first, it downloads and caches any
remote scripts. Second, it feeds all of the specified scripts into the Closure
compiler. The compiler outputs a file with the same name but with a `.js` extension under
`path/to/resource_managed/main/js`

For example, if your manifest
file is at `src/main/javascript/foo.jsm` in the source tree, the final
path would be `resource_managed/main/js/foo.js` in the target tree.

If, on compilation, the plugin finds remote scripts already cached on your
filesystem, it won't try to download them again. Running `sbt clean` will
delete the cache.

## Customization

If you're using [xsbt-web-plugin](https://github.com/siasia/xsbt-web-plugin "xsbt-web-plugin"), add the output files to the webapp with:

    // add managed resources to the webapp
    (webappResources in Compile) <+= (resourceManaged in Compile)

### Changing the directory that is scanned, use:

    (sourceDirectory in (Compile, ClosureKeys.closure)) <<= (sourceDirectory in Compile)(_ / "path" / "to" / "jsmfiles")

### Changing target js destination:

To change the default location of compiled js files, add the following to your build definition

    (resourceManaged in (Compile, ClosureKeys.closure)) <<= (crossTarget in Compile)(_ / "your_preference" / "js")

## File versioning

The plugin has a setting for a file suffix that is appended to the output file name before the file extension.
This allows you to update the version whenever you make changes to your Javascript files. Useful when you are
caching your js files in production. To use, add the following to your build.sbt:

    (ClosureKeys.suffix in (Compile, ClosureKeys.closure)) := "4"

Then if you have manifest file `src/main/javascript/script.jsm` it will be output as
`resource_managed/src/main/js/script-4.js`

This is only half of the puzzle, though. In order to know what that suffix is in your code,
you can use the [sbt-buildinfo](https://github.com/sbt/sbt-buildinfo) plugin. Add the plugin
to your project, then add the following to your build.sbt:

    seq(buildInfoSettings: _*)

    buildInfoPackage := "mypackage"

    buildInfoKeys := Seq[Scoped](ClosureKeys.suffix in (Compile, ClosureKeys.closure))

    sourceGenerators in Compile <+= buildInfo

This will generate a Scala file with your suffix in `src_managed/main/BuildInfo.scala` and
you can access it in your code like this:

    mypackage.BuildInfo.closure_suffix

In my Lift project I have the following snippet:

    package mypackage
    package snippet

    import net.liftweb._
    import util.Helpers._

    object JavaScript {
      def render = "* [src]" #> "/js/script-%s.js".format(BuildInfo.closure_suffix)
    }

Which is called in my template like:

    <script lift="JavaScript"></script>

## Acknowledgements

This plugin is a sbt 0.11.2 port of
[sbt-closure](https://github.com/davegurnell/sbt-closure)

It was modeled after and heavily influenced by [less-sbt](https://github.com/softprops/less-sbt "less-sbt")
