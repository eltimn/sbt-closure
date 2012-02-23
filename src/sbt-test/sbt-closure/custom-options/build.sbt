seq(closureSettings:_*)

(ClosureKeys.closureOptions in (Compile, ClosureKeys.closure)) := myClosureOptions

TaskKey[Unit]("check") <<= (baseDirectory, resourceManaged) map { (baseDirectory, resourceManaged) =>
  val fixture = sbt.IO.read(baseDirectory / "fixtures" / "script.js")
  val out = sbt.IO.read(resourceManaged / "main" / "js" / "script.js")
  if (out.trim != fixture.trim) error("unexpected output: \n\n" + out.length + "\n\n" + fixture.length)
  ()
}
