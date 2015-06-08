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
  final val doop = "doop"
  final val wala = "wala"
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
  final lazy val intFormat = "%,d"
  //  final lazy val perFormat = "%5s"

  // keys for table of differences
  final val valueKey = "value"
  //  final val perKey = "percentage"

  // Delimiters & special characters
  final val sep = "\t"
  //  final val perChar = "\\%"

  final val data = new PrintStream("tex/data.tex")

  final val edgesFull = Map[String, String](a2a -> "application call graph edges",
    a2l -> "library call graph edges",
    l2a -> "library call back edges")

  final val base = "all-output/1/callgraphs"

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

  def emitCounts(title: String, edgeFilter: CallEdge => Boolean) = {
    println(title)
    println("=" * title.length)

    for {
      tool <- tools
      prog <- benchmarks
      benchmark = benchmarkFull(prog)
    } {
      val edges = new TextReader().readCallGraph(s"$base/$benchmark/$tool.txt.gzip").edges filter edgeFilter

      val count = edges.size
      if (prog != benchmarks.last) print(count + "\t")
      else print(count + "\n")
    }

    println
    println
  }

  def emitSoundness(title: String, edgeFilter: CallEdge => Boolean) = {
    println(title)
    println("=" * title.length)

    for {
      tool <- soundnessTools
      prog <- benchmarks
      benchmark = benchmarkFull(prog)
    } {
      val edges = new TextReader().readCallGraph(s"$base/$benchmark/$tool.txt.gzip").edges filter edgeFilter
      val dyn = new TextReader().readCallGraph(s"$base/$benchmark/dynamic.txt.gzip").edges filter edgeFilter

      val soundness = (dyn diff edges).size
      if (prog != benchmarks.last) print(soundness + "\t")
      else print(soundness + "\n")
    }

    println
    println
  }

  def emitPrecision(title: String, edgeFilter: CallEdge => Boolean) = {
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

      val precision = (ave diff svan).size
      if (prog != benchmarks.last) print(precision + "\t")
      else print(precision + "\n")
    }

    println
    println
  }

  def main(args: Array[String]) = {
    // First output the data that goes to the numbers spreadsheet
    // Edge counts
    emitCounts("Counts::A2A", a2aFilter)
    emitCounts("Counts::A2L", a2lFilter)
    emitCounts("Counts::L2A", l2aFilter)

    // Soundness
    emitSoundness("Soundness::A2A", a2aFilter)
    emitSoundness("Soundness::A2L", a2lFilter)
    emitSoundness("Soundness::L2A", l2aFilter)

    // Precision
    emitPrecision("Precision::A2A", a2aFilter)
    emitPrecision("Precision::A2L", a2lFilter)
    emitPrecision("Precision::L2A", l2aFilter)

    // Emit latex files
    //    emitSoundnessTables
    //    emitPrecisionTables
    //    emitLibraryCallBackSummaries
    //    emitCGSizeTable

    // Close streams
    data.close

    def emitCGSizeTable = {
      val table = new PrintStream(s"tex/table-cgsize.tex")
      val base = "stats"

      // Emit Header
      table.println("\\begin{table}")
      table.println("  \\centering")
      table.println("  \\caption{Comparing the size of the call graph generated by the \\ave-based tools to \\spark, \\doop, and \\wala in terms of call graph edges.}")
      table.println("  \\label{table:cgsize}")
      table.println("  \\begin{tabularx}{\\textwidth}{lRRRRRR}")
      table.println("    \\toprule")
      table.println("    & \\spark & \\sparkave & \\doop & \\doopave & \\wala & \\walaave \\\\")
      table.println("    \\cmidrule(l){2-2} \\cmidrule(l){3-3} \\cmidrule(l){4-4} \\cmidrule(l){5-5} \\cmidrule(l){6-6} \\cmidrule(l){7-7}")

      for (benchmark <- benchmarks) {
        var row = new StringBuilder("    ")
        val benchFull = benchmarkFull(benchmark)

        // add benchmark name in italics
        row append s"\\$benchmark"

        // Read the edges info
        emit(s"$spark $whole", edges(spark, false, a2a) + edges(spark, false, a2l) + edges(spark, false, l2a))
        emit(s"$spark $averroes", edges(spark, true, a2a) + edges(spark, true, a2l) + edges(spark, true, l2a))
        emit(s"$doop $whole", edges(doop, false, a2a) + edges(doop, false, a2l) + edges(doop, false, l2a))
        emit(s"$doop $averroes", edges(doop, true, a2a) + edges(doop, true, a2l) + edges(doop, true, l2a))
        emit(s"$wala $whole", edges(wala, false, a2a) + edges(wala, false, a2l) + edges(wala, false, l2a))
        emit(s"$wala $averroes", edges(wala, true, a2a) + edges(wala, true, a2l) + edges(wala, true, l2a))
        row append " \\\\"
        if (benchmark != benchmarks.last) row append " \\midrule" // Midrule except for the last row
        table.println(row)

        def emit(analysis: String, v: Int) = {
          val key = s"$analysis $benchmark $cgsize"
          val value = intFormat format v
          data.println(s"\\pgfkeyssetvalue{$key}{$value}")
          row append s" & \\pgfkeysvalueof{$key}" // add the key to the current results row
        }

        def extractEdges(log: List[String], tpe: String) = log.find(_ startsWith edgesFull(tpe)).get.split("=").last.trim.toInt
        def edges(tool: String, isAve: Boolean, tpe: String) = extractEdges(io.Source.fromFile(s"$base/${outputdir(tool, isAve)}/$benchFull/${statfile(tool, isAve)}.stats").getLines.toList, tpe)
      }

      // Emit Footer
      table.println("    \\bottomrule")
      table.println("  \\end{tabularx}")
      table.println("\\end{table}")
      table.close
    }

    def emitLibraryCallBackSummaries = {
      emitLibraryCallBackSummariesFor(spark)
      emitLibraryCallBackSummariesFor(doop)
      emitLibraryCallBackSummariesFor(wala)
    }

    def emitLibraryCallBackSummariesFor(analysis: String) = {
      val table = new PrintStream(s"tex/table-freqs-${analysis}ave.tex")
      val log = io.Source.fromFile(s"callbacks/${analysis}ave.num").getLines.toList.drop(1)
      val first = if (analysis == "sparkave") "R" else "r"

      // Emit Header
      table.println("\\begin{table}")
      table.println("  \\centering")
      table.println("  \\caption{" + s"Frequencies of extra library callback edges computed by \\${analysis}ave compared to \\${analysis}. \\italicize{Other} methods include all methods that are encountered only in one benchmark.}")
      table.println("  \\label{table:freqs:" + s"${analysis}ave}")
      //      table.println("  \\begin{tabularx}{\\textwidth}{l" + first + "rrrrrrrrrrrrr>{\\bfseries}R}")
      table.println("  \\begin{tabularx}{\\textwidth}{lRRRRRRRRRRRRRR>{\\bfseries}R}")
      table.println("    \\toprule")
      table.println("    & " + benchmarks.map(b => s"\\rot{\\small \\$b}").mkString(" & ") + "& \\rot{\\small Total} \\\\")
      table.println("    \\midrule")

      for (l <- log) {
        var row = new StringBuilder("    ")
        val line = l.split("\t")
        val name = line.head

        // append method names, special treatment for "Other" and "Total"
        if (name == "Other") row append s"\\italicize{\\small $name}"
        else if (line.head == "Total") row append s"\\boldify{\\small $name}"
        else row append s"\\codesm{$name}"

        // append the values
        for ((t, b) <- line.tail zip (benchmarks :+ "Total")) {
          emit(t.toInt, b)
        }

        row append " \\\\"
        if (l != log.last) row append " \\midrule" // Midrule except for the last row
        table.println(row)

        def emit(v: Int, benchmark: String) = {
          val key = s"$analysis $name $benchmark $callbackfreq"
          val value = intFormat format v

          // ignore 0 frequencies, clutters the table
          if (Set("Total", "Other")(name) || v != 0) data.println(s"\\pgfkeyssetvalue{$key}{$value}")

          // add the key to the current results row
          if (name == "Other") row append s" & \\italicize{\\small \\pgfkeysvalueof{$key}}"
          else if (name == "Total") row append s" & \\boldify{\\small \\pgfkeysvalueof{$key}}"
          else row append s" & \\small \\pgfkeysvalueof{$key}"
        }
      }

      // Emit Footer
      table.println("    \\bottomrule")
      table.println("  \\end{tabularx}")
      table.println("\\end{table}")
      table.close
    }

    def emitSoundnessTables = {
      //      emitSoundnessTable(a2a)
      //      emitSoundnessTable(a2l)
      //      emitSoundnessTable(l2a)
      emitSoundnessTableTotal
    }

    def emitSoundnessTable(tpe: String) = {
      val table = new PrintStream(s"tex/table-$soundness-$tpe.tex")
      val base = "stats"
      val edFull = edgesFull(tpe)

      // Emit Header
      table.println("\\begin{table}")
      table.println("  \\centering")
      table.println("  \\caption{Comparing the soundness of \\spark and \\doop when analyzing the whole program vs using \\ave with respect to " + edFull + "}")
      table.println("  \\label{table:soundness:" + tpe + "}")
      table.println("  \\begin{tabularx}{\\textwidth}{lRRRRR}")
      table.println("    \\toprule")
      table.println("    & & \\multicolumn{2}{c}{\\mathify{\\dynamic \\setdiff \\spark}} & \\multicolumn{2}{c}{\\mathify{\\dynamic \\setdiff \\doop}} \\\\")
      table.println("    \\cmidrule(l){3-4} \\cmidrule(l){5-6}")
      table.println("    & \\dynamic & \\whole & \\ave  & \\whole  & \\ave  \\\\")
      table.println("    \\cmidrule(l){2-2} \\cmidrule(l){3-4} \\cmidrule(l){5-6}")

      for (benchmark <- benchmarks) {
        var row = new StringBuilder("    ")
        val benchFull = benchmarkFull(benchmark)

        // add benchmark name in italics
        row append s"\\$benchmark"

        // Read the edges info
        emit(dynamic, edges_dyn)
        emit(s"$spark $whole", edges_spark)
        emit(s"$spark $averroes", edges_sparkave)
        emit(s"$doop $whole", edges_doop)
        emit(s"$doop $averroes", edges_doopave)
        row append " \\\\"
        if (benchmark != benchmarks.last) row append " \\midrule" // Midrule except for the last row
        table.println(row)

        def emit(analysis: String, v: Int) = {
          val key = s"$analysis $benchmark $soundness $tpe"
          val value = intFormat format v
          data.println(s"\\pgfkeyssetvalue{$key}{$value}")
          row append s" & \\pgfkeysvalueof{$key}" // add the key to the current results row
        }

        def extractEdges(log: List[String], key: String = s"unsound $edFull") = log.find(_ contains key).get.split("=").last.trim.toInt
        lazy val edges_dyn = extractEdges(io.Source.fromFile(s"$base/dynamic/$benchFull/dyn.stats").getLines.toList, edFull)
        lazy val edges_spark = extractEdges(io.Source.fromFile(s"$base/spark/$benchFull/spark.stats").getLines.toList)
        lazy val edges_sparkave = extractEdges(io.Source.fromFile(s"$base/spark-averroes/$benchFull/sparkave.stats").getLines.toList)
        lazy val edges_doop = extractEdges(io.Source.fromFile(s"$base/doop/$benchFull/doop.stats").getLines.toList)
        lazy val edges_doopave = extractEdges(io.Source.fromFile(s"$base/doop-averroes/$benchFull/doopave.stats").getLines.toList)
      }

      // Emit Footer
      table.println("    \\bottomrule")
      table.println("  \\end{tabularx}")
      table.println("\\end{table}")
      table.close
    }

    def emitSoundnessTableTotal = {
      val table = new PrintStream(s"tex/table-$soundness.tex")
      val base = "stats"

      // Emit Header
      table.println("\\begin{table}")
      table.println("  \\centering")
      table.println("  \\caption{Comparing the soundness of \\spark, \\doop, and \\wala when analyzing the whole program to using \\ave with respect to the dynamically observed call edges. The quantity \\mathify{\\dynamic \\setdiff \\spark} represents the number of edges in the static call graph computed by \\spark that are missing in the dynamic call graph. The quantities \\mathify{\\dynamic \\setdiff \\sparkave}, \\mathify{\\dynamic \\setdiff \\doop}, \\mathify{\\dynamic \\setdiff \\doopave}, \\mathify{\\dynamic \\setdiff \\wala}, and \\mathify{\\dynamic \\setdiff \\walaave} are defined similarly.}")
      table.println("  \\label{table:soundness}")
      table.println("  \\begin{tabularx}{\\textwidth}{lRRRRRRR}")
      table.println("    \\toprule")
      table.println("    & \\dynamic & \\footnotesize \\mathify{\\dynamic \\setdiff \\spark} & \\footnotesize \\mathify{\\dynamic \\setdiff \\sparkave} & \\footnotesize \\mathify{\\dynamic \\setdiff \\doop} & \\footnotesize \\mathify{\\dynamic \\setdiff \\doopave} & \\footnotesize \\mathify{\\dynamic \\setdiff \\wala} & \\footnotesize \\mathify{\\dynamic \\setdiff \\walaave} \\\\")
      table.println("    \\cmidrule(l){2-2} \\cmidrule(l){3-3} \\cmidrule(l){4-4} \\cmidrule(l){5-5} \\cmidrule(l){6-6} \\cmidrule(l){7-7} \\cmidrule(l){8-8}")

      for (benchmark <- benchmarks) {
        var row = new StringBuilder("    ")
        val benchFull = benchmarkFull(benchmark)

        // add benchmark name in italics
        row append s"\\$benchmark"

        // Read the edges info
        emit(dynamic, edges_dyn(a2a) + edges_dyn(a2l) + edges_dyn(l2a))
        emit(s"$spark $whole", edges(spark, false, a2a) + edges(spark, false, a2l) + edges(spark, false, l2a))
        emit(s"$spark $averroes", edges(spark, true, a2a) + edges(spark, true, a2l) + edges(spark, true, l2a))
        emit(s"$doop $whole", edges(doop, false, a2a) + edges(doop, false, a2l) + edges(doop, false, l2a))
        emit(s"$doop $averroes", edges(doop, true, a2a) + edges(doop, true, a2l) + edges(doop, true, l2a))
        emit(s"$wala $whole", edges(wala, false, a2a) + edges(wala, false, a2l) + edges(wala, false, l2a))
        emit(s"$wala $averroes", edges(wala, true, a2a) + edges(wala, true, a2l) + edges(wala, true, l2a))
        row append " \\\\"
        if (benchmark != benchmarks.last) row append " \\midrule" // Midrule except for the last row
        table.println(row)

        def emit(analysis: String, v: Int) = {
          val key = s"$analysis $benchmark $soundness"
          val value = intFormat format v
          data.println(s"\\pgfkeyssetvalue{$key}{$value}")
          row append s" & \\pgfkeysvalueof{$key}" // add the key to the current results row
        }

        def extractEdges(log: List[String], tpe: String) = log.find(_ contains s"unsound ${edgesFull(tpe)}").get.split("=").last.trim.toInt
        def edges_dyn(tpe: String) = io.Source.fromFile(s"$base/dynamic/$benchFull/dyn.stats").getLines.toList.find(_ contains edgesFull(tpe)).get.split("=").last.trim.toInt
        def edges(tool: String, isAve: Boolean, tpe: String) = extractEdges(io.Source.fromFile(s"$base/${outputdir(tool, isAve)}/$benchFull/${statfile(tool, isAve)}.stats").getLines.toList, tpe)
      }

      // Emit Footer
      table.println("    \\bottomrule")
      table.println("  \\end{tabularx}")
      table.println("\\end{table}")
      table.close
    }

    def emitPrecisionTables = {
      emitPrecisionTable(a2a)
      emitPrecisionTable(a2l)
      emitPrecisionTable(l2a)
      //      emitPrecisionTableTotal
    }

    def emitPrecisionTable(tpe: String) = {
      val table = new PrintStream(s"tex/table-$imprecision-$tpe.tex")
      val base = "stats"
      val edFull = edgesFull(tpe)

      // Emit Header
      table.println("\\begin{table}")
      table.println("  \\centering")
      table.println("  \\caption{Comparing the precision of using \\ave to analyzing the whole program in \\spark, \\doop, and \\wala with respect to " + edFull + ". The quantity \\mathify{\\sparkave \\setdiff \\spark} represents the number of edges in the call graph computed by \\sparkave that are missing in the call graph generated by \\spark. The quantities \\mathify{\\doopave \\setdiff \\doop} and \\mathify{\\walaave \\setdiff \\wala} are defined similarly.}")
      table.println("  \\label{table:precision:" + tpe + "}")
      table.println("  \\begin{tabularx}{\\textwidth}{lRRRRRR}")
      table.println("    \\toprule")
      table.println("    & \\spark & \\small \\mathify{\\sparkave \\setdiff \\spark} & \\doop & \\small \\mathify{\\doopave \\setdiff \\doop} & \\wala & \\small \\mathify{\\walaave \\setdiff \\wala} \\\\")
      table.println("    \\cmidrule(l){2-3} \\cmidrule(l){4-5} \\cmidrule(l){6-7}")

      for (benchmark <- benchmarks) {
        var row = new StringBuilder("    ")
        val benchFull = benchmarkFull(benchmark)

        // add benchmark name in italics
        row append s"\\$benchmark"

        // Read the edges info
        emit(s"$spark $whole", edges(spark, false))
        emit(s"$spark $averroes", edges(spark, true))
        emit(s"$doop $whole", edges(doop, false))
        emit(s"$doop $averroes", edges(doop, true))
        emit(s"$wala $whole", edges(wala, false))
        emit(s"$wala $averroes", edges(wala, true))
        row append " \\\\"
        if (benchmark != benchmarks.last) row append " \\midrule" // Midrule except for the last row
        table.println(row)

        def emit(analysis: String, v: Int) = {
          val key = s"$analysis $benchmark $soundness $tpe"
          val value = intFormat format v
          data.println(s"\\pgfkeyssetvalue{$key}{$value}")
          row append s" & \\pgfkeysvalueof{$key}" // add the key to the current results row
        }

        def extractEdges(log: List[String], key: String) = log.find(_ contains key).get.split("=").last.trim.toInt
        def edges(tool: String, isAve: Boolean) = extractEdges(io.Source.fromFile(s"$base/${outputdir(tool, isAve)}/$benchFull/${statfile(tool, isAve)}.stats").getLines.toList, (if (isAve) s"imprecise $edFull" else edFull))
      }

      // Emit Footer
      table.println("    \\bottomrule")
      table.println("  \\end{tabularx}")
      table.println("\\end{table}")
      table.close
    }
  }
}