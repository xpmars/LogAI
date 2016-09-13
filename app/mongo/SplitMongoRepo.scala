package mongo


import model.SplitJob
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.Future

class SplitMongoRepo(val reactiveMongoApi: ReactiveMongoApi) {

  import reactivemongo.play.json._

  import scala.concurrent.ExecutionContext.Implicits.global

  private val SplitCollection = "splits"

  private def collection = reactiveMongoApi.database.map(_.collection[JSONCollection](SplitCollection))

  def find(splitId: String) = {
    collection.flatMap(_.find(BSONDocument(
      "_id" -> splitId
    )).requireOne[SplitJob])
  }

  def save(split: SplitJob): Future[WriteResult] = collection.flatMap(_.insert(split))

  def update(split: SplitJob) = {
    val selector = Json.obj("_id" -> split._id)
    val update = Json.obj("$set" -> Json.toJson(split))
    collection.flatMap(_.update(selector, update))
  }
}
