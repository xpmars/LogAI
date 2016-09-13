package logai.split

import model.SplitJob

/**
  * Created by gnagar on 25/08/16.
  */
object SplitProcessor {

  def processSplit(split:SplitJob)={
    new SplitLogsAggregator(split,"").processLogs()
  }
}
