package election

import java.io.{File, PrintWriter}

import com.twitter.finagle.{Http, Service, http}
import com.twitter.util.{Await, Future}

class isLeader(id: Int, portEnding: Int) extends Service[http.Request, http.Response] {

  private def getFormattedPort(port: Int,
                               direction: String,
                               numPorts: Int) = {

    val portDirection = direction match {
      case "left" => Math.floorMod(portEnding - 1, numPorts)
      case "right" => Math.floorMod(portEnding + 1, numPorts)
    }
    "%02d".format(portDirection)
  }

  private def makeClientRequest(id: Int,
                                iterRemaining: Int,
                                isIncoming: Boolean,
                                direction: String,
                                numPorts: Int,
                                phase: Int,
                                port: String): Future[http.Response] = {

    val client: Service[http.Request, http.Response] = Http.newService(s"localhost:80${port}")
    val request = http.Request(http.Method.Post,
      s"/?fromId=${id}&iterRemaining=${iterRemaining}&isIncoming=${isIncoming}&direction=${direction}&numPorts=${numPorts}&phase=${phase}")
    println(s"posting /?fromId=${id}&iterRemaining=${iterRemaining}&isIncoming=${isIncoming}&direction=${direction}&numPorts=${numPorts}&phase=${phase}" +
      s"to port 80${port}")
    client(request)
  }

  def outgoingHandler(request: http.Request): Future[http.Response] = {
    Future {
      val fromId = request.getIntParam("fromId")
      var iterRemaining = request.getIntParam("iterRemaining")
      var isIncoming = request.getBooleanParam("isIncoming")
      var direction = request.getParam("direction")
      var numPorts = request.getIntParam("numPorts")
      val phase = request.getIntParam("phase")

      println(s"fromId = ${fromId.toString}")
      println(s"direction = ${direction}")
      println(s"iterRemaining = ${iterRemaining.toString}")
      println(s"phase = ${phase}")
      println("outgoing")

      id - fromId match {
        case x if x < 0 => {
          iterRemaining = iterRemaining - 1
          if (iterRemaining == 0) {
            isIncoming = true
            direction = direction match {
              case "left" => "right"
              case "right" => "left"
            }
          }
          val formattedPort = getFormattedPort(portEnding, direction, numPorts)
          Await.result(makeClientRequest(fromId, iterRemaining, isIncoming, direction, numPorts, phase, formattedPort))
        }
        case x if x > 0 => {
          println("not the leader!")
          val response = http.Response(request.version, http.Status.Ok)
          response
        }
        case 0 => {
          println("leader found!")
          val pw = new PrintWriter(new File(s"data/leader${fromId}")); pw.write(s"leader id = ${id}"); pw.close()
          println("want to send OK status!")
          val response = http.Response(request.version, http.Status.Ok)
          response
        }
      }
    }
  }

  def incomingHandler(request: http.Request): Future[http.Response] = {
    Future {
      val fromId = request.getIntParam("fromId")
      var isIncoming = request.getBooleanParam("isIncoming")
      var direction = request.getParam("direction")
      var numPorts = request.getIntParam("numPorts")
      val phase = request.getIntParam("phase")
      val iterRemaining = request.getIntParam("iterRemaining")

      println(s"fromId = ${fromId.toString}")
      println(s"direction = ${direction}")
      println(s"phase = ${phase}")
      println("incoming")

      if (fromId == id) {
        val response = http.Response(request.version, http.Status.Ok)
        response.setContentString(s"${false.toString}\n")
        println(s"finished with phase = ${phase}")
        val pw = new PrintWriter(new File(s"data/phase_success_${fromId}_${direction}_${phase}")); pw.write(s"id = ${id}"); pw.close()
        response
      } else {
        val formattedPort = getFormattedPort(portEnding, direction, numPorts)
        Await.result(makeClientRequest(fromId, iterRemaining,isIncoming, direction, numPorts, phase, formattedPort))
      }
    }
  }

  override def apply(request: http.Request): Future[http.Response] = {
    val params = request.getParams()
    println(params)
    val isIncoming = request.getBooleanParam("isIncoming")

    if (isIncoming) {
      incomingHandler(request)
    } else {
      outgoingHandler(request)
    }
  }
}
