package logai.reader

import java.io.File

import logai.LogAIUtils
import logai.parser.grok.GrokParser
import model.TestCaseStatus
import org.joda.time.format.DateTimeFormat
import play.api.Logger
import play.api.libs.json.Json

import scala.collection.mutable
import scala.io.Source

class TestCaseMetadataReader(path: String) {

  private val TestCaseStatusFile =
    if (new File(LogAIUtils.sanitizePath(path) +"testcaselogs/test_case_status.log").exists())
      LogAIUtils.sanitizePath(path) + "testcaselogs/test_case_status.log"
    else
      LogAIUtils.sanitizePath(path) + "artifacts/test_case_status.log"

  private val TestCaseStatusTxtFile =
    if (new File(LogAIUtils.sanitizePath(path) +"testcaselogs/test_case_status.txt").exists())
      LogAIUtils.sanitizePath(path) + "testcaselogs/test_case_status.txt"
    else
      LogAIUtils.sanitizePath(path) + "artifacts/test_case_status.txt"


  private val TestKey: String = "test"
  private val DateKey: String = "date"

  private val pattern1 = "%{TIMESTAMP_ISO8601:date}.*RUNNING TEST \"%{DATA:test}\".*"
  private val pattern2 = "%{TIMESTAMP_ISO8601:date}.*TEST \"%{DATA:test}\" FAILED"
  private val pattern3 = "%{TIMESTAMP_ISO8601:date}.*TEST \"%{DATA:test}\" PASSED"

  private val parser = new GrokParser("runningTest", pattern1)
  private val failedTestParser = new GrokParser("failedTest", pattern2)
  private val passedTestParser = new GrokParser("passedTest", pattern3)

  private val dateTime = DateTimeFormat.forPattern("YYYY-MM-dd HH:mm:ss,SSS")

  val tests = mutable.Map[String, TestCaseMetaData]()

  def parseMetaData() = {

    Source.fromFile(TestCaseStatusFile).getLines().foreach {
      line =>
        val map: Map[String, AnyRef] = parser.parse(line)
        val map2: Map[String, AnyRef] = failedTestParser.parse(line)
        val map3: Map[String, AnyRef] = passedTestParser.parse(line)

        if (!map.isEmpty) {
          val st = dateTime.parseDateTime(map.get(DateKey).get.toString).getMillis
          val name = map.get(TestKey).get.toString
          tests += ((name, TestCaseMetaData(name, st)))
        } else if (!map2.isEmpty) {
          val et = dateTime.parseDateTime(map2.get(DateKey).get.toString).getMillis
          val name = map2.get(TestKey).get.toString
          tests += ((name, tests.get(name).get.setEndTime(et, TestCaseStatus.TestFailed)))
        } else if (!map3.isEmpty) {
          val et = dateTime.parseDateTime(map3.get(DateKey).get.toString).getMillis
          val name = map3.get(TestKey).get.toString
          tests += ((name, tests.get(name).get.setEndTime(et, TestCaseStatus.TestSuccessful)))
        }
    }

    try {
      addTestSuite()
    } catch {
      case e:Exception => Logger.warn("Adding test Suite failed.",e)
    }
    tests.values.filter(t => t.endTime != 0 && t.startTime != 0 && t.status != 0).toSeq
  }

  def addTestSuite() ={
    Source.fromFile(TestCaseStatusTxtFile).getLines().foreach{
      line =>
        val json = Json.parse(line.replace("'","\""))
        val suite = (json \"suite").as[String]
        val name = (json \"name").as[String]
        tests += ((name, tests.get(name).get.setTestSuite(suite)))
    }
  }

}

case class TestCaseMetaData(name: String, startTime: Long, endTime: Long = 0, status: Int = 0, testSuite:Option[String]=None) {
  def setEndTime(end: Long, status: Int): TestCaseMetaData = {
    TestCaseMetaData(name, startTime, end, status, testSuite)
  }

  def setTestSuite(suite:String) : TestCaseMetaData = {
    TestCaseMetaData(name,startTime,endTime,status,Some(suite))
  }
}