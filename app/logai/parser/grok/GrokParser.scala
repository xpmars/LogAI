package logai.parser.grok

import logai.parser.Parser
import oi.thekraken.grok.api.Grok

import scala.io.Source

/**
  * Created by gnagar on 20/07/16.
  */
class GrokParser(name:String, pattern: String) extends Parser {

  val grok = Grok.create("patterns/patterns")
  grok.compile(pattern, true)

  def parse(line : String) = {
    val gm = grok.`match`(line)
    gm.captures()
    import collection.JavaConverters._
    gm.toMap.asScala.toMap
  }

  override def getName(): String = name
}

object GrokParser {

  val stream = getClass.getResourceAsStream("/logpatterns")
  val parsers = Source.fromInputStream(stream).getLines().map{
    line =>
      val spaceIndex = line.indexOf(" ")
      val name = line.substring(0, spaceIndex)
      val pattern = line.substring(spaceIndex + 1)
      (name, new GrokParser(name, pattern))
  }.toMap

  def getParser(name:String): GrokParser = {
    parsers.get(name).get
  }
}
