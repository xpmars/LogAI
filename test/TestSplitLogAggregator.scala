import actor.split.Split
import logai.reader.{LogDirReader, TestCaseMetadataReader}
import logai.split.SplitLogsAggregator

/**
  * Created by gnagar on 25/08/16.
  */
object TestSplitLogAggregator extends App {
  //new SplitLogsAggregator(Split("122","http://qelog.hortonworks.com/log/hbase1-hb25-1471847573",""),"/Users/gnagar/work/collect/").processLogs()
  //new TestCaseMetadataReader("/Users/gnagar/work/collect/hbase1-hb25-1471847573").parseMetaData().foreach(println(_))
  LogDirReader.readDir("/Users/gnagar/work/collect/hbase1-hb25-1471847573")
}
