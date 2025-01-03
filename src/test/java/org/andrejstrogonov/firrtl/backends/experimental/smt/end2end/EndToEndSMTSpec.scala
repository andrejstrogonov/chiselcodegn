// SPDX-License-Identifier: Apache-2.0

package org.andrejstrogonov.firrtl.backends.experimental.smt.end2end

import firrtl.annotations.{Annotation, CircuitTarget, PresetAnnotation}
import firrtl.backends.experimental.smt.random.{InvalidToRandomPass, UndefinedMemoryBehaviorPass}
import firrtl.backends.experimental.smt.{Btor2Emitter, SMTLibEmitter}
import firrtl.options.{Dependency, TargetDirAnnotation}
import firrtl.stage.{FirrtlCircuitAnnotation, FirrtlStage, OutputFileAnnotation, RunFirrtlTransformAnnotation}
import firrtl.util.BackendCompilationUtilities.timeStamp
import logger.{LazyLogging, LogLevel, LogLevelAnnotation}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import os._

import scala.sys.process._
import scala.util.Properties

class EndToEndSMTSpec extends EndToEndSMTBaseSpec with LazyLogging {
  "we" should "check if Z3 is available" taggedAs (RequiresZ3) in {
    val log = ProcessLogger(_ => (), logger.warn(_))
    val ret = Process(Seq("which", "z3")).run(log).exitValue()
    if (ret != 0) {
      logger.error("""The z3 SMT-Solver seems not to be installed.
                     |You can exclude the end-to-end smt backend tests which rely on z3 like this:
                     |sbt testOnly -- -l RequiresZ3
                     |""".stripMargin)
    }
    assert(ret == 0)
  }

  "Z3" should "be available in version 4" taggedAs (RequiresZ3) in {
    assert(Z3ModelChecker.getZ3Version.startsWith("4."))
  }

  "a simple combinatorial check" should "pass" taggedAs (RequiresZ3) in {
    val in =
      """circuit CC00:
        |  module CC00:
        |    input c: Clock
        |    input a: UInt<1>
        |    input b: UInt<1>
        |    assert(c, lt(add(a, b), UInt(3)), UInt(1), "a + b < 3")
        |""".stripMargin
    test(in, MCSuccess)
  }

  "a simple combinatorial check" should "fail immediately" taggedAs (RequiresZ3) in {
    val in =
      """circuit CC01:
        |  module CC01:
        |    input c: Clock
        |    input a: UInt<1>
        |    input b: UInt<1>
        |    assert(c, gt(add(a, b), UInt(3)), UInt(1), "a + b > 3")
        |""".stripMargin
    test(in, MCFail(0))
  }

  "adding the right assumption" should "make a test pass" taggedAs (RequiresZ3) in {
    val in0 =
      """circuit CC01:
        |  module CC01:
        |    input c: Clock
        |    input a: UInt<1>
        |    input b: UInt<1>
        |    assert(c, neq(add(a, b), UInt(0)), UInt(1), "a + b != 0")
        |""".stripMargin
    val in1 =
      """circuit CC01:
        |  module CC01:
        |    input c: Clock
        |    input a: UInt<1>
        |    input b: UInt<1>
        |    assert(c, neq(add(a, b), UInt(0)), UInt(1), "a + b != 0")
        |    assume(c, neq(a, UInt(0)), UInt(1), "a != 0")
        |""".stripMargin
    test(in0, MCFail(0))
    test(in1, MCSuccess)

    val in2 =
      """circuit CC01:
        |  module CC01:
        |    input c: Clock
        |    input a: UInt<1>
        |    input b: UInt<1>
        |    input en: UInt<1>
        |    assert(c, neq(add(a, b), UInt(0)), UInt(1), "a + b != 0")
        |    assume(c, neq(a, UInt(0)), en, "a != 0 if en")
        |""".stripMargin
    test(in2, MCFail(0))
  }

  "a register connected to preset reset" should "be initialized with the reset value" taggedAs (RequiresZ3) in {
    def in(rEq: Int) =
      s"""circuit Preset00:
         |  module Preset00:
         |    input c: Clock
         |    input preset: AsyncReset
         |    reg r: UInt<4>, c with: (reset => (preset, UInt(3)))
         |    assert(c, eq(r, UInt($rEq)), UInt(1), "r = $rEq")
         |""".stripMargin
    test(in(3), MCSuccess, kmax = 1)
    test(in(2), MCFail(0))
  }

  "a register's initial value" should "should not change" taggedAs (RequiresZ3) in {
    val in =
      """circuit Preset00:
        |  module Preset00:
        |    input c: Clock
        |    input preset: AsyncReset
        |
        |    ; the past value of our register will only be valid in the 1st unrolling
        |    reg past_valid: UInt<1>, c with: (reset => (preset, UInt(0)))
        |    past_valid <= UInt(1)
        |
        |    reg r: UInt<4>, c
        |    reg r_past: UInt<4>, c
        |    r_past <= r
        |    assert(c, eq(r, r_past), past_valid, "past_valid => r == r_past")
        |""".stripMargin
    test(in, MCSuccess, kmax = 2)
  }
}

