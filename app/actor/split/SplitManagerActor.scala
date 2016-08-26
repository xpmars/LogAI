package actor.split

import akka.actor.{Actor, ActorRef}
import akka.actor.Actor.Receive
import play.api.Logger

import scala.collection.mutable

/**
  * Created by gnagar on 25/08/16.
  */
class SplitManagerActor extends Actor {

  val noOfWorkers = 5

  private val availableSplitActors = mutable.Queue[ActorRef]()
  private val splitsToProcess = mutable.Queue[Split]()

  override def preStart(): Unit = {
    for(i <- 1 to noOfWorkers) {
      context.actorOf(SplitActor.props(),s"split-worker-actor-$i")
    }
  }

  override def receive = {
    case WorkAvalibale =>
      Logger.info(s"${sender.path} is free now. Adding to available actors.")
      availableSplitActors.enqueue(sender)
      scheduleSplit
    case s:Split =>
      Logger.info(s"Received Split with id: ${s}")
      splitsToProcess.enqueue(s)
      scheduleSplit
  }


  private def scheduleSplit = {
    if(!splitsToProcess.isEmpty && !availableSplitActors.isEmpty){
      Logger.info(s"Scheduling split: ${splitsToProcess.head} on worker: ${availableSplitActors.head.path}")
      availableSplitActors.dequeue() ! splitsToProcess.dequeue()
    }
  }
}
