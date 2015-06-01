package ca.uwaterloo.averroes.callgraph.drivers;

import java.io.FileOutputStream;
import java.util.zip.GZIPOutputStream;

import probe.CallGraph;
import probe.TextWriter;
import averroes.properties.AverroesProperties;
import averroes.util.TimeUtils;
import ca.uwaterloo.averroes.callgraph.CallGraphFactory;
import ca.uwaterloo.averroes.util.FileUtils;
import ca.uwaterloo.averroes.util.ProbeUtils;

/**
 * A driver class that generates call graph for Doop.
 * 
 * @author Karim Ali
 * 
 */
public class DoopCallGraphGenerator {

	public static void main(String[] args) {
		try {
			TimeUtils.reset();
			if (args.length != 4) {
				usage();
			}

			// Process the arguments
			String doopHome = args[0];
			String base = args[1];
			String benchmark = args[2];
			boolean isAverroes = Boolean.parseBoolean(args[3]);

			FileUtils.createDirectory(AverroesProperties.getOutputDir());
			CallGraph doop = CallGraphFactory.generateDoopCallGraph(doopHome, base, benchmark, isAverroes);
			System.out.println("Total time to finish: " + TimeUtils.elapsedTime());

			// collapse and write the call graph
			probe.CallGraph collapsed = ProbeUtils.collapse(doop);
			new TextWriter()
					.write(collapsed, new GZIPOutputStream(new FileOutputStream(FileUtils.callGraphGzipFile())));

			// Print some statistics
			System.out.println("=================================================");
			System.out.println("# edges = " + collapsed.edges().size());
			System.out.println("=================================================");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void usage() {
		System.out.println("");
		System.out.println("Usage: java -jar doop.jar <absolute_path_to_doop_home> <base> <benchmark> <isAverroes>");
		System.out.println("");
		System.exit(1);
	}
}