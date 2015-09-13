package averroes.experiments.stats

import java.io.FileInputStream
import java.io.PrintStream
import java.text.DecimalFormat
import java.util.zip.GZIPInputStream
import scala.collection.JavaConversions.asScalaSet
import probe.TextReader
import averroes.experiments.util.ProbeUtils
import probe.CallEdge
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
  final val wholedyn = "wholedyn"
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

  lazy val freqsout = Map[String, PrintStream](
    sparkave -> new PrintStream(s"tex/sparkave-$jre.stats"),
    doopave -> new PrintStream(s"tex/doopave-$jre.stats"),
    walaave -> new PrintStream(s"tex/walaave-$jre.stats"))

  final val data = collection.mutable.Map[String, Int]()
  final val freqs = Map[String, collection.mutable.Map[String, Map[String, Int]]](
    spark + dynamic -> collection.mutable.Map[String, Map[String, Int]]().withDefaultValue(Map[String, Int]()),
    doop + dynamic -> collection.mutable.Map[String, Map[String, Int]]().withDefaultValue(Map[String, Int]()),
    wala + dynamic -> collection.mutable.Map[String, Map[String, Int]]().withDefaultValue(Map[String, Int]()),
    sparkave + dynamic -> collection.mutable.Map[String, Map[String, Int]]().withDefaultValue(Map[String, Int]()),
    doopave + dynamic -> collection.mutable.Map[String, Map[String, Int]]().withDefaultValue(Map[String, Int]()),
    walaave + dynamic -> collection.mutable.Map[String, Map[String, Int]]().withDefaultValue(Map[String, Int]()),
    sparkave -> collection.mutable.Map[String, Map[String, Int]]().withDefaultValue(Map[String, Int]()),
    doopave -> collection.mutable.Map[String, Map[String, Int]]().withDefaultValue(Map[String, Int]()),
    walaave -> collection.mutable.Map[String, Map[String, Int]]().withDefaultValue(Map[String, Int]()))

  final val edgesFull = Map[String, String](a2a -> "application call graph edges",
    a2l -> "library call graph edges",
    l2a -> "library call back edges")

  var base = "???"
  var jre = "1.6"

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
      tool <- tools.tail
      prog <- benchmarks
      benchmark = benchmarkFull(prog)
    } {
      val dyn = new TextReader().readCallGraph(s"$base/$benchmark/dynamic.txt.gzip").edges filter edgeFilter
      val cg = new TextReader().readCallGraph(s"$base/$benchmark/$tool.txt.gzip").edges filter edgeFilter
      
      val diff = cg diff dyn

      // process the call back frequencies
      if (tpe == l2a) {
        val cbs = collection.mutable.Map[String, Int]().withDefaultValue(0)
        diff.foreach(d => cbs(d.dst.name) += 1)
        cbs.keys.foreach(m => freqs(tool + dynamic)(m) += (prog -> cbs(m)))
      }

      val precision = diff.size
      data += (imprecisionKey(tool + dynamic, prog, tpe) -> precision)
      if (prog != benchmarks.last) print(precision + "\t")
      else print(precision + "\n")
    }

    println
    println
  }
  
    def emitPrecisionSoundComparison(title: String, edgeFilter: CallEdge => Boolean, tpe: String) = {
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

      // process the call back frequencies
      if (tpe == l2a) {
        val cbs = collection.mutable.Map[String, Int]().withDefaultValue(0)
        diff.foreach(d => cbs(d.dst.name) += 1)
        cbs.keys.foreach(m => freqs(tool)(m) += (prog -> cbs(m)))
      }

      val precision = diff.size
      data += (imprecisionKey(tool, prog, tpe) -> precision)
      if (prog != benchmarks.last) print(precision + "\t")
      else print(precision + "\n")
    }

    println
    println
  }

  def emitSoundnessTable = {
    val table = new PrintStream(s"tex/table-$soundness-jre$jre.tex")
    
    // Emit Header
    table.println("\\begin{table}")
    table.println("  \\centering")
    table.println(s"  \\caption{Comparing the soundness of \\spark, \\doop, and \\wala when analyzing the whole program to using \\ave with respect to the dynamically observed call edges for JRE $jre.}")
    table.println(s"  \\label{table:soundness:$jre}")
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
  
  def emitPrecisionTable = {
    val table = new PrintStream(s"tex/table-$imprecision-$jre.tex")
    val base = "stats"

    // Emit Header
    table.println("\\begin{table}")
    table.println("  \\centering")
    table.println(s"  \\caption{Comparing the precision of using \\ave to analyzing the whole program in \\spark, \\doop, and \\wala for JRE $jre.}")
    table.println(s"  \\label{table:precision:$jre}")
    table.println("  \\resizebox{\\textwidth}{!}{")
    table.println("  \\begin{tabular}{lrrrrrr}")
    table.println("    \\toprule")
    table.println("    & \\multicolumn{2}{c}{\\spark} & \\multicolumn{2}{c}{\\doop} & \\multicolumn{2}{c}{\\wala} \\\\")
    table.println("    \\cmidrule(l){2-3} \\cmidrule(l){4-5} \\cmidrule(l){6-7}")
    table.println("    & \\whole & \\mathify{\\ave \\setdiff \\whole} & \\whole & \\mathify{\\ave \\setdiff \\whole} & \\whole & \\mathify{\\ave \\setdiff \\whole} \\\\")
    table.println("    \\cmidrule(l){2-3} \\cmidrule(l){4-5} \\cmidrule(l){6-7}")

    for (benchmark <- benchmarks) {
      var row = new StringBuilder("    ")

      // add benchmark name in italics
      row append s"\\$benchmark"

      // Read the edges info
      row append s" & ${intFormat(data(countKey(spark, benchmark, a2a)) + data(countKey(spark, benchmark, a2l)) + data(countKey(spark, benchmark, l2a)))}"
      row append s" & ${intFormat(data(imprecisionKey(sparkave, benchmark, a2a)) + data(imprecisionKey(sparkave, benchmark, a2l)) + data(imprecisionKey(sparkave, benchmark, l2a)))}"
      row append s" & ${intFormat(data(countKey(doop, benchmark, a2a)) + data(countKey(doop, benchmark, a2l)) + data(countKey(doop, benchmark, l2a)))}"
      row append s" & ${intFormat(data(imprecisionKey(doopave, benchmark, a2a)) + data(imprecisionKey(doopave, benchmark, a2l)) + data(imprecisionKey(doopave, benchmark, l2a)))}"
      row append s" & ${intFormat(data(countKey(wala, benchmark, a2a)) + data(countKey(wala, benchmark, a2l)) + data(countKey(wala, benchmark, l2a)))}"
      row append s" & ${intFormat(data(imprecisionKey(walaave, benchmark, a2a)) + data(imprecisionKey(walaave, benchmark, a2l)) + data(imprecisionKey(walaave, benchmark, l2a)))}"
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
  
  def emitCGSizeTable = {
    val table = new PrintStream(s"tex/table-cgsize-$jre.tex")
    val base = "stats"

    // Emit Header
    table.println("\\begin{table}")
    table.println("  \\centering")
    table.println(s"  \\caption{Comparing the size of the call graph generated by the \\ave-based tools to \\spark, \\doop, and \\wala in terms of call graph edges for JRE $jre.}")
    table.println(s"  \\label{table:cgsize:$jre}")
    table.println("  \\resizebox{\\textwidth}{!}{")
    table.println("  \\begin{tabular}{lrrrrrr}")
    table.println("    \\toprule")
    table.println("    & \\multicolumn{2}{c}{\\spark} & \\multicolumn{2}{c}{\\doop} & \\multicolumn{2}{c}{\\wala} \\\\")
    table.println("    \\cmidrule(l){2-3} \\cmidrule(l){4-5} \\cmidrule(l){6-7}")
    table.println("    & \\whole & \\ave & \\whole & \\ave & \\whole & \\ave \\\\")
    table.println("    \\cmidrule(l){2-3} \\cmidrule(l){4-5} \\cmidrule(l){6-7}")

    for (benchmark <- benchmarks) {
      var row = new StringBuilder("    ")
      val benchFull = benchmarkFull(benchmark)

      // add benchmark name in italics
      row append s"\\$benchmark"

      // Read the edges info
      row append s" & ${intFormat(data(countKey(spark, benchmark, a2a)) + data(countKey(spark, benchmark, a2l)) + data(countKey(spark, benchmark, l2a)))}"
      row append s" & ${intFormat(data(countKey(sparkave, benchmark, a2a)) + data(countKey(sparkave, benchmark, a2l)) + data(countKey(sparkave, benchmark, l2a)))}"
      row append s" & ${intFormat(data(countKey(doop, benchmark, a2a)) + data(countKey(doop, benchmark, a2l)) + data(countKey(doop, benchmark, l2a)))}"
      row append s" & ${intFormat(data(countKey(doopave, benchmark, a2a)) + data(countKey(doopave, benchmark, a2l)) + data(countKey(doopave, benchmark, l2a)))}"
      row append s" & ${intFormat(data(countKey(wala, benchmark, a2a)) + data(countKey(wala, benchmark, a2l)) + data(countKey(wala, benchmark, l2a)))}"
      row append s" & ${intFormat(data(countKey(walaave, benchmark, a2a)) + data(countKey(walaave, benchmark, a2l)) + data(countKey(walaave, benchmark, l2a)))}"
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
    if (args.nonEmpty) jre = args(0)
    base = s"all-output-$jre/1/callgraphs"

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
    
    // Precision vs corrected Whole
    emitPrecisionSoundComparison("PrecisionSound::A2A", a2aFilter, a2a)
    emitPrecisionSoundComparison("PrecisionSound::A2L", a2lFilter, a2l)
    emitPrecisionSoundComparison("PrecisionSound::L2A", l2aFilter, l2a)

    // Emit latex files
    emitSoundnessTable
    emitPrecisionTable
    emitCGSizeTable

    freqs(sparkave).foreach { m => freqsout(sparkave).println(m._1 + "\t" + benchmarks.map(m._2.getOrElse(_, 0)).mkString("\t")) }
    freqs(doopave).foreach { m => freqsout(doopave).println(m._1 + "\t" + benchmarks.map(m._2.getOrElse(_, 0)).mkString("\t")) }
    freqs(walaave).foreach { m => freqsout(walaave).println(m._1 + "\t" + benchmarks.map(m._2.getOrElse(_, 0)).mkString("\t")) }

    // Close streams
    freqsout.foreach(_._2.close)
  }
}