package com.gawalt.wav_project

import akka.actor.Actor

/**
 * This source file created by Brian Gawalt, 11/27/16.
 * It is subject to the MIT license bundled with this package in the file LICENSE.txt.
 * Copyright (c) Brian Gawalt, 2016
 */

class Publisher(val filename_base: String) extends Actor {

  def generateFilename(updateNum: Int): String = {
    s"${filename_base}_$updateNum.wav"
  }

  def receive = {
    case ResultMsg(approx, updateNum, lastUpdate) =>
      val filename = generateFilename(updateNum)
      WavUtil.writeFile(approx, filename)
      println(s"Saved Update $updateNum to $filename")
      if (lastUpdate) context.system.shutdown()

    case _ =>
      throw new IllegalArgumentException("Unknown message type!!")
  }

}
