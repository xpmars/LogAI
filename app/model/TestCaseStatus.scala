package model

import play.api.libs.json.Json


case class TestCaseStatus(splitId:String, name:String, status:Int,startTime:Long, endTime:Long,suite:Option[String])

object TestCaseStatus {
  implicit val formatter = Json.format[TestCaseStatus]

  val TestStarted = 0
  val TestSuccessful = 1
  val TestFailed = 2
}

case class ErrorTestCount(splitId:String, category:String, testName:String, count:Int)

object ErrorTestCount {
  implicit val formatter = Json.format[ErrorTestCount]
}

case class CategoryCount(splitId:String, category:String, count:Int)

object CategoryCount {
  implicit val formatter = Json.format[CategoryCount]
}
