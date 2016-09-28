package mongo

import play.modules.reactivemongo.ReactiveMongoApi
import scala.concurrent.ExecutionContext


class MongoRepo(val reactiveMongoApi: ReactiveMongoApi)(implicit ec: ExecutionContext) {
  val logsRepo = new LogsMongoRepo(reactiveMongoApi)
  val splitJobRepo = new SplitMongoRepo(reactiveMongoApi)
  val logsCategoryRepo = new LogsCategoryMongoRepo(reactiveMongoApi)
  val testcaseRepo = new TestCaseMongoRepo(reactiveMongoApi)
  val spiltInfoRepo = new SplitInfoMongoRepo(reactiveMongoApi)
}
