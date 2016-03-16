package averroes.experiments;

import java.io.IOException;

import averroes.experiments.options.ExperimentsOptions;
import averroes.experiments.util.ProbeUtils;

public class CallGraphConverter {
	public static void main(String[] args) {
		try {
			ProbeUtils.convertWalaDynamicCallGraph(ExperimentsOptions
					.getDynamicCallGraphReportLocation());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
