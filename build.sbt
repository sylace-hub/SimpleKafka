name := "SimpleKafka"

version := "0.1"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq("com.typesafe" % "config" % "1.2.0",
  "org.apache.kafka"%"kafka-clients"%"0.10.2.1",
  "ch.qos.logback"%"logback-classic"%"1.0.13",
  "ch.qos.logback"%"logback-core"%"1.0.13",
  "org.apache.hadoop" % "hadoop-common" % "2.2.0",
  "org.apache.hadoop" % "hadoop-hdfs" % "2.2.0",
)

libraryDependencies +=  "org.scalaj" %% "scalaj-http" % "2.4.1"

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}