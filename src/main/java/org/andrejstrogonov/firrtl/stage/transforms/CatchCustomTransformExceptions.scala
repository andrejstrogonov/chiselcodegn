// SPDX-License-Identifier: Apache-2.0

package org.andrejstrogonov.firrtl.stage.transforms

import firrtl.{CircuitState, CustomTransformException, Transform}

class CatchCustomTransformExceptions(val underlying: Transform) extends Transform with WrappedTransform {

  override def execute(c: CircuitState): CircuitState = try {
    underlying.transform(c)
  } catch {
    case e: Exception if CatchCustomTransformExceptions.isCustomTransform(trueUnderlying) =>
      throw CustomTransformException(e)
  }

}

object CatchCustomTransformExceptions {

  private[firrtl] def isCustomTransform(xform: Transform): Boolean = {
    def getTopPackage(pack: java.lang.Package): java.lang.Package =
      Package.getPackage(pack.getName.split('.').head)
    // We use the top package of the Parser to get the top firrtl package
    Option(xform.getClass.getPackage).forall { p =>
      getTopPackage(p) != firrtl.Parser.getClass.getPackage
    }
  }

  def apply(a: Transform): CatchCustomTransformExceptions = new CatchCustomTransformExceptions(a)

}
