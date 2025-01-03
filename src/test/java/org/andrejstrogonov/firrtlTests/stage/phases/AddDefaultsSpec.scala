// SPDX-License-Identifier: Apache-2.0

package org.andrejstrogonov.firrtlTests.stage.phases

import firrtl.ChirrtlEmitter
import firrtl.annotations.Annotation
import firrtl.stage.phases.AddDefaults
import firrtl.transforms.BlackBoxTargetDirAnno
import firrtl.stage.{InfoModeAnnotation, RunFirrtlTransformAnnotation}
import firrtl.options.{Dependency, Phase, TargetDirAnnotation}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class AddDefaultsSpec extends AnyFlatSpec with Matchers {

  class Fixture { val phase: Phase = new AddDefaults }

  behavior.of(classOf[AddDefaults].toString)

  it should "add expected default annotations and nothing else" in new Fixture {
    val expected = Seq(
      (a: Annotation) => a match { case BlackBoxTargetDirAnno(b) => b == TargetDirAnnotation().directory },
      (a: Annotation) =>
        a match {
          case RunFirrtlTransformAnnotation(e: firrtl.Emitter) =>
            Dependency.fromTransform(e) == Dependency[firrtl.VerilogEmitter]
        },
      (a: Annotation) => a match { case InfoModeAnnotation(b) => b == InfoModeAnnotation().modeName }
    )

    phase.transform(Seq.empty).zip(expected).map { case (x, f) => f(x) should be(true) }
  }

  it should "not overwrite existing annotations" in new Fixture {
    val input = Seq(
      BlackBoxTargetDirAnno("foo"),
      RunFirrtlTransformAnnotation(new ChirrtlEmitter),
      InfoModeAnnotation("ignore")
    )

    phase.transform(input).toSeq should be(input)
  }
}
