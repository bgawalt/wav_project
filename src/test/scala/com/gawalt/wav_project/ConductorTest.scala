package com.gawalt.wav_project

import akka.actor.{ActorSystem, Actor}
import org.scalatest._
import akka.testkit.TestActorRef

/**
 * This source file created by Brian Gawalt, 11/24/16.
 * It is subject to the MIT license bundled with this package in the file LICENSE.txt.
 * Copyright (c) Brian Gawalt, 2015
 */



class ConductorTest extends FunSuite with Matchers {

  implicit val system = ActorSystem("test-conductor")
  val basis  = Vector(1.0, 2.0, 3.0, 4.0)
  val target = Vector(1.0, 2.0, 3.0, 3.0, -2.0, -1.0, 0.0, 6.0, 8.0)

  test("Initial values") {
    val pubRef = TestActorRef[DummyPublisher]
    val conRef = TestActorRef(new Conductor(target = target, basis = basis, publisher = pubRef))
    val con = conRef.underlyingActor
    con.numBases should be (6)
    con.residual should be (Array(1.0, 2.0, 3.0, 3.0, -2.0, -1.0, 0.0, 6.0, 8.0))
    con.approx should be (Array(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0))
    con.residAbsSum should be (26.0)
    con.numUpdates should be (0)
    con.maxReduction should be (Double.NegativeInfinity)
    con.maxWorker should be (-1)
    con.maxScale.isNaN should be (true)
  }

  test("Impacted Workers") {
    val pubRef = TestActorRef[DummyPublisher]
    val conRef = TestActorRef(new Conductor(target = target, basis = basis, publisher = pubRef))
    val con = conRef.underlyingActor
    con.impactedWorkers(0) should be (Seq(0, 1, 2, 3))
    con.impactedWorkers(1) should be (Seq(0, 1, 2, 3, 4))
    con.impactedWorkers(4) should be (Seq(1, 2, 3, 4, 5))
    con.impactedWorkers(7) should be (Seq(4, 5))
    con.impactedWorkers(8) should be (Seq(5))
  }

  test("Checkpointing") {
    val pubRef = TestActorRef[DummyPublisher]
    val pub = pubRef.underlyingActor
    val conRef = TestActorRef(
      new Conductor(target = target, basis = basis, publisher = pubRef, checkpointBase = 1))
    val con = conRef.underlyingActor

    conRef ! StartMsg




  }


}
