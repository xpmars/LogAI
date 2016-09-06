name := "LogAI"

version := "1.0"

lazy val `logai` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq( jdbc , cache , ws   , specs2 % Test )

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )  

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"


libraryDependencies += "org.reactivemongo" %% "play2-reactivemongo" % "0.11.14"

libraryDependencies += "io.thekraken" % "grok" % "0.1.4"

libraryDependencies += "joda-time" % "joda-time" % "2.9.4"

libraryDependencies += "org.jsoup" % "jsoup" % "1.9.2"

libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.4"

libraryDependencies += "commons-io" % "commons-io" % "2.5"

libraryDependencies += ws

val spark = "2.0.0-preview"

libraryDependencies += "org.apache.spark"  %% "spark-core" % spark

libraryDependencies += "org.apache.spark" %% "spark-sql" % spark

libraryDependencies += "org.apache.spark" %% "spark-mllib" % spark

libraryDependencies += "com.fasterxml.jackson" % "jackson-parent" % "2.8"
