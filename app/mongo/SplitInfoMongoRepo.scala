package mongo

import model.{SplitInfo, SplitJob}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.Future

class SplitInfoMongoRepo(reactiveMongoApi: ReactiveMongoApi) {

  import scala.concurrent.ExecutionContext.Implicits.global

  private val SplitInfoCollection = "splitInfo"

  private def collection = reactiveMongoApi.database.map(_.collection[JSONCollection](SplitInfoCollection))

  import reactivemongo.play.json._

  def save(split: SplitInfo): Future[WriteResult] = collection.flatMap(_.insert(split))

  def get(runId: String) : Future[Seq[SplitInfo]] =  collection.flatMap(_.find(
    BSONDocument(
        "runId" -> runId
      )).cursor[SplitInfo]().collect[List]())

  def get(runId: String, componentId: String) : Future[Seq[SplitInfo]] =  collection.flatMap(_.find(
    BSONDocument(
      "runId" -> runId,
      "componentId" -> componentId
    )).cursor[SplitInfo]().collect[List]())

  def find(limit: Int) = {
    val sort = BSONDocument("_id" -> -1)
    collection.flatMap(_.find(BSONDocument()).cursor[SplitInfo]().collect[Seq](limit))
  }


}
