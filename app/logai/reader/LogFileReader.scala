package logai.reader

import java.io.File

import logai.Utils
import logai.parser.grok.GrokParser
import play.api.Logger

import scala.collection.mutable.ListBuffer
import scala.io.Source


class LogFileReader(file: File, requiredKeys: Set[String] = Set("date", "loglevel", "message")) {

  private val parsers = GrokParser.parsers.values
  private var currentParser: Option[GrokParser] = None

  /**
    * Extract features for log line. It will look for specific parser in repository.
    * If no parsers found for filetype in repo, then it will try to parse with best possible parser.
    *
    * @return Seq of Map[String,AnyRef]. Where Map represents the parsed feature of a logline.
    */
  def parse(): Seq[Map[String, Any]] = {
    val logs: ListBuffer[Map[String, Any]] = ListBuffer()
    var currentLog = Map.empty[String, AnyRef]
    val message = new StringBuilder()
    val stacktrace = new StringBuilder()
    var lineCount = 0

    import logai.LogKeys._

    def getStacktraceMap = if (stacktrace.isEmpty) Map() else Map(Stacktrace -> stacktrace.toString())

    Source.fromFile(file).getLines().foreach {
      line =>
        lineCount += 1
        val map: Map[String, AnyRef] = parseMessage(line)
        if (!map.isEmpty) {
          if (!currentLog.isEmpty) {
            logs += (currentLog ++
              Map(Filename -> file.getName, Message -> message.toString(),
                Timestamp -> Utils.parseTime(currentLog.get("date").get.toString))
              ++ getStacktraceMap)
          }
          currentLog = map
          message.clear()
          stacktrace.clear()
          message append map.get(Message).get.toString
        } else if (!message.isEmpty) {
          if (!stacktrace.isEmpty) stacktrace append "\n"
          stacktrace append line
        }
    }

    if (!currentLog.isEmpty) logs += currentLog ++ Map(Filename -> file.getName,
      Timestamp -> Utils.parseTime(currentLog.get("date").get.toString)) ++ getStacktraceMap

    if (logs.isEmpty && lineCount != 0) onParserNotFound()

    logs
  }

  /**
    * Called When no suitable parser found for the File
    */
  private def onParserNotFound() = {
    Logger.warn(s"No logs parsed for ${file.getName}")
  }

  private def parseMessage(line: String): Map[String, AnyRef] = {
    if (currentParser.isEmpty) {
      for (parser <- parsers) {
        val map = parser.parse(line)
        if (!map.isEmpty && requiredKeys.subsetOf(map.keySet)) {
          currentParser = Some(parser)
          return map
        }
      }
      Map.empty[String, AnyRef]
    } else {
      return currentParser.get.parse(line)
    }
  }
}
