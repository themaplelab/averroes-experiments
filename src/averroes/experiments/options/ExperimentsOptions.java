package averroes.experiments.options;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import probe.ProbeClass;

/**
 * A class that holds all the properties required by the experiments we run 
 * for Averroes to run. For the possible values of each property, you 
 * can consult the accompanying
 * properties/averroes.properties.sample file or the online tutorial at
 * {@link http ://karimali.ca/averroes}
 * 
 * @author Karim Ali
 * 
 */
public final class ExperimentsOptions {

	private static List<String> dynamicClasses = null;

	private static Option doopHome = Option.builder("p")
			.longOpt("doop-home")
			.desc("the location where doop is installed")
			.hasArg()
			.argName("directory")
			.required()
			.build();
	
	private static Option tool = Option.builder("t")
			.longOpt("tool")
			.desc("the tool to run (spark, doop, or wala)")
			.hasArg()
			.argName("name")
			.required()
			.build();
	
	private static Option base = Option.builder("b")
			.longOpt("base")
			.desc("the base directory for experiments")
			.hasArg()
			.argName("directory")
			.required()
			.build();
	
	private static Option program = Option.builder("p")
			.longOpt("program")
			.desc("the benchmark program to analyze")
			.hasArg()
			.argName("name")
			.required()
			.build();
	
	private static Option averroes = Option.builder("a")
			.longOpt("averroes")
			.desc("run averroes or the vanilla analysis?")
			.hasArg(false)
			.required(false)
			.build();
	
	private static Option applicationRegex = Option.builder("r")
			.longOpt("application-regex")
			.desc("a list of regular expressions for application packages or classes separated by File.pathSeparator")
			.hasArg()
			.argName("regex")
			.required()
			.build();
	
	private static Option mainClass = Option.builder("m")
			.longOpt("main-class")
			.desc("the main class that runs the application when the program executes")
			.hasArg()
			.argName("class")
			.required()
			.build();
	
	private static Option dynamicClassesFile = Option.builder("d")
			.longOpt("dynamic-classes-file")
			.desc("a file that contains a list of classes that are loaded dynamically by Averroes (e.g., classes instantiated through reflection)")
			.hasArg()
			.argName("file")
			.required()
			.build();
	
	private static Option outputDirectory = Option.builder("o")
			.longOpt("output-directory")
			.desc("the directory to which Averroes will write any output files/folders.")
			.hasArg()
			.argName("directory")
			.required()
			.build();
	
	private static Options options = new Options()
			.addOption(doopHome)
			.addOption(tool)
			.addOption(base)
			.addOption(program)
			.addOption(averroes)
			.addOption(applicationRegex)
			.addOption(mainClass)
			.addOption(dynamicClassesFile)
			.addOption(outputDirectory);
	
	private static CommandLine cmd;

	/**
	 * Process the input arguments of Averroes.
	 * 
	 * @param args
	 */
	public static void processArguments(String[] args) {
		try {
			cmd = new DefaultParser().parse(options, args);
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	/**
	 * The home of the DOOP installation.
	 * 
	 * @return
	 */
	public static String getDoopHome() {
		return cmd.getOptionValue(doopHome.getOpt());
	}
	
	/**
	 * The analysis to run.
	 * 
	 * @return
	 */
	public static String getTool() {
		return cmd.getOptionValue(tool.getOpt());
	}
	
	/**
	 * The base directory for the experiments.
	 * 
	 * @return
	 */
	public static String getBaseDirectory() {
		return cmd.getOptionValue(base.getOpt());
	}
	
	/**
	 * The benchmark program to run.
	 * 
	 * @return
	 */
	public static String getProgram() {
		return cmd.getOptionValue(program.getOpt());
	}
	
	/**
	 * Should we run averroes or vanilla?
	 * 
	 * @return
	 */
	public static boolean isAverroes() {
		return cmd.hasOption(averroes.getOpt());
	}
	
	/**
	 * The list of application packages or classes separated by {@link File#pathSeparator}.
	 * 
	 * @return
	 */
	public static String[] getApplicationRegex() {
		return cmd.getOptionValue(applicationRegex.getOpt()).split(File.pathSeparator);
	}

	/**
	 * The main class that runs the application when the program executes.
	 * 
	 * @return
	 */
	public static String getMainClass() {
		return cmd.getOptionValue(mainClass.getOpt());
	}

	/**
	 * Get the names of classes that might be dynamically loaded by the input
	 * program.
	 * 
	 * @return
	 */
	public static List<String> getDynamicClasses() throws IOException {
		if (dynamicClasses == null) {
			dynamicClasses = new ArrayList<String>();

			BufferedReader in = new BufferedReader(new FileReader(cmd.getOptionValue(dynamicClassesFile.getOpt())));
			String line;
			while ((line = in.readLine()) != null) {
				dynamicClasses.add(line);
			}
			in.close();
		}

		return dynamicClasses;
	}

	/**
	 * The directory to which Averroes will write any output files/folders.
	 * 
	 * @return
	 */
	public static String getOutputDirectory() {
		return cmd.getOptionValue(outputDirectory.getOpt());
	}

	/**
	 * Check if a class belongs to the application, based on the
	 * {@value #APPLICATION_INCLUDES} property.
	 * 
	 * @param probeClass
	 * @return
	 */
	public static boolean isApplicationClass(ProbeClass probeClass) {
		for (String entry : getApplicationRegex()) {
			/*
			 * 1. If the entry ends with .* then this means it's a package. 2.
			 * If the entry ends with .** then it's a super package. 3. If the
			 * entry is **, then it's the default package. 4. Otherwise, it's
			 * the full class name.
			 */
			if (entry.endsWith(".*")) {
				String pkg = entry.replace(".*", "");
				if (probeClass.pkg().equalsIgnoreCase(pkg)) {
					return true;
				}
			} else if (entry.endsWith(".**")) {
				String pkg = entry.replace("**", "");
				if (probeClass.toString().startsWith(pkg)) {
					return true;
				}
			} else if (entry.equalsIgnoreCase("**") && probeClass.pkg().isEmpty()) {
				return true;
			} else if (entry.equalsIgnoreCase(probeClass.toString())) {
				return true;
			}
		}
		return false;
	}
}