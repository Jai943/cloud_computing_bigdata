import org.apache.spark.SparkContext
import org.apache.spark.SparkConf

//case classes for the two matrices M and N

@SerialVersionUID(123L)
case class M(a: Long, b: Long, c: Double) extends Serializable {}

@SerialVersionUID(123L)
case class N(d: Long, e: Long, f: Double) extends Serializable {}

//main object for the program

object Multiply {
  def main(args: Array[String]) {
    

    val conf = new SparkConf().setAppName("Multiply")
    val sc = new SparkContext(conf)

    // minimum split size for input files - 1000000 bytes
    // so that output will be in a single fiel without partitions
    sc.hadoopConfiguration.set("mapreduce.input.fileinputformat.split.minsize", "1000000")
    
    conf.set("spark.logConf", "false")
    conf.set("spark.eventLog.enabled", "false")

    // reading M and N matrix input files
    val m = sc.textFile(args(0)).map(line => {
      val a = line.split(",")
      M(a(0).toLong, a(1).toLong, a(2).toDouble)
    })

    val n = sc.textFile(args(1)).map(line => {
      val a = line.split(",")
      N(a(0).toLong, a(1).toLong, a(2).toDouble)
    })

    // Joining m and n matrices and calculating thier product
    val res = m.map(m => (m.b, (m.a, m.c))).join(n.map(n => (n.d, (n.e, n.f))))
      .map { case (b, ((a, c), (e, f))) => ((a, e), c * f) }
      .reduceByKey(_ + _)
      .sortByKey()
      .map { case ((a, e), sum) => (a, e, sum) }

    // zipping the result. prevents from modifying
    res.zipWithIndex

    // Converting into desired output format
    val printres = res.map({ case (a: Long, e: Long, sum: Double) => (a + "," + e + "," + sum) })

    // Saving to output file
    printres.saveAsTextFile(args(2));
   
    sc.stop()

  }
}
