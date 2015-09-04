package averroes.experiments.stats

import java.text.DecimalFormat

import averroes.experiments.util.Math

object ErrorBarsGenerator {
  // The benchmarks
  final val dacapo = List("antlr", "bloat", "chart", "hsqldb", "luindex", "lusearch", "pmd", "xalan")
  final val specjvm = List("compress", "db", "jack", "javac", "jess", "raytrace")
  final val benchmarks = dacapo ++ specjvm

  def main(args: Array[String]) = {
    emitAverroesTime

    emitTime(tool = "Spark", isAve = false)
    emitTime(tool = "Spark", isAve = true)
    emitDoopTime(isAve = false)
    emitDoopTime(isAve = true)
    emitTime(tool = "Wala", isAve = false)
    emitTime(tool = "Wala", isAve = true)
    
    emitMemory(tool = "Spark", isAve = false)
    emitMemory(tool = "Spark", isAve = true)
    emitMemory(tool = "Doop", isAve = false)
    emitMemory(tool = "Doop", isAve = true)
    emitMemory(tool = "Wala", isAve = false)
    emitMemory(tool = "Wala", isAve = true)
  }

  def emitDoopTime(isAve: Boolean) = {
    emitDoopAnalysisTime(isAve)
    emitDoopOverheadTime(isAve)
  }

  def emitDoopAnalysisTime(isAve: Boolean) = {
    val title = if (isAve) "DoopAve Analysis Time" else "Doop Analysis Time"

    println(title)
    println("=" * title.length)

    for {
      iteration <- 1 to 10
      prog <- benchmarks
      benchmark = benchmarkFull(prog)
    } {
      val tool = if (isAve) s"doop-averroes" else s"doop"
      val log = io.Source.fromFile(s"all-output/$iteration/callgraphs/$benchmark/$tool.log").getLines.toList

      val analysis = doopExtractNumber(_ startsWith "MBBENCH logicblox START", log)

      if (prog != benchmarks.last) print(analysis + "\t")
      else print(analysis + "\n")
    }

    println
    println
  }

  def emitDoopOverheadTime(isAve: Boolean) = {
    val title = if (isAve) "DoopAve Overhead Time" else "Doop Overhead Time"

    println(title)
    println("=" * title.length)

    for {
      iteration <- 1 to 10
      prog <- benchmarks
      benchmark = benchmarkFull(prog)
    } {
      val tool = if (isAve) s"doop-averroes" else s"doop"
      val log = io.Source.fromFile(s"all-output/$iteration/callgraphs/$benchmark/$tool.log").getLines.toList

      var total = 0d

      total += doopExtractNumber(_ startsWith "Adding archive for resolving", log)
      total += doopExtractNumber(_ startsWith "creating database in", log)
      total += doopExtractNumber(_ startsWith "loading fact declarations ...", log)
      total += doopExtractNumber(_ startsWith "loading facts ...", log)
      total += doopExtractNumber(_ startsWith "loading context-insensitive declarations...", log)
      total += doopExtractNumber(_ startsWith "loading context-insensitive delta rules...", log)
      total += doopExtractNumber(_ startsWith "loading reflection delta rules...", log)
      total += doopExtractNumber(_ startsWith "loading client delta rules...", log)
      total += doopExtractNumber(_ startsWith "retrieving call graph edges ...", log)
      if(!isAve) total += doopExtractNumber(_ startsWith "retrieving reflective call graph edges ...", log)
      total += doopExtractNumber(_ startsWith "retrieving entry points ...", log)

      val overhead = floatFormat format total

      if (prog != benchmarks.last) print(overhead + "\t")
      else print(overhead + "\n")
    }

    println
    println
  }

  def emitTime(tool: String, isAve: Boolean) = {
    emitAnalysisTime(tool, isAve)
    emitOverheadTime(tool, isAve)
  }

