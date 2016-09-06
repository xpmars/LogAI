package logai

import scala.collection.mutable.ListBuffer

/**
  * Created by gnagar on 02/09/16.
  */
class LogEnricher {

  val components = Set("accumulo","hive","hbase","sql","hiveserve2","hdfs","oozie","webhcat","yarn","ambari")

  def process(log : Map[String,Any]) = {
    val filepath = log.getOrElse("path","").toString

    //Get origin component from file path */service-logs/{origin-component}/*
    val originComponent = if(filepath.contains("service-logs")){
      filepath.substring(filepath.indexOf("service-logs")).split("/")(1)
    } else {
      log.getOrElse("fn","").toString.split("\\.").head
    }

    // Get related components from the stracktrace
    val message = log.getOrElse("message","").toString.toLowerCase
    val stacktrace = log.getOrElse("stacktrace","").toString.toLowerCase
    val relatedComponents = components.filter{
      c =>
        stacktrace.contains(c)
    }.toSeq

    log ++ Map("origin" -> originComponent,"rc" -> relatedComponents)
  }
}
