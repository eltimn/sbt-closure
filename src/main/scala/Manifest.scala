package sbtclosure

import java.nio.charset.Charset

import scala.io.Source
import EitherExtras._

import sbt._

trait Manifest {
  protected def stripComments(line: String) = "#.*$".r.replaceAllIn(line, "").trim
  protected def isSkippable(line: String): Boolean = stripComments(line) == ""
  protected def isUrl(line: String): Boolean = stripComments(line).matches("^https?:.*")
  private val Extension = """.+\.(.+$)""".r

  protected def parse(lines: List[String]): Either[List[String], List[ManifestObject]] =
    sequence(lines
      .map(stripComments _)
      .filter(item => !isSkippable(item))
      .map(line => if (isUrl(line)) ManifestUrl(line).succeed else line match {
        case Extension("ls") => ManifestLsFile(line).succeed
        case Extension("js") => ManifestJsFile(line).succeed
        case _ => s"""Could not recognize file type in manifest for file: $line""".fail
    }))
}

object Manifest extends Manifest {
  /**
    * Get the files for a manifest, downloading the urls to a temp dir
    */
  def files(manifest: File, downloadDir: File, charset: Charset): Either[List[String],List[File]] = {
    lazy val manifestDir: File = file(manifest.getParent)
    lazy val manifests = parse(IO.readLines(manifest, charset)).right

    def moToFile(mo: ManifestObject):  Either[String, File] = mo match {
      case u: ManifestUrl => u.file(downloadDir)
      case f => f.file(manifestDir)
    }

    // Using flatMap on Either avoids to compute compilation if manifest file couldnt be parsed
    manifests flatMap (xs => sequence(xs map moToFile))
  }
}

sealed abstract class ManifestObject {
  def file(parent: File): Either[String,File]
  /*def path(parent: File): File =
    filename.split("""[/\\]""").foldLeft(parent)(_ / _)*/
}

case class ManifestJsFile(filename: String) extends ManifestObject {
  def file(parent: File): Either[String,File] = (parent / filename).succeed
}

case class ManifestLsFile(filename: String) extends ManifestObject {
  def file(parent: File): Either[String,File] = {
    val outJs = parent / (sbt.file(filename).base.toString ++ ".js" )
    LiveScriptCompiler.compile(parent / filename, outJs)
  }
}

case class ManifestUrl(url: String) extends ManifestObject {
  lazy val filename: String = """[^A-Za-z0-9.]""".r.replaceAllIn(url, "_")

  def content: String = Source.fromInputStream(new URL(url).openStream).mkString

  def file(parent: File): Either[String,File] = {
    val out = parent / filename
    IO.createDirectory(parent)
    IO.write(out, content)
    out.succeed
  }
}
