package logai

import scala.collection.mutable.ListBuffer

/**
  * Created by gnagar on 02/09/16.
  */
class LogEnricher {

  val components = Set("accumulo","hive","hbase","sql","hiveserve2","hdfs","oozie","webhcat","yarn","ambari")

  import LogKeys._

  def process(log : Map[String,Any]) = {
    val filepath = log.getOrElse("path","").toString

    val originComponent = getOriginComponent(log, filepath)

    // Get related components from the stracktrace
    val message = log.getOrElse(Message,"").toString.toLowerCase
    val stacktrace = log.getOrElse(Stacktrace,"").toString.toLowerCase
    val relatedComponents = components.filter{
      c =>
        stacktrace.contains(c)
    }.toSeq

    log ++ Map(Origin -> originComponent,"rc" -> relatedComponents)
  }

  //Get origin component from file path */service-logs/{origin-component}/*
  private def getOriginComponent(log: Map[String, Any], filepath: String): String = {
    if (filepath.contains("service-logs")) {
      filepath.substring(filepath.indexOf("service-logs")).split("/")(1)
    } else {
      log.getOrElse("fn", "").toString.split("\\.").head
    }
  }
}
