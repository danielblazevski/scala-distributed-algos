package com.leader.election

import com.twitter.finagle.http.RequestBuilder
import com.twitter.finagle.{Http, Service, http}
import com.twitter.util.Future

class LeaderClient {
  private def getFormattedNeighborPort(portEnding: Int,
                                       direction: String,
                                       numPorts: Int) = {

    val port = direction match {
      case "left" => Math.floorMod(portEnding - 1, numPorts)
      case "right" => Math.floorMod(portEnding + 1, numPorts)
    }
    "%02d".format(port)
  }

  def processRequest(params : Map[String, String], portNeighbor: String): Future[http.Response] = {

    val client: Service[http.Request, http.Response] = Http.newService(s"localhost:80${portNeighbor}")
    val request : http.Request = RequestBuilder()
      .url(http.Request.queryString(s"http://localhost:/80${portNeighbor}", params))
      .buildGet()
    client(request)
  }

  def checkLeaderOutgoing(id: Int,
                          iterRemaining: Int,
                          isIncoming: Boolean,
                          direction: String,
                          numPorts: Int,
                          portEnding: Int): Future[http.Response] = {

    val portNeighbor = getFormattedNeighborPort(portEnding, direction, numPorts)
    val params = Map(
      "fromId" -> id.toString,
      "iterRemaining" -> iterRemaining.toString,
      "isIncoming" -> isIncoming.toString,
      "direction" -> direction.toString,
      "numPorts" -> numPorts.toString
    )
    processRequest(params, portNeighbor)

  }

  def passTokenInward(id: Int,
                  direction: String,
                  numPorts: Int,
                  portEnding: Int): Future[http.Response] = {

    val portNeighbor = getFormattedNeighborPort(portEnding, direction, numPorts)
    val params = Map(
      "fromId" -> id.toString,
      "direction" -> direction.toString,
      "numPorts" -> numPorts.toString
    )
    processRequest(params, portNeighbor)
  }

  def checkLeader(id: Int,
                  iterRemaining: Int,
                  isIncoming: Boolean,
                  direction: String,
                  numPorts: Int,
                  phase: Int,
                  portEnding: Int): Future[http.Response] = {

    val portNeighbor = getFormattedNeighborPort(portEnding, direction, numPorts)
    val params = Map(
      "fromId" -> id.toString,
      "iterRemaining" -> iterRemaining.toString,
      "isIncoming" -> isIncoming.toString,
      "direction" -> direction.toString,
      "numPorts" -> numPorts.toString,
      "phase" -> phase.toString
    )
    processRequest(params, portNeighbor)
  }

}