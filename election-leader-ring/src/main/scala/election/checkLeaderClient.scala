package election

import com.twitter.util.Await

object checkLeaderClient {

  def checkIfLeader(portEnding : Int, id: Int, numPorts: Int): Unit = {
    var unsureIfLeader = true
    var phase = 0

    while (unsureIfLeader) {
      phase += 1
      println(s"phase ${phase}" )
      val isIncoming = false

      val leftResponse = makeRequest.checkLeader(id, phase, isIncoming, "left", numPorts, phase, portEnding)
      val rightResponse = makeRequest.checkLeader(id, phase, isIncoming, "right", numPorts, phase, portEnding)

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
