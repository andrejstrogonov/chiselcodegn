// SPDX-License-Identifier: Apache-2.0

package org.andrejstrogonov.tutorial.lesson1-circuit-traversal

// Compiler Infrastructure
import firrtl.{CircuitState, LowForm, Transform, Utils}
// Firrtl IR classes
import firrtl.ir.{DefModule, Expression, Mux, Statement}
// Map functions
import firrtl.Mappers._
// Scala's mutable collections
import scala.collection.mutable

/** Ledger tracks [[firrtl.ir.Circuit]] statistics
  *
  * In this lesson, we want to count the number of muxes in each module in our design.
  *
  * This [[Ledger]] class will be passed along as we walk our circuit, and help us count each [[firrtl.ir.Mux Mux]] we
  * find.
  *
  * See [[lesson1.AnalyzeCircuit]]
  */
class Ledger {
  private var moduleName: Option[String] = None
  private val modules = mutable.Set[String]()
  private val moduleMuxMap = mutable.Map[String, Int]()
  def foundMux(): Unit = moduleName match {
    case None       => sys.error("Module name not defined in Ledger!")
    case Some(name) => moduleMuxMap(name) = moduleMuxMap.getOrElse(name, 0) + 1
  }
  def getModuleName: String = moduleName match {
    case None       => Utils.error("Module name not defined in Ledger!")
    case Some(name) => name
  }
  def setModuleName(myName: String): Unit = {
    modules += myName
    moduleName = Some(myName)
  }
  def serialize: String = {
    modules.map { myName =>
      s"$myName => ${moduleMuxMap.getOrElse(myName, 0)} muxes!"
    }.mkString("\n")
  }
}

/** AnalyzeCircuit Transform
  *
  * Walks [[firrtl.ir.Circuit Circuit]], and records the number of muxes it finds, per module.
  *
  * While some compiler frameworks operate on graphs, we represent a Firrtl circuit using a tree representation:
  *   - A Firrtl [[firrtl.ir.Circuit Circuit]] contains a sequence of [[firrtl.ir.DefModule DefModule]]s.
  *   - A [[firrtl.ir.DefModule DefModule]] contains a sequence of [[firrtl.ir.Port Port]]s, and maybe a
  *     [[firrtl.ir.Statement Statement]].
  *   - A [[firrtl.ir.Statement Statement]] can contain other [[firrtl.ir.Statement Statement]]s, or
  *     [[firrtl.ir.Expression Expression]]s.
  *   - A [[firrtl.ir.Expression Expression]] can contain other [[firrtl.ir.Expression Expression]]s.
  *
  * To visit all Firrtl IR nodes in a circuit, we write functions that recursively walk down this tree. To record
  * statistics, we will pass along the [[Ledger]] class and use it when we come across a [[firrtl.ir.Mux Mux]].
  *
  * See the following links for more detailed explanations:
  * Firrtl's IR:
  *   - https://github.com/ucb-bar/firrtl/wiki/Understanding-Firrtl-Intermediate-Representation
  * Traversing a circuit:
  *   - https://github.com/ucb-bar/firrtl/wiki/traversing-a-circuit for more
  * Common Pass Idioms:
  *   - https://github.com/ucb-bar/firrtl/wiki/Common-Pass-Idioms
  */
class AnalyzeCircuit extends Transform {

  /** Requires the [[firrtl.ir.Circuit Circuit]] form to be "low" */
  def inputForm = LowForm

  /** Indicates the output [[firrtl.ir.Circuit Circuit]] form to be "low" */
  def outputForm = LowForm

  /** Called by [[firrtl.Compiler Compiler]] to run your pass. [[firrtl.CircuitState CircuitState]] contains the circuit
    * and its form, as well as other related data.
    */
  def execute(state: CircuitState): CircuitState = {
    val ledger = new Ledger()
    val circuit = state.circuit

    /* Execute the function walkModule(ledger) on every [[firrtl.ir.DefModule DefModule]] in circuit, returning a new
     * [[Circuit]] with new [[scala.collection.Seq Seq]] of [[firrtl.ir.DefModule DefModule]].
     *   - "higher order functions" - using a function as an object
     *   - "function currying" - partial argument notation
     *   - "infix notation" - fancy function calling syntax
     *   - "map" - classic functional programming concept
     *   - discard the returned new [[firrtl.ir.Circuit Circuit]] because circuit is unmodified
     */
    circuit.map(walkModule(ledger))

    // Print our ledger
    println(ledger.serialize)

    // Return an unchanged [[firrtl.CircuitState CircuitState]]
    state
  }

  /** Deeply visits every [[firrtl.ir.Statement Statement]] in m. */
  def walkModule(ledger: Ledger)(m: DefModule): DefModule = {
    // Set ledger to current module name
    ledger.setModuleName(m.name)

    /* Execute the function walkStatement(ledger) on every [[firrtl.ir.Statement Statement]] in m.
     *   - return the new [[firrtl.ir.DefModule DefModule]] (in this case, its identical to m)
     *   - if m does not contain [[firrtl.ir.Statement Statement]], map returns m.
     */
    m.map(walkStatement(ledger))
  }

  /** Deeply visits every [[firrtl.ir.Statement Statement]] and [[firrtl.ir.Expression Expression]] in s. */
  def walkStatement(ledger: Ledger)(s: Statement): Statement = {

    /* Execute the function walkExpression(ledger) on every [[firrtl.ir.Expression Expression]] in s.
     *   - discard the new [[firrtl.ir.Statement Statement]] (in this case, its identical to s)
     *   - if s does not contain [[firrtl.ir.Expression Expression]], map returns s.
     */
    s.map(walkExpression(ledger))

    /* Execute the function walkStatement(ledger) on every [[firrtl.ir.Statement Statement]] in s.
     *   - return the new [[firrtl.ir.Statement Statement]] (in this case, its identical to s)
     *   - if s does not contain [[firrtl.ir.Statement Statement]], map returns s.
     */
    s.map(walkStatement(ledger))
  }

  /** Deeply visits every [[firrtl.ir.Expression Expression]] in e.
    *   - "post-order traversal"
    *   - handle e's children [[firrtl.ir.Expression Expression]] before e
    */
  def walkExpression(ledger: Ledger)(e: Expression): Expression = {

    /** Execute the function walkExpression(ledger) on every [[firrtl.ir.Expression Expression]] in e.
      *   - return the new [[firrtl.ir.Expression Expression]] (in this case, its identical to e)
      *   - if s does not contain [[firrtl.ir.Expression Expression]], map returns e.
      */
    val visited = e.map(walkExpression(ledger))

    visited match {
      // If e is a [[firrtl.ir.Mux Mux]], increment our ledger and return e.
      case Mux(cond, tval, fval, tpe) =>
        ledger.foundMux()
        e
      // If e is not a [[firrtl.ir.Mux Mux]], return e.
      case notmux => notmux
    }
  }
}
