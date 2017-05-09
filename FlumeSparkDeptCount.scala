/**
  * Created by Prashant Chopra.
  */

/* build.sbt
name := "FlumeSpark"

version := "1.0"

scalaVersion := "2.10.6"

libraryDependencies += "org.apache.spark" % "spark-core_2.10" % "1.6.2"
libraryDependencies += "org.apache.spark" % "spark-sql_2.10" % "1.6.2"
libraryDependencies += "org.apache.spark" % "spark-hive_2.10" % "1.6.2"
libraryDependencies += "org.apache.spark" % "spark-streaming_2.10" % "1.6.2"
libraryDependencies += "org.apache.spark" % "spark-streaming-flume_2.10" % "1.6.2"
libraryDependencies += "org.apache.spark" % "spark-streaming-flume-sink_2.10" % "1.6.2"
*/

/* spark submit command
// Build jar file, ship it to cluster, make sure jars files are available as specified under jars
// Then run spark-submit
spark-submit --class FlumeSparkDeptCount \
  --master yarn \
  --conf spark.ui.port=22231 \
  --jars "/usr/hdp/2.5.0.0-1245/spark/lib/spark-streaming-flume_2.10-1.6.2.jar,/usr/hdp/2.5.0.0-1245/spark/lib/spark-streaming_2.10-1.6.2.jar,/usr/hdp/2.5.0.0-1245/flume/lib/commons-lang3-3.5.jar,/usr/hdp/2.5.0.0-1245/spark/lib/spark-streaming-flume-sink_2.10-1.6.2.jar,/usr/hdp/2.5.0.0-1245/flume/lib/flume-ng-sdk-1.5.2.2.5.0.0-1245.jar" \
  retail_2.10-1.0.jar
*/

import org.apache.spark.SparkConf
import org.apache.spark.streaming._
import org.apache.spark.streaming.dstream.ReceiverInputDStream
import org.apache.spark.streaming.flume._

object FlumeSparkDeptCount {
  def main(args: Array[String]): Unit = {
    val batchInterval = Seconds(30)
    val sparkConf = new SparkConf().setAppName("FlumePollingEventCount").setMaster("yarn-client")
    val ssc = new StreamingContext(sparkConf, batchInterval)
    val host = "gw01.itversity.com"
    val port = 19999

    val stream: ReceiverInputDStream[SparkFlumeEvent] = FlumeUtils.
      createPollingStream(ssc, host, port)
    stream.map(e => new String(e.event.getBody.array())).
      filter(rec => rec.contains("GET /department/")).
      map(rec => (rec.split(" ")(6).split("/")(2), 1)).
      reduceByKey(_ + _).
      print()

    ssc.start()
    ssc.awaitTermination()

  }

}
