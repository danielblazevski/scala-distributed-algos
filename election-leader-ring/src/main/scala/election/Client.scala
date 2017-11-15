package election

import java.io.File

import com.twitter.finagle.{Http, Service}
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
      phase = phase + 1
      println(s"phase ${phase}" )
      val leftRequest = http.Request(http.Method.Get,
        s"/?fromId=${id}&iterRemaining=${phase}&isIncoming=false&direction=left&numPorts=${numPorts}&phase=${phase}")
      val rightRequest = http.Request(http.Method.Get,
      s"/?fromId=${id}&iterRemaining=${phase}&isIncoming=false&direction=right&numPorts=${numPorts}&phase=${phase}")

      val leftResponse = leftClient(leftRequest)
      val rightResponse = rightClient(rightRequest)

      Await.result(leftResponse.onSuccess {
        response => println(response)
      })
      Await.result(rightResponse.onSuccess{
        response => println(response)
      })
      if (!(new java.io.File(s"data/phase_success_${id}_left_${phase}").exists) ||
        !(new java.io.File(s"data/phase_success_${id}_right_${phase}").exists)){
        println("not moving to next phase!")
        isMaybeLeader = false
      }
      if (new java.io.File(s"data/leader${id}").exists){
        isMaybeLeader = false
      }
    }
  }
}
