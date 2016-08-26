package model

/**
  * Created by gnagar on 16/08/16.
  */
case class TestCaseStatus(testId:String, splitId:String, name:String, status:Int,startTime:Long, endTime:Long )

case class SplitDetails(splitId:String, runId:String, componentId:String, componentName:String)
