package model

import play.api.libs.json.Json

import logai.LogKeys._

case class LogLine(timestamp: Long, splitId: String, message: String, filename: String, stacktrace: Option[String],
                   hash: Int, origin: String, category: String,
                   testName: Option[String] = None, testStatus: Option[Int] = None, testSuite:Option[String]=None) {

  def getAsMap = {

    val stMap = stacktrace.map(st => Map(Stacktrace -> st)).getOrElse(Map())

    Map(Timestamp -> timestamp, Message -> message, Filename -> filename,
      Hash -> hash, Origin -> origin, Category -> category) ++ stMap
  }
}

case class TestDetails(test: String, splitId: String)

object TestDetails {
  implicit val testDetails = Json.format[TestDetails]
}

object LogLine {
  implicit val formatter = Json.format[LogLine]

  def apply(splitId: String, log: Map[String, Any]): LogLine = {
    val timestamp = log.get(Timestamp).get.asInstanceOf[Long]
    val message = log.get(Message).get.asInstanceOf[String]
    val filename = log.get(Filename).get.asInstanceOf[String]
    val stacktrace = log.get(Stacktrace).map(_.toString)

    val hash = log.get(Hash).get.asInstanceOf[Int]
    val origin = log.get(Origin).get.asInstanceOf[String]
    val category = log.get(Category).get.asInstanceOf[String]

    val testName = log.get(TestName).map(_.toString)
    val testStatus = log.get(TestStatus).map(_.asInstanceOf[Int])
    val testSuite = log.get(TestSuite).map(_.asInstanceOf[String])

    LogLine(timestamp, splitId, message, filename, stacktrace, hash, origin, category, testName, testStatus,testSuite)
  }
}

