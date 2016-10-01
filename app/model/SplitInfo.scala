package model

import play.api.libs.json.{Json, Reads}


case class SplitInfo(_id: String, splitNo:String, resultId: String, componentId: String, releaseId: String, runId: String, createdAt:Long)

object SplitInfo {
  implicit val formatter = Json.format[SplitInfo]
}