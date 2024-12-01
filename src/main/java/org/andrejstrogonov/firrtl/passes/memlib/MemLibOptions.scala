// SPDX-License-Identifier: Apache-2.0

package org.andrejstrogonov.firrtl.passes.memlib

import firrtl.options.{RegisteredLibrary, ShellOption}

class MemLibOptions extends RegisteredLibrary {
  val name: String = "MemLib Options"

  val options: Seq[ShellOption[_]] = Seq(new InferReadWrite, new ReplSeqMem)
    .flatMap(_.options)

}
