package logai.split.scrapper

import java.io.File
import java.net.URL
import java.util.concurrent.TimeUnit

import actor.split.Split
import logai.split.LogCollector
import org.apache.commons.io.FileUtils
import org.joda.time.format.DateTimeFormat
import org.jsoup.Jsoup
import play.api.Logger

import scala.collection.mutable
import scala.concurrent.forkjoin.{ForkJoinPool, RecursiveTask}

/**
  * Created by gnagar on 25/08/16.
  */
class LogScrapperParallel(url: String,split:Split,outputDirPath:String) extends RecursiveTask[Unit] with LogCollector{

  val includeFiles: Set[String] = Set("AllTestsDebugLOG", "test_case_status.txt")
  val ignoreDirs: Set[String] = Set("src/", "target/")

  override def compute(): Unit = {
    collectlogs(url)
  }

  override def collect(): Unit = {
    val pool = new ForkJoinPool(10)
    pool.invoke(this)
    pool.shutdown()
    pool.awaitTermination(50, TimeUnit.MINUTES)
  }

  private def collectlogs(url: String): Unit = {
    try {
      val doc = Jsoup.parse(new URL(url), 30000)
      import collection.JavaConverters._
      doc.select("table").select("tr").listIterator(3).asScala.foreach {
        e =>
          val link = e.select("a").attr("href")
          if (link.endsWith(".log") || link.endsWith(".lzma") || includeFiles.contains(link)) {
            val time = e.select("td").get(2).ownText()
            val pattern = DateTimeFormat.forPattern("dd-MMM-yyyy HH:mm")
            try {
              saveFile(url, link, pattern.parseDateTime(time).getMillis)
            } catch {
              case e: Exception =>
                Logger.warn(s"Failed Downloading of file: $link", e)
            }
          }

          if (link.endsWith("/") && !ignoreDirs.contains(link)) {
            val newLink: String = url + link
            val newOutputDir = outputDirPath + link + "/"
            new LogScrapperParallel(newLink,split,newOutputDir).fork()
          }
      }
    } catch {
      case e: Exception =>
        Logger.warn(s"For split: ${split.id}.Skipping url : $url. ${e.getMessage}")
    }
  }

  private def saveFile(baseUrl: String, link: String, time: Long) = {
    val logFile = baseUrl + link
    val pathToSave: String = if (baseUrl.startsWith(url))
      outputDirPath + baseUrl.stripPrefix(url)
    else outputDirPath
    val file = new File(pathToSave + link)
    Logger.info(baseUrl + link)

    if (!file.exists() || (file.exists() && file.lastModified() < time)) {
      Logger.info(s"Writing file ${file.getAbsoluteFile}")
      FileUtils.copyURLToFile(new URL(logFile), file)
      file.setLastModified(time)
      Logger.info(s"Done ${file.getAbsoluteFile}")
    }

  }
}
