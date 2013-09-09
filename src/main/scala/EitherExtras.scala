package sbtclosure

import scala.sys.process._
import scala.util.{Try, Failure, Success}

/**
 *
 * sbt-closure / LogiDev - [Fun Functional] / Logikujo.com
 *
 * 8/09/13 :: 9:59 :: eof
 *
 * Code copied from: https://github.com/robcd/scala-either-extras/blob/simple/src/main/scala/EitherExtras.scala
 * by robcd http://robsscala.blogspot.co.uk/2012/04/validation-without-scalaz.html
 *
 */
trait EitherExtras[T] {
  def succeed[L]: Right[L, T]
  def fail[R]: Left[T, R]
  def check[L](checks: ((T) => Either[L, T])*): Either[List[L], T]
  def checkAndMap[L, R](checks: ((T) => Either[L, T])*)(f: (T) => R): Either[List[L], R]
}

object EitherExtras {
  def tryCommand(cmd: Seq[String]): Either[String, String] = {
    var error = ""
    lazy val logger = ProcessLogger(
      (_:String) => (),
      (e:String) => {error ++= s"\n$e"; ()})
    Try(cmd !!(logger)) match {
      case Failure(_) => error.fail
      case Success(s) => s.succeed
    }
  }

  def sequence[E,U](l: List[Either[E,U]]) = {
    val lefts = l collect {case Left(e) => e}
    val rights = l collect {case Right(s) => s}
    if (lefts.isEmpty) Right(rights) else Left(lefts)
  }

  implicit def any2EitherExtras[T](any: T): EitherExtras[T] = new EitherExtras[T] {
    def succeed[L] = Right[L, T](any)
    def fail[R] = Left[T, R](any)
    def check[L](checks: ((T) => Either[L, T])*): Either[List[L], T] =
      checkAndMap[L, T](checks: _*) { t => t }
    def checkAndMap[L, R](checks: ((T) => Either[L, T])*)(f: (T) => R): Either[List[L], R] = {
      val msgs = for {
        check <- checks.toList // from WrappedArray
        msg <- check(any).left.toSeq
      } yield msg
      if (msgs.isEmpty) Right(f(any)) else Left(msgs)
    }
  }
}
