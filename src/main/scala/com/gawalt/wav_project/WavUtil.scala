package com.gawalt.wav_project

import com.labbooks.WavFile
import java.io.File
import scala.collection.mutable.ArrayBuffer

/**
 * This source file created by Brian Gawalt, 11/13/16.
 * It is subject to the MIT license bundled with this package in the file LICENSE.txt.
 * Copyright (c) Brian Gawalt, 2016
 */
object WavUtil {

  val BUFFER_SIZE = 1000
  val SAMPLE_RATE = 44100
  val BITS_PER_SAMPLE = 16

  def printStats(wav: Vector[Double]) {
    val avg = wav.sum/wav.length
    val std = math.sqrt(
      wav.map(wi => (wi - avg)*(wi - avg)).sum
    )/(wav.length - 1)
    println(s"Length: ${wav.length}")
    println(s"Max: ${wav.max}")
    println(s"Min: ${wav.min}")
    println(s"Avg: $avg")
    println(s"Std: $std")
  }

  def readFile(filename: String): Vector[Double] = {
    val wavFile = WavFile.openWavFile(new File(filename))
    val numChannels = wavFile.getNumChannels
    val output = ArrayBuffer.empty[Double]

    val buffer = new Array[Double](BUFFER_SIZE * numChannels)
    var numFramesRead = 0
    do {
      numFramesRead = wavFile.readFrames(buffer, BUFFER_SIZE)
      val channelAverages = for(
        frameNum <- 0 until numFramesRead;
        channels = buffer.slice(numChannels * frameNum, numChannels * (frameNum + 1) )
      ) yield {
        channels.map(_ / numChannels).sum
      }
      output.appendAll(channelAverages)
    } while (numFramesRead > 0)

    wavFile.close()
    output.toVector
  }

  def writeFile(wav: Vector[Double], filename: String) {
    val wavFile = WavFile.newWavFile(
      new File(filename), 1, wav.length, BITS_PER_SAMPLE, SAMPLE_RATE)
    val numFrames = wav.length

    var frameCounter: Int = 0
    while (frameCounter < numFrames) {
      val remaining = wavFile.getFramesRemaining
      val amountToWrite = (if (remaining < BUFFER_SIZE) BUFFER_SIZE else remaining).toInt
      val amountWritten = wavFile.writeFrames(
        wav.slice(frameCounter, frameCounter + amountToWrite).toArray,
        amountToWrite)
      frameCounter += amountWritten
    }
    wavFile.close()
  }

  def clip(wav: Vector[Double], ratio: Double = 0.99): Vector[Double] = {
    var left = 0
    var right = wav.length - 1
    val squared = wav.map(wi => wi*wi)
    val clipped = 1 - ratio
    val thresh = squared.sum*clipped

    var accum = 0.0
    while (accum < thresh && left < right) {
      if (squared(left) < squared(right)) {
        left += 1
        accum += squared(left)
      }
      else {
        right -= 1
        accum += squared(right)
      }
    }
    wav.slice(left, right)
  }

  def main(args: Array[String]) {
    val wav = readFile(args(0))
    for (ratio <- List(0.99, 0.9, 0.7, 0.5)) {
      writeFile(clip(wav, ratio), s"${args(1)}_${(100*ratio).toInt}.wav")
    }
  }
}