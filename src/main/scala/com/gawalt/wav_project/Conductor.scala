package com.gawalt.wav_project

import akka.actor.{Props, ActorRef, Actor}
import scala.collection.mutable
import scala.util.Random

case object ResidualUpdated
case object BasisFitRequest
case object StartMsg
case object FinishMsg
case class ResultMsg(approximation: Vector[Double], updateNum: Int, lastUpdate: Boolean)

/**
 * This source file created by Brian Gawalt, 10/27/16.
 * It is subject to the MIT license bundled with this package in the file LICENSE.txt.
 * Copyright (c) Brian Gawalt, 2016
 */

/**
 * Actor that coordinates the fitting of scaled, shifted bases to the target signal.
 * @param target Long signal to be approximated
 * @param bases Short basis functions whose scaled and shifted versions will approximate target
 * @param publisher Actor to handle the resulting approximation
 * @param maxUpdates If num. of update steps exceeds this value, send results to publisher and quit
 * @param tolerance If absolute error is below this value, send results to publisher and quit
 * @param checkpointBase Initial checkpoint value; send results to publisher each time the number of
 *                       updates reaches a checkpoint, calculate next checkpoint value, and continue
 * @param checkpointMultiplier Amount to multiply the current checkpoint value to calculate the
 *                             next checkpoint.
 */
class Conductor(val target: Vector[Double],
                val bases: Vector[Vector[Double]],
                val publisher: ActorRef,
                val maxUpdates: Int = -1,
                val tolerance: Double = 1e-6,
                val checkpointBase: Int = -1,
                val checkpointMultiplier: Int = 2,
                val numFittersToPoll: Option[Int] = None,
                val seed: Int = 0) extends Actor {
  val targetLength: Int = target.length

  val pollAllFitters = numFittersToPoll.isEmpty
  val rng = new Random(seed)

  val approx: Array[Double] = Array.fill[Double](targetLength)(0.0)
  var residAbsSum: Double = Double.NaN

  val fitters = (0 until bases.length).flatMap(basisId => {
    val basis = bases(basisId)
    (0 until targetLength - basis.length + 1).map(pos =>
      context.actorOf(Props(classOf[BasisFitter], basisId, pos, self, basis)))
  })
  val numFitters = fitters.length

  // How many coordinate descent steps have we taken?
  var numUpdates: Int = 0
  var checkpoint: Int = checkpointBase

  // Which workers have not yet replied to their BasisFitRequest with a FitBasisMsg?
  //val outstandingRequests: mutable.HashSet[(Int, Int)] = new mutable.HashSet()
  var numOutstandingRequests = 0
  // What's the smallest variance seen in a worker's reply?
  // What was that worker's ID?  What was that worker's associated basis scale?
  var maxReduction: Double = Double.NegativeInfinity
  var maxBasis: Int = -1
  var maxPos: Int = -1
  var maxScale: Double = Double.NaN

  def done: Boolean = (numUpdates == maxUpdates) || (residAbsSum/targetLength < tolerance)

  def requestUpdates() {
    require(numOutstandingRequests == 0,
      "requestUpdates invoked with non-empty outstandingRequests")
    val fittersToPoll = if (pollAllFitters) {
      0 until numFitters
    } else {
      (0 until numFittersToPoll.get).map(_ => rng.nextInt(numFitters)).distinct
    }
    fittersToPoll.foreach(i => {numOutstandingRequests += 1; fitters(i) ! BasisFitRequest})
    maxReduction = Double.NegativeInfinity
    maxBasis = -1
    maxPos = -1
    maxScale = Double.NaN
  }

  def handleFitterReply(basisId: Int, pos: Int, varReduction: Double, scale: Double) {
    require(numOutstandingRequests > 0,
      s"Received fitter reply from $basisId, $pos, but " +
        s"numOutstandingRequest = $numOutstandingRequests")
    numOutstandingRequests -= 1
    if (varReduction > maxReduction) {
      maxReduction = varReduction
      maxBasis = basisId
      maxPos = pos
      maxScale = scale
    }
    if (numOutstandingRequests == 0) {
      applyUpdate(basisId = maxBasis, pos = maxPos, scale = maxScale)
    }
  }

  def applyUpdate(basisId: Int, pos: Int, scale: Double) {
    require(!scale.isNaN, "scale must not be NaN")
    val basis = bases(basisId)
    for (i <- 0 until basis.length) {
      residAbsSum -= GlobalResidual.residual(pos + i).abs
      approx(pos + i) += scale*basis(i)
      GlobalResidual.residual(pos + i) = target(pos + i) - approx(pos + i)
      residAbsSum += GlobalResidual.residual(pos + i).abs
    }
    numUpdates += 1
    if (numUpdates == checkpoint) {
      checkpoint *= checkpointMultiplier
      publisher ! ResultMsg(approx.toVector, numUpdates, lastUpdate = false)
    }
    if (done) {
      self ! FinishMsg
    }
    else {
      // Tell each impacted worker about the new residual.
      // TODO: Only alert the relevant subset of fitters.
      fitters.foreach(f => f ! ResidualUpdated)
      // Request basis fits from all workers.
      requestUpdates()
    }
  }

  def receive = {
    case StartMsg =>
      GlobalResidual.residual = Array.fill[Double](target.length)(0.0)
      target.copyToArray(GlobalResidual.residual)
      residAbsSum = GlobalResidual.residual.map(_.abs).sum
      fitters.foreach(f => f ! ResidualUpdated)
      requestUpdates()
    case FitBasisMsg(basisId, pos, varReduced, scale) =>
      handleFitterReply(basisId = basisId, pos = pos, varReduction = varReduced, scale = scale)
    case FinishMsg =>
      publisher ! ResultMsg(approx.toVector, numUpdates, lastUpdate = true)
  }
}


