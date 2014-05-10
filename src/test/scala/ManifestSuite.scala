package sbtclosure

import org.scalatest._
import sbt._
import java.io.File

class ManifestSuite extends FunSuite with Manifest {

  test("stripComments") {
    assertResult(""){ stripComments("#this and that") }
    assertResult(""){ stripComments("  #this and that") }
    assertResult(""){ stripComments("#") }
    assertResult(""){ stripComments("###") }

    assertResult("this and that"){ stripComments("this and that  #this and that") }
    assertResult("this and that"){ stripComments("this and that") }
  }

  test("isUrl") {
    assertResult(true){ isUrl("http://www.untyped.com/") }
    assertResult(true){ isUrl("https://www.untyped.com/") }
    assertResult(true){ isUrl("   http://www.untyped.com/  ") }

    assertResult(false){ isUrl("foobar") }
    assertResult(false){ isUrl("untyped.com") }
    assertResult(false){ isUrl("#http://www.untyped.com/") }
  }

  test("parse") {
    assertResult(List(ManifestFile("foo.js"))){ parse(List("foo.js")) }
    assertResult(List(ManifestFile("foo.js"),
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
    assertResult("http___untyped.com_"){ ManifestUrl("http://untyped.com/").filename }
    assertResult("http___code.jquery.com_jquery_1.5.1.js"){
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
