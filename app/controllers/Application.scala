package controllers

import javax.inject.Inject

import actor.split.SplitManagerActor
import akka.actor.ActorSystem
import api.QeDashboardApi
import logai.{LogKeys, Utils}
import logai.parser.grok.GrokParser
import model.{SplitJob, SplitJobStatus}
import mongo.MongoRepo
import play.api.{Configuration, Logger}
import play.api.libs.json.{JsError, Json}
import play.api.libs.ws.WSClient
import play.api.mvc._
import play.modules.reactivemongo.ReactiveMongoApi

import scala.concurrent.{ExecutionContext, Future}

class Application @Inject()(configuration: Configuration,
                            system: ActorSystem,
                            reactiveMongoApi: ReactiveMongoApi,
                            ws: WSClient)
                           (implicit ec: ExecutionContext) extends Controller {


  val repo = new MongoRepo(reactiveMongoApi)
  val qeDashboardApi = new QeDashboardApi(configuration, ws, repo)
  val splitManagerActor = system.actorOf(SplitManagerActor.props(configuration, repo, qeDashboardApi), "split-manager-actor")

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def testApp = Action {
    Ok(GrokParser.parsers.keySet.mkString(","))
  }

  def testParsers = Action(BodyParsers.parse.json) { request =>
    val log = (request.body \ "log").as[String]
    val parserResult = GrokParser.parsers.map{
      p =>
        val map = p._2.parse(log)
        if(!map.isEmpty){
          Logger.info(map.get("date").get.toString)
          Logger.info(Utils.parseTime(map.get("date").get.toString).toString)
        }
        (p._1,map.mkString(","))
    }.toSeq
    Ok(parserResult.toString())
  }

  def checkPattern = Action(BodyParsers.parse.json) { request =>
    val pattern  = (request.body \ "pattern").as[String]
    val log = (request.body \ "log").as[String]
    val parsed = new GrokParser("test", pattern).parse(log)
    Ok(parsed.mkString(","))
  }

  def processSplit = Action.async(BodyParsers.parse.json) { request =>
    request.body.validate[SplitJob] fold(
      errors => {
        Future {
          BadRequest(JsError.toJson(errors))
        }
      },
      s => {
        val split: SplitJob = s.withStatus(SplitJobStatus.QUEUED)
        repo.splitJobRepo.save(split).map {
          result =>
            splitManagerActor ! split
            Created(Json.toJson(split))
        }
      }
    )
  }
}