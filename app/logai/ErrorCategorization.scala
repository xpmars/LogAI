package logai

import java.util.UUID

import logai.LogKeys._
import model.LogLine
import mongo.LogsCategoryMongoRepo

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class ErrorCategorization(splitId: String, errors: Seq[Map[String, Any]], logsCategoryRepo: LogsCategoryMongoRepo) {

  private val categoryMap = collection.mutable.Map[Int, String]()

  def categorize(): Seq[Map[String, Any]] = {
    val errorsWithHash = errors.map {
      e =>
        val hash = e.get(Stacktrace) match {
          case Some(st) if (!st.toString.isEmpty) => st.hashCode()
          case _ => e.get(Message).get.hashCode()
        }

        e ++ Map(Hash -> hash)
    }

    val errorsWithUniqueHashAndOriginGrouped = errorsWithHash.groupBy(_.getOrElse("hash", "")).map {
      g => g._2.head
    }.groupBy {
      e => e.get(Origin).get
    }

    //Apply Levenshtein for similar errors
    errorsWithUniqueHashAndOriginGrouped.foreach {
      g =>

        val categoryLogsFuture: Future[List[LogLine]] = logsCategoryRepo.getByOrigin(g._1.toString)
        Await.result(categoryLogsFuture, Duration.Inf)
        val categoryLogs: Seq[Map[String, Any]] = categoryLogsFuture.value.get.get.map(_.getAsMap)

        val hashCategoryLogsMap = categoryLogs.map { l =>
          (l.get(Hash).get.asInstanceOf[Int], l)
        }.toMap

        val logsWithoutCategory = ListBuffer[Map[String, Any]]()

        val e = g._2.toSeq

        for (i <- 0 to e.size - 1) {
          val errorHash: Int = e(i).get(Hash).get.asInstanceOf[Int]
          val hashCategory = hashCategoryLogsMap.get(errorHash)
          if (!hashCategory.isEmpty) {
            //Match hash only
            categoryMap.put(errorHash, hashCategory.get.get(Category).get.toString)
          } else {
            // Match to errors against already defined categories
            var matched = false
            for (j <- 0 to categoryLogs.size - 1) {
              if (matchError(e(i), categoryLogs(j))) {
                matched = true
                categoryMap.put(errorHash, categoryLogs(j).get(Category).get.toString)
              }
            }
            if (!matched) logsWithoutCategory += e(i)

          }
        }

        for (
          i <- 0 to logsWithoutCategory.size - 1;
          j <- i + 1 to logsWithoutCategory.size - 1
        ) {
          val e1 = logsWithoutCategory(i)
          val e2 = logsWithoutCategory(j)

          if (matchError(e1, e2)) {
            val fh = e1.get(Hash).get.asInstanceOf[Int]
            val sh = e2.get(Hash).get.asInstanceOf[Int]
            val cat = categoryMap.getOrElseUpdate(fh, generateCategory())
            categoryMap.put(sh, cat)
          }
        }

        def addCategory(e: Map[String, Any]): Map[String, Any] = {
          val hash = e.get(Hash).get.asInstanceOf[Int]
          val cat = categoryMap.getOrElseUpdate(hash, generateCategory())
          e ++ Map(Category -> cat)
        }

        // Save new Categories
        val newCategories = logsWithoutCategory.map(addCategory(_)).groupBy {
          l =>
            l.get(Category).get
        }.map {
          s => LogLine(splitId, s._2.head)
        }.toSeq

        logsCategoryRepo.save(newCategories)

    }


    // Now each hash should have category
    val err = errorsWithHash.map {
      e =>
        val hash = e.get(Hash).get.asInstanceOf[Int]
        val cat = categoryMap.get(hash).get
        e ++ Map(Category -> cat)
    }

    err
  }

  private def matchError(e1: Map[String, Any], e2: Map[String, Any]): Boolean = {
    val first = (e1.getOrElse(Message, "") + " " + e1.getOrElse(Stacktrace, "").toString.replace("\n", " ")).replace(" at ", "").split(" ").toSeq
    val second = (e2.getOrElse(Message, "") + " " + e2.getOrElse(Stacktrace, "").toString.replace("\n", " ")).split(" ").toSeq
    val distance = LevenshteinDistance.distance(first, second)

    val maxLength = Math.max(first.size, second.size)

    (distance < 3) || (distance < 4 && maxLength > 8) || (distance < 8 && maxLength > 24)
  }

  private def generateCategory() = UUID.randomUUID().toString

}