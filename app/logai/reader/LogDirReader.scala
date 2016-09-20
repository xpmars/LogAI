package logai.reader

import java.io.File

import play.api.Logger

object LogDirReader {

  def readDir(inputDir: String): Seq[Map[String, Any]] = {
    val dir = new File(inputDir)
    Logger.info(s"Reading dir :$inputDir")
    dir.listFiles().flatMap {
      file =>
        if (file.isDirectory) readDir(file.getAbsolutePath)
        else if (file.getAbsolutePath.endsWith(".log")) {
          try {
            new LogFileReader(file).parse().filter(_.getOrElse("loglevel", "").equals("ERROR"))
          } catch {
            case e: Exception =>
              Logger.warn(s"Failed to read file : ${file.getName} : ${e.getMessage}")
              List[Map[String, AnyRef]]()
          }
        } else Seq[Map[String, AnyRef]]()
    }.toSeq
  }
}
