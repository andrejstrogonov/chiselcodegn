// SPDX-License-Identifier: Apache-2.0

package org.andrejstrogonov.firrtl.passes.wiring

import firrtl._
import firrtl.Utils._
import scala.collection.mutable
import firrtl.annotations._
import firrtl.options.Dependency
import firrtl.stage.Forms

/** A class for all exceptions originating from firrtl.passes.wiring */
case class WiringException(msg: String) extends PassException(msg)

/** A component, e.g. register etc. Must be declared only once under the TopAnnotation */
case class SourceAnnotation(target: ComponentName, pin: String) extends SingleTargetAnnotation[ComponentName] {
  def duplicate(n: ComponentName) = this.copy(target = n)
}

/** A module, e.g. ExtModule etc., that should add the input pin */
case class SinkAnnotation(target: Named, pin: String) extends SingleTargetAnnotation[Named] {
  def duplicate(n: Named) = this.copy(target = n)
}

/** Wires a Module's Source Target to one or more Sink
  * Modules/Components
  *
  * Sinks are wired to their closest source through their lowest
  * common ancestor (LCA). Verbosely, this modifies the circuit in
  * the following ways:
  *   - Adds a pin to each sink module
  *   - Punches ports up from source signals to the LCA
  *   - Punches ports down from LCAs to each sink module
  *   - Wires sources up to LCA, sinks down from LCA, and across each LCA
  *
  * @throws WiringException if a sink is equidistant to two sources
  */
class WiringTransform extends Transform with DependencyAPIMigration {

  override def prerequisites = Forms.MidForm
  override def optionalPrerequisites = Seq.empty
  override def optionalPrerequisiteOf = Forms.MidEmitters ++
    // once wire targets are turned into nodes, our logic to wire them up no longer works
    Seq(Dependency[firrtl.transforms.RemoveWires])

  private val invalidates = Forms.VerilogOptimized.toSet -- Forms.MinimalHighForm
  override def invalidates(a: Transform): Boolean = invalidates(Dependency.fromTransform(a))

  /** Defines the sequence of Transform that should be applied */
  private def transforms(w: Seq[WiringInfo]): Seq[Transform] = Seq(
    new Wiring(w)
  )
  def execute(state: CircuitState): CircuitState = {
    val annos = state.annotations.collect {
      case a @ (_: SinkAnnotation | _: SourceAnnotation) => a
    }
    annos match {
      case Seq() => state
      case p =>
        val sinks = mutable.HashMap[String, Seq[Named]]()
        val sources = mutable.HashMap[String, ComponentName]()
        val errors = p.flatMap {
          case SinkAnnotation(m, pin) =>
            sinks(pin) = sinks.getOrElse(pin, Seq.empty) :+ m
            None
          case SourceAnnotation(c, pin) =>
            val res = if (sources.contains(pin)) Some(pin) else None
            sources(pin) = c
            res
        }
        if (errors.nonEmpty) {
          throw WiringException(s"Multiple sources specified for wiring pin(s): " + errors.distinct.mkString(", "))
        }
        (sources.size, sinks.size) match {
          case (0, p) => state
          case (s, p) if (p > 0) =>
            val wis = sources.foldLeft(Seq[WiringInfo]()) {
              case (seq, (pin, source)) =>
                seq :+ WiringInfo(source, sinks(pin), pin)
            }
            val annosx = state.annotations.filterNot(annos.toSet.contains)
            transforms(wis)
              .foldLeft(state) { (in, xform) => xform.runTransform(in) }
              .copy(annotations = annosx)
          case _ => error("Wrong number of sources or sinks!")
        }
    }
  }
}
