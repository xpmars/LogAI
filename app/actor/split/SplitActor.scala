package actor.split

import java.io.File

import akka.actor.{Actor, Props}
import api.QeDashboardApi
import logai.reader.LogDirIterable
import logai.split.SCPLogCollector
import logai.{AnalyzeTestCase, ErrorCategorization, LogEnricher, TestCaseToErrorMapper}
import model.{LogLine, SplitJob, SplitJobStatus}
import mongo.MongoRepo
import org.apache.commons.io.FileUtils
import play.api.{Configuration, Logger}
import reactivemongo.api.commands.UpdateWriteResult

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class SplitActor(config: Configuration, repo: MongoRepo, dashboardApi: QeDashboardApi) extends Actor {

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
      val localDir = config.underlying.getString("split.logpath")

      updateSplitJobStatus(split._id, SplitJobStatus.LOGS_COLLECTION).flatMap { result =>

        val remoteDir = config.underlying.getString("logserver.logpath") + split.dir
        val host = config.underlying.getString("logserver.host")
        val key = config.underlying.getString("logserver.key")
        new SCPLogCollector(host, remoteDir, localDir, key).collect()
        updateSplitJobStatus(split._id, SplitJobStatus.LOGS_ANALYZING)
      }.flatMap { result =>
        val errors = new LogDirIterable(dir)
          .filter(_.getOrElse("loglevel", "").equals("ERROR"))
          .map(new LogEnricher().process(_)).toSeq
        val categorizedErrors: Seq[Map[String, Any]] = new ErrorCategorization(split._id, errors, repo.logsCategoryRepo).categorize()
        updateSplitJobStatus(split._id, SplitJobStatus.TEST_CASE_MAPPING).map(result => categorizedErrors)
      }.flatMap {
        e =>
          val errorMapper = new TestCaseToErrorMapper(split._id, dir, repo.testcaseRepo)
          val errors = e.map(errorMapper.mapTests(_))
          repo.logsRepo.save(errors.map(LogLine(split._id, _))).flatMap {
            r =>
              updateSplitJobStatus(split._id, SplitJobStatus.TEST_CASE_ANALYZING)
          }.flatMap {
            result =>
              new AnalyzeTestCase(split._id, repo, errors).analyze()
          }
      }.onComplete {
        case Success(result) =>
          updateSplitJobStatus(split._id, SplitJobStatus.FINISHED)
          parent ! WorkAvalibale
          dashboardApi.saveSplitInfo(split._id)
          Logger.info(s"Split Job with id : ${split._id} completed Successfully.")

          Try(FileUtils.deleteDirectory(new File(localDir + split.dir)))
        case Failure(t) =>
          updateSplitJobStatus(split._id, SplitJobStatus.FAILED)
          parent ! WorkAvalibale
          dashboardApi.saveSplitInfo(split._id)
          Logger.error(s"Split Job with id : ${split._id} failed. ${t.getMessage}", t)
      }

    case a@_ => Logger.info("Unknown Message " + a)
  }
}

object SplitActor {
  def props(config: Configuration, repo: MongoRepo, dashboardApi: QeDashboardApi) = Props(classOf[SplitActor], config, repo, dashboardApi)
}