package election


import com.twitter.finagle.{Service, http}
import com.twitter.io.{Buf, Reader}
import com.twitter.util.Future

// TO-DO: add better format of responses instead of simple string "leader", "not leader" (json/proto)
class isLeader(id: Int, portEnding: Int) extends Service[http.Request, http.Response] {

  def outgoingHandler(request: http.Request): Future[http.Response] = {
      val fromId = request.getIntParam("fromId")
      var iterRemaining = request.getIntParam("iterRemaining")
      var isIncoming = request.getBooleanParam("isIncoming")
      var direction = request.getParam("direction")
      val numPorts = request.getIntParam("numPorts")

      id - fromId match {

        case x if x < 0 => {
          iterRemaining -= 1
          if (iterRemaining == 0) {
            isIncoming = true
            direction = direction match {
              case "left" => "right"
              case "right" => "left"
            }
            makeRequest.passTokenInward(fromId, direction, numPorts, portEnding)
          } else {
            makeRequest.checkLeaderOutgoing(fromId, iterRemaining, isIncoming, direction, numPorts, portEnding)
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

  def incomingHandler(request: http.Request): Future[http.Response] = {
      val fromId = request.getIntParam("fromId")
      val direction = request.getParam("direction")
      val numPorts = request.getIntParam("numPorts")

      if (fromId == id) {
        Future.value(http.Response(request.version, http.Status.Ok, Reader.fromBuf(Buf.Utf8("advance"))))
      } else {
       makeRequest.passTokenInward(fromId, direction, numPorts, portEnding)
      }
    }

  override def apply(request: http.Request): Future[http.Response] = {
    val params = request.getParams()
    println(params)
    val isIncoming = request.getBooleanParam("isIncoming", true)

    if (isIncoming) {
      incomingHandler(request)
    } else {
      outgoingHandler(request)
    }
  }
}
