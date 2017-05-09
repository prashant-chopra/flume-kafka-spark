/**
  * Created by Prashant Chopra
  */

/* build.sbt
name := "Flukas"
version := "1.0"
scalaVersion := "2.10.6"
libraryDependencies += "org.apache.spark" % "spark-core_2.10" % "1.6.2"
libraryDependencies += "org.apache.spark" % "spark-sql_2.10" % "1.6.2"
libraryDependencies += "org.apache.spark" % "spark-hive_2.10" % "1.6.2"
libraryDependencies += "org.apache.spark" % "spark-streaming_2.10" % "1.6.2"
//libraryDependencies += "org.apache.spark" % "spark-streaming-flume_2.10" % "1.6.2"
//libraryDependencies += "org.apache.spark" % "spark-streaming-flume-sink_2.10" % "1.6.2"
libraryDependencies += "org.apache.spark" % "spark-streaming-kafka_2.10" % "1.6.2"
*/

/* spark submit command
spark-submit --class StreamingDepartmentAnalysis \
  --master yarn \
  --conf spark.ui.port=22231 \
  --jars "/usr/hdp/2.5.0.0-1245/spark/lib/spark-streaming_2.10-1.6.2.jar,/usr/hdp/2.5.0.0-1245/kafka/libs/spark-streaming-kafka_2.10-1.6.2.jar,/usr/hdp/2.5.0.0-1245/kafka/libs/kafka_2.10-0.8.2.1.jar,/usr/hdp/2.5.0.0-1245/kafka/libs/metrics-core-2.2.0.jar" \
  retail_2.10-1.0.jar /user/pchopra/streaming/streamingdepartmentanalysis
*/

/* flume and kafka integration configuration file
# Name the components on this agent
kandf.sources = logsource
kandf.sinks = ksink
kandf.channels = mchannel
# Describe/configure the source
kandf.sources.logsource.type = exec
kandf.sources.logsource.command = tail -F /opt/gen_logs/logs/access.log
# Describe the sink
kandf.sinks.ksink.type = org.apache.flume.sink.kafka.KafkaSink
kandf.sinks.ksink.brokerList = nn02.mycluster.com:6667
kandf.sinks.ksink.topic = kafkapc
# Use a channel which buffers events in memory
kandf.channels.mchannel.type = memory
kandf.channels.mchannel.capacity = 1000
kandf.channels.mchannel.transactionCapacity = 100
# Bind the source and sink to the channel
kandf.sources.logsource.channels = mchannel
kandf.sinks.ksink.channel = mchannel
*/

import kafka.serializer.StringDecoder
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka._
import org.apache.spark.SparkConf
import org.apache.spark.streaming.dstream.InputDStream

object StreamingDepartmentAnalysis {
  def main(args: Array[String]) {
    val sparkConf = new SparkConf().
      setAppName("DepartmentWiseCount").setMaster("yarn-client")
    val topicsSet = "kafkapc".split(",").toSet
    val kafkaParams =
      Map[String, String]("metadata.broker.list" -> "nn02.mycluster.com:6667")
    val ssc = new StreamingContext(sparkConf, Seconds(60))
    val messages: InputDStream[(String, String)] = KafkaUtils.
      createDirectStream[String, String, StringDecoder, StringDecoder](
      ssc, kafkaParams, topicsSet)

    val lines = messages.map(_._2)
    val linesFiltered = lines.filter(rec => rec.contains("GET /department/"))
    val countByDepartment = linesFiltered.
      map(rec => (rec.split(" ")(6).split("/")(2), 1)).
    //  reduceByKey(_ + _)
      reduceByKeyAndWindow((a:Int, b:Int) => (a + b), Seconds(300), Seconds(60))
    // Below function call will save the data into HDFS
    countByDepartment.saveAsTextFiles(args(0))

    ssc.start()
    ssc.awaitTermination()
  }
}
