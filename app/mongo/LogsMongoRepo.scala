package mongo

import model.{CategoryCount, ErrorTestCount, LogLine, TestCaseStatus}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.MultiBulkWriteResult
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.Future

class LogsMongoRepo(reactiveMongoApi: ReactiveMongoApi){

  import scala.concurrent.ExecutionContext.Implicits.global

  private val LogsCollection = "logs"
  private val TestCaseErrorLogCountCollection = "tc_error_count"
  private val ErrorCategoryCountCollection = "error_category_count"

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

  def saveTestCaseErrorCount(errors : Seq[ErrorTestCount]) = {
    reactiveMongoApi.database.map(_.collection[JSONCollection](TestCaseErrorLogCountCollection)).flatMap {
      logcollection =>
        val documents = errors.map(implicitly[logcollection.ImplicitlyDocumentProducer](_))
        logcollection.bulkInsert(ordered = true)(documents: _*)
    }
  }

  def saveErrorCategoryCount(errors:Seq[CategoryCount]) = {
    reactiveMongoApi.database.map(_.collection[JSONCollection](ErrorCategoryCountCollection)).flatMap {
      logcollection =>
        val documents = errors.map(implicitly[logcollection.ImplicitlyDocumentProducer](_))
        logcollection.bulkInsert(ordered = true)(documents: _*)
    }
  }

}
