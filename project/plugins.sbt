resolvers += Resolver.url("scalasbt", new URL(
  "http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases"))(
  Resolver.ivyStylePatterns)

resolvers ++= Seq(
  "coda" at "http://repo.codahale.com"
)

libraryDependencies <+= sbtVersion { v =>
  "org.scala-sbt" % "scripted-plugin" % v
}

addSbtPlugin("me.lessis" % "ls-sbt" % "0.1.3")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.1.5")

addSbtPlugin("com.typesafe.sbteclipse" %% "sbteclipse-plugin" % "2.2.0")

