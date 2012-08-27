sbtPlugin := true

organization := "org.scala-sbt"

name := "sbt-closure"

version <<= sbtVersion(v =>
  if(v.startsWith("0.11")) "0.1.2"
  else if(v.startsWith("0.12")) "0.1.3"
  else error("unsupported sbt version %s" format v)
)

libraryDependencies += "com.google.javascript" % "closure-compiler" % "r1741"

libraryDependencies += "org.scalatest" %% "scalatest" % "1.6.1" % "test"

seq(scriptedSettings:_*)

seq(lsSettings:_*)

(LsKeys.tags in LsKeys.lsync) := Seq("sbt", "closure")

(description in LsKeys.lsync) :=
  "Sbt plugin for compiling JavaScript manifest sources using Google Closure Compiler"

homepage := Some(url("https://github.com/eltimn/sbt-closure"))

publishTo := Some(Resolver.url("publishTo", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns))

publishMavenStyle := false

publishArtifact in Test := false

licenses in GlobalScope += "Apache License 2.0" -> url("https://github.com/eltimn/sbt-closure/raw/master/LICENSE")

pomExtra := (
  <scm>
    <url>git@github.com:eltimn/sbt-closure.git</url>
    <connection>scm:git:git@github.com:eltimn/sbt-closure.git</connection>
  </scm>
  <developers>
    <developer>
      <id>eltimn</id>
      <name>Tim Nelson</name>
      <url>http://eltimn.com/</url>
    </developer>
  </developers>
)

scalacOptions := Seq("-deprecation", "-unchecked", "-encoding", "utf8")
