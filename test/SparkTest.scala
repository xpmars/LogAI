import logai.reader.LogDirIterable
import org.apache.spark.mllib.clustering.KMeans
import org.apache.spark.mllib.feature.{HashingTF, IDF}
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession

/**
  * Created by gnagar on 29/08/16.
  */
object SparkTest extends App{

  val sparkSession = SparkSession.builder.
    master("local[*]")
    .appName("example")
    .getOrCreate()

  val iter = new LogDirIterable("/Users/gnagar/work/collect/hbase1-hb25-1471847573/").iterator
  val errors = iter.filter(_.getOrElse("loglevel","").equals("ERROR")).toSeq

  val documents = sparkSession.sparkContext.parallelize(errors)
//    .map{
//      m =>
//        (m.getOrElse("message","").toString + " " + m.getOrElse("message","").toString).split(" ").toSeq
//    }

  val hashingTF = new HashingTF()
  val tf: RDD[Vector] = hashingTF.transform(documents)

  tf.cache()
  val idf = new IDF().fit(tf)
  val tfidf: RDD[Vector] = idf.transform(tf)

  val kmeans = new KMeans().setK(10).setSeed(1L)
  val model = kmeans.run(tfidf)

  val WSSSE = model.computeCost(tfidf)
  println(s"Within Set Sum of Squared Errors = $WSSSE")

  // Shows the result.
  println("Cluster Centers: ")
  model.clusterCenters.foreach(println)

}
