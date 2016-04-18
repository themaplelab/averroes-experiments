package averroes.experiments;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import averroes.experiments.options.ExperimentsOptions;
import averroes.experiments.util.Files;
import averroes.experiments.util.ProbeUtils;
import probe.CallGraph;
import probe.TextWriter;

public class CallGraphConverter {
	public static void main(String[] args) {
		try {
			// Process the arguments
			ExperimentsOptions.processArguments(args);

			CallGraph cg = ProbeUtils
					.convertWalaDynamicCallGraph(ExperimentsOptions.getDynamicCallGraphReportLocation());
			// new TextWriter().write(cg, new GZIPOutputStream(new
			// FileOutputStream("raw.txt.gzip")));
			CallGraph collapsed = ProbeUtils.collapse(cg);
			// new TextWriter().write(collapsed, new GZIPOutputStream(new
			// FileOutputStream("collapsed.txt.gzip")));
			new TextWriter().write(collapsed, new GZIPOutputStream(new FileOutputStream(Files.callGraphGzipFile())));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
