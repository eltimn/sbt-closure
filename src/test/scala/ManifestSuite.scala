package sbtclosure

import org.scalatest._
import sbt._
import java.io.File

class ManifestSuite extends FunSuite with Manifest {

  test("stripComments") {
    expectResult(""){ stripComments("#this and that") }
    expectResult(""){ stripComments("  #this and that") }
    expectResult(""){ stripComments("#") }
    expectResult(""){ stripComments("###") }

    expectResult("this and that"){ stripComments("this and that  #this and that") }
    expectResult("this and that"){ stripComments("this and that") }
  }

  test("isUrl") {
    expectResult(true){ isUrl("http://www.untyped.com/") }
    expectResult(true){ isUrl("https://www.untyped.com/") }
    expectResult(true){ isUrl("   http://www.untyped.com/  ") }

    expectResult(false){ isUrl("foobar") }
    expectResult(false){ isUrl("untyped.com") }
    expectResult(false){ isUrl("#http://www.untyped.com/") }
  }

  test("parse") {
    expectResult(List(ManifestFile("foo.js"))){ parse(List("foo.js")) }
    expectResult(List(ManifestFile("foo.js"),
                ManifestUrl("http://untyped.com/"),
                ManifestFile("bar.js"))){
      parse("foo.js" :: "#A Comment" :: "http://untyped.com/ #T3h best website evar!" :: "" :: "bar.js" :: Nil)
    }
  }

  test("ManifestUrl.content"){
    val url = ManifestUrl("http://www.untyped.com/")

    assert(url.content.contains("Untyped"))
  }

  test("ManifestUrl filenames"){
    expectResult("http___untyped.com_"){ ManifestUrl("http://untyped.com/").filename }
    expectResult("http___code.jquery.com_jquery_1.5.1.js"){
      ManifestUrl("http://code.jquery.com/jquery-1.5.1.js").filename
    }
  }

  /*
  test("ManifestFile path where filename contains forward slashes"){
    expectResult(file("foo/bar/baz.js")){
      ManifestFile("bar/baz.js").path(file("foo"))
    }
  }
  */
}
