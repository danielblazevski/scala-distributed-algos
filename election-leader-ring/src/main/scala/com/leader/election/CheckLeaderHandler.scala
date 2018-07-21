package com.leader.election

import com.twitter.util.Await

class CheckLeaderHandler(leaderClient: LeaderClient) {

  def checkIfLeader(portEnding : Int, id: Int, numPorts: Int): Unit = {
    var unsureIfLeader = true
    var phase = 0

    while (unsureIfLeader) {
      phase += 1
      println(s"phase ${phase}" )

      val leftResponse = leaderClient.checkLeader(id, phase, "left", numPorts, phase, portEnding)
      val rightResponse = leaderClient.checkLeader(id, phase, "right", numPorts, phase, portEnding)

      val bodies = Seq(leftResponse, rightResponse).map{ resp =>
        Await.result(resp.onSuccess{
          case response =>
        }).contentString
      }

      if (bodies.contains("leader")){
        println("you are the leader!")
        unsureIfLeader = false
      }

      if (bodies.contains("notLeader")){
        println("not moving to next phase not the leader!")
        unsureIfLeader = false
      }

    }
  }
}
