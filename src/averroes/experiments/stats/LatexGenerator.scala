package averroes.experiments.stats

import java.io.FileInputStream
import java.io.PrintStream
import java.text.DecimalFormat
import java.util.zip.GZIPInputStream
import scala.collection.JavaConversions.asScalaSet
import probe.TextReader
import averroes.experiments.util.ProbeUtils
import probe.CallEdge
import averroes.soot.Names
import probe.ObjectManager

object LatexGenerator {
  // The benchmarks
  final val dacapo = List("antlr", "bloat", "chart", "hsqldb", "luindex", "lusearch", "pmd", "xalan")
  final val specjvm = List("compress", "db", "jack", "javac", "jess", "raytrace")
  final val benchmarks = dacapo ++ specjvm

  // The various types of call graphs
  final val dynamic = "dynamic"
  final val spark = "spark"
  final val sparkave = s"$spark-$averroes"
  final val doop = "doop"
  final val doopave = s"$doop-$averroes"
  final val wala = "wala"
  final val walaave = s"$wala-$averroes"
  final val whole = "whole"
  final val averroes = "averroes"

  // The tools
  final val tools = List(dynamic, spark, s"$spark-$averroes", doop, s"$doop-$averroes", wala, s"$wala-$averroes")
  final val soundnessTools = tools filter { x => x != dynamic }
  final val precistionTools = tools filter { x => x.endsWith(averroes) }

  // The various types of edges
  final val a2a = "a2a"
  final val a2l = "a2l"
  final val l2a = "l2a"
  final val l2l = "l2l"
  final val total = "total"

  // Various things we measure
  final val soundness = "soundness"
  final val imprecision = "imprecision"
  final val cgsize = "call graph size"
  final val callbackfreq = "call back frequencies"

  // Formatting
  //  final lazy val floatFormat = new DecimalFormat("#,###.#")
  def intFormat(v: Int) = "%,d" format v
  //  def perFormat(v: String) = "%5s" format v

  // keys for table of differences
  final val valueKey = "value"
  //  final val perKey = "percentage"

  // Delimiters & special characters
  final val sep = "\t"
  //  final val perChar = "\\%"

  final val dataout = new PrintStream("tex/data.tex")
  final val data = collection.mutable.Map[String, Int]()

  final val edgesFull = Map[String, String](a2a -> "application call graph edges",
    a2l -> "library call graph edges",
    l2a -> "library call back edges")

  var base = "???"

  def doubleLines(str: String) = {
    val tokens = str.split(';') // should yield 1 or 2 tokens exactly
    if (tokens.size == 1) s"\\rot{\\textbf{$str}}" // one line
    else if (tokens.size == 2) s"\\rot{\\textbf{${tokens(0)}}} \\rot{\\textbf{${tokens(1)}}}" // two lines
    else throw new RuntimeException("more than 2 lines is not allowed.")
  }

  def benchmarkFull(benchmark: String) = (if (dacapo contains benchmark) "dacapo/" else "specjvm/") + benchmark
  def outputdir(tool: String, isAve: Boolean) = tool + (if (isAve) "-averroes" else "")
  def statfile(tool: String, isAve: Boolean) = tool + (if (isAve) "ave" else "")

  def a2aFilter(e: CallEdge) = e.src != ProbeUtils.LIBRARY_BLOB && e.dst != ProbeUtils.LIBRARY_BLOB
  def a2lFilter(e: CallEdge) = e.src != ProbeUtils.LIBRARY_BLOB && e.dst == ProbeUtils.LIBRARY_BLOB
  def l2aFilter(e: CallEdge) = e.src == ProbeUtils.LIBRARY_BLOB && e.dst != ProbeUtils.LIBRARY_BLOB

  def countKey(tool: String, benchmark: String, tpe: String) = s"$tool $benchmark $tpe"
  def soundnessKey(tool: String, benchmark: String, tpe: String) = s"$tool $benchmark $tpe $soundness"
  def imprecisionKey(tool: String, benchmark: String, tpe: String) = s"$tool $benchmark $tpe $imprecision"

