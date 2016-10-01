package controllers

import javax.inject.Inject

import actor.split.SplitManagerActor
import akka.actor.ActorSystem
import api.QeDashboardApi
import logai.{LogKeys, Utils}
import logai.parser.grok.GrokParser
import model.{ErrorTestCount, LogLine, SplitJob, SplitJobStatus}
import mongo.MongoRepo
import play.api.{Configuration, Logger}
import play.api.libs.json.{JsError, JsObject, JsValue, Json}
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

  def getComponentsForRun = Action.async { implicit request =>
    val runId = request.queryString.get("runId").map(_.head)

    runId match {
      case Some(id) =>
        repo.spiltInfoRepo.get (id).map {
          result =>
            Ok (Json.toJson(result))
        }
      case _ =>
        Future {
          BadRequest ("RunId is required")
        }
    }
  }

  def getFailedTestCasesForRunAndComponent = Action.async { implicit request =>
    val runIdOpt = request.queryString.get("runId").map(_.head)
    val componentIdOpt = request.queryString.get("componentId").map(_.head)
    runIdOpt.flatMap(runId => componentIdOpt.map(cId => (runId,cId))) match {
      case Some((runId, componentId)) =>
        repo.spiltInfoRepo.get(runId,componentId).flatMap{
          infos =>
            val splitIds = infos.map(_._id)
            repo.testcaseRepo.getFailedTests(splitIds).map{
              tests =>
                Ok (Json.toJson(tests))
            }
        }
      case  _ =>
        Future {
        BadRequest ("RunId and ComponentId is required")
        }
    }
  }

  def getErrorLogsForSplit = Action.async{ implicit request =>
    val splitIdOpt = request.queryString.get("splitId").map(_.head)

    splitIdOpt match {
      case Some(splitId) =>
        repo.logsRepo.getUniqueTestLogs(splitId).flatMap {
          result =>
            mergeErrorCountLogsWithCategoryLogs(result).map{
              json =>
                Ok (Json.toJson(json))
            }
        }
      case _ =>
        Future {
          BadRequest ("SplitId and TestName is required")
        }
    }
  }

  def mergeErrorCountLogsWithCategoryLogs(logs : Seq[ErrorTestCount]) ={
        val categories = logs.map(_.category).toSeq
        repo.logsCategoryRepo.getByCategories(categories).map{
          logsWithCategory =>
            val lcMap = logsWithCategory.map(a => (a.category, a)).toMap
            val json = Json.toJson(logs)
            val mergeJson = json.as[Seq[JsObject]].map{
              j =>
                val logJson = Json.toJson(lcMap.get((j\ "category").as[String]).get)
                j ++ logJson.as[JsObject]
            }
            mergeJson
        }
  }

  def getLogsForTest = Action.async { implicit request =>
    val splitIdOpt = request.queryString.get("splitId").map(_.head)
    val testNameOpt = request.queryString.get("testName").map(_.head)
    splitIdOpt.flatMap(splitId => testNameOpt.map(testName => (splitId, testName))) match {
      case Some((splitId,testName)) =>
        val unique = request.queryString.get("unique").map(_.head).getOrElse("true").toBoolean

        if(unique){
          repo.logsRepo.getUniqueTestLogs(splitId,testName).flatMap{
            logs =>
              mergeErrorCountLogsWithCategoryLogs(logs).map{
                json =>
                   Ok(Json.toJson(json))
              }
          }
        } else  repo.logsRepo.getTestLogs(splitId,testName).map{
          logs =>
            Ok (Json.toJson(logs))
        }

      case _ =>
        Future {
          BadRequest ("SplitId and TestName is required")
        }
    }
  }

  def getSplits = Action.async{ implicit request =>
    val limit = request.queryString.get("limit").map(_.head.toInt).getOrElse(30)
    repo.splitJobRepo.find(limit).map{
      split =>
        Ok(Json.toJson(split))
    }
  }

  def getSplitInfo = Action.async{ implicit request =>
    val limit = request.queryString.get("limit").map(_.head.toInt).getOrElse(30)
    repo.spiltInfoRepo.find(limit).map{
      split =>
        Ok(Json.toJson(split))
    }
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
        val split: SplitJob = s.withStatus(SplitJobStatus.QUEUED,None)
        repo.splitJobRepo.save(split).map {
          result =>
            splitManagerActor ! split
            Created(Json.toJson(split))
        }
      }
    )
  }
}