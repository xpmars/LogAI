package logai

import logai.reader.{TestCaseMetaData, TestCaseMetadataReader}
import model.TestCaseStatus
import mongo.TestCaseMongoRepo

import scala.annotation.tailrec

class TestCaseToErrorMapper(splitId: String, logDir: String, testCasesMongoRepo: TestCaseMongoRepo) {

  private val testCases = new TestCaseMetadataReader(logDir).parseMetaData()

  testCasesMongoRepo.save(testCases.map(t => TestCaseStatus(splitId, t.name, t.status, t.startTime, t.endTime, t.testSuite)))

  @tailrec
  private def getTest(time: Long, tests: Seq[TestCaseMetaData] = testCases): Option[TestCaseMetaData] = tests match {
    case Nil => None
    case head :: tail =>
      if (head.startTime < time && head.endTime > time) return Some(head)
      getTest(time, tail)
  }


  def mapTests(log: Map[String, Any]) = {
    val time = log.get(LogKeys.Timestamp).get.asInstanceOf[Long]

    getTest(time) match {
      case Some(test) =>
        val testSuiteMap = test.testSuite.map(suite => Map(LogKeys.TestSuite -> suite)).getOrElse(Map())
        log ++ Map(LogKeys.TestName -> test.name, LogKeys.TestStatus -> test.status) ++ testSuiteMap
      case None =>
        log
    }
  }

}
