package averroes.experiments.options;

import java.util.Arrays;
import java.util.List;

/**
 * A class that holds the benchmark names.
 * 
 * @author Karim Ali
 * 
 */
public final class Benchmarks {

	public static String DaCapo = "dacapo";
	public static String SpecJvm = "specjvm";

	private static List<String> dacapo = Arrays.asList("antlr", "bloat", "chart", "hsqldb", "luindex", "lusearch",
			"pmd", "xalan");
	private static List<String> specjvm = Arrays.asList("compress", "db", "jack", "javac", "jess", "raytrace");

	/**
	 * Get the name of the benchmark this program belongs to.
	 * 
	 * @param program
	 * @return
	 */
	public static String getBenchmark(String program) {
		if (isDacapo(program)) {
			return DaCapo;
		} else if (isSpecjvm(program)) {
			return SpecJvm;
		} else {
			throw new RuntimeException("Unknown benchmark for " + program);
		}
	}
	
	public static boolean isDacapo(String program) {
		return dacapo.contains(program);
	}
	
	public static boolean isSpecjvm(String program) {
		return specjvm.contains(program);
	}
}