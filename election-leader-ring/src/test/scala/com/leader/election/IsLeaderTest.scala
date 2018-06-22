package com.leader.election

import com.twitter.finagle.http
import com.twitter.finagle.http.RequestBuilder
import com.twitter.util.Future
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito.{times, verify, when}
import org.mockito.Matchers.{anyBoolean, anyInt, anyString}
import org.scalatest.{FlatSpec, Matchers}


class IsLeaderTest extends FlatSpec with Matchers with MockitoSugar {

  "isLeader.outgoingHandler" should "properly handle requests about whether a node is a leader" in {
    val mockedClient = mock[LeaderClient]
    val response = Future.value(http.Response(http.Status.Ok))
    val id = 12
    val portEnding = 2
    val iterRemaining = 2
    val fromId = 13
    val isIncoming = false
    val direction = "left"
    val numPorts = 3

    val params = Map(
      "fromId" -> fromId.toString,
      "iterRemaining" -> iterRemaining.toString,
      "isIncoming" -> isIncoming.toString,
      "direction" -> direction,
      "numPorts" -> numPorts.toString
    )

    val request : http.Request = RequestBuilder()
      .url(http.Request.queryString(s"http://localhost:/8002", params))
      .buildGet()

    when(mockedClient.checkLeaderOutgoing(anyInt(),
      anyInt(),
      anyBoolean(),
      anyString(),
      anyInt(),
      anyInt())).thenReturn(response)

    val isLeader = new IsLeader(id, portEnding, mockedClient)
    val actual = isLeader.outgoingHandler(request)

    verify(mockedClient, times(1)).checkLeaderOutgoing(fromId,
      iterRemaining - 1,
      isIncoming,
      direction,
      numPorts,
      portEnding)
  }


  "isLeader.outgoingHandler" should "switch incoming vs outgoing" in {
    val mockedClient = mock[LeaderClient]
    val response = Future.value(http.Response(http.Status.Ok))
    val id = 12
    val portEnding = 2
    val iterRemaining = 1
    val fromId = 13
    val isIncoming = false
    val direction = "left"
    val numPorts = 3

    val params = Map(
      "fromId" -> fromId.toString,
      "iterRemaining" -> iterRemaining.toString,
      "isIncoming" -> isIncoming.toString,
      "direction" -> direction,
      "numPorts" -> numPorts.toString
    )

    val request : http.Request = RequestBuilder()
      .url(http.Request.queryString(s"http://localhost:/8002", params))
      .buildGet()

    when(mockedClient.passTokenInward(anyInt(),
      anyString(),
      anyInt(),
      anyInt())).thenReturn(response)

    val isLeader = new IsLeader(id, portEnding, mockedClient)
    val actual = isLeader.outgoingHandler(request)

    verify(mockedClient, times(1)).passTokenInward(fromId,
      "right",
      numPorts,
      portEnding)
  }

}
