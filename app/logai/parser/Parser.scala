package logai.parser

/**
  * Created by gnagar on 20/07/16.
  */
trait Parser {

  def getName() : String

  def parse(line : String) : Map[String,AnyRef]
}
