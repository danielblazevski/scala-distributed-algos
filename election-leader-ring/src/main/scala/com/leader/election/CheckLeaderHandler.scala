package com.leader.election

import com.twitter.util.Await

class CheckLeaderHandler(leaderClient: LeaderClient) {

  def checkIfLeader(portEnding : Int, id: Int, numPorts: Int): Unit = {
    var unsureIfLeader = true
    var phase = 0

    while (unsureIfLeader) {
      phase += 1
      println(s"phase ${phase}" )

      val leftResponse = leaderClient.checkLeaderOutgoing(id, phase, "left", numPorts, portEnding)
      val rightResponse = leaderClient.checkLeaderOutgoing(id, phase, "right", numPorts, portEnding)

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
