package election

import com.twitter.finagle.Http
import com.twitter.util.Await

object electionLeaderFinagle  {

  def main(args: Array[String]) {
    val portEnding = args(0)
    val id = args(1).toInt
    println(s"starting server with id = ${id} on port = ${portEnding}")

    val isLeaderService = new isLeader(id, portEnding.toInt)
    val server = Http.serve(s":80${portEnding}", isLeaderService)
    Await.ready(server)
  }
}