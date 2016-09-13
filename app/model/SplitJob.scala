package model

import model.SplitJobStatus.SplitJobStatus
import org.joda.time.DateTime
import play.api.libs.json.{Json, Reads}


case class SplitJob(_id:String, dir :String, status:Option[SplitJobStatus],lastStatusUpdated:Option[Long] ,history: Option[Seq[SplitJobHistory]]){
  def withStatus(status:SplitJobStatus) = {
    if(this.status.isEmpty) {
      SplitJob(_id,dir,Some(status), Some(DateTime.now().getMillis),Some(Nil))
    } else {
      val updatedHistory =  Seq(SplitJobHistory(this.status.get,this.lastStatusUpdated.get,DateTime.now().getMillis)) ++  history.get
      SplitJob(_id,dir,Some(status),Some(DateTime.now().getMillis),Some(updatedHistory))
    }
  }
}

case class SplitJobHistory(status: SplitJobStatus, start:Long, end: Long)

object SplitJob {
  implicit val splitReads = Reads.enumNameReads(SplitJobStatus)
  implicit val splitHistoryFormatter = Json.format[SplitJobHistory]
  implicit val formatter = Json.format[SplitJob]
}


object SplitJobStatus extends Enumeration {
  type SplitJobStatus = Value
  val QUEUED,LOGS_COLLECTION,LOGS_ANALYZING,TETS_CASE_MAPPING,FINISHED,FAILED = Value
}