  def emitAnalysisTime(tool: String, isAve: Boolean) = {
    val title = if (isAve) s"${tool}Ave Analysis Time" else s"${tool} Analysis Time"
    val filename = if (isAve) s"${tool.toLowerCase}-averroes" else s"${tool.toLowerCase}"

    println(title)
    println("=" * title.length)
    for {
      iteration <- 1 to 10
      prog <- benchmarks
      benchmark = benchmarkFull(prog)
    } {
      val analysis = io.Source.fromFile(s"all-output/$iteration/callgraphs/$benchmark/$filename.log").getLines.toList.find(_ startsWith s"[$tool] Solution found in").get.split(" ").dropRight(1).last.trim.toFloat

      if (prog != benchmarks.last) print(analysis + "\t")
      else print(analysis + "\n")
    }

    println
    println
  }

  def emitOverheadTime(tool: String, isAve: Boolean) = {
    val title = if (isAve) s"${tool}Ave Overhead Time" else s"${tool} Overhead Time"
    val filename = if (isAve) s"${tool.toLowerCase}-averroes" else s"${tool.toLowerCase}"

    println(title)
    println("=" * title.length)

    for {
      iteration <- 1 to 10
      prog <- benchmarks
      benchmark = benchmarkFull(prog)
    } {
      val log = io.Source.fromFile(s"all-output/$iteration/callgraphs/$benchmark/$filename.log").getLines.toList

      val analysis = log.find(_ startsWith s"[$tool] Solution found in").get.split(" ").dropRight(1).last.trim.toFloat
      val total = log.find(_ startsWith "Total time to finish").get.split(":").last.trim.toFloat
      val overhead = floatFormat format (total - analysis)

      if (prog != benchmarks.last) print(overhead + "\t")
      else print(overhead + "\n")
    }

    println
    println
  }

  def emitMemory(tool: String, isAve: Boolean) = {
    val title = if (isAve) s"${tool}Ave Memory" else s"${tool} Memory"
    val filename = if (isAve) s"${tool.toLowerCase}-averroes" else s"${tool.toLowerCase}"

    println(title)
    println("=" * title.length)

    for {
      iteration <- 1 to 10
      prog <- benchmarks
      benchmark = benchmarkFull(prog)
    } {
      // This is divided by 4 due to a known bug in how the /usr/bin/time command computes the resident memory 
      var memory = Math.kb2gb(io.Source.fromFile(s"all-output/$iteration/callgraphs/$benchmark/$filename.log").
        getLines.toList.find(_ startsWith "maximum resident set size: ").get.split(":").last.trim.toLong / 4)

      if (prog != benchmarks.last) print(memory + "\t")
      else print(memory + "\n")
    }

    println
    println
  }

  def emitAverroesTime = {
    val title = "Averroes Time"

    println(title)
    println("=" * title.length)

    for {
      iteration <- 1 to 10
      prog <- benchmarks
      benchmark = benchmarkFull(prog)
    } {
      val log = io.Source.fromFile(s"all-output/$iteration/benchmarks-averroes/$benchmark/averroes.log").getLines.toList
      val line = log.find(_ startsWith "Total time (without verification)").get
      val averroes = Math.round(extractNumber(line))

      if (prog != benchmarks.last) print(averroes + "\t")
      else print(averroes + "\n")
    }

    println
    println
  }

  def benchmarkFull(benchmark: String) = (if (dacapo contains benchmark) "dacapo/" else "specjvm/") + benchmark
  final lazy val floatFormat = new DecimalFormat("#,###.##")
  def extractNumber(line: String) = "\\d+(.\\d+)?".r.findFirstMatchIn(line).get.matched
  def getLineAfter(p: String => Boolean, log: List[String]) = { val index = log.indexWhere(p); log(index + 1) }
  def doopExtractNumber(p: String => Boolean, log: List[String]) = { val line = getLineAfter(p, log); Math.round(extractNumber(line)) }

}