seq(closureSettings:_*)

(ClosureKeys.prettyPrint in (Compile, ClosureKeys.closure)) := true

TaskKey[Unit]("check") <<= (baseDirectory, resourceManaged) map { (baseDirectory, resourceManaged) =>
  val fixture = sbt.IO.read(baseDirectory / "fixtures" / "script.js")
  val out = sbt.IO.read(resourceManaged / "main" / "js" / "script.js")
  if (out.trim != fixture.trim) error("unexpected output: \n\n" + out + "\n\n" + fixture)
  ()
}
