package mongo

import model.{LogLine, TestCaseStatus}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.MultiBulkWriteResult
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.Future

class LogsMongoRepo(reactiveMongoApi: ReactiveMongoApi){

  import scala.concurrent.ExecutionContext.Implicits.global

  private val LogsCollection = "logs"

  private def collection = reactiveMongoApi.database.map(_.collection[JSONCollection](LogsCollection))

  def save(logs:Seq[LogLine]) : Future[MultiBulkWriteResult] = collection.flatMap{
    logcollection =>
      val documents = logs.map(implicitly[logcollection.ImplicitlyDocumentProducer](_))
      logcollection.bulkInsert(ordered = true)(documents: _*)
  }

//  def getAllFailedTestLogs(splitId: String) = {
//    collection.flatMap(_.find(BSONDocument(
//      "_id" -> splitId, "testStatus" -> TestCaseStatus.TestSuccessful
//    )).cursor[LogLine]().collect[List]())
//  }

}
