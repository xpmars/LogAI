package model

import play.api.libs.json.Json


case class TestCaseStatus(splitId:String, name:String, status:Int,startTime:Long, endTime:Long )

object TestCaseStatus {
  implicit val formatter = Json.format[TestCaseStatus]

  val TestStarted = 0
  val TestSuccessful = 1
  val TestFailed = 2
}


case class SplitDetails(splitId:String, runId:String, componentId:String, componentName:String)
