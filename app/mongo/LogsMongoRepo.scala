package mongo

import model.LogLine
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.MultiBulkWriteResult
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.Future

class LogsMongoRepo(reactiveMongoApi: ReactiveMongoApi){

  import scala.concurrent.ExecutionContext.Implicits.global

  val LogsCollection = "logs"

  def collection = reactiveMongoApi.database.map(_.collection[JSONCollection](LogsCollection))

  def save(logs:Seq[LogLine]) : Future[MultiBulkWriteResult] = collection.flatMap{
    logcollection =>
      val documents = logs.map(implicitly[logcollection.ImplicitlyDocumentProducer](_))
      logcollection.bulkInsert(ordered = true)(documents: _*)
  }

}
