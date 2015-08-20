package averroes.experiments.stats

import averroes.experiments.util.ProbeUtils
import probe.GXLReader
import probe.TextWriter

/** upgrade the call graphs to the new library blob format */
object CallGraphConverter {
  //  val blob = ObjectManager.v().getMethod(
  //    ObjectManager.v().getClass("ca.uwaterloo." + Names.AVERROES_LIBRARY_CLASS), Names.BLOB, "")

  def main(args: Array[String]) = {
    //    val cg = new TextReader().readCallGraph(args(0))
    //    val out = args(0).replaceFirst("callgraphs-vanilla", "callgraphs-vanilla2")
    //    val result = new CallGraph
    //
    //    for (e <- cg.entryPoints) { if (e == blob) result.entryPoints.add(ProbeUtils.LIBRARY_BLOB) else result.entryPoints.add(e) }
    //    for (e <- cg.edges) {
    //      val src = if (e.src == blob) ProbeUtils.LIBRARY_BLOB else e.src
    //      val dst = if (e.dst == blob) ProbeUtils.LIBRARY_BLOB else e.dst
    //      result.edges.add(new CallEdge(src, dst))
    //    }
    //
    //    new TextWriter().write(result, out)

    val cg = new GXLReader().readCallGraph(args(0))
    val out = "callgraph.txt.gzip"
    val result = ProbeUtils.collapse(cg)
    new TextWriter().write(result, out)
  }
}