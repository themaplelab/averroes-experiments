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
	 * Get the original JAR file for a benchmark
	 * 
	 * @param base
	 * @param program
	 * @return
	 */
	public static File applicationJarFile(String base, String program) {
		String benchmark = Benchmarks.getBenchmark(program);
		String extension = Benchmarks.isDacapo(program) ? ".jar" : ".zip";
		return Paths.get(base, "benchmarks", benchmark, program + extension).toFile();
	}

	/**
	 * Get the original JAR file for a benchmark dependency
	 * 
	 * @param base
	 * @param program
	 * @return
	 */
	public static String libraryPath(String base, String program) {
		String benchmark = Benchmarks.getBenchmark(program);

		if (Benchmarks.isDacapo(program)) {
			return Paths.get(base, "benchmarks", benchmark, program + "-deps.jar").toFile().getPath();
		} else if (Benchmarks.isSpecjvm(program)) {
			File spec = Paths.get(base, "benchmarks", benchmark, "spec.zip").toFile();
			File sun = Paths.get(base, "benchmarks", benchmark, "sunrsasign.jar").toFile();
			return composeClassPath(spec, sun);
		} else {
			throw new RuntimeException("Unknown benchmark program " + program);
		}
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
	public static String composeClassPath(File... args) {
		return Arrays.asList(args).stream().map(File::getPath).collect(Collectors.joining(File.pathSeparator));
	}

	/**
	 * Compose a path from the given arguments.
	 * 
	 * @param first
	 * @param more
	 * @return
	 */
	public static String composeClassPath(String... args) {
		return Arrays.asList(args).stream().collect(Collectors.joining(File.pathSeparator));
	}
}
