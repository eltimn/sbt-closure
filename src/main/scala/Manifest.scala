package sbtclosure

import java.nio.charset.Charset

import scala.io.Source

import sbt._

trait Manifest {
  protected def stripComments(line: String) = "#.*$".r.replaceAllIn(line, "").trim
  protected def isSkippable(line: String): Boolean = stripComments(line) == ""
  protected def isUrl(line: String): Boolean = stripComments(line).matches("^https?:.*")

  protected def parse(lines: List[String]): List[ManifestObject] =
    lines
      .map(stripComments _)
      .filter(item => !isSkippable(item))
      .map(line => if (isUrl(line)) ManifestUrl(line) else ManifestFile(line))
}

object Manifest extends Manifest {
  /**
    * Get the files for a manifest, downloading the urls to a temp dir
    */
  def files(manifest: File, downloadDir: File, charset: Charset): List[File] = {
    val manifestDir: File = file(manifest.getParent)

    parse(IO.readLines(manifest, charset)).map(mo => mo match {
      case f: ManifestFile => f.file(manifestDir)
      case u: ManifestUrl =>  u.file(downloadDir)
    })
  }
}

sealed abstract class ManifestObject {
  def file(parent: File): File
  /*def path(parent: File): File =
    filename.split("""[/\\]""").foldLeft(parent)(_ / _)*/
}

case class ManifestFile(filename: String) extends ManifestObject {
  def file(parent: File): File = parent / filename
}

case class ManifestUrl(url: String) extends ManifestObject {
  lazy val filename: String = """[^A-Za-z0-9.]""".r.replaceAllIn(url, "_")

  def content: String = Source.fromInputStream(new URL(url).openStream).mkString

  def file(parent: File): File = {
    val out = parent / filename
    IO.createDirectory(parent)
    IO.write(out, content)
    out
  }
}
