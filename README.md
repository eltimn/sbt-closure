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

## Acknowledgements

This plugin is a sbt 0.11.2 port of
[sbt-closure](https://github.com/davegurnell/sbt-closure)

It was modeled after and heavily influenced by [less-sbt](https://github.com/softprops/less-sbt "less-sbt")
