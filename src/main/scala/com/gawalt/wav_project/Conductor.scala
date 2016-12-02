package com.gawalt.wav_project

import akka.actor.{Props, ActorRef, Actor}
import scala.collection.mutable

case object BasisFitRequest
case object StartMsg
case object FinishMsg
case class ResidualSnippetMsg(residual: Array[Double])
case class ResultMsg(approximation: Vector[Double], updateNum: Int, lastUpdate: Boolean)

/**
 * This source file created by Brian Gawalt, 10/27/16.
 * It is subject to the MIT license bundled with this package in the file LICENSE.txt.
 * Copyright (c) Brian Gawalt, 2016
 */

/**
 * Actor that coordinates the fitting of scaled, shifted bases to the target signal.
 * @param target Long signal to be approximated
 * @param basis Short basis whose scaled and shifted versions will approximate target
 * @param publisher Actor to handle the resulting approximation
 * @param maxUpdates If num. of update steps exceeds this value, send results to publisher and quit
 * @param tolerance If absolute error is below this value, send results to publisher and quit
 * @param checkpointBase Initial checkpoint value; send results to publisher each time the number of
 *                       updates reaches a checkpoint, calculate next checkpoint value, and continue
 * @param checkpointMultiplier Amount to multiply the current checkpoint value to calculate the
 *                             next checkpoint.
 */
class Conductor(val target: Vector[Double],
                val basis: Vector[Double],
                val publisher: ActorRef,
                val maxUpdates: Int = -1,
                val tolerance: Double = 1e-6,
                val checkpointBase: Int = -1,
                val checkpointMultiplier: Int = 2) extends Actor {
  val targetLength: Int = target.length
  val basisLength: Int = basis.length

  val residual:  Array[Double] = target.toArray
  val approx: Array[Double] = Array.fill[Double](targetLength)(0.0)
  var residAbsSum: Double = residual.map(_.abs).sum

  val numBases: Int = targetLength - basisLength + 1 // a.k.a, num workers
  // Current scale contribution of basis(i):
  val currentScale: Array[Double] = Array.fill[Double](numBases)(0.0)
  val fitters = (0 until numBases).map(id =>
      context.actorOf(Props(classOf[BasisFitter], id, self, basis)))

  // How many coordinate descent steps have we taken?
  var numUpdates: Int = 0
  var checkpoint: Int = checkpointBase

  // Which workers have not yet replied to their BasisFitRequest with a FitBasisMsg?
  val outstandingRequests: mutable.HashSet[Int] = new mutable.HashSet()
  // What's the smallest variance seen in a worker's reply?
  // What was that worker's ID?  What was that worker's associated basis scale?
  var maxReduction: Double = Double.NegativeInfinity
  var maxWorker: Int = -1
  var maxScale: Double = Double.NaN

  def impactedWorkers(idx: Int): Seq[Int] = {
    val naiveMin = idx - basisLength + 1
    val min = if (naiveMin >= 0) naiveMin else 0
    val naiveMax = idx + basisLength - 1
    val max = if (naiveMax < numBases) naiveMax else numBases - 1
    min to max
  }

  def done: Boolean = (numUpdates == maxUpdates) || (residAbsSum/targetLength < tolerance)

  def requestUpdates() {
    require(outstandingRequests.isEmpty,
      "requestUpdates invoked with non-empty outstandingRequests")
    (0 until numBases).foreach(outstandingRequests.add)
    fitters.foreach(_ ! BasisFitRequest)
    maxReduction = Double.NegativeInfinity
    maxWorker = -1
    maxScale = Double.NaN
  }

  def handleFitterReply(id: Int, varReduction: Double, scale: Double) {
    require(outstandingRequests.contains(id),
      s"Received fitter reply from $id, but that id was not in outstandingRequests")
    outstandingRequests.remove(id)
    if (varReduction > maxReduction) {
      maxReduction = varReduction
      maxWorker = id
      maxScale = scale
    }
    if (outstandingRequests.isEmpty) {
      println(
        s"Applying update $numUpdates: worker $maxWorker\treduction $maxReduction\tscale $maxScale")
      applyUpdate(maxWorker, maxScale)
    }
  }

  def applyUpdate(idx: Int, scale: Double) {
    require(!scale.isNaN, "scale must not be NaN")
    for (i <- 0 until basisLength) {
      residAbsSum -= residual(idx + i).abs
      approx(idx + i) += scale*basis(i)
      residual(idx + i) = target(idx + i) - approx(idx + i)
      residAbsSum += residual(idx + i).abs
    }
    currentScale(idx) = scale
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
      for (i <- impactedWorkers(idx)) {
        val slice = residual.slice(i, i + basisLength)
        fitters(i) ! ResidualSnippetMsg(slice)
      }
      // Request basis fits from all workers.
      requestUpdates()
    }
  }

  def receive = {
    case StartMsg =>
      for (i <- 0 until numBases) {
        val slice = residual.slice(i, i + basisLength)
        fitters(i) ! ResidualSnippetMsg(slice)
      }
      requestUpdates()
    case FitBasisMsg(id, varReduced, scale) =>
      handleFitterReply(id = id, varReduction = varReduced, scale = scale)
    case FinishMsg =>
      publisher ! ResultMsg(approx.toVector, numUpdates, lastUpdate = true)
  }
}


