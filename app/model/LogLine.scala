package model

import reactivemongo.bson.BSONObjectID

/**
  * Created by gnagar on 03/08/16.
  */
case class LogLine(_id : Option[BSONObjectID], timestamp : Long, message : String, stackTrace : Option[String], filename : String)
