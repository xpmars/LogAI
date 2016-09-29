package logai

import logai.parser.grok.GrokParser._
import org.joda.time.format.DateTimeFormat

import scala.io.Source

/**
  * Created by gnagar on 20/07/16.
  */
object Utils {

  private val stream = getClass.getResourceAsStream("/datepatterns")

  private val formatters = Source.fromInputStream(stream).getLines().map {
    line =>
      DateTimeFormat.forPattern(line)
  }.toSeq

  def parseTime(time : String) : Long = {
    for( formatter <- formatters) {
      try {
        return formatter.parseDateTime(time).getMillis
      } catch {
        case e:Exception => //println(e)
      }
    }
    return 0
  }

}
