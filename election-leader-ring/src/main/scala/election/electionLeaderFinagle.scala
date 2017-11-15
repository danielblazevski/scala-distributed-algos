package election

import com.twitter.finagle.{Http, Service}
import com.twitter.finagle.http
import com.twitter.util.{Await, Future}

import java.io._

object electionLeaderFinagle  {

  class isLeader(id: Int, portEnding: Int) extends Service[http.Request, http.Response] {

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
            val portDirection = direction match {
              case "left" => Math.floorMod(portEnding - 1, numPorts)
              case "right" => Math.floorMod(portEnding + 1, numPorts)
            }

            val formattedPort = "%02d".format(portDirection)
            println(s"formattedPort = ${formattedPort}")
            println(s"/?fromId=${fromId}&iterRemaining=${iterRemaining}&isIncoming=${isIncoming}&direction=${direction}&numPorts=${numPorts}")
            val client: Service[http.Request, http.Response] = Http.newService(s"localhost:80${formattedPort}")
            val request = http.Request(http.Method.Post,
              s"/?fromId=${fromId}&iterRemaining=${iterRemaining}&isIncoming=${isIncoming}&direction=${direction}&numPorts=${numPorts}&phase=${phase}")
            val response = client(request)
            Await.result(response)
          }
          case x if x > 0 => {
            val response = http.Response(request.version, http.Status.Ok)
            response.setContentString(s"${false.toString}\n")
            println("not the leader!")
            response
          }
          case 0 => {
            println("leader found!")
            val pw = new PrintWriter(new File(s"data/leader${fromId}"))
            pw.write(s"leader id = ${id}")
            pw.close()
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

        println(s"fromId = ${fromId.toString}")
        println(s"direction = ${direction}")
        println(s"phase = ${phase}")
        println("incoming")
        if (fromId == id) {
          val response = http.Response(request.version, http.Status.Ok)
          response.setContentString(s"${false.toString}\n")
          println(s"finished with phase = ${phase}")
          val pw = new PrintWriter(new File(s"data/phase_success_${fromId}_${direction}_${phase}"))
          pw.write(s"id = ${id}")
          pw.close()
          response
        } else {
          val portDirection = direction match {
            case "left" => Math.floorMod(portEnding - 1, numPorts)
            case "right" => Math.floorMod(portEnding + 1, numPorts)
          }
          val formattedPort = "%02d".format(portDirection)
          println(s"formattedPort = ${formattedPort}")
          val client: Service[http.Request, http.Response] = Http.newService(s"localhost:80${formattedPort}")
          val request = http.Request(http.Method.Post,
            s"/?fromId=${fromId}&isIncoming=${isIncoming}&direction=${direction}&phase=${phase}")
          val response = client(request)
          Await.result(response)
        }
      }
    }

    override def apply(request: http.Request): Future[http.Response] = {
      val params = request.getParams()
      println(params)
      val isIncoming = request.getBooleanParam("isIncoming")

      if (isIncoming) {
        println("about to make an incoming request")
        incomingHandler(request)
      } else {
        outgoingHandler(request)
      }
    }
  }

  def main(args: Array[String]) {
    val portEnding = args(0)
    val id = args(1).toInt
    println(s"starting server with id = ${id} on port = ${portEnding}")

    val isLeaderService = new isLeader(id, portEnding.toInt)
    val server = Http.serve(s":80${portEnding}", isLeaderService)
    Await.ready(server)
  }
}