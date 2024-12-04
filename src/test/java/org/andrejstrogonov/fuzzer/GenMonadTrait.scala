package org.andrejstrogonov.fuzzer

import org.scalacheck.{Gen, GenMonad, Properties}

trait GenMonadTrait {

  def flatMap[A, B](a: Any[A])(f: A => Any[B]): Any[B]

  def map[A, B](a: Any[A])(f: A => B): Any[B]

  def choose(min: Int, max: Int): Any[Int]

  def oneOf[A](items: A*): Any[A]

  def const[A](c: A): Any[A]

  def widen[A, B >: A](ga: Any[A]): Any[B]

  def generate[A](ga: Any[A]): A
}
