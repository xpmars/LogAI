package actor.split

import akka.actor.{Actor, Props}
import akka.util.LineNumbers.Result
import play.api.Logger

/**
  * Created by gnagar on 18/08/16.
  */
class SplitActor extends Actor {

  override def preStart() = {
    context.parent ! WorkAvalibale
  }

  override def receive = {
    case split:Split =>
      //saveLogs
      //saveTestData
      //ProcessLogs
    case a@_ => Logger.info("Unknown Message "+a)
  }
}

object SplitActor {
  def props() = Props(classOf[SplitActor])
}


case class Split(id:String, logUrl :String, resultId:String )