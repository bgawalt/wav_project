package com.gawalt.wav_project

/**
 * This source file created by Brian Gawalt, 11/27/16.
 * It is subject to the MIT license bundled with this package in the file LICENSE.txt.
 * Copyright (c) Brian Gawalt, 2016
 */

/*
  args(0) Input audo
  args(1) Clip remainder
  args(2) Output audio
 */
object WavTrim {

  def main(args: Array[String]) {
    val wav_filename = args(0)
    val trim_to = args(1).toDouble
    val trimmed_filename = args(2)

    val wav = WavUtil.readFile(wav_filename)
    val trimmed_wav = WavUtil.clip(wav, trim_to)

    println("Wav:")
    WavUtil.printStats(wav)

    println("Trimmed Wav:")
    WavUtil.printStats(trimmed_wav)

    WavUtil.writeFile(trimmed_wav, trimmed_filename)
  }

}
