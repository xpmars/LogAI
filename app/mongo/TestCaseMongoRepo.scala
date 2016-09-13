package mongo

import model.TestCaseStatus
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.MultiBulkWriteResult
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.Future

class TestCaseMongoRepo(reactiveMongoApi: ReactiveMongoApi){

  import scala.concurrent.ExecutionContext.Implicits.global

  val TestCasesCollection = "testcases"

  def collection = reactiveMongoApi.database.map(_.collection[JSONCollection](TestCasesCollection))

  def save(testcases:Seq[TestCaseStatus]) : Future[MultiBulkWriteResult] = collection.flatMap{
    logcollection =>
      val documents = testcases.map(implicitly[logcollection.ImplicitlyDocumentProducer](_))
      logcollection.bulkInsert(ordered = true)(documents: _*)
  }

}
