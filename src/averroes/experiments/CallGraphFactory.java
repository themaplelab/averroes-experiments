package averroes.experiments;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.jar.JarFile;

import probe.CallGraph;
import averroes.experiments.soot.SparkCallGraphTransformer;
import averroes.experiments.util.CommandExecuter;
import averroes.experiments.util.FileUtils;
import averroes.experiments.util.ProbeUtils;
import averroes.options.AverroesOptions;
import averroes.soot.Names;
import averroes.util.TimeUtils;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.JarFileModule;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisOptions.ReflectionOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.BasicCallGraph;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXCFABuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.strings.Atom;

/**
 * A factory that generates call graphs for some tools.
 * 
 * @author karim
 * 
 */
public class CallGraphFactory {

	/**
	 * Generate the call graph for Spark.
	 * 
	 * @return
	 * @throws IOException
	 */
	public static CallGraph generateSparkCallGraph(String base, String benchmark, boolean isAverroes)
			throws IOException {
		CallGraph spark = new SparkCallGraphTransformer(base, benchmark, isAverroes).getProbeCallGraph();
		System.out.println("size of original spark is: " + spark.edges().size());
		return spark;
	}

	/**
	 * Generate the call graph for DoopAverroes.
	 * 
	 * @param doopHome
	 * @param benchmark
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static CallGraph generateDoopCallGraph(String doopHome, String base, String benchmark, boolean isAverroes)
			throws IOException, InterruptedException {
		// 1. Run doop's analysis
		CommandExecuter.runDoop(doopHome, base, benchmark, isAverroes);

		// 2. Convert the Doop call graph
		return ProbeUtils.convertDoopCallGraph(doopHome, isAverroes);
	}

	/**
	 * Generate the call graph for Wala.
	 * 
	 * @param benchmark
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ClassHierarchyException
	 * @throws CallGraphBuilderCancelException
	 * @throws IllegalArgumentException
	 * @throws InvalidClassFileException
	 */
	public static CallGraph generateWalaCallGraph(String base, String benchmark, boolean isAve) throws IOException,
			InterruptedException, ClassHierarchyException, IllegalArgumentException, CallGraphBuilderCancelException,
			InvalidClassFileException {
		// 1. build the call graph
		String classpath = FileUtils.composeClassPath(FileUtils.organizedApplicationJarFile(base, benchmark),
				FileUtils.organizedLibraryJarFile(base, benchmark));

		String exclusionFile = CallGraphFactory.class.getClassLoader()
				.getResource(CallGraphTestUtil.REGRESSION_EXCLUSIONS).getPath();

		AnalysisScope scope = isAve ? makeAverroesAnalysisScope(base, benchmark) : AnalysisScopeReader
				.makeJavaBinaryAnalysisScope(classpath, new File(exclusionFile));

		ClassHierarchy cha = ClassHierarchy.make(scope);

		Iterable<Entrypoint> entrypoints = makeMainEntrypoints(scope.getApplicationLoader(), cha, new String[] { "L"
				+ AverroesOptions.getMainClass().replaceAll("\\.", "/") }, isAve);

		AnalysisOptions options = new AnalysisOptions(scope, entrypoints);
		options.setReflectionOptions(isAve ? ReflectionOptions.NONE
				: ReflectionOptions.MULTI_FLOW_TO_CASTS_APPLICATION_GET_METHOD);
		options.setHandleZeroLengthArray(isAve ? false : true);

		SSAPropagationCallGraphBuilder builder = isAve ? makeZeroOneCFABuilder(options, new AnalysisCache(), cha,
				scope, null, null) : Util.makeZeroOneCFABuilder(options, new AnalysisCache(), cha, scope, null, null);

		TimeUtils.splitStart();
		BasicCallGraph<?> cg = (BasicCallGraph<?>) builder.makeCallGraph(options, null);
		System.out.println("[Wala] Solution found in " + TimeUtils.elapsedSplitTime() + " seconds.");

		// dumpCG(scope.getApplicationLoader(), builder.getPointerAnalysis(),
		// cg);

		// 2. Convert the Wala call graph to probe and collapse it
		return ProbeUtils.convertWalaCallGraph(cg);
	}

