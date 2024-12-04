package org.andrejstrogonov.fuzzer

import org.scalacheck.{Gen, GenMonad, Properties}

object GenMonad {
  implicit def scalaCheckGenMonadInstance: GenMonad[Gen] = new GenMonad[Gen] with GenMonadTrait {
    override def flatMap[A, B](a: Gen[A])(f: A => Gen[B]): Gen[B] = a.flatMap(f)

    override def map[A, B](a: Gen[A])(f: A => B): Gen[B] = a.map(f)

    override def choose(min: Int, max: Int): Gen[Int] = Gen.choose(min, max)

    override def oneOf[A](items: A*): Gen[A] = Gen.oneOf(items)

    override def const[A](c: A): Gen[A] = Gen.const(c)

    override def widen[A, B >: A](ga: Gen[A]): Gen[B] = ga

    override def generate[A](ga: Gen[A]): A = ga.sample.get
  }
}