abstract class EndToEndSMTBaseSpec extends AnyFlatSpec with Matchers {
  def test(src: String, expected: MCResult, kmax: Int = 0, clue: String = "", annos: Seq[Annotation] = Seq()): Unit = {
    expected match {
      case MCFail(k) =>
        assert(kmax >= k, s"Please set a kmax that includes the expected failing step! ($kmax < $expected)")
      case _ =>
    }
    val fir = firrtl.Parser.parse(src)
    val name = fir.main
    // @todo rewrite BackendCompilationUtilities
    val testBaseDir = os.pwd / "test_run_dir" / s"EndToEndSMT.$name"
    os.makeDir.all(testBaseDir)
    val testDir = os.temp.dir(testBaseDir, timeStamp, deleteOnExit = false)
    // we automagically add a preset annotation if an input called preset exists
    val presetAnno = if (!src.contains("input preset")) { None }
    else {
      Some(PresetAnnotation(CircuitTarget(name).module(name).ref("preset")))
    }
    val res = (new FirrtlStage).execute(
      Array(),
      Seq(
        LogLevelAnnotation(LogLevel.Error), // silence warnings for tests
        RunFirrtlTransformAnnotation(Dependency(SMTLibEmitter)),
        RunFirrtlTransformAnnotation(Dependency(Btor2Emitter)),
        FirrtlCircuitAnnotation(fir),
        TargetDirAnnotation(testDir.toString)
      ) ++ presetAnno ++ annos
    )
    assert(res.collectFirst { case _: OutputFileAnnotation => true }.isDefined)
    val r = Z3ModelChecker.bmc(testDir, name, kmax)
    assert(r == expected, clue + "\n" + s"$testDir")
  }

  val UndefinedMemAnnos = Seq(RunFirrtlTransformAnnotation(Dependency(UndefinedMemoryBehaviorPass)))
  val InvalidToRandomAnnos = Seq(RunFirrtlTransformAnnotation(Dependency(InvalidToRandomPass)))
}

/** Minimal implementation of a Z3 based bounded model checker.
  * A more complete version of this with better use feedback should eventually be provided by a
  * chisel3 formal verification library. Do not use this implementation outside of the firrtl test suite!
  */
private object Z3ModelChecker extends LazyLogging {
  def getZ3Version: String = {
    val (out, ret) = executeCmd("-version")
    assert(ret == 0, "failed to call z3")
    assert(out.startsWith("Z3 version"), s"$out does not start with 'Z3 version'")
    val version = out.split(" ")(2)
    version
  }

  def bmc(testDir: Path, main: String, kmax: Int): MCResult = {
    assert(kmax >= 0 && kmax < 50, "Trying to keep kmax in a reasonable range.")
    (0 to kmax).foreach { k =>
      val stepFile = testDir / s"${main}_step$k.smt2"
      os.copy(testDir / s"$main.smt2", stepFile)
      os.write.append(stepFile, s"""${step(main, k)}
                                   |(check-sat)
                                   |""".stripMargin)
      val success = executeStep(stepFile)
      if (!success) return MCFail(k)
    }
    MCSuccess
  }

  private def executeStep(path: Path): Boolean = {
    val (out, ret) = executeCmd(path.toString)
    assert(ret == 0, s"expected success (0), not $ret: `$out`\nz3 ${path.toString}")
    assert(
      out == "sat" + Properties.lineSeparator || out == "unsat" + Properties.lineSeparator,
      s"Unexpected output: $out"
    )
    out == "unsat" + Properties.lineSeparator
  }

  private def executeCmd(cmd: String): (String, Int) = {
    val process = os.proc("z3", cmd).call(stderr = ProcessOutput.Readlines(logger.warn(_)))
    (process.out.chunks.mkString, process.exitCode)
  }

  private def step(main: String, k: Int): String = {
    // define all states
    (0 to k).map(ii => s"(declare-fun s$ii () $main$StateTpe)") ++
      // assert that init holds in state 0
      List(s"(assert ($main$Init s0))") ++
      // assert transition relation
      (0 until k).map(ii => s"(assert ($main$Transition s$ii s${ii + 1}))") ++
      // assert that assumptions hold in all states
      (0 to k).map(ii => s"(assert ($main$Assumes s$ii))") ++
      // assert that assertions hold for all but last state
      (0 until k).map(ii => s"(assert ($main$Asserts s$ii))") ++
      // check to see if we can violate the assertions in the last state
      List(s"(assert (not ($main$Asserts s$k)))")
  }.mkString("\n")

  // the following suffixes have to match the ones in [[SMTTransitionSystemEncoder]]
  private val Transition = "_t"
  private val Init = "_i"
  private val Asserts = "_a"
  private val Assumes = "_u"
  private val StateTpe = "_s"
}

private sealed trait MCResult
private case object MCSuccess extends MCResult
private case class MCFail(k: Int) extends MCResult