  def emitCounts(title: String, edgeFilter: CallEdge => Boolean, tpe: String) = {
    println(title)
    println("=" * title.length)

    for {
      tool <- tools
      prog <- benchmarks
      benchmark = benchmarkFull(prog)
    } {
      val edges = new TextReader().readCallGraph(s"$base/$benchmark/$tool.txt.gzip").edges filter edgeFilter

      val count = edges.size
      data += (countKey(tool, prog, tpe) -> count)
      if (prog != benchmarks.last) print(count + "\t")
      else print(count + "\n")
    }

    println
    println
  }

  def emitSoundness(title: String, edgeFilter: CallEdge => Boolean, tpe: String) = {
    println(title)
    println("=" * title.length)

    for {
      tool <- soundnessTools
      prog <- benchmarks
      benchmark = benchmarkFull(prog)
    } {
      val edges = new TextReader().readCallGraph(s"$base/$benchmark/$tool.txt.gzip").edges filter edgeFilter
      val dyn = new TextReader().readCallGraph(s"$base/$benchmark/dynamic.txt.gzip").edges filter edgeFilter
      val diff = dyn diff edges

      val soundness = diff.size
      data += (soundnessKey(tool, prog, tpe) -> soundness)
      if (prog != benchmarks.last) print(soundness + "\t")
      else print(soundness + "\n")
    }

    println
    println
  }

  def emitPrecision(title: String, edgeFilter: CallEdge => Boolean, tpe: String) = {
    println(title)
    println("=" * title.length)

    for {
      tool <- precistionTools
      prog <- benchmarks
      benchmark = benchmarkFull(prog)
    } {
      val dyn = new TextReader().readCallGraph(s"$base/$benchmark/dynamic.txt.gzip").edges filter edgeFilter
      val ave = new TextReader().readCallGraph(s"$base/$benchmark/$tool.txt.gzip").edges filter edgeFilter
      val van = new TextReader().readCallGraph(s"$base/$benchmark/${tool.replace(s"-$averroes", "")}.txt.gzip").edges filter edgeFilter
      val svan = dyn union van
      val diff = ave diff svan

      val precision = diff.size
      data += (imprecisionKey(tool, prog, tpe) -> precision)
      if (prog != benchmarks.last) print(precision + "\t")
      else print(precision + "\n")
    }

    println
    println
  }

