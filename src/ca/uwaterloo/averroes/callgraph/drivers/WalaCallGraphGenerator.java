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
 * A driver class that generates call graph for WALA.
 * 
 * @author karim
 * 
 */
public class WalaCallGraphGenerator {

	public static void main(String[] args) {
		try {
			// Generate the call graph
			TimeUtils.reset();
			if (args.length != 3) {
				usage();
			}

			// Process the arguments
			String base = args[0];
			String benchmark = args[1];
			boolean isAverroes = Boolean.parseBoolean(args[2]);

			FileUtils.createDirectory(AverroesProperties.getOutputDir());
			CallGraph wala = CallGraphFactory.generateWalaCallGraph(base, benchmark, isAverroes);
			System.out.println("Total time to finish: " + TimeUtils.elapsedTime());

			// collapse and write the call graph
			probe.CallGraph collapsed = ProbeUtils.collapse(wala);
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

	public static void usage() {
		System.out.println("");
		System.out.println("Usage: java -jar wala.jar <base> <benchmark> <isAverroes>");
		System.out.println("");
		System.exit(1);
	}
}
