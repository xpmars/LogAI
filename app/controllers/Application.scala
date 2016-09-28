package controllers

import javax.inject.Inject

import actor.split.SplitManagerActor
import akka.actor.ActorSystem
import api.QeDashboardApi
import logai.parser.grok.GrokParser
import model.{SplitJob, SplitJobStatus}
import mongo.MongoRepo
import play.api.Configuration
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