  def emitSoundnessTable = {
    val table = new PrintStream(s"tex/table-$soundness.tex")
    val base = "stats"

    // Emit Header
    table.println("\\begin{table}")
    table.println("  \\centering")
    table.println("  \\caption{Comparing the soundness of \\spark, \\doop, and \\wala when analyzing the whole program to using \\ave with respect to the dynamically observed call edges.}")
    table.println("  \\label{table:soundness}")
    table.println("  \\resizebox{\\textwidth}{!}{")
    table.println("  \\begin{tabular}{lrrrrrrr}")
    table.println("    \\toprule")
    table.println("    & & \\multicolumn{2}{c}{\\mathify{\\dynamic \\setdiff \\spark}} & \\multicolumn{2}{c}{\\mathify{\\dynamic \\setdiff \\doop}} & \\multicolumn{2}{c}{\\mathify{\\dynamic \\setdiff \\wala}} \\\\")
    table.println("    \\cmidrule(l){3-4} \\cmidrule(l){5-6} \\cmidrule(l){7-8}")
    table.println("    & \\dynamic & \\whole & \\ave & \\whole & \\ave & \\whole & \\ave \\\\")
    table.println("    \\cmidrule(l){2-2} \\cmidrule(l){3-4} \\cmidrule(l){5-6} \\cmidrule(l){7-8}")

    for (benchmark <- benchmarks) {
      var row = new StringBuilder("    ")

      // add benchmark name in italics
      row append s"\\$benchmark"

      // Read the edges info
      row append s" & ${intFormat(data(countKey(dynamic, benchmark, a2a)) + data(countKey(dynamic, benchmark, a2l)) + data(countKey(dynamic, benchmark, l2a)))}"
      row append s" & ${intFormat(data(soundnessKey(spark, benchmark, a2a)) + data(soundnessKey(spark, benchmark, a2l)) + data(soundnessKey(spark, benchmark, l2a)))}"
      row append s" & ${intFormat(data(soundnessKey(sparkave, benchmark, a2a)) + data(soundnessKey(sparkave, benchmark, a2l)) + data(soundnessKey(sparkave, benchmark, l2a)))}"
      row append s" & ${intFormat(data(soundnessKey(doop, benchmark, a2a)) + data(soundnessKey(doop, benchmark, a2l)) + data(soundnessKey(doop, benchmark, l2a)))}"
      row append s" & ${intFormat(data(soundnessKey(doopave, benchmark, a2a)) + data(soundnessKey(doopave, benchmark, a2l)) + data(soundnessKey(doopave, benchmark, l2a)))}"
      row append s" & ${intFormat(data(soundnessKey(wala, benchmark, a2a)) + data(soundnessKey(wala, benchmark, a2l)) + data(soundnessKey(wala, benchmark, l2a)))}"
      row append s" & ${intFormat(data(soundnessKey(walaave, benchmark, a2a)) + data(soundnessKey(walaave, benchmark, a2l)) + data(soundnessKey(walaave, benchmark, l2a)))}"
      row append " \\\\"
      if (benchmark != benchmarks.last) row append " \\midrule" // Midrule except for the last row
      table.println(row)
    }

    // Emit Footer
    table.println("    \\bottomrule")
    table.println("  \\end{tabular}}")
    table.println("\\end{table}")
    table.close
  }

  def emitPrecisionTable(tpe: String) = {
    val table = new PrintStream(s"tex/table-$imprecision-$tpe.tex")
    val edFull = edgesFull(tpe)

    // Emit Header
    table.println("\\begin{table}")
    table.println("  \\centering")
    table.println("  \\caption{Comparing the precision of using \\ave to analyzing the whole program in \\spark, \\doop, and \\wala with respect to " + edFull + ".}")
    table.println("  \\label{table:precision:" + tpe + "}")
    table.println("  \\resizebox{\\textwidth}{!}{")
    table.println("  \\begin{tabular}{lrrrrrr}")
    table.println("    \\toprule")
    table.println("    & \\spark & \\mathify{\\sparkave \\setdiff \\spark} & \\doop & \\mathify{\\doopave \\setdiff \\doop} & \\wala & \\mathify{\\walaave \\setdiff \\wala} \\\\")
    table.println("    \\cmidrule(l){2-3} \\cmidrule(l){4-5} \\cmidrule(l){6-7}")

    for (benchmark <- benchmarks) {
      var row = new StringBuilder("    ")

      // add benchmark name in italics
      row append s"\\$benchmark"

      // Read the edges info
      row append s" & ${intFormat(data(countKey(spark, benchmark, tpe)))}"
      row append s" & ${intFormat(data(imprecisionKey(sparkave, benchmark, tpe)))}"
      row append s" & ${intFormat(data(countKey(doop, benchmark, tpe)))}"
      row append s" & ${intFormat(data(imprecisionKey(doopave, benchmark, tpe)))}"
      row append s" & ${intFormat(data(countKey(wala, benchmark, tpe)))}"
      row append s" & ${intFormat(data(imprecisionKey(walaave, benchmark, tpe)))}"
      row append " \\\\"
      if (benchmark != benchmarks.last) row append " \\midrule" // Midrule except for the last row
      table.println(row)
    }

    // Emit Footer
    table.println("    \\bottomrule")
    table.println("  \\end{tabular}}")
    table.println("\\end{table}")
    table.close
  }