	public static SSAPropagationCallGraphBuilder makeZeroOneCFABuilder(AnalysisOptions options, AnalysisCache cache,
			IClassHierarchy cha, AnalysisScope scope, ContextSelector customSelector,
			SSAContextInterpreter customInterpreter) {

		if (options == null) {
			throw new IllegalArgumentException("options is null");
		}
		Util.addDefaultSelectors(options, cha);

		return ZeroXCFABuilder.make(cha, options, cache, customSelector, customInterpreter,
				ZeroXInstanceKeys.ALLOCATIONS | ZeroXInstanceKeys.SMUSH_MANY
						| ZeroXInstanceKeys.SMUSH_PRIMITIVE_HOLDERS | ZeroXInstanceKeys.SMUSH_STRINGS
						| ZeroXInstanceKeys.SMUSH_THROWABLES);
	}

	public static AnalysisScope makeAverroesAnalysisScope(String base, String benchmark) throws IOException,
			IllegalArgumentException, InvalidClassFileException {
		AnalysisScope scope = AnalysisScope.createJavaAnalysisScope();

		// There should be no exclusions when using Averroes
		// scope.setExclusions(new FileOfClasses(fs));

		// Library stuff
		scope.addToScope(ClassLoaderReference.Application,
				new JarFileModule(new JarFile(new File(FileUtils.averroesLibraryClassJarFile(base, benchmark)))));
		scope.addToScope(ClassLoaderReference.Primordial,
				new JarFileModule(new JarFile(new File(FileUtils.placeholderLibraryJarFile(base, benchmark)))));

		// Application JAR
		scope.addToScope(ClassLoaderReference.Application,
				new JarFileModule(new JarFile(new File(FileUtils.organizedApplicationJarFile(base, benchmark)))));

		return scope;
	}

	public static Iterable<Entrypoint> makeMainEntrypoints(final ClassLoaderReference loaderRef,
			final IClassHierarchy cha, final String[] classNames, boolean isAve) throws IllegalArgumentException,
			IllegalArgumentException, IllegalArgumentException {
		return new Iterable<Entrypoint>() {
			@Override
			public Iterator<Entrypoint> iterator() {
				final Atom mainMethod = Atom.findOrCreateAsciiAtom("main");

				return new Iterator<Entrypoint>() {
					private int index = 0;
					private boolean clinitTaken = false;

					@Override
					public void remove() {
						Assertions.UNREACHABLE();
					}

					@Override
					public boolean hasNext() {
						return index < classNames.length || (isAve && !clinitTaken);
					}

					@Override
					public Entrypoint next() {
						if (index < classNames.length) {
							TypeReference T = TypeReference.findOrCreate(loaderRef,
									TypeName.string2TypeName(classNames[index++]));
							MethodReference mainRef = MethodReference.findOrCreate(T, mainMethod,
									Descriptor.findOrCreateUTF8("([Ljava/lang/String;)V"));
							return new DefaultEntrypoint(mainRef, cha);
						} else if (isAve && !clinitTaken) {
							clinitTaken = true;
							TypeReference T = TypeReference.findOrCreate(loaderRef,
									TypeName.string2TypeName("Laverroes/Library"));
							MethodReference clinitRef = MethodReference.findOrCreate(T, MethodReference.clinitName,
									MethodReference.clinitSelector.getDescriptor());
							return new DefaultEntrypoint(clinitRef, cha);
						} else {
							throw new IllegalStateException("No more entry points. This should never happen!");
						}
					}
				};
			}
		};
	}

