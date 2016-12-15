package com.gawalt.wav_project

import akka.actor.{ActorSystem, Props}
import scala.util.Try

/**
 * This source file created by Brian Gawalt, 11/27/16.
 * It is subject to the MIT license bundled with this package in the file LICENSE.txt.
 * Copyright (c) Brian Gawalt, 2016
 */

/**
 * args(0): Target audio (long) WAV filename
 * args(1): Basis audio (short) WAV filename
 * args(2): Base name for output WAV files
 *
 * Produces files named `basename_1.wav`, `basename_1.wav`, `basename_2.wav`, `basename_4.wav`, ...
 * until you tell it to stop, or until the target audio's been just about entirely reconstructed.
 */
object WavProject {

  def main(args: Array[String]) {
    val targetFilename = args(0)
    val basisFilename = args(1)
    val outfileBase = args(2)
    val numFitters = Try(args(3).toInt).toOption

    val target = WavUtil.readFile(targetFilename)
    val basis = WavUtil.readFile(basisFilename)

    println("TARGET STATS")
    WavUtil.printStats(target)

    println("BASIS STATS")
    WavUtil.printStats(basis)

    implicit val system = ActorSystem("wav-project")

    val publisher = system.actorOf(Props(new Publisher(outfileBase)), "publisher")
    val conductor = system.actorOf(Props(
      new Conductor(target = target,
        basis = basis,
        publisher = publisher,
        checkpointBase = 1,
        numFittersToPoll = numFitters
      )), "conductor")
    conductor ! StartMsg
  }

}
