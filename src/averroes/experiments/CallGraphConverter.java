package averroes.experiments;

import java.io.IOException;

import averroes.experiments.options.ExperimentsOptions;
import averroes.experiments.util.ProbeUtils;
import probe.CallGraph;

public class CallGraphConverter {
	public static void main(String[] args) {
		try {
			// Process the arguments
			ExperimentsOptions.processArguments(args);

			CallGraph probecg = ProbeUtils
					.convertWalaDynamicCallGraph(ExperimentsOptions.getDynamicCallGraphReportLocation());
			ProbeUtils.collapse(probecg);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
