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

class node(var id: Int, var leaderGuess: Int, var leftNeighbor: node, var rightNeighbor: node){
  // need to override hashcode to avoid stackoverflow in HashSet
  override def hashCode : Int = {
    this.id
  }
}
case class passLeaderGuess(from: node, to: node)

// timeout for actors
object MyImplicits{
  implicit val timeout = Timeout(1 second)
}

// main class
class Ring(numNodes: Int, system: ActorSystem, nodes: Array[node]) {
  import MyImplicits._

  val actorMap = new HashMap[Int, ActorRef]()
  nodes.foreach { n =>
    actorMap(n.id) = system.actorOf(Props(new NodeActor), name = n.id.toString)
  }

  class NodeActor extends Actor {
    def receive = {

      case passLeaderGuess(from: node, to: node) => {
        if (from.leaderGuess >= to.leaderGuess) {
          to.leaderGuess = from.leaderGuess
        }
        sender ! (from.leaderGuess >= to.leaderGuess)
      }
    }
  }

  // Essentially the Hirschberg and Sinclair algorithm
  // developed to minimize the number of time we pass tokens to nodes.
  def findLeaderNlogN(): Int = {
    // send out info to left and right 2^k times
    // stop sending if we find something larger
    // if we make it to the end of the out phase, send message back in
    // a process does not send messages again if it did not receive both incoming messages

    var nodeSet = new scala.collection.mutable.HashSet[node]()
    nodes.foreach{n => nodeSet.add(n)}

    var foundLeader = false
    var phase = 0
    var leader = 100000000
    while (!foundLeader) {

      nodeSet.foreach { n =>
        var currLeft = n
        var currRight = n

        val allOut = (1 to Math.pow(2, phase).toInt).forall { i =>
          val futureLeft = actorMap(currLeft.id) ? passLeaderGuess(currLeft, currLeft.leftNeighbor)
          val resultLeft = Await.result(futureLeft, timeout.duration).asInstanceOf[Boolean]
          val futureRight = actorMap(currRight.id) ? passLeaderGuess(currRight, currRight.rightNeighbor)
          val resultRight = Await.result(futureRight, timeout.duration).asInstanceOf[Boolean]
          val returnRes = resultLeft && resultRight
          currLeft = currLeft.leftNeighbor
          currRight = currRight.rightNeighbor
          if ( (currLeft.id == n.id) && (n.leaderGuess == n.id)) {
            println("leader found! " +  n.id)
            leader = n.id
            foundLeader = true
          }
          returnRes
        }
        if (!allOut) {
          nodeSet.remove(n)
        }
      }
      phase = phase + 1
    }
    leader
  }
}


object Main extends App {
  val numNodes = 10

  // define the node ring
  // TODO: clean up input node ring and/or move elsewhere
  var root = new node(0, 0, null, null)
  var current = root
  var nodeMap = new scala.collection.mutable.HashMap[Int, node]()
  nodeMap(0) = current
  (1 until numNodes).foreach{ i =>
    current.leftNeighbor = new node(i, i, null, null)
    current = current.leftNeighbor
    nodeMap(current.id) = current
 }
  current.leftNeighbor = root

  // hacky, just using a map to define right neighbors for now
  // but this only defines the input ring
  current = root
  (0 until numNodes).foreach{ i =>
    current.rightNeighbor = nodeMap(numNodes  - 1 - i)
    current = current.rightNeighbor
  }

  // put in Array, since don't like the idea of the network taking "root" as a input
  // since that signals priority to a node, somewhat defeating the purpose of choosing a
  // distinguished node
  val nodes = new Array[node](numNodes)
  current = root
  (0 until numNodes).foreach{ i =>
    nodes(i) = current
    current = current.leftNeighbor
  }

  val system = ActorSystem("ElectionSystem")
  val network = new Ring(numNodes, system, nodes)
  val leader = network.findLeaderNlogN()
  println(leader)
  println(s"leader was found to be: ${leader}")
  system.shutdown
}
