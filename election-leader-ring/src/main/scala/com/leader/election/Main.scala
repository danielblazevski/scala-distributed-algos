package com.leader.election

import com.twitter.finagle.Http
import com.twitter.util.Await

object Main  {

  def main(args: Array[String]) {
    // TO-DO: make a config to pass in numPorts, or use command line args
    // and add heartbeat/timeout to avoid Thread.sleep call

    val portEnding = args(0)
    val id = args(1).toInt
    val numPorts = args(2).toInt
    println(s"starting server with id = ${id} on port = ${portEnding}")

    // setup server to process requests
    val leaderClient = new LeaderClient()
    val isLeaderService = new IsLeader(id, portEnding.toInt, leaderClient)
    val server = Http.serve(s":80${portEnding}", isLeaderService)

    Thread.sleep(20000)
    // separate thread that checks if the leader
    val checkLeaderHandler = new CheckLeaderHandler(leaderClient)
    checkLeaderHandler.checkIfLeader(portEnding.toInt, id, numPorts)

    Await.ready(server)
  }
}
