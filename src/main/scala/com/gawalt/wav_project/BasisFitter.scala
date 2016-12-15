package com.gawalt.wav_project

import akka.actor.{ActorRef, Actor}

case class FitBasisMsg(id: Int, varianceReduction: Double, scale: Double)

/**
 * This source file created by Brian Gawalt, 11/2/16.
 * It is subject to the MIT license bundled with this package in the file LICENSE.txt.
 * Copyright (c) Brian Gawalt, 2016
 */
class BasisFitter(val id: Int,
                  val boss: ActorRef,
                  val basis: Vector[Double]) extends Actor {

  val n = basis.length
  // We'll overwrite this as messages arrive
  val residual: Array[Double] = Array.fill[Double](n)(0.0)

  var scaleFactor: Double = 0.0
  var varianceReduction: Double = Double.NaN
  var fitYet = false


  val basisSumSquared: Double = basis.map(bi => bi*bi).sum

  def receive = {
    case ResidualSnippetMsg(newResid) =>
      require(newResid.length == n,
        s"residual length (${newResid.length}) does not match basis length (${basis.length})")
      var i = 0
      while (i < n) {
        residual(i) = newResid(i)
        i += 1
      }
      fitYet = false

    case BasisFitRequest =>
      if (!fitYet) {
        var dot = 0.0
        var i = 0
        while (i  < n) {
          dot += residual(i)*basis(i)
          i += 1
        }
        scaleFactor = dot/basisSumSquared
        varianceReduction = dot*dot/basisSumSquared
        fitYet = true
      }
      boss ! FitBasisMsg(id = id,
                         varianceReduction = varianceReduction,
                         scale = scaleFactor)

    case e: Any =>
      throw new IllegalArgumentException(s"Unrecognized message of type ${e.getClass.getName}")

  }
}

