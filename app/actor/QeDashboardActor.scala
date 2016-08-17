package actor

import actor.QeDashboardActor.GetCompletedSplit
import akka.actor.{Actor, Props}
import api.QeDashboardApi
import play.api.Logger

/**
  * Created by gnagar on 16/08/16.
  */
class QeDashboardActor(qeDashboardApi:QeDashboardApi) extends Actor {

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._
  context.system.scheduler.schedule(50 milliseconds, 1 minute, self, GetCompletedSplit)

  override def receive = {
    case GetCompletedSplit =>
      qeDashboardApi.getCompletedSplits().map{
        case result =>
          Logger.info(result.toString)
          Logger.info(result.body)
      }
    case _ =>
      Logger.info("Unknown Message")
  }
}

object QeDashboardActor {

  def props(qeDashboardApi: QeDashboardApi) =
    Props(classOf[QeDashboardActor], qeDashboardApi)

  case class GetCompletedSplit()
}

