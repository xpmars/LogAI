package actor.split

import akka.actor.{Actor, ActorRef, Props}
import api.QeDashboardApi
import model.SplitJob
import mongo.MongoRepo
import play.api.{Configuration, Logger}

import scala.collection.mutable

class SplitManagerActor(configuration: Configuration, repo : MongoRepo,qeDashboardApi: QeDashboardApi) extends Actor {

  val noOfWorkers = configuration.underlying.getInt("parallel.splits")

  private val availableSplitActors = mutable.Queue[ActorRef]()
  private val splitsToProcess = mutable.Queue[SplitJob]()


  override def preStart(): Unit = {
    for(i <- 1 to noOfWorkers) {
      context.actorOf(SplitActor.props(configuration,repo,qeDashboardApi),s"split-worker-actor-$i")
    }
  }

  override def receive = {
    case WorkAvalibale =>
      Logger.info(s"${sender.path} is free now. Adding to available actors.")
      availableSplitActors.enqueue(sender)
      scheduleSplit
    case s:SplitJob =>
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

object SplitManagerActor {
  def props(configuration: Configuration, repo : MongoRepo, qeDashboardApi: QeDashboardApi) =
    Props(classOf[SplitManagerActor],configuration, repo,qeDashboardApi)
}
