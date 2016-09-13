package logai.split

import model.SplitJob
import play.api.Logger

/**
  * Created by gnagar on 25/08/16.
  */
class SplitLogsAggregator(split: SplitJob, path: String) {

  private val outputDirPath = path + split.dir + "/"

  def processLogs() = {
    Logger.info(s"Starting logs collection for ${split._id} from dir: ${split.dir}. Logs will be placed in ${outputDirPath}")
   // collectServiceLogs()
    collectTestLogs()
    Logger.info(s"Finished logs collection for ${split._id}")
  }

  private def collectServiceLogs() = {
    //val logCollector = new LogScrapperParallel(url+"/service-logs/",split,outputDirPath + "/service-logs/")
   // logCollector.collect()
  }

  private def collectTestLogs() = {
    //val logCollector = new LogScrapperSimple(url+"/artifacts/",split,outputDirPath + "/testcaselogs/")
   // logCollector.collectLogFile("test_case_status.log")
  }

}
