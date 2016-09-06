package logai

import scala.collection.immutable.Iterable

/**
  * Created by gnagar on 02/09/16.
  */
class ErrorCategorization(errors: Seq[Map[String, Any]]) {

  var catCount= -1

  private def getNextCategory = {
    catCount += 1
    s"cat-$catCount"
  }

  val categoryMap = collection.mutable.Map[Int,String]()

  def categorize() : Seq[Map[String,Any]] = {
    val errorsWithHash = errors.map {
      e =>
        val hash = if (!e.getOrElse("stacktrace", "").toString.isEmpty) {
          e.getOrElse("stacktrace", "").toString.hashCode
        } else {
          e.getOrElse("message", "").toString.hashCode
        }

        e ++ Map("hash" -> hash)
    }

    val errorsWithUniqueHashAndOriginGrouped = errorsWithHash.groupBy(_.getOrElse("hash", "")).map {
      g => g._2.head
    }.groupBy {
      e => e.getOrElse("origin", "")
    }

    //Apply Levenshtein for similar errors
    errorsWithUniqueHashAndOriginGrouped.foreach {
      g =>
        val e = g._2.toSeq

        for (
          i <- 0 to e.size - 1 ;
            j <- i+1 to e.size - 1
        ) {
          val first = (e(i).getOrElse("message","") +" " + e(i).getOrElse("stacktrace","").toString.replace("\n", " ")).replace(" at ","").split(" ").toSeq
          val second = (e(j).getOrElse("message","") +" " + e(j).getOrElse("stacktrace","").toString.replace("\n", " ")).split(" ").toSeq
          val distance = LevenshteinDistance.distance(first,second)

          val maxLength = Math.max(first.size,second.size)

          if((distance < 4 && maxLength > 8) || (distance < 8 && maxLength > 24)){
//            println("*********************************")
//            println(distance)
//            println(first)
//            println(second)
            val fh = e(i).get("hash").get.asInstanceOf[Int]
            val sh = e(j).get("hash").get.asInstanceOf[Int]
            val cat = categoryMap.getOrElseUpdate(fh,getNextCategory)
            categoryMap.put(sh,cat)
          }

        }
    }

    val err = errorsWithHash.map{
      e =>
        val hash = e.get("hash").get.asInstanceOf[Int]
        val cat = categoryMap.getOrElseUpdate(hash,getNextCategory)
        e ++ Map("cat" -> cat)
    }

    err
  }

}