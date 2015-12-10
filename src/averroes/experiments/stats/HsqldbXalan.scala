package averroes.experiments.stats

import scala.collection.JavaConversions.asScalaSet

import averroes.experiments.util.ProbeUtils
import probe.CallEdge
import probe.CallGraph
import probe.ProbeMethod
import probe.TextReader

object HsqldbXalan {

  val base = "all-output/1/callgraphs"

  final val tools = List("spark-averroes", "doop-averroes", "wala-averroes")

  def l2aFilter(e: CallEdge) = e.src == ProbeUtils.LIBRARY_BLOB && e.dst != ProbeUtils.LIBRARY_BLOB

  def emit(title: String, pkgs: List[String]) = {
    println(title)
    println("=" * title.length)

    tools.foreach { tool =>
      val dyn = collapse(new TextReader().readCallGraph(s"$base/$title/dynamic.txt.gzip"), pkgs).edges filter l2aFilter
      val ave = collapse(new TextReader().readCallGraph(s"$base/$title/$tool.txt.gzip"), pkgs).edges filter l2aFilter
      val van = collapse(new TextReader().readCallGraph(s"$base/$title/${tool.replace("-averroes", "")}.txt.gzip"), pkgs).edges filter l2aFilter
      val svan = dyn union van
      val diff = ave diff svan

      val precision = diff.size
      println(tool + " :: " + precision)
    }
  }

  def collapse(cg: CallGraph, pkgs: List[String]) = {
    def isCollapsable(m: ProbeMethod) = pkgs.exists(m.cls.pkg.startsWith)

    val result = new CallGraph

    cg.entryPoints.foreach { e =>
      if (!isCollapsable(e)) result.entryPoints.add(e)
      else result.entryPoints.add(ProbeUtils.LIBRARY_BLOB)
    }

    cg.edges.foreach { e =>
      val s = isCollapsable(e.src)
      val d = isCollapsable(e.dst)

      if (!s && !d) result.edges.add(new CallEdge(e.src, e.dst))
      else if (!s && d) result.edges.add(new CallEdge(e.src, ProbeUtils.LIBRARY_BLOB))
      else if (s && !d) result.edges.add(new CallEdge(ProbeUtils.LIBRARY_BLOB, e.dst))
    }

    result
  }

  def main(args: Array[String]) = {
    // Precision
    emit("dacapo/hsqldb", List[String]("org.hsqldb.jdbc"))
    emit("dacapo/xalan", List[String]("org.apache.xalan", "org.apache.xml"))
  }
}