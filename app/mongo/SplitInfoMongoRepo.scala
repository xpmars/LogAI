package mongo

import model.{SplitInfo, SplitJob}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.WriteResult
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.Future

class SplitInfoMongoRepo(reactiveMongoApi: ReactiveMongoApi) {

  import scala.concurrent.ExecutionContext.Implicits.global

  private val SplitInfoCollection = "splitInfo"

  private def collection = reactiveMongoApi.database.map(_.collection[JSONCollection](SplitInfoCollection))

  import reactivemongo.play.json._

  def save(split: SplitInfo): Future[WriteResult] = collection.flatMap(_.insert(split))

}
