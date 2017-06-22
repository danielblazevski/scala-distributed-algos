package election

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.pattern.ask
import akka.util.Timeout

import scala.collection.mutable.HashMap
import scala.concurrent.Await
import scala.concurrent.duration._

case class node(id: Int, leaderGuess: Int, leftNeighbor: Int, rightNeighbor: Int)
case class askLeader(id: Int)
case class updateLeader(id: Int)

object MyImplicits{
  implicit val timeout = Timeout(1 second)
}

class Ring(numNodes: Int, system: ActorSystem, nodes: Array[node]) {
  import MyImplicits._

  var nodesMap = new HashMap[Int, node]
  nodes.foreach{n => nodesMap(n.id) = n}

  val actorMap = new HashMap[Int, ActorRef]()
  nodes.foreach { n =>
    actorMap(n.id) = system.actorOf(Props(new NodeActor), name = n.id.toString)
  }

  class NodeActor extends Actor {
    def receive = {
      case askLeader(from: Int) => {
        sender ! nodesMap(from).leaderGuess
      }
    }
  }

  def findLeader(): Int = {
    var inAgreement = false
    while (!inAgreement){
      inAgreement = nodesMap.forall{ nTup =>
        val n = nTup._2
        val future = actorMap(n.id) ? askLeader(n.leftNeighbor)
        val result = Await.result(future, timeout.duration).asInstanceOf[Int]
        result == n.leaderGuess
      }

      // else update neighbors on guess
      nodes.foreach{ n =>
        val futureLeft  = actorMap(n.id) ? askLeader(n.leftNeighbor)
        val futureRight  = actorMap(n.id) ? askLeader(n.rightNeighbor)
        val leaderLeftGuess = Await.result(futureLeft, timeout.duration).asInstanceOf[Int]
        val leaderRightGuess = Await.result(futureRight, timeout.duration).asInstanceOf[Int]
        val curLeaderGuess = Array(n.leaderGuess, leaderLeftGuess, leaderRightGuess).min

        nodesMap(n.id) = node(n.id, curLeaderGuess, n.leftNeighbor, n.rightNeighbor)
      }
    }
    nodes(0).leaderGuess
  }
}

object Main extends App {
  val numNodes = 10

  var nodes = (0 to numNodes).map{x =>
    node(x,x, Math.floorMod(x + 1, numNodes), Math.floorMod(x - 1, numNodes))
  }.toArray

  val system = ActorSystem("ElectionSystem")
  val network = new Ring(numNodes, system, nodes)
  val leader = network.findLeader()
  println(leader)
  println(s"leader was found to be: ${leader}")
  system.shutdown
}
