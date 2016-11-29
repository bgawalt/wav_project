package com.gawalt.wav_project

import javax.sound.sampled._
import java.io._

/**
 * This source file created by Brian Gawalt, 10/25/16.
 * It is subject to the MIT license bundled with this package in the file LICENSE.txt.
 * Copyright (c) Brian Gawalt, 2016
 */
object WavFunGenerateSine {

  def main(args: Array[String]) {
    println("Hi WavFun")
    val pcm_data = new Array[Byte](44100*2)
    val L1 = 44100.0/240.0
    val L2 = 44100.0/245.0
    for (i <- 0 until pcm_data.length) {
      pcm_data(i) = (55*Math.sin((i/L1)*Math.PI*2)).toByte
      pcm_data(i) = (pcm_data(i) + 55*Math.sin((i/L2)*Math.PI*2)).toByte
    }

    val frmt = new AudioFormat(44100, 8, 1, true, true)
    val ais = new AudioInputStream(
      new ByteArrayInputStream(pcm_data), frmt,
      pcm_data.length / frmt.getFrameSize
    )

    println("TRYING")
    for (k <- AudioSystem.getAudioFileTypes) {
      println(k)
    }
    println("DONE TRUING")

    try {
      AudioSystem.write(ais, AudioFileFormat.Type.AIFF, new
          File("/tmp/test.aiff")
      )
    }
    catch {
      case e: Exception => e.printStackTrace()
    }
  }

}
