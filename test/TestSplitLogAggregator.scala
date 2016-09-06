import java.io.File

import actor.split.Split
import logai.{ErrorCategorization, LevenshteinDistance, LogEnricher}
import logai.reader.{LogDirIterable, LogDirReader, LogFileIterable, TestCaseMetadataReader}
import logai.split.SplitLogsAggregator
import org.joda.time.DateTime

/**
  * Created by gnagar on 25/08/16.
  */
object TestSplitLogAggregator extends App {
  //new SplitLogsAggregator(Split("122","http://qelog.hortonworks.com/log/hbase1-hb25-1471847573",""),"/Users/gnagar/work/collect/").processLogs()
  //new TestCaseMetadataReader("/Users/gnagar/work/collect/hbase1-hb25-1471847573").parseMetaData().foreach(println(_))
  val iter = new LogDirIterable("/Users/gnagar/work/collect/hbase1-hb25-1471847573/").iterator

  //  val iter = new LogFileIterable(
//    new File("/Users/gnagar/work/collect/hbase1-hb25-1471847573/service-logs/ambari-agent/10.0.0.34/ambari-agent.log")
//  ).iterator
  //println(iter.next())

  private val seq = iter
//  println(seq.size)
//  println(DateTime.now())

  val t = seq.filter(_.getOrElse("loglevel","").equals("ERROR"))

  val x = t.map(new LogEnricher().process(_)).toSeq

  val errors = new ErrorCategorization(x).categorize()

  errors.groupBy(_.getOrElse("cat","")).foreach{
    g =>
      println(g._1 + "," + g._2.head)
  }

//  grouped.foreach{
//    e => println(e._1 + "," + e._2)
//  }
//    println(grouped.size)
//  println(t.map(println(_)))

//  t.foreach(println)
//  println(t.size)
  println(DateTime.now())

//  val d = LevenshteinDistance.distance(Seq("abcd", "def" ),Seq("dsad","abcd", "asdsa"))
//  print(d)
}
