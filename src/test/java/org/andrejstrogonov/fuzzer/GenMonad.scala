package org.andrejstrogonov.fuzzer

import org.scalacheck.{Gen, GenMonad, Properties}

object GenMonad {
  given scalaCheckGenMonadInstance with 

  def genMonad: GenMonad[Gen] = new GenMonad[Gen] {
    def flatMap[A, B](a: Gen[A])(f: A => Gen[B]): Gen[B] = a.flatMap(f)

    def map[A, B](a: Gen[A])(f: A => B): Gen[B] = a.map(f)

    def choose(min: Int, max: Int): Gen[Int] = Gen.choose(min, max)

    def oneOf[A](items: A*): Gen[A] = Gen.oneOf(items)

    def const[A](c: A): Gen[A] = Gen.const(c)

    def widen[A, B >: A](ga: Gen[A]): Gen[B] = ga

    def generate[A](ga: Gen[A]): A = ga.sample.get
  }
}
