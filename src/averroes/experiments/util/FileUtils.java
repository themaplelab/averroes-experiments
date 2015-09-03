/**
 * 
 */
package averroes.experiments.util;

import java.io.File;

import averroes.experiments.options.ExperimentsOptions;

/**
 * Utility class for file-related operations.
 * 
 * @author Karim Ali
 * 
 */
public class FileUtils {

	/**
	 * The path to the call graph.
	 * 
	 * @return
	 */
	public static String callGraphGzipFile() {
		return ExperimentsOptions.getOutputDirectory().concat(File.separator).concat("callgraph.txt.gzip");
	}

	/**
	 * The path to the organized application JAR file of a benchmark.
	 * 
	 * @return
	 */
	public static String organizedApplicationJarFile(String base, String benchmark) {
		return composePath(base, "benchmarks-averroes", benchmark, "organized-app.jar");
	}

	/**
	 * The path to the placeholder library JAR file of a benchmark.
	 * 
	 * @return
	 */
	public static String placeholderLibraryJarFile(String base, String benchmark) {
		return composePath(base, "benchmarks-averroes", benchmark, "placeholder-lib.jar");
	}

	/**
	 * The path to the JAR file that contains the single file averroes.Library
	 * 
	 * @return
	 */
	public static String averroesLibraryClassJarFile(String base, String benchmark) {
		return composePath(base, "benchmarks-averroes", benchmark, "averroes-lib-class.jar");
	}

	/**
	 * The path to the organized library JAR file of a benchmark.
	 * 
	 * @return
	 */
	public static String organizedLibraryJarFile(String base, String benchmark) {
		return composePath(base, "benchmarks-averroes", benchmark, "organized-lib.jar");
	}

	/**
	 * The path to the Doop executable
	 * 
	 * @return
	 */
	public static String doopRunExe(String doopHome) {
		return doopHome.concat(File.separator).concat("alt-run");
	}

	/**
	 * The path to the DoopAverroes executable
	 * 
	 * @return
	 */
	public static String doopAverroesRunExe(String doopHome) {
		return doopHome.concat(File.separator).concat("alt-run-averroes");
	}

	/**
	 * Compose a path from the given arguments.
	 * 
	 * @param args
	 * @return
	 */
	public static String composePath(String... args) {
		String path = "";

		for (int i = 0; i < args.length - 1; i++) {
			path = path + args[i] + File.separator;
		}
		path = path.concat(args[args.length - 1]);

		return path;
	}
}
