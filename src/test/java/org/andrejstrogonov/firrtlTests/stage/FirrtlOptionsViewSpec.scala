// SPDX-License-Identifier: Apache-2.0

package org.andrejstrogonov.firrtlTests.stage

import firrtl.stage._

import firrtl.{ir, NoneCompiler, Parser}
import firrtl.options.Viewer.view
import firrtl.stage.{FirrtlOptions, FirrtlOptionsView}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class BazCompiler extends NoneCompiler

class Baz_Compiler extends NoneCompiler

class FirrtlOptionsViewSpec extends AnyFlatSpec with Matchers {

  behavior.of(FirrtlOptionsView.getClass.getName)

  def circuitString(main: String): String = s"""|circuit $main:
                                                |  module $main:
                                                |    node x = UInt<1>("h0")
                                                |""".stripMargin

  val grault: ir.Circuit = Parser.parse(circuitString("grault"))

  val annotations = Seq(
    /* FirrtlOptions */
    OutputFileAnnotation("bar"),
    CompilerAnnotation(new BazCompiler()),
    InfoModeAnnotation("use"),
    FirrtlCircuitAnnotation(grault)
  )

  it should "construct a view from an AnnotationSeq" in {
    val out = view[FirrtlOptions](annotations)

    out.outputFileName should be(Some("bar"))
    out.infoModeName should be("use")
    out.firrtlCircuit should be(Some(grault))
  }

  /* This test only exists to catch changes to existing behavior. This test does not indicate that this is the correct
   * behavior, only that modifications to existing code will not change behavior that people may expect.
   */
  it should "overwrite or append to earlier annotation information with later annotation information" in {
    val grault_ = Parser.parse(circuitString("thud_"))

    val overwrites = Seq(
      OutputFileAnnotation("bar_"),
      CompilerAnnotation(new Baz_Compiler()),
      InfoModeAnnotation("gen"),
      FirrtlCircuitAnnotation(grault_)
    )

    val out = view[FirrtlOptions](annotations ++ overwrites)

    out.outputFileName should be(Some("bar_"))
    out.infoModeName should be("gen")
    out.firrtlCircuit should be(Some(grault_))
  }

}
