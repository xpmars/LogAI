package mongo

import javax.inject.{Inject, Singleton}

import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.Future

/**
  * Created by gnagar on 03/08/16.
  */

@Singleton
class MongoConnection @Inject() (val reactiveMongoApi: ReactiveMongoApi){
//
//  def collection(name: String): Future[JSONCollection] =
//    reactiveMongoApi.database.map(_.collection[JSONCollection](name))
}
