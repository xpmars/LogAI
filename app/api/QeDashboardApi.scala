package api

import javax.inject.Inject

import model.SplitInfo
import mongo.MongoRepo
import play.api.libs.json.{JsLookupResult, JsObject, Json}
import play.api.{Configuration, Logger}
import play.api.libs.ws.{WSClient, WSResponse}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.play.json._
import play.modules.reactivemongo.json.collection._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by gnagar on 16/08/16.
  */
class QeDashboardApi(configuration: Configuration, ws: WSClient, repo:MongoRepo)(implicit ec: ExecutionContext) {

  private val dashboardUrl = configuration.underlying.getString("qe.dashboard.uri")

  // Get resultid from splitId
  private val splitUrl = dashboardUrl + "/v1/splits?id=%s"
  private val resultUrl = dashboardUrl + "/v1/results?id=%s"

  import play.api.libs.json._
  def saveSplitInfo(splitId: String) = {
    ws.url(splitUrl.format(splitId)).get()
      .flatMap { splitResp =>
        val splitJson: JsLookupResult = (Json.parse(splitResp.body)\"splits")(0)
        val resultId = (splitJson \ "result_id").as[String]
        ws.url(resultUrl.format(resultId)).get()
          .flatMap { resp =>
            val json = (Json.parse(resp.body)\"results")(0)
            val releaseId= (json\"release_id").as[String]
            val runId = (json\"run_info_id").as[String]
            val componentId = (json\"component_id").as[String]
            val splitNo = (splitJson \ "split_number").as[String]
            val info = SplitInfo(splitId, splitNo, resultId,componentId,releaseId,runId)
            Logger.info(info.toString)
            repo.spiltInfoRepo.save(info)
          }
      }
  }

  //  def getCompletedSplits() : Future[WSResponse] = {
  //    lastCheckTime.flatMap{
  //      lct =>
  //        val currentTime = System.currentTimeMillis()/1000;
  //        val url: String = dashboardUrl + s"/v1/splits?query=${endTimeColumn}>${lct}"
  //        Logger.info(url)
  //        val future = ws.url(url).get()
  //        future.onSuccess{
  //          case resp => if(resp.status == 200) saveLastCheckTime(currentTime)
  //        }
  //        future
  //    }
  //  }
  //
  //  private val LastCheckedCollection = "lastChecked"
  //
  //  import reactivemongo.bson._
  //  import reactivemongo.play.json.BSONFormats._
  //
  //
  //  private def lastCheckTime: Future[Long] = {
  //    reactiveMongoApi.database.map(_.collection[JSONCollection](LastCheckedCollection)).flatMap {
  //      collection =>
  //        collection.find(document()).cursor[BSONDocument]().headOption.map {
  //          case Some(doc) => doc.getAs[BSONInteger]("time").get.value
  //          case None => System.currentTimeMillis() / 1000 - 1 * 24 * 60 * 60
  //        }
  //    }
  //  }
  //
  //  private def saveLastCheckTime(time : Long) = {
  //    reactiveMongoApi.database.map(_.collection[JSONCollection](LastCheckedCollection))
  //      .map(_.update(document("_id" -> 1), document("_id" -> 1, "time" -> time), upsert = true))
  //  }

}

