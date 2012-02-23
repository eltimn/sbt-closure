package sbtclosure

import org.scalatest._
import sbt._
import java.io.File

class ManifestSuite extends FunSuite with Manifest {

  test("stripComments") {
    expect(""){ stripComments("#this and that") }
    expect(""){ stripComments("  #this and that") }
    expect(""){ stripComments("#") }
    expect(""){ stripComments("###") }

    expect("this and that"){ stripComments("this and that  #this and that") }
    expect("this and that"){ stripComments("this and that") }
  }

  test("isUrl") {
    expect(true){ isUrl("http://www.untyped.com/") }
    expect(true){ isUrl("https://www.untyped.com/") }
    expect(true){ isUrl("   http://www.untyped.com/  ") }

    expect(false){ isUrl("foobar") }
    expect(false){ isUrl("untyped.com") }
    expect(false){ isUrl("#http://www.untyped.com/") }
  }

  test("parse") {
    expect(List(ManifestFile("foo.js"))){ parse(List("foo.js")) }
    expect(List(ManifestFile("foo.js"),
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
    expect("http___untyped.com_"){ ManifestUrl("http://untyped.com/").filename }
    expect("http___code.jquery.com_jquery_1.5.1.js"){
      ManifestUrl("http://code.jquery.com/jquery-1.5.1.js").filename
    }
  }

  /*
  test("ManifestFile path where filename contains forward slashes"){
    expect(file("foo/bar/baz.js")){
      ManifestFile("bar/baz.js").path(file("foo"))
    }
  }
  */
}
