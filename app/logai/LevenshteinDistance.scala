package logai

import spire.syntax.cfor._

/**
  * Created by gnagar on 02/09/16.
  */
object LevenshteinDistance {

  def distance[T](a:Seq[T],b: Seq[T]): Int ={
    var row0 = new Array[Int](b.length + 1)
    var row1 = new Array[Int](b.length + 1)

    cfor(0)(_ < row0.length, _ + 1)(j => row0(j) = j)

    cfor(0)(_ < a.length, _ + 1) { i =>
      row1(0) = i + 1
      val c = a(i)
      cfor(1)(_ < row1.length, _ + 1) { j =>
        val d = row0(j - 1) + (if (c == b(j - 1)) 0 else 1)
        val h = row1(j - 1) + 1
        val v = row0(j) + 1

        row1(j) = if (d < h) {
          if (v < d) v else d
        } else {
          if (v < h) v else h
        }
      }

      var tmp = row0; row0 = row1; row1 = tmp
    }

    row0(b.length)
  }
}
