package averroes.experiments;

import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.FileUtils;

import probe.CallGraph;
import probe.TextWriter;
import averroes.experiments.options.ExperimentsOptions;
import averroes.experiments.util.Files;
import averroes.experiments.util.ProbeUtils;
import averroes.experiments.util.TimeUtils;

/**
 * A driver class that generates a call graph for the given tool.
 * 
 * @author Karim Ali
 * 
 */
public class CallGraphGenerator {

	public static void main(String[] args) {
		try {
			TimeUtils.reset();

			// Process the arguments
			ExperimentsOptions.processArguments(args);

			// Create the output directory
			FileUtils.forceMkdir(new File(ExperimentsOptions.getOutputDirectory()));

			CallGraph probecg = null;

			if (ExperimentsOptions.getTool().equalsIgnoreCase("spark")) {
				probecg = CallGraphFactory.generateSparkCallGraph(ExperimentsOptions.getBaseDirectory(),
						ExperimentsOptions.getProgram(), ExperimentsOptions.isAverroes());
			} else if (ExperimentsOptions.getTool().equalsIgnoreCase("doop")) {
				probecg = CallGraphFactory.generateDoopCallGraph(ExperimentsOptions.getDoopHome(),
						ExperimentsOptions.getBaseDirectory(), ExperimentsOptions.getProgram(),
						ExperimentsOptions.isAverroes());
			} else if (ExperimentsOptions.getTool().equalsIgnoreCase("wala")) {
				probecg = CallGraphFactory.generateWalaCallGraph(ExperimentsOptions.getBaseDirectory(),
						ExperimentsOptions.getProgram(), ExperimentsOptions.isAverroes());
			} else {
				throw new IllegalStateException(
						ExperimentsOptions.getTool()
								+ " is unknow. Please provide one of the following tool names: spark, doop, or wala (case-insensitive)");
			}
			System.out.println("Total time to finish: " + TimeUtils.elapsedTime());

			// collapse and write the call graph
			CallGraph cg = ProbeUtils.collapse(probecg);
			new TextWriter().write(cg, new GZIPOutputStream(new FileOutputStream(Files.callGraphGzipFile())));

			// Print some statistics
			System.out.println("=================================================");
			System.out.println("# edges = " + cg.edges().size());
			System.out.println("=================================================");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}