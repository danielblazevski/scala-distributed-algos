package election

import com.twitter.finagle.{Service, Http}
import com.twitter.finagle.http
import com.twitter.util.Await

object Client {
  def main(args: Array[String]): Unit = {

    // assume portEnding are from 10 to 99, so ports are from 8000 to 8099
    val portEnding = args(0).toInt
    val id = args(1).toInt

    val numPorts = 3
    var isLeader = false
    var isMaybeLeader = true
    var phase = 0

    val leftPort = "%02d".format(Math.floorMod(portEnding - 1 , numPorts))
    val rightPort = "%02d".format(Math.floorMod(portEnding + 1, numPorts))
    val leftClient: Service[http.Request, http.Response] = Http.newService(s"localhost:80${leftPort}")
    val rightClient: Service[http.Request, http.Response] = Http.newService(s"localhost:80${rightPort}")

    while (isMaybeLeader){
      val iterRemaining = phase + 1
      val leftRequest = http.Request(http.Method.Post,
        s"/?fromId=${id}&iterRemaining=${iterRemaining}&isIncoming=false&direction=left&numPorts=${numPorts}")
      val rightRequest = http.Request(http.Method.Post,
      s"/?fromId=${id}&iterRemaining=${iterRemaining}&isIncoming=false&direction=right&numPorts=${numPorts}")

      val leftResponse = leftClient(leftRequest)
      val rightResponse = rightClient(rightRequest)

      Await.result(leftResponse.onSuccess {
        response => println(response)
      })
      Await.result(rightResponse.onSuccess{
        response => println(response)
      })
    }
  }
}
