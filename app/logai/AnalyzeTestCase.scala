package logai

import model.{CategoryCount, ErrorTestCount, TestCaseStatus}
import mongo.MongoRepo
import reactivemongo.api.commands.MultiBulkWriteResult

import scala.concurrent.Future


class AnalyzeTestCase(splitId: String, repo: MongoRepo, errors: Seq[Map[String, Any]]) {

  //  val failedTests = repo.logsRepo.getAllFailedTestLogs(splitId).map{
  //    logs =>
  //  }

  import LogKeys._
  import scala.concurrent.ExecutionContext.Implicits.global

  def analyze() = {
    val testCasesErrorCounts = errors.filter(e => e.contains(TestName)
      && e.getOrElse(TestStatus, TestCaseStatus.TestSuccessful).asInstanceOf[Int] == TestCaseStatus.TestFailed)
      .groupBy(
        e => (e.get(Category).get, e.get(TestName).get)
      ).map {
      case ((category, testName), es) =>
        ErrorTestCount(splitId, category.toString, testName.toString, es.length)
    }

    val future1 = repo.logsRepo.saveTestCaseErrorCount(testCasesErrorCounts.toSeq)

    val errorCounts = errors.groupBy(e => e.get(Category).get).map{
      e =>
        CategoryCount(splitId, e._1.toString,e._2.length)
    }.toSeq

    val future2 = repo.logsRepo.saveErrorCategoryCount(errorCounts)

    val futures : Seq[Future[MultiBulkWriteResult]] = Seq(future1,future2)

    Future.sequence(futures)
  }

}
