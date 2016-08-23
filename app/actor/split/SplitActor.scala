package actor.split

import akka.actor.Actor
import akka.util.LineNumbers.Result
import play.api.Logger

/**
  * Created by gnagar on 18/08/16.
  */
class SplitActor extends Actor {
  override def receive = {
    case split:Split =>
      //saveLogs
      //saveTestData
      //ProcessLogs
    case a@_ => Logger.info("Unknown Message "+a)
  }
}


case class Split(id:String, cluster :String, resultId:String, )