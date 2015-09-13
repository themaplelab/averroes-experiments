package averroes.experiments.stats

import java.io.PrintStream

object FreqsGenerator {
  // The benchmarks
  final val dacapo = List("antlr", "bloat", "chart", "hsqldb", "luindex", "lusearch", "pmd", "xalan")
  final val specjvm = List("compress", "db", "jack", "javac", "jess", "raytrace")
  final val benchmarks = dacapo ++ specjvm

  final val spark = "spark"
  final val doop = "doop"
  final val wala = "wala"
  
  var jre = "1.6"
  
  def intFormat(v: Int) = "%,d" format v

  def emitLibraryCallBackSummariesFor(analysis: String) = {
    val table = new PrintStream(s"tex/table-freqs-${analysis}ave-$jre.tex")
    val log = io.Source.fromFile(s"${analysis}ave.stats").getLines.toList.drop(1)

    // Emit Header
    table.println("\\begin{table}")
    table.println("  \\centering")
    table.println("  \\caption{" + s"Frequencies of extra library callback edges computed by \\${analysis}ave compared to \\${analysis} for JRE ${jre}. \\italicize{Other} methods include all methods that are encountered only in one benchmark.}")
    table.println(s"  \\label{table:freqs:" + s"${analysis}ave}:$jre")
    table.println("  \\resizebox{\\textwidth}{!}{")
    table.println("  \\begin{tabular}{lrrrrrrrrrrrrrr>{\\bfseries}r}")
    table.println("    \\toprule")
    table.println("    & " + benchmarks.map(b => s"\\rot{\\$b}").mkString(" & ") + "& \\rot{Total} \\\\")
    table.println("    \\midrule")

    for (l <- log) {
      var row = new StringBuilder("    ")
      val line = l.split("\t")
      val name = line.head

      // append method names, special treatment for "Other" and "Total"
      if (name == "Other") row append s"\\italicize{$name}"
      else if (line.head == "Total") row append s"\\boldify{$name}"
      else row append s"\\code{$name}"

      // append the values
      for ((t, b) <- line.tail zip (benchmarks :+ "Total")) {
        emit(t.toInt, b)
      }

      row append " \\\\"
      if (l != log.last) row append " \\midrule" // Midrule except for the last row
      table.println(row)

      def emit(v: Int, benchmark: String) = {
        // ignore 0 frequencies, clutters the table
        val value = if (Set("Total", "Other")(name) || v != 0) intFormat(v) else " "

        // add the key to the current results row
        if (name == "Other") row append s" & \\italicize{$value}"
        else if (name == "Total") row append s" & \\boldify{$value}"
        else row append s" & $value"
      }
    }

    // Emit Footer
    table.println("    \\bottomrule")
    table.println("  \\end{tabular}}")
    table.println("\\end{table}")
    table.close
  }
  
  def main(args: Array[String]) = {
    if (args.nonEmpty) jre = args(0)
    
    emitLibraryCallBackSummariesFor(spark)
    emitLibraryCallBackSummariesFor(doop)
    emitLibraryCallBackSummariesFor(wala)
  }
  
}