  def main(args: Array[String]) = {
    // base directory
    base = if (args.nonEmpty) s"${args(0)}/callgraphs" else "all-output/1/callgraphs"

    // First output the data that goes to the numbers spreadsheet
    // Edge counts
    emitCounts("Counts::A2A", a2aFilter, a2a)
    emitCounts("Counts::A2L", a2lFilter, a2l)
    emitCounts("Counts::L2A", l2aFilter, l2a)

    // Soundness
    emitSoundness("Soundness::A2A", a2aFilter, a2a)
    emitSoundness("Soundness::A2L", a2lFilter, a2l)
    emitSoundness("Soundness::L2A", l2aFilter, l2a)

    // Precision
    emitPrecision("Precision::A2A", a2aFilter, a2a)
    emitPrecision("Precision::A2L", a2lFilter, a2l)
    emitPrecision("Precision::L2A", l2aFilter, l2a)

    //    data.keys.foreach { k => println(k + " :: " + data(k)) }

    // Emit latex files
    emitSoundnessTable
    emitPrecisionTable(a2a)
    emitPrecisionTable(a2l)
    emitPrecisionTable(l2a)
    
    //    emitLibraryCallBackSummaries
    //    emitCGSizeTable

    // Close streams
    dataout.close

    //    def emitCGSizeTable = {
    //      val table = new PrintStream(s"tex/table-cgsize.tex")
    //      val base = "stats"
    //
    //      // Emit Header
    //      table.println("\\begin{table}")
    //      table.println("  \\centering")
    //      table.println("  \\caption{Comparing the size of the call graph generated by the \\ave-based tools to \\spark, \\doop, and \\wala in terms of call graph edges.}")
    //      table.println("  \\label{table:cgsize}")
    //      table.println("  \\begin{tabularx}{\\textwidth}{lRRRRRR}")
    //      table.println("    \\toprule")
    //      table.println("    & \\spark & \\sparkave & \\doop & \\doopave & \\wala & \\walaave \\\\")
    //      table.println("    \\cmidrule(l){2-2} \\cmidrule(l){3-3} \\cmidrule(l){4-4} \\cmidrule(l){5-5} \\cmidrule(l){6-6} \\cmidrule(l){7-7}")
    //
    //      for (benchmark <- benchmarks) {
    //        var row = new StringBuilder("    ")
    //        val benchFull = benchmarkFull(benchmark)
    //
    //        // add benchmark name in italics
    //        row append s"\\$benchmark"
    //
    //        // Read the edges info
    //        emit(s"$spark $whole", edges(spark, false, a2a) + edges(spark, false, a2l) + edges(spark, false, l2a))
    //        emit(s"$spark $averroes", edges(spark, true, a2a) + edges(spark, true, a2l) + edges(spark, true, l2a))
    //        emit(s"$doop $whole", edges(doop, false, a2a) + edges(doop, false, a2l) + edges(doop, false, l2a))
    //        emit(s"$doop $averroes", edges(doop, true, a2a) + edges(doop, true, a2l) + edges(doop, true, l2a))
    //        emit(s"$wala $whole", edges(wala, false, a2a) + edges(wala, false, a2l) + edges(wala, false, l2a))
    //        emit(s"$wala $averroes", edges(wala, true, a2a) + edges(wala, true, a2l) + edges(wala, true, l2a))
    //        row append " \\\\"
    //        if (benchmark != benchmarks.last) row append " \\midrule" // Midrule except for the last row
    //        table.println(row)
    //
    //        def emit(analysis: String, v: Int) = {
    //          val key = s"$analysis $benchmark $cgsize"
    //          val value = intFormat format v
    //          dataout.println(s"\\pgfkeyssetvalue{$key}{$value}")
    //          row append s" & \\pgfkeysvalueof{$key}" // add the key to the current results row
    //        }
    //
    //        def extractEdges(log: List[String], tpe: String) = log.find(_ startsWith edgesFull(tpe)).get.split("=").last.trim.toInt
    //        def edges(tool: String, isAve: Boolean, tpe: String) = extractEdges(io.Source.fromFile(s"$base/${outputdir(tool, isAve)}/$benchFull/${statfile(tool, isAve)}.stats").getLines.toList, tpe)
    //      }
    //
    //      // Emit Footer
    //      table.println("    \\bottomrule")
    //      table.println("  \\end{tabularx}")
    //      table.println("\\end{table}")
    //      table.close
    //    }

    //    def emitLibraryCallBackSummaries = {
    //      emitLibraryCallBackSummariesFor(spark)
    //      emitLibraryCallBackSummariesFor(doop)
    //      emitLibraryCallBackSummariesFor(wala)
    //    }

    //    def emitLibraryCallBackSummariesFor(analysis: String) = {
    //      val table = new PrintStream(s"tex/table-freqs-${analysis}ave.tex")
    //      val log = io.Source.fromFile(s"callbacks/${analysis}ave.num").getLines.toList.drop(1)
    //      val first = if (analysis == "sparkave") "R" else "r"
    //
    //      // Emit Header
    //      table.println("\\begin{table}")
    //      table.println("  \\centering")
    //      table.println("  \\caption{" + s"Frequencies of extra library callback edges computed by \\${analysis}ave compared to \\${analysis}. \\italicize{Other} methods include all methods that are encountered only in one benchmark.}")
    //      table.println("  \\label{table:freqs:" + s"${analysis}ave}")
    //      //      table.println("  \\begin{tabularx}{\\textwidth}{l" + first + "rrrrrrrrrrrrr>{\\bfseries}R}")
    //      table.println("  \\begin{tabularx}{\\textwidth}{lRRRRRRRRRRRRRR>{\\bfseries}R}")
    //      table.println("    \\toprule")
    //      table.println("    & " + benchmarks.map(b => s"\\rot{\\small \\$b}").mkString(" & ") + "& \\rot{\\small Total} \\\\")
    //      table.println("    \\midrule")
    //
    //      for (l <- log) {
    //        var row = new StringBuilder("    ")
    //        val line = l.split("\t")
    //        val name = line.head
    //
    //        // append method names, special treatment for "Other" and "Total"
    //        if (name == "Other") row append s"\\italicize{\\small $name}"
    //        else if (line.head == "Total") row append s"\\boldify{\\small $name}"
    //        else row append s"\\codesm{$name}"
    //
    //        // append the values
    //        for ((t, b) <- line.tail zip (benchmarks :+ "Total")) {
    //          emit(t.toInt, b)
    //        }
    //
    //        row append " \\\\"
    //        if (l != log.last) row append " \\midrule" // Midrule except for the last row
    //        table.println(row)
    //
    //        def emit(v: Int, benchmark: String) = {
    //          val key = s"$analysis $name $benchmark $callbackfreq"
    //          val value = intFormat format v
    //
    //          // ignore 0 frequencies, clutters the table
    //          if (Set("Total", "Other")(name) || v != 0) dataout.println(s"\\pgfkeyssetvalue{$key}{$value}")
    //
    //          // add the key to the current results row
    //          if (name == "Other") row append s" & \\italicize{\\small \\pgfkeysvalueof{$key}}"
    //          else if (name == "Total") row append s" & \\boldify{\\small \\pgfkeysvalueof{$key}}"
    //          else row append s" & \\small \\pgfkeysvalueof{$key}"
    //        }
    //      }
    //
    //      // Emit Footer
    //      table.println("    \\bottomrule")
    //      table.println("  \\end{tabularx}")
    //      table.println("\\end{table}")
    //      table.close
    //    }

    //    def emitSoundnessTables = {
    //      emitSoundnessTable(a2a)
    //      emitSoundnessTable(a2l)
    //      emitSoundnessTable(l2a)
    //      emitSoundnessTableTotal
    //    }
  }
}