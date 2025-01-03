// SPDX-License-Identifier: Apache-2.0

package org.andrejstrogonov.firrtlTests

import firrtl._
import firrtl.passes._
import firrtl.testutils._

class ChirrtlSpec extends FirrtlFlatSpec {
  def transforms = Seq(
    CheckChirrtl,
    CInferTypes,
    CInferMDir,
    RemoveCHIRRTL,
    ToWorkingIR,
    CheckHighForm,
    ResolveKinds,
    InferTypes,
    CheckTypes,
    ResolveFlows,
    CheckFlows,
    new InferWidths,
    CheckWidths,
    PullMuxes,
    ExpandConnects,
    RemoveAccesses,
    ExpandWhens,
    CheckInitialization
  )

  "Chirrtl memories" should "allow ports with clocks defined after the memory" in {
    val input =
      """circuit Unit :
        |  module Unit :
        |    input clock : Clock
        |    smem ram : UInt<32>[128]
        |    node newClock = clock
        |    infer mport x = ram[UInt(2)], newClock
        |    x <= UInt(3)
        |    when UInt(1) :
        |      infer mport y = ram[UInt(4)], newClock
        |      y <= UInt(5)
       """.stripMargin
    val circuit = Parser.parse(input.split("\n").toIterator)
    transforms.foldLeft(CircuitState(circuit, UnknownForm)) { (c: CircuitState, p: Transform) =>
      p.runTransform(c)
    }
  }

  "Chirrtl" should "catch undeclared wires" in {
    val input =
      """circuit Unit :
        |  module Unit :
        |    input clock : Clock
        |    smem ram : UInt<32>[128]
        |    node newClock = clock
        |    infer mport x = ram[UInt(2)], newClock
        |    x <= UInt(3)
        |    when UInt(1) :
        |      infer mport y = ram[UInt(4)], newClock
        |      y <= z
       """.stripMargin
    intercept[PassException] {
      val circuit = Parser.parse(input.split("\n").toIterator)
      transforms.foldLeft(CircuitState(circuit, UnknownForm)) { (c: CircuitState, p: Transform) =>
        p.runTransform(c)
      }
    }
  }

  behavior.of("Uniqueness")
  for ((description, input) <- CheckSpec.nonUniqueExamples) {
    it should s"be asserted for $description" in {
      assertThrows[CheckHighForm.NotUniqueException] {
        Seq(ToWorkingIR, CheckHighForm).foldLeft(Parser.parse(input)) { case (c, tx) => tx.run(c) }
      }
    }
  }
}

class ChirrtlMemsExecutionTest extends ExecutionTest("ChirrtlMems", "/features")
class EmptyChirrtlMemCompilationTest extends CompilationTest("EmptyChirrtlMem", "/features")
class NodeTypeCompilationTest extends CompilationTest("NodeType", "/features")
