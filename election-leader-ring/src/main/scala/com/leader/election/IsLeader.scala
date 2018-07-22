package com.leader.election

import com.twitter.finagle.{Service, http}
import com.twitter.io.{Buf, Reader}
import com.twitter.util.Future

// TO-DO: add better format of responses instead of simple string "leader", "not leader" (json/proto)
class IsLeader(id: Int, portEnding: Int, leaderClient: LeaderClient) extends Service[http.Request, http.Response] {

  def checkLeaderHandler(request: http.Request): Future[http.Response] = {
      val fromId = request.getIntParam("fromId")
      var iterRemaining = request.getIntParam("iterRemaining")
      val direction = request.getParam("direction")
      val numPorts = request.getIntParam("numPorts")

      id - fromId match {

        case x if x < 0 => {
          iterRemaining -= 1
          if (iterRemaining == 0) {
            Future.value(http.Response(request.version, http.Status.Ok, Reader.fromBuf(Buf.Utf8("advance"))))
          } else {
            leaderClient.checkLeaderOutgoing(fromId, iterRemaining, direction, numPorts, portEnding)
          }
        }

        case x if x > 0 => {
          Future.value(http.Response(request.version, http.Status.Ok, Reader.fromBuf(Buf.Utf8("notLeader"))))
        }

        case 0 => {
          Future.value(http.Response(request.version, http.Status.Ok, Reader.fromBuf(Buf.Utf8("leader"))))
        }
      }
    }

  override def apply(request: http.Request): Future[http.Response] = {
    val params = request.getParams()
    println(params)
    checkLeaderHandler(request)
  }
}
