package election

import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.Props
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration._

case class node(id: Int, leaderGuess: Int)
case class askLeader(id: Int)
case class updateLeader(id: Int)

object MyImplicits{
    implicit val timeout = Timeout(1 second)
}

class Ring(numNodes: Int, system: ActorSystem) {
    import MyImplicits._
    // TODO: allow indices below not assume that node ids are from 0 to nodeNums
    var nodes = (0 to numNodes).map(x => node(x,x))

    val nodeIds = nodes.map(x => x.id.toString)
    val actorArray = nodeIds.map(x => system.actorOf(Props(new NodeActor), name = x))
    //val actorMap = scala.collection.Mutable.HashMap
    class NodeActor extends Actor {
        def receive = {
            case askLeader(from: Int) => {
                sender ! nodes(from).leaderGuess
            }
        }
    }

    def findLeader(): Int = {
        var inAgreement = false
        while (!inAgreement){

            inAgreement = nodes.forall{ n => 
                val neighbor = Math.floorMod(n.id + 1, numNodes)
                val future = actorArray(n.id) ? askLeader(neighbor)
                val result = Await.result(future, timeout.duration).asInstanceOf[Int]
                result == n.leaderGuess
            }

            // else update neighbors on guess
            nodes = nodes.map{ n =>
                val lowerNeighbor = Math.floorMod(n.id - 1, numNodes)
                val upperNeighbor = Math.floorMod(n.id + 1, numNodes)
                val futureLower  = actorArray(n.id) ? askLeader(lowerNeighbor)
                val futureHigher  = actorArray(n.id) ? askLeader(upperNeighbor)
                val leaderLowerGuess = Await.result(futureLower, timeout.duration).asInstanceOf[Int]
                val higherLowerGuess = Await.result(futureHigher, timeout.duration).asInstanceOf[Int]
                val curLeaderGuess = Array(n.leaderGuess, leaderLowerGuess, higherLowerGuess).min
                node(n.id, curLeaderGuess)
            }
        }
        nodes(0).leaderGuess
    }
}

object Main extends App {
    val numNodes = 10
    val system = ActorSystem("ElectionSystem")
    val network = new Ring(numNodes, system)
    val leader = network.findLeader()
    println(leader)
    println(s"leader was found to be: ${leader}")
    system.shutdown
}
