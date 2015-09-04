package averroes.experiments.util;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;

import averroes.experiments.options.Benchmarks;
import averroes.experiments.options.ExperimentsOptions;

/**
 * Utility class for file-related operations.
 * 
 * @author Karim Ali
 * 
 */
public class Files {

	/**
	 * The path to the call graph.
	 * 
	 * @return
	 */
	public static File callGraphGzipFile() {
		return new File(ExperimentsOptions.getOutputDirectory(), ExperimentsOptions.getToolName() + ".txt.gzip");
	}

	/**
	 * The path to the organized application JAR file of a benchmark program.
	 * 
	 * @return
	 */
	public static File organizedApplicationJarFile(String base, String program) {
		return Paths.get(base, "benchmarks-averroes", Benchmarks.getBenchmark(program), program, "organized-app.jar")
				.toFile();
	}

	/**
	 * The path to the placeholder library JAR file of a benchmark program.
	 * 
	 * @return
	 */
	public static File placeholderLibraryJarFile(String base, String program) {
		return Paths.get(base, "benchmarks-averroes", Benchmarks.getBenchmark(program), program, "placeholder-lib.jar")
				.toFile();
	}

	/**
	 * The path to the JAR file that contains the single file averroes.Library
	 * for a benchmark program.
	 * 
	 * @return
	 */
	public static File averroesLibraryClassJarFile(String base, String program) {
		return Paths.get(base, "benchmarks-averroes", Benchmarks.getBenchmark(program), program,
				"averroes-lib-class.jar").toFile();
	}

	/**
	 * The path to the organized library JAR file of a benchmark program.
	 * 
	 * @return
	 */
	public static File organizedLibraryJarFile(String base, String program) {
		return Paths.get(base, "benchmarks-averroes", Benchmarks.getBenchmark(program), program, "organized-lib.jar")
				.toFile();
	}

	/**
	 * The path to the Doop executable
	 * 
	 * @return
	 */
	public static File doopRunExe(String doopHome) {
		return new File(doopHome, "alt-run");
	}

	/**
	 * The path to the DoopAverroes executable
	 * 
	 * @return
	 */
	public static File doopAverroesRunExe(String doopHome) {
		return new File(doopHome, "alt-run-averroes");
	}

	/**
	 * Compose a path from the given arguments.
	 * 
	 * @param first
	 * @param more
	 * @return
	 */
	public static String composeClassPath(File ... args) {
		return Arrays.asList(args).stream().map(File::getPath).collect(Collectors.joining(File.pathSeparator));
	}
}
