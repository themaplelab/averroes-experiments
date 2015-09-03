package averroes.experiments.util;

/**
 * A utility class to time operations.
 * 
 * @author karim
 * 
 */
public class TimeUtils {

	private static long start = System.currentTimeMillis();
	private static long splitStart = System.currentTimeMillis();

	/**
	 * Calculate the elapsed time in seconds.
	 * 
	 * @return
	 */
	public static double elapsedTime() {
		return Math.round((System.currentTimeMillis() - start) / 1000.0, Math.round$default$2());
	}

	/**
	 * Calculate the elapsed time in seconds starting at the split start.
	 * 
	 * @return
	 */
	public static double elapsedSplitTime() {
		return Math.round((System.currentTimeMillis() - splitStart) / 1000.0, Math.round$default$2());
	}

	/**
	 * Split the timer.
	 */
	public static void splitStart() {
		splitStart = System.currentTimeMillis();
	}

	/**
	 * Reset the start time used to calculate the elapsed time.
	 */
	public static void reset() {
		start = System.currentTimeMillis();
	}
}
