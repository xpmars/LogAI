package logai.split

import actor.split.Split
import logai.split.scrapper.{LogScrapperParallel, LogScrapperSimple}
import play.api.Logger

/**
  * Created by gnagar on 25/08/16.
  */
class SplitLogsAggregator(split: Split, path: String) {

  private val url = split.logUrl

  private val outputDirPath = path + url.split("/").last + "/"

  def processLogs() = {
    Logger.info(s"Starting logs collection for ${split.id} from url: ${url}. Logs will be placed in ${outputDirPath}")
   // collectServiceLogs()
    collectTestLogs()
    Logger.info(s"Finished logs collection for ${split.id}")
  }

  private def collectServiceLogs() = {
    val logCollector = new LogScrapperParallel(url+"/service-logs/",split,outputDirPath + "/service-logs/")
    logCollector.collect()
  }

  private def collectTestLogs() = {
    val logCollector = new LogScrapperSimple(url+"/artifacts/",split,outputDirPath + "/testcaselogs/")
    logCollector.collectLogFile("test_case_status.log")
  }

}
