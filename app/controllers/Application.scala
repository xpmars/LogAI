package controllers

import javax.inject.Inject

import actor.QeDashboardActor
import akka.actor.ActorSystem
import api.QeDashboardApi
import logai.LogKeys
import logai.parser.grok.GrokParser
import logai.reader.LogDirReader
import model.LogLine
import mongo.LogsMongoRepo
import play.api.Configuration
import play.api.libs.ws.WSClient
import play.api.mvc._
import play.modules.reactivemongo.ReactiveMongoApi

import scala.concurrent.{ExecutionContext, Future}

class Application @Inject() (configuration:Configuration,
                             system: ActorSystem,
                             reactiveMongoApi: ReactiveMongoApi,
                             qeDashboardApi: QeDashboardApi,
                             ws: WSClient)
                            (implicit ec: ExecutionContext)  extends Controller {

  val logsRepo = new LogsMongoRepo(reactiveMongoApi)
  val qeDashboardActor = system.actorOf(QeDashboardActor.props(qeDashboardApi), "qe-dashboard-actor")

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def testApp = Action {
    Ok(GrokParser.parsers.keySet.mkString(","))
  }

  def parseError = Action.async { implicit request =>
    val head = request.queryString.get("path").map(_.head)
    head match {
      case Some(value) =>
        val logs: Seq[Map[String, Any]] = LogDirReader.readDir(value).toSeq
        val loglines = logs.map(getLogLine(_))
        logsRepo.save(loglines).map(x => Ok(x.toString))
      case _ => Future {
        BadRequest
      }
    }

  }

  def getLogLine(map:Map[String,Any]) : LogLine = {
    val timestamp = map.getOrElse(LogKeys.Timestamp, 0L).asInstanceOf[Long]
    val data = map.getOrElse(LogKeys.Message, "") + "\n" + map.getOrElse(LogKeys.Stacktrace,"")
    val filename = map.getOrElse(LogKeys.Filename, "").asInstanceOf[String]
    new LogLine(timestamp, data, filename)
  }
}