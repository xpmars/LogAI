package controllers

import javax.inject.Inject

import akka.actor.ActorSystem
import logai.parser.grok.GrokParser
import logai.reader.LogDirReader
import play.api._
import play.api.libs.json.Json
import play.api.mvc._

class Application extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def testApp = Action {
    Ok(GrokParser.parsers.keySet.mkString(","))
  }

  def parseError = Action { implicit request =>
    val head = request.queryString.get("path").map(_.head)
    head match {
      case Some(value) =>
        val logs: Seq[Map[String, Any]] = LogDirReader.readDir(value)
        logs.foreach{
          map => Logger.info(map.toString())
        }
        Ok(logs.mkString("\n"))
      case _ => BadRequest
    }

  }
}