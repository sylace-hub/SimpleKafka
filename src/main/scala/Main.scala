import java.util.Properties

import com.typesafe.config.ConfigFactory

import org.apache.kafka.clients.consumer.{ConsumerRecord, KafkaConsumer}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs._

import scalaj.http.{Http, HttpResponse}

import scala.collection.JavaConverters._

import java.io.File
import File.separatorChar

object Main {

  def main(args: Array[String]): Unit = {

    // Topics :
    val topics = Array("USA", "FRA", "UK", "JAP")

    // Producer
    val producerProperties = new Properties()

    // Config Kafka
    producerProperties.put("bootstrap.servers", "http://127.0.0.1:9092")
    producerProperties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    producerProperties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

    val producer = new KafkaProducer[String, String](producerProperties)

    // Consumer
    val consumerProperties = new Properties()

    // Config Kafka
    consumerProperties.put("bootstrap.servers", "http://127.0.0.1:9092")
    consumerProperties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    consumerProperties.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    consumerProperties.put("group.id", "sylace-group")

    val consumer = new KafkaConsumer[String, String](consumerProperties)
    consumer.subscribe(topics.toList.asJava)

    // Requests to News API
    val requests: scala.collection.mutable.Map[String, HttpResponse[String]] = scala.collection.mutable.Map()

    // System.setProperty("HADOOP_USER_NAME", "kos")

    for (topic <- topics) {
      requests += (topic -> Http("https://newsapi.org/v2/everything").params(Map(
        "q" -> topic,
        "from" -> "2019-04-15",
        "to" -> "2019-04-21",
        "sortBy" -> "relevancy",
        "apiKey" -> "4e86ff31690e4f9b9b65c06ae033cc28"
      )).asString)
    }

    val conf = new Configuration()
    conf.set("fs.defaultFS", "hdfs://10.0.0.218:8020")
    conf.set("fs.hdfs.impl", classOf[org.apache.hadoop.hdfs.DistributedFileSystem].getName)
    conf.set("fs.file.impl", classOf[org.apache.hadoop.fs.LocalFileSystem].getName)

    val fileSystem = FileSystem.get(conf)

    val outputDirectory = ConfigFactory.load().getString("cluster-env.app.outdir")


    while(true) {
      // Writing
      for ((k, v) <- requests) {
        val record = new ProducerRecord[String, String](k, "key", v.body)
        println("Sending record...")
        producer.send(record)
        println("Record sent")
      }

      // Reading
      val records = consumer.poll(500)
      for (record: ConsumerRecord[String, String] <- records.asScala) {
        // Create file using the topic name and the timestamp
        val path = outputDirectory + "" + record.topic() + separatorChar + record.timestamp() + ".txt"

        // Creating the writer
        val output = fileSystem.create(new Path(path))

        output.write(record.value().getBytes())
        output.close()
      }

      println("Sleeping a bit...")
      Thread.sleep(10000)
    }

    producer.close()
    consumer.close()
  }
}