	public static void dumpCG(ClassLoaderReference loaderRef, PointerAnalysis<InstanceKey> PA,
			com.ibm.wala.ipa.callgraph.CallGraph CG) {
		TypeReference T = TypeReference.findOrCreate(loaderRef, TypeName.string2TypeName("Lorg/hsqldb/Table"));
		MethodReference M = MethodReference
				.findOrCreate(T, "deleteNoCheck", "(Lorg/hsqldb/Session;Lorg/hsqldb/Row;Z)V");
		CGNode N = CG.getNodes(M).iterator().next();
		System.err.print("callees of node " + getShortName(N) + " : [");
		boolean fst = true;
		for (Iterator<? extends CGNode> ns = CG.getSuccNodes(N); ns.hasNext();) {
			if (fst)
				fst = false;
			else
				System.err.print(", ");
			System.err.print(getShortName(ns.next()));
		}
		System.err.println("]");
		System.err.println("\nIR of node " + N.getGraphNodeId() + ", context " + N.getContext());
		IR ir = N.getIR();
		if (ir != null) {
			System.err.println(ir);
		} else {
			System.err.println("no IR!");
		}

		System.err.println("pointer analysis");
		System.err.println(PA.getHeapModel().getPointerKeyForLocal(N, 19) + " -->");
		PA.getPointsToSet(PA.getHeapModel().getPointerKeyForLocal(N, 19)).forEach(
				p -> System.out.println(p + " :: " + p.getClass()));
		System.err.println(PA.getHeapModel().getPointerKeyForLocal(N, 20) + " -->");
		PA.getPointsToSet(PA.getHeapModel().getPointerKeyForLocal(N, 20)).forEach(System.out::println);

		TypeReference T2 = TypeReference.findOrCreate(loaderRef, TypeName.string2TypeName("Laverroes/Library"));
		MethodReference M2 = MethodReference.findOrCreate(T2, Names.AVERROES_DO_IT_ALL_METHOD_NAME, "()V");
		CGNode N2 = CG.getNodes(M2).iterator().next();
		System.err.print("callees of node " + getShortName(N2) + " : [");
		fst = true;
		for (Iterator<? extends CGNode> ns = CG.getSuccNodes(N2); ns.hasNext();) {
			if (fst)
				fst = false;
			else
				System.err.print(", ");
			System.err.print(getShortName(ns.next()));
		}
		System.err.println("]");
		System.err.println("\nIR of node " + N2.getGraphNodeId() + ", context " + N2.getContext());
		ir = N2.getIR();
		if (ir != null) {
			System.err.println(ir);
		} else {
			System.err.println("no IR!");
		}

		System.err.println("pointer analysis");
		System.err.println(PA.getHeapModel().getPointerKeyForLocal(N2, 1870) + " -->");
		PA.getPointsToSet(PA.getHeapModel().getPointerKeyForLocal(N2, 1870)).forEach(System.out::println);
		System.err.println(PA.getHeapModel().getPointerKeyForLocal(N2, 1871) + " -->");
		PA.getPointsToSet(PA.getHeapModel().getPointerKeyForLocal(N2, 1871)).forEach(System.out::println);
		System.err.println(PA.getHeapModel().getPointerKeyForLocal(N2, 613) + " -->");
		PA.getPointsToSet(PA.getHeapModel().getPointerKeyForLocal(N2, 613)).forEach(System.out::println);
	}

	public static String getShortName(CGNode nd) {
		IMethod method = nd.getMethod();
		return getShortName(method);
	}

	public static String getShortName(IMethod method) {
		String origName = method.getName().toString();
		String result = origName;
		if (origName.equals("do") || origName.equals("ctor")) {
			result = method.getDeclaringClass().getName().toString();
			result = result.substring(result.lastIndexOf('/') + 1);
			if (origName.equals("ctor")) {
				if (result.equals("LFunction")) {
					String s = method.toString();
					if (s.indexOf('(') != -1) {
						String functionName = s.substring(s.indexOf('(') + 1, s.indexOf(')'));
						functionName = functionName.substring(functionName.lastIndexOf('/') + 1);
						result += " " + functionName;
					}
				}
				result = "ctor of " + result;
			}
		}
		return result;
	}
}