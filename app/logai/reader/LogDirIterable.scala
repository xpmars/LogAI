package logai.reader

import java.io.File

class LogDirIterable(logDir: String) extends Iterable[Map[String, Any]] {

  override def iterator: Iterator[Map[String, Any]] = new Iterator[Map[String, Any]] {

    private val filesIterator = new File(logDir).listFiles().iterator
    private var currentIterator: Iterator[Map[String, Any]] = Iterator.empty
    private var nextElement: Option[Map[String, Any]] = None
    getNextElement()

    override def hasNext: Boolean = !nextElement.isEmpty

    private def checkFileToProcess(name:String) : Boolean = {
      name.contains(".log") || name.contains(".out")
    }

    private def getNextElement(): Unit = {
      nextElement = None
      if (currentIterator.isEmpty && filesIterator.hasNext) {
        val file = filesIterator.next()
        if (file.isDirectory) {
          currentIterator = new LogDirIterable(file.getAbsolutePath).iterator
        } else {
          if(checkFileToProcess(file.getName)){
            currentIterator = new LogFileIterable(file).iterator
          }
        }
        getNextElement()
      } else if (!currentIterator.isEmpty) {
        nextElement = Some(currentIterator.next())
      }
    }

    override def next(): Map[String, Any] = {
      if (nextElement.isEmpty) throw new NoSuchElementException()
      val element = nextElement.get
      getNextElement()
      element
    }
  }
}
