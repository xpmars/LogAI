package api

import javax.inject.Inject

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
class QeDashboardApi @Inject()(configuration: Configuration, ws: WSClient, reactiveMongoApi: ReactiveMongoApi)(implicit ec: ExecutionContext) {

  private val dashboardUrl = configuration.underlying.getString("qe.dashboard.uri")

  private val endTimeColumn = "test_job_end_time"
//  private val endTimeColumn = "end_time"

  def getCompletedSplits() : Future[WSResponse] = {
    lastCheckTime.flatMap{
      lct =>
        val url: String = dashboardUrl + s"/v1/splits?query=${endTimeColumn}>${lct}"
        Logger.info(url)
        val future = ws.url(url).get()
        future.onSuccess{
          case resp => if(resp.status == 200) saveLastCheckTime(lct)
        }
        future
    }
  }

  private val LastCheckedCollection = "lastChecked"

  import reactivemongo.bson._
  private def lastCheckTime : Future[Long] = {
    reactiveMongoApi.database.map(_.collection[JSONCollection](LastCheckedCollection)).flatMap{
      collection =>
        collection.find(document()).one[BSONDocument]
          .map{
          case Some(document) => document.getAs[Long]("time").get // Need to check document.This is not working.
          case None => System.currentTimeMillis()/1000 - 1*24*60*60
        }
    }
  }

  private def saveLastCheckTime(time : Long) = {
    reactiveMongoApi.database.map(_.collection[JSONCollection](LastCheckedCollection))
      .map(_.update(document("_id" -> 1), document("id" -> 1, "time" -> time), upsert = true))
  }

}

