package mongo

import javax.inject.{Inject, Singleton}

import model.LogLine
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.commands.MultiBulkWriteResult
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by gnagar on 03/08/16.
  */

@Singleton
class LogsMongoRepo(val reactiveMongoApi: ReactiveMongoApi)(implicit ec: ExecutionContext)  {

  val LogsCollection = "logs"

  def collection = reactiveMongoApi.database.map(_.collection[JSONCollection](LogsCollection))

  def save(logs:Seq[LogLine]) : Future[MultiBulkWriteResult] = collection.flatMap{
    logcollection =>
      val documents = logs.map(implicitly[logcollection.ImplicitlyDocumentProducer](_))
      logcollection.bulkInsert(ordered = true)(documents: _*)
  }
}
