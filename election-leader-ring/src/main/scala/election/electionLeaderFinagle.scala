package election

import com.twitter.finagle.{Http, Service}
import com.twitter.finagle.http
import com.twitter.util.{Await, Future}

object electionLeaderFinagle  {

  class isLeader(id: Int, portEnding: Int) extends Service[http.Request, http.Response] {


    def incomingHandler() = {

    }


    def outgoingHandler() = {

    }


    override def apply(request: http.Request): Future[http.Response] = {
      Future {
        val params = request.getParams()
        println(params)

        val fromId = request.getIntParam("fromId")
        var iterRemaining = request.getIntParam("iterRemaining")
        var isIncoming = request.getBooleanParam("isIncoming")
        var direction = request.getParam("direction")
        var numPorts = request.getIntParam("numPorts")
        println(s"fromId = ${fromId.toString}")
        println(s"direction = ${direction}")
        println(s"iterRemaining = ${iterRemaining.toString}")


        // if isIncoming => no need to check, just pass info to incoming direction
        // if outgoing, need to check and maybe do the RPC


        id - fromId match {
          case x if x < 0 => {
            iterRemaining = iterRemaining - 1
            if (iterRemaining == 0) {
              isIncoming = true
              direction = direction match{
                case "left" => "right"
                case "right" => "left"
              }
            }
            val portDirection = direction match{
              case "left" => Math.floorMod(portEnding - 1 , numPorts)
              case "right" => Math.floorMod(portEnding + 1, numPorts)
            }

            println(s"portDirection = ${portDirection}")
            val client: Service[http.Request, http.Response] = Http.newService(s"localhost:80${portDirection.toString}")
            val request = http.Request(http.Method.Post,
              s"/?fromId=${fromId}/?iterRemaining=${(iterRemaining - 1).toString}/?isIncoming=${isIncoming}/?direction=${direction}")
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
            val response = http.Response(request.version, http.Status.Ok)
            response
          }
        }
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