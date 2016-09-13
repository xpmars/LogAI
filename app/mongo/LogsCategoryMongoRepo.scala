package mongo

import model.LogLine
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.MultiBulkWriteResult
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.Future


class LogsCategoryMongoRepo(reactiveMongoApi: ReactiveMongoApi) {

  import scala.concurrent.ExecutionContext.Implicits.global

  val LogsCategoryCollection = "logs_category"

  import reactivemongo.play.json._

  def collection = reactiveMongoApi.database.map(_.collection[JSONCollection](LogsCategoryCollection))

  def save(logs:Seq[LogLine]) : Future[MultiBulkWriteResult] = collection.flatMap{
    logcollection =>
      val documents = logs.map(implicitly[logcollection.ImplicitlyDocumentProducer](_))
      logcollection.bulkInsert(ordered = true)(documents: _*)
  }

  def getByOrigin(origin:String)  = {
    collection.flatMap(_.find(BSONDocument(
      "origin" -> origin
    )).cursor[LogLine]().collect[List]())
  }
}
