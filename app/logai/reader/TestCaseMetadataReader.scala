package logai.reader

import logai.LogAIUtils
import logai.parser.grok.GrokParser
import org.joda.time.format.DateTimeFormat

import scala.collection.mutable
import scala.io.Source

/**
  * Created by gnagar on 25/08/16.
  */
class TestCaseMetadataReader(path:String) {

  private val TestCaseStatusFile = "testcaselogs/test_case_status.log"

  private val TestKey: String = "test"
  private val DateKey: String = "date"

  val pattern1 = "%{TIMESTAMP_ISO8601:date}.*RUNNING TEST \"%{DATA:test}\".*"
  val pattern2 = "%{TIMESTAMP_ISO8601:date}.*TEST \"%{DATA:test}\" FAILED"
  val pattern3 = "%{TIMESTAMP_ISO8601:date}.*TEST \"%{DATA:test}\" PASSED"

  val parser = new GrokParser("runningTest", pattern1)
  val failedTestParser = new GrokParser("failedTest", pattern2)
  val passedTestParser = new GrokParser("passedTest", pattern3)

  val dateTime = DateTimeFormat.forPattern("YYYY-MM-dd HH:mm:ss,SSS")

  def parseMetaData() = {
    val tests = mutable.Map[String, TestCaseMetaData]()

    val filepath = LogAIUtils.sanitizePath(path) + TestCaseStatusFile

    Source.fromFile(filepath).getLines().foreach {
      line =>
        val map: Map[String, AnyRef]= parser.parse(line)
        val map2: Map[String, AnyRef] = failedTestParser.parse(line)
        val map3: Map[String, AnyRef] = passedTestParser.parse(line)

        if (!map.isEmpty) {
          val st = dateTime.parseDateTime(map.get(DateKey).get.toString).getMillis
          val name = map.get(TestKey).get.toString
          tests += ((name, TestCaseMetaData(name, st)))
        } else if (!map2.isEmpty) {
          val et = dateTime.parseDateTime(map2.get(DateKey).get.toString).getMillis
          val name = map2.get(TestKey).get.toString
          tests += ((name, tests.get(name).get.setEndTime(et, 1)))
        } else if(!map3.isEmpty){
          val et = dateTime.parseDateTime(map3.get(DateKey).get.toString).getMillis
          val name = map3.get(TestKey).get.toString
          tests += ((name, tests.get(name).get.setEndTime(et, 0)))
        }
    }
    tests.values.toSeq
  }

}

case class TestCaseMetaData(name:String, startTime:Long, endTime:Long = 0, status:Int = 0) {
  def setEndTime(end : Long, status: Int): TestCaseMetaData = {
    TestCaseMetaData(name, startTime, end, status)
  }
}