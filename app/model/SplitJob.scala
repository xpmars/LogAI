package model

import model.SplitJobStatus.SplitJobStatus
import org.joda.time.DateTime
import play.api.libs.json.{Json, Reads}


case class SplitJob(_id: String, dir: String, status: Option[SplitJobStatus], lastStatusUpdated: Option[Long], history: Option[Seq[SplitJobHistory]],comments:Option[String]) {
  def withStatus(newStatus: SplitJobStatus,comments:Option[String]) = {

    val updatedHistory =
      for {
        _status <- status
        _lastUpdated <- lastStatusUpdated
        _history <- history
      } yield Seq(SplitJobHistory(_status, _lastUpdated, DateTime.now().getMillis)) ++ _history

    SplitJob(_id, dir, Some(newStatus), Some(DateTime.now().getMillis), Some(updatedHistory.getOrElse(Nil)),comments)
  }
}

case class SplitJobHistory(status: SplitJobStatus, start: Long, end: Long)

object SplitJob {
  implicit val splitReads = Reads.enumNameReads(SplitJobStatus)
  implicit val splitHistoryFormatter = Json.format[SplitJobHistory]
  implicit val formatter = Json.format[SplitJob]
}


object SplitJobStatus extends Enumeration {
  type SplitJobStatus = Value
  val QUEUED, LOGS_COLLECTION, LOGS_ANALYZING, TEST_CASE_MAPPING, TEST_CASE_ANALYZING, FINISHED, FAILED = Value
}
