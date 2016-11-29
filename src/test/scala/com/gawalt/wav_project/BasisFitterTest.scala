package com.gawalt.wav_project

import org.scalatest._
import akka.testkit.TestActorRef
import akka.actor.{ActorSystem, Actor}
import scala.collection.mutable.ArrayBuffer

/**
 * This source file created by Brian Gawalt, 11/19/16.
 * It is subject to the MIT license bundled with this package in the file LICENSE.txt.
 * Copyright (c) Brian Gawalt, 2016
 */

class DummyBoss extends Actor {
  private val receivedMsgs = new ArrayBuffer[FitBasisMsg]
  def getReceivedMsgs = receivedMsgs.toList
  def receive = {
    case fbm: FitBasisMsg => receivedMsgs.append(fbm)
    case _ => throw new IllegalArgumentException("Unexpected message type!")
  }
}

case object BogusMsg

class BasisFitterTest extends FunSuite with Matchers {

  implicit val system = ActorSystem("test-basis-fitter")
  val basis = Vector(1.0, 2.0, 3.0, 4.0)

  test("Inital Values") {
    val bossRef = TestActorRef[DummyBoss]
    val bfRef = TestActorRef(new BasisFitter(1, bossRef, basis))
    val bf = bfRef.underlyingActor
    bf.varianceReduction.isNaN should be (true)
    bf.scaleFactor should be (0.0)
    bf.basisSumSquared should be (30.0)
  }

  test("Calculate Residuals") {
    val bossRef = TestActorRef[DummyBoss]
    val bfRef = TestActorRef(new BasisFitter(1, bossRef, basis))
    val bf = bfRef.underlyingActor
    bfRef ! ResidualSnippetMsg(List(2.0, 4.0, 6.0, 8.0))
    bf.scaleFactor should be (2.0 +- 1e-6)
    bf.varianceReduction should be (120.0 +- 1e-6)

    bfRef ! ResidualSnippetMsg(List(-1.5, -3.0, -4.5, -6.0))
    bf.scaleFactor should be (-1.5 +- 1e-6)
    bf.varianceReduction should be (67.5 +- 1e-6)


    bfRef ! ResidualSnippetMsg(List(1.0, 1.0, 1.0, 1.0))
    bf.scaleFactor should be (0.33333333333 +- 1e-6)
    bf.varianceReduction should be (3.3333333 +- 1e-6)

    // Check out repeated residual reduction.
    val original = List(3.0, 4.0, 9.0, 2.0)
    bfRef ! ResidualSnippetMsg(original)
    bf.scaleFactor should be (1.5333333333333334 +- 1e-6)
    val updated = original.zip(basis).map({case (oi, bi) => oi - 1.5333333333333334*bi})
    bfRef ! ResidualSnippetMsg(updated)
    println(updated.mkString("<", ",", ">"))
    bf.scaleFactor should be (0.0 +- 1e-6)
  }

  test("Reply To Boss") {
    val bossRef = TestActorRef[DummyBoss]
    val boss = bossRef.underlyingActor
    val bfRef = TestActorRef(new BasisFitter(9, bossRef, basis))

    bfRef ! ResidualSnippetMsg(List(2.0, 4.0, 6.0, 8.0))
    bfRef ! BasisFitRequest
    var msgs = boss.getReceivedMsgs
    msgs.length should be (1)
    msgs(0).id should be (9)
    msgs(0).varianceReduction should be (120.0 +- 1e-6)
    msgs(0).scale should be (2.0 +- 1e-6)

    bfRef ! ResidualSnippetMsg(List(1.0, 1.0, 1.0, 1.0))
    bfRef ! BasisFitRequest
    msgs = boss.getReceivedMsgs
    msgs.length should be (2)
    msgs(0).id should be (9)
    msgs(0).varianceReduction should be (120.0 +- 1e-6)
    msgs(0).scale should be (2.0 +- 1e-6)
    msgs(1).id should be (9)
    msgs(1).varianceReduction should be (3.33333333 +- 1e-6)
    msgs(1).scale should be (0.333333333 +- 1e-6)
  }

  test("Bogus message") {
    val bossRef = TestActorRef[DummyBoss]
    val bfRef = TestActorRef(new BasisFitter(9, bossRef, basis))
    val exception = intercept[Exception] {
      bfRef.receive(BogusMsg)
    }
    exception shouldBe a [IllegalArgumentException]
    exception.getMessage should be ("Unrecognized message of type com.gawalt.wav_project.BogusMsg$")
  }

  test("Incorrect snippet length") {
    val bossRef = TestActorRef[DummyBoss]
    val bfRef = TestActorRef(new BasisFitter(9, bossRef, basis))
    val tooLongException = intercept[Exception] {
      bfRef.receive(ResidualSnippetMsg(List(1.0, 2.0, 3.0, 4.0, 5.0)))
    }
    tooLongException shouldBe a [IllegalArgumentException]
    tooLongException.getMessage should be (
      "requirement failed: residual length (5) does not match basis length (4)")

    val tooShortException = intercept[Exception] {
      bfRef.receive(ResidualSnippetMsg(List(1.0, 2.0, 3.0)))
    }
    tooShortException shouldBe a [IllegalArgumentException]
    tooShortException.getMessage should be (
      "requirement failed: residual length (3) does not match basis length (4)")
  }

}
