package mongo

import model.{SplitJob, TestCaseStatus}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.MultiBulkWriteResult
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.Future

class TestCaseMongoRepo(reactiveMongoApi: ReactiveMongoApi){

  import scala.concurrent.ExecutionContext.Implicits.global

  private val TestCasesCollection = "testcases"

  import reactivemongo.play.json._

  private def collection = reactiveMongoApi.database.map(_.collection[JSONCollection](TestCasesCollection))

  def save(testcases:Seq[TestCaseStatus]) : Future[MultiBulkWriteResult] = collection.flatMap{
    logcollection =>
      val documents = testcases.map(implicitly[logcollection.ImplicitlyDocumentProducer](_))
      logcollection.bulkInsert(ordered = true)(documents: _*)
  }

  def getFailedTests(splitIds:Seq[String]) = {
    collection.flatMap(_.find(BSONDocument(
      "splitId"  -> BSONDocument("$in" -> splitIds)
      , "status" -> TestCaseStatus.TestFailed
    )).cursor[TestCaseStatus]().collect[List]())
  }

}
