resolvers += Resolver.url("scalasbt", new URL(
  "http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases"))(
  Resolver.ivyStylePatterns)

resolvers ++= Seq(
  "less is" at "http://repo.lessis.me",
  "coda" at "http://repo.codahale.com"
)

libraryDependencies <+= sbtVersion { v =>
  if (v == "0.11.2") "org.scala-tools.sbt" %% "scripted-plugin" % v
  else "org.scala-sbt" %% "scripted-plugin" % v
}

addSbtPlugin("me.lessis" % "ls-sbt" % "0.1.1")
