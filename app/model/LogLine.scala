package model

import play.api.libs.json.Json

/**
  * Created by gnagar on 03/08/16.
  */
case class LogLine(timestamp:Long, data:String, filename:String, testDetails: Option[TestDetails] = None)

case class TestDetails(test : String, splitId: String)

object TestDetails {
  implicit val testDetails = Json.format[TestDetails]
}

object LogLine {
  implicit val formatter = Json.format[LogLine]
}

