package logai.reader

import java.io.File
import java.util.NoSuchElementException

import logai.Utils
import logai.parser.grok.GrokParser
import play.api.Logger

import scala.io.Source

class LogFileIterable(file: File, requiredKeys: Set[String] = Set("date", "loglevel", "message")) extends Iterable[Map[String, Any]] {

  override def iterator: Iterator[Map[String, Any]] = new Iterator[Map[String, Any]] {

    private val parsers = GrokParser.parsers.values
    private var currentParser: Option[GrokParser] = None

    private val fileIterator = Source.fromFile(file).getLines()

    private var nextElement: Option[Map[String, Any]] = None

    private var currentLog = Map.empty[String, AnyRef]

    var lineCount = 0

    var parsedMessages = 0

    parseNextElement()

    import logai.LogKeys._

    private def parseNextElement(): Unit = {
      nextElement = None
      val stacktrace = new StringBuilder()

      def getStacktraceMap = if (stacktrace.isEmpty) Map() else Map(Stacktrace -> stacktrace.toString())

      try {
        while (fileIterator.hasNext && nextElement.isEmpty) {
          val line = fileIterator.next()
          lineCount += 1
          val map: Map[String, AnyRef] = parseMessage(line)
          if (!map.isEmpty) {
            if (!currentLog.isEmpty) {
              nextElement = Some(
                currentLog ++
                  Map(Filename -> file.getName, "path" -> file.getAbsolutePath,
                    Timestamp -> Utils.parseTime(currentLog.get("date").get.toString))
                  ++ getStacktraceMap)

              parsedMessages += 1
            }
            currentLog = map
            stacktrace.clear()
          } else if (!currentLog.isEmpty) {
            if (!stacktrace.isEmpty) stacktrace append "\n"
            stacktrace append line
          }
        }

        if (fileIterator.isEmpty) {
          if (!currentLog.isEmpty) {
            nextElement = Some(currentLog ++ Map(Filename -> file.getName, "path" -> file.getAbsolutePath,
              Timestamp -> Utils.parseTime(currentLog.get("date").get.toString)) ++ getStacktraceMap)
            currentLog = Map.empty
          } else if (currentParser.isEmpty && lineCount != 0) {
            onParserEmpty()
          }
        }

      } catch {
        case e: Exception =>
          Logger.warn(s"Failed to read file : ${file.getPath} : ${e.getMessage}")
      }
    }

    override def hasNext: Boolean = !nextElement.isEmpty

    override def next(): Map[String, Any] = {
      if (nextElement.isEmpty) throw new NoSuchElementException()
      val element = nextElement.get
      parseNextElement()
      element
    }

    /**
      * Called When no suitable parser found for the File
      */
    private def onParserEmpty() = {
      Logger.warn(s"No logs parsed for ${file.getPath}")
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

}
