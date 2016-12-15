package com.gawalt.wav_project

import akka.actor.{Props, ActorSystem, Actor}
import scala.collection.mutable.ArrayBuffer

/**
 * This source file created by Brian Gawalt, 11/24/16.
 * It is subject to the MIT license bundled with this package in the file LICENSE.txt.
 * Copyright (c) Brian Gawalt, 2016
 */

class DummyPublisher extends Actor {
  private val results: ArrayBuffer[ResultMsg] = ArrayBuffer.empty[ResultMsg]
  def getResults = results.toVector
  def receive = {
    case res: ResultMsg =>
      results.append(res)
      println(s"\nRESULT ${res.updateNum} RECEIVED")
      println(res)
      if (res.lastUpdate) context.system.shutdown()
    case _ => throw new IllegalArgumentException("Unexpected message type!")
  }
}

object DemoWavProject {

  def main(args: Array[String]) {
    val basis  = Vector(1.0, 2.0, 3.0, 4.0)
    val target = Vector(1.0, 2.0, 3.0, 3.0, -2.0, -1.0, 0.0, 6.0, 8.0)

    implicit val system = ActorSystem("demo-wav-project")

    val publisher = system.actorOf(Props[DummyPublisher])
    val conductor = system.actorOf(Props(
      new Conductor(target = target,
                    basis = basis,
                    publisher = publisher,
                    maxUpdates = 20,
                    checkpointBase = 1,
      numFittersToPoll = Some(2)
      )), "conductor")
    conductor ! StartMsg
  }

}
