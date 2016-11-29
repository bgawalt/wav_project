package com.gawalt.wav_project

import akka.actor.{ActorSystem, Props}

/**
 * This source file created by Brian Gawalt, 11/27/16.
 * It is subject to the MIT license bundled with this package in the file LICENSE.txt.
 * Copyright (c) Brian Gawalt, 2016
 */
object WavProject {

  def main(args: Array[String]) {
    val target_filename = args(0)
    val basis_filename = args(1)
    val outfile_base = args(2)

    val target = WavUtil.readFile(target_filename)
    val basis = WavUtil.readFile(basis_filename)

    println("TARGET STATS")
    WavUtil.printStats(target)

    println("BASIS STATS")
    WavUtil.printStats(basis)

    implicit val system = ActorSystem("wav-project")

    val publisher = system.actorOf(Props(new Publisher(outfile_base)), "publisher")
    val conductor = system.actorOf(Props(
      new Conductor(target = target,
        basis = basis,
        publisher = publisher,
        checkpointBase = 1
      )), "conductor")
    conductor ! StartMsg
  }

}