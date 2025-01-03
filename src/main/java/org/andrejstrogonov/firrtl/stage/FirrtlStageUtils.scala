// SPDX-License-Identifier: Apache-2.0

package org.andrejstrogonov.firrtl.stage

private[stage] sealed trait FileExtension
private[stage] case object FirrtlFile extends FileExtension
private[stage] case object ProtoBufFile extends FileExtension

/** Utilities that help with processing FIRRTL options */
object FirrtlStageUtils {

  private[stage] def getFileExtension(file: String): FileExtension = file.drop(file.lastIndexOf('.')) match {
    case ".pb" => ProtoBufFile
    case _     => FirrtlFile
  }

}
