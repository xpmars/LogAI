package logai

import org.joda.time.format.DateTimeFormat

/**
  * Created by gnagar on 20/07/16.
  */
object Utils {

  val formatters = Seq(
    DateTimeFormat.forPattern("dd MMM YYYY HH:mm:ss,SSS"),
    DateTimeFormat.forPattern("YYYY-MM-dd HH:mm:ss,SSS")
  )

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
