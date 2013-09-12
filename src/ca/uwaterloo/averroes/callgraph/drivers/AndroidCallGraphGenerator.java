package ca.uwaterloo.averroes.callgraph.drivers;

import java.io.FileOutputStream;

import probe.CallGraph;
import probe.GXLWriter;
import ca.uwaterloo.averroes.callgraph.CallGraphFactory;
import ca.uwaterloo.averroes.properties.AverroesProperties;
import ca.uwaterloo.averroes.util.TimeUtils;
import ca.uwaterloo.averroes.util.io.FileUtils;

/**
 * A driver class that generates call graph for Spark using the original library.
 * 
 * @author karim
 * 
 */
public class AndroidCallGraphGenerator {

	public static void main(String[] args) {
		try {
			// Generate the call graph
			TimeUtils.reset();
			FileUtils.createDirectory(AverroesProperties.getOutputDir());
			CallGraph android = CallGraphFactory.generateAndroidCallGraph();
			System.out.println("Total time to finish: " + TimeUtils.elapsedTime());
			new GXLWriter().write(android, new FileOutputStream(FileUtils.androidCallGraphFile()));

			// Print some statistics
			System.out.println("=================================================");
			System.out.println("# edges = " + android.edges().size());
			System.out.println("=================================================");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static void usage() {
		System.out.println("");
		System.out.println("Usage: java -jar spark.jar");
		System.out.println("");
	}
}
