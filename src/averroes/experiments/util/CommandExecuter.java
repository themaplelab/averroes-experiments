package averroes.experiments.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

import averroes.experiments.options.ExperimentsOptions;

public class CommandExecuter {

	public static boolean run(String[] cmdarray) throws IOException, InterruptedException {
		System.out.println("Spawning process " + Arrays.toString(cmdarray));
		Process p = Runtime.getRuntime().exec(cmdarray);

		BufferedReader stdout = new BufferedReader(new InputStreamReader(p.getInputStream()));

		String line;
		while ((line = stdout.readLine()) != null) {
			System.out.println(line);
		}

		stdout.close();
		return p.waitFor() == 0;
	}

	/**
	 * Run the executable for Doop running with Averroes.
	 * 
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static boolean runDoop(String doopHome, String base, String benchmark, boolean isAverroes)
			throws IOException, InterruptedException {
		File exec = isAverroes ? Files.doopAverroesRunExe(doopHome) : Files.doopRunExe(doopHome);
		File lib = isAverroes ? Files.placeholderLibraryJarFile(base, benchmark) : Files
				.organizedLibraryJarFile(base, benchmark);
		String[] cmdArray = { exec.getPath(), ExperimentsOptions.getJreVersion(), ExperimentsOptions.getMainClass(),
				Files.organizedApplicationJarFile(base, benchmark).getPath(), lib.getPath() };
		// AverroesProperties.getInputJarFilesForSpark().trim(),
		// AverroesProperties.getLibraryClassPath().trim() };
		return run(cmdArray);
	}
}