// SPDX-License-Identifier: Apache-2.0

package org.andrejstrogonov.firrtl.passes.memlib

sealed abstract class MemPort(val name: String) { override def toString = name }

case object ReadPort extends MemPort("read")
case object WritePort extends MemPort("write")
case object MaskedWritePort extends MemPort("mwrite")
case object ReadWritePort extends MemPort("rw")
case object MaskedReadWritePort extends MemPort("mrw")

object MemPort {

  // This is the order that ports will render in MemConf.portsStr
  val ordered: Seq[MemPort] = Seq(
    MaskedReadWritePort,
    MaskedWritePort,
    ReadWritePort,
    WritePort,
    ReadPort
  )

  val all: Set[MemPort] = ordered.toSet
  // uses orderedPorts when sorting MemPorts
  implicit def ordering: Ordering[MemPort] = {
    val orderedPorts = ordered.zipWithIndex.toMap
    Ordering.by(e => orderedPorts(e))
  }

  def apply(s: String): Option[MemPort] = MemPort.all.find(_.name == s)

  def fromString(s: String): Map[MemPort, Int] = {
    s.split(",")
      .toSeq
      .map(MemPort.apply)
      .map(_ match {
        case Some(x) => x
        case _       => throw new Exception(s"Error parsing MemPort string : ${s}")
      })
      .groupBy(identity)
      .mapValues(_.size)
      .toMap
  }
}

case class MemConf(
  name:            String,
  depth:           BigInt,
  width:           Int,
  ports:           Map[MemPort, Int],
  maskGranularity: Option[Int]) {

  private def portsStr =
    ports.toSeq.sortBy(_._1).map { case (port, num) => Seq.fill(num)(port.name).mkString(",") }.mkString(",")
  private def maskGranStr = maskGranularity.map((p) => s"mask_gran $p").getOrElse("")

  // Assert that all of the entries in the port map are greater than zero to make it easier to compare two of these case classes
  // (otherwise an entry of XYZPort -> 0 would not be equivalent to another with no XYZPort despite being semantically the same)
  ports.foreach { case (k, v) => require(v > 0, "Cannot have negative or zero entry in the port map") }

  override def toString = s"name ${name} depth ${depth} width ${width} ports ${portsStr} ${maskGranStr} \n"
}

object MemConf {

  val regex = raw"\s*name\s+(\w+)\s+depth\s+(\d+)\s+width\s+(\d+)\s+ports\s+([^\s]+)\s+(?:mask_gran\s+(\d+))?\s*".r

  def fromString(s: String): Seq[MemConf] = {
    s.split("\n")
      .toSeq
      .map(_ match {
        case MemConf.regex(name, depth, width, ports, maskGran) =>
          Some(MemConf(name, BigInt(depth), width.toInt, MemPort.fromString(ports), Option(maskGran).map(_.toInt)))
        case "" => None
        case _  => throw new Exception(s"Error parsing MemConf string : ${s}")
      })
      .flatten
  }

  def apply(
    name:            String,
    depth:           BigInt,
    width:           Int,
    readPorts:       Int,
    writePorts:      Int,
    readWritePorts:  Int,
    maskGranularity: Option[Int]
  ): MemConf = {
    val ports: Seq[(MemPort, Int)] = (if (maskGranularity.isEmpty) {
                                        (if (writePorts == 0) Seq() else Seq(WritePort -> writePorts)) ++
                                          (if (readWritePorts == 0) Seq() else Seq(ReadWritePort -> readWritePorts))
                                      } else {
                                        (if (writePorts == 0) Seq() else Seq(MaskedWritePort -> writePorts)) ++
                                          (if (readWritePorts == 0) Seq()
                                           else Seq(MaskedReadWritePort -> readWritePorts))
                                      }) ++ (if (readPorts == 0) Seq() else Seq(ReadPort -> readPorts))
    new MemConf(name, depth, width, ports.toMap, maskGranularity)
  }
}
