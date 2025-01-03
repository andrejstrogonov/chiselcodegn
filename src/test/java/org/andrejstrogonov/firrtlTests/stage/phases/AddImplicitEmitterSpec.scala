// SPDX-License-Identifier: Apache-2.0

package org.andrejstrogonov.firrtlTests.stage.phases

import firrtl.{EmitAllModulesAnnotation, EmitCircuitAnnotation, HighFirrtlEmitter, VerilogCompiler}
import firrtl.annotations.NoTargetAnnotation
import firrtl.options.Phase
import firrtl.stage.{CompilerAnnotation, RunFirrtlTransformAnnotation}
import firrtl.stage.phases.AddImplicitEmitter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class AddImplicitEmitterSpec extends AnyFlatSpec with Matchers {

  case class FooAnnotation(x: Int) extends NoTargetAnnotation
  case class BarAnnotation(x: String) extends NoTargetAnnotation

  class Fixture { val phase: Phase = new AddImplicitEmitter }

  val someAnnos = Seq(FooAnnotation(1), FooAnnotation(2), BarAnnotation("bar"))

  behavior.of(classOf[AddImplicitEmitter].toString)

  it should "do nothing if no CompilerAnnotation is present" in new Fixture {
    phase.transform(someAnnos).toSeq should be(someAnnos)
  }

  it should "add an EmitCircuitAnnotation derived from a CompilerAnnotation" in new Fixture {
    val input = CompilerAnnotation(new VerilogCompiler) +: someAnnos
    val expected = input.flatMap {
      case a @ CompilerAnnotation(b) =>
        Seq(a, RunFirrtlTransformAnnotation(b.emitter), EmitCircuitAnnotation(b.emitter.getClass))
      case a => Some(a)
    }
    phase.transform(input).toSeq should be(expected)
  }

  it should "not add an EmitCircuitAnnotation if an EmitAnnotation already exists" in new Fixture {
    val input =
      Seq(CompilerAnnotation(new VerilogCompiler), EmitAllModulesAnnotation(classOf[HighFirrtlEmitter])) ++ someAnnos
    phase.transform(input).toSeq should be(input)
  }

}
