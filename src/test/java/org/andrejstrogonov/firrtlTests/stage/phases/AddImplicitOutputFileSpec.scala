// SPDX-License-Identifier: Apache-2.0

package org.andrejstrogonov.firrtlTests.stage.phases

import firrtl.{ChirrtlEmitter, EmitAllModulesAnnotation, Parser}
import firrtl.options.Phase
import firrtl.stage.{FirrtlCircuitAnnotation, OutputFileAnnotation}
import firrtl.stage.phases.AddImplicitOutputFile
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class AddImplicitOutputFileSpec extends AnyFlatSpec with Matchers {

  class Fixture { val phase: Phase = new AddImplicitOutputFile }

  val foo = """|circuit Foo:
               |  module Foo:
               |    node a = UInt<1>("h0")
               |""".stripMargin

  val circuit = Parser.parse(foo)

  behavior.of(classOf[AddImplicitOutputFile].toString)

  it should "default to an output file named 'a'" in new Fixture {
    phase.transform(Seq.empty).toSeq should be(Seq(OutputFileAnnotation("a")))
  }

  it should "set the output file based on a FirrtlCircuitAnnotation's main" in new Fixture {
    val in = Seq(FirrtlCircuitAnnotation(circuit))
    val out = OutputFileAnnotation(circuit.main) +: in
    phase.transform(in).toSeq should be(out)
  }

  it should "do nothing if an OutputFileAnnotation or EmitAllModulesAnnotation already exists" in new Fixture {

    info("OutputFileAnnotation works")
    val outputFile = Seq(OutputFileAnnotation("Bar"), FirrtlCircuitAnnotation(circuit))
    phase.transform(outputFile).toSeq should be(outputFile)

    info("EmitAllModulesAnnotation works")
    val eam = Seq(EmitAllModulesAnnotation(classOf[ChirrtlEmitter]), FirrtlCircuitAnnotation(circuit))
    phase.transform(eam).toSeq should be(eam)
  }

}
