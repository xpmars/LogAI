package logai.split

import actor.split.Split

/**
  * Created by gnagar on 25/08/16.
  */
object SplitProcessor {

  def processSplit(split:Split)={
    new SplitLogsAggregator(split,"").processLogs()
  }
}
