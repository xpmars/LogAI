package actor.split

import akka.actor.{Actor, Props}
import logai.reader.LogDirIterable
import logai.split.SCPLogCollector
import logai.{ErrorCategorization, LogEnricher, TestCaseToErrorMapper}
import model.{LogLine, SplitJob, SplitJobStatus}
import mongo.MongoRepo
import play.api.{Configuration, Logger}
import reactivemongo.api.commands.UpdateWriteResult

import scala.concurrent.Future
import scala.util.{Failure, Success}

class SplitActor(config: Configuration, repo: MongoRepo) extends Actor {

  override def preStart() = {
    context.parent ! WorkAvalibale
  }

  //TODO use proper pools
  import scala.concurrent.ExecutionContext.Implicits.global

  private def updateSplitJobStatus(id: String, splitJobStatus: SplitJobStatus.SplitJobStatus): Future[UpdateWriteResult] = {
    repo.splitJobRepo.find(id).flatMap { newSplit =>
      repo.splitJobRepo.update(newSplit.withStatus(splitJobStatus))
    }
  }

  override def receive = {
    case split: SplitJob =>

      val dir = config.underlying.getString("split.logpath") + split.dir
      val parent = context.parent

      updateSplitJobStatus(split._id, SplitJobStatus.LOGS_COLLECTION).flatMap { result =>
        //collect logs is skipped as it is taking long time
        val localDir = config.underlying.getString("split.logpath")
        val remoteDir = config.underlying.getString("logserver.logpath") + split.dir
        val host = config.underlying.getString("logserver.host")
        val key = config.underlying.getString("logserver.key")
        new SCPLogCollector(host, remoteDir, localDir, key).collect()
        updateSplitJobStatus(split._id, SplitJobStatus.LOGS_ANALYZING)
      }.flatMap { result =>
        val errors = new LogDirIterable(dir)
          .filter(_.getOrElse("loglevel", "").equals("ERROR"))
          .map(new LogEnricher().process(_)).toSeq
        val categorizedErrors: Seq[Map[String, Any]] = new ErrorCategorization(errors, repo.logsCategoryRepo).categorize()
        updateSplitJobStatus(split._id, SplitJobStatus.TEST_CASE_MAPPING).map(result => categorizedErrors)
      }.flatMap {
        e =>
          val errors = e.map(new TestCaseToErrorMapper(split._id, dir, repo.testcaseRepo).mapTests(_))
          repo.logsRepo.save(errors.map(LogLine(_)))
        //              .map(result => updateSplitJobStatus(split._id, SplitJobStatus.TEST_CASE_ANALYZING))
        //      }.map {
        //          repo.testcaseRepo.save()
        //          repo.testcaseRepo.getFailedTests(split._id).map { tests =>
        //          }
        //        Map()
      }.onComplete {
        case Success(result) =>
          updateSplitJobStatus(split._id, SplitJobStatus.FINISHED)
          parent ! WorkAvalibale
          Logger.info(s"Split Job with id : ${split._id} completed Successfully.")
        case Failure(t) =>
          updateSplitJobStatus(split._id, SplitJobStatus.FAILED)
          parent ! WorkAvalibale
          Logger.error(s"Split Job with id : ${split._id} failed. ${t.getMessage}", t)
      }

    case a@_ => Logger.info("Unknown Message " + a)
  }
}

object SplitActor {
  def props(config: Configuration, repo: MongoRepo) = Props(classOf[SplitActor], config, repo)
}