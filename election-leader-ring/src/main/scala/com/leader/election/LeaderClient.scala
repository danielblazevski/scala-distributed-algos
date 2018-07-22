package com.leader.election

import com.twitter.finagle.http.RequestBuilder
import com.twitter.finagle.{Http, Service, http}
import com.twitter.util.Future

class LeaderClient {

  def checkLeaderOutgoing(id: Int,
                          iterRemaining: Int,
                          direction: String,
                          numPorts: Int,
                          portEnding: Int): Future[http.Response] = {

    val portNeighbor = getFormattedNeighborPort(portEnding, direction, numPorts)
    val params = Map(
      "fromId" -> id.toString,
      "iterRemaining" -> iterRemaining.toString,
      "direction" -> direction.toString,
      "numPorts" -> numPorts.toString
    )
    val client: Service[http.Request, http.Response] = Http.newService(s"localhost:80${portNeighbor}")
    val request : http.Request = RequestBuilder()
      .url(http.Request.queryString(s"http://localhost:/80${portNeighbor}", params))
      .buildGet()
    client(request)

  }

  private def getFormattedNeighborPort(portEnding: Int,
                                       direction: String,
                                       numPorts: Int) = {

    val port = direction match {
      case "left" => Math.floorMod(portEnding - 1, numPorts)
      case "right" => Math.floorMod(portEnding + 1, numPorts)
    }
    "%02d".format(port)
  }

}