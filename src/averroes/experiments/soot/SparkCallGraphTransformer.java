package averroes.experiments.soot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import probe.CallGraph;
import probe.ObjectManager;
import probe.ProbeClass;
import probe.ProbeMethod;
import soot.ClassProvider;
import soot.G;
import soot.Kind;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.SourceLocator;
import soot.jimple.spark.SparkTransformer;
import soot.jimple.spark.pag.AllocNode;
import soot.jimple.spark.pag.Node;
import soot.jimple.spark.pag.PAG;
import soot.jimple.spark.sets.DoublePointsToSet;
import soot.jimple.spark.sets.P2SetVisitor;
import soot.options.Options;
import averroes.properties.AverroesProperties;
import averroes.soot.Names;

public class SparkCallGraphTransformer {
	private AverroesClassProvider provider;
	private boolean isAverroes;

	public SparkCallGraphTransformer(String base, String benchmark, boolean isAverroes) {
		this.isAverroes = isAverroes;
		provider = new AverroesClassProvider(base, benchmark, this.isAverroes);
	}

	public CallGraph getProbeCallGraph() throws IOException {
		System.out.println("Generating the call graph from Spark" + (isAverroes ? "Averroes." : "."));

		// Reset Soot
		G.reset();
		provider.prepare();

		// Set some soot parameters
		SourceLocator.v().setClassProviders(Collections.singletonList((ClassProvider) provider));
		Options.v().classes().addAll(provider.getApplicationClassNames());
		Options.v().set_main_class(AverroesProperties.getMainClass());
		Options.v().set_whole_program(true);

		// Dynamic classes are only relevant for the vanilla Spark
		if (!isAverroes) {
			addCommonDynamicClasses(provider);
			Options.v().set_dynamic_class(AverroesProperties.getDynamicClasses());
		} else {
			// required since this class is not related to any other class now
			Options.v().classes().add(Names.AVERROES_LIBRARY_CLASS);
		}

		// Load the necessary classes
		Scene.v().loadNecessaryClasses();
		Scene.v().setMainClassFromOptions();

		// Setting entry points (i.e., main method of the main class)
		Scene.v().setEntryPoints(entryPoints());

		// Run the Spark transformer
		SparkTransformer.v().transform("", Transformer.sparkOptions(isAverroes));

		// Retrieve the call graph edges
		CallGraph probecg = new CallGraph();
		soot.jimple.toolkits.callgraph.CallGraph cg = Scene.v().getCallGraph();

		// TODO
		// PAG pag = (PAG) Scene.v().getPointsToAnalysis();
		// SootField f =
		// Scene.v().getSootClass(Names.AVERROES_ABSTRACT_LIBRARY_CLASS)
		// .getFieldByName(Names.LIBRARY_POINTS_TO);
		//
		// SootMethod m =
		// Scene.v().getMethod(Names.AVERROES_LIBRARY_DO_IT_ALL_METHOD_SIGNATURE);
		// System.out.println(m.getActiveBody());
		// m.getActiveBody().getLocals().stream().filter(l ->
		// l.getName().equals("r0"))
		// .map(l -> pag.reachingObjects(l, f)).forEach(pt ->
		// ((DoublePointsToSet) pt).forall(new P2SetVisitor() {
		// public void visit(Node n) {
		// AllocNode key = (AllocNode) n;
		// Arrays.stream(pag.allocLookup(key)).forEach(System.out::println);
		// }
		// }));
		//
		// SootMethod m2 =
		// Scene.v().getSootClass("org.hsqldb.Table").getMethodByName("deleteNoCheck");
		// List<String> locals = Arrays.asList("$r7", "$r8");
		// System.out.println(m2.getActiveBody());
		// m2.getActiveBody().getLocals().stream().filter(l ->
		// locals.contains(l.getName()))
		// .map(l -> pag.reachingObjects(l)).forEach(pt -> ((DoublePointsToSet)
		// pt).forall(new P2SetVisitor() {
		// public void visit(Node n) {
		// AllocNode key = (AllocNode) n;
		// Arrays.stream(pag.allocLookup(key)).forEach(System.out::println);
		// }
		// }));

		Iterator<soot.jimple.toolkits.callgraph.Edge> it = cg.listener();
		while (it.hasNext()) {
			soot.jimple.toolkits.callgraph.Edge e = it.next();
			if (e.isExplicit() || e.kind().equals(Kind.NEWINSTANCE)) {
				probecg.edges().add(new probe.CallEdge(probeMethod(e.src()), probeMethod(e.tgt())));
			}
		}

		// Retrieve the call graph entry points
		for (SootMethod method : Scene.v().getEntryPoints()) {
			probecg.entryPoints().add(probeMethod(method));
		}

		// DijkstraAlgorithm alg = new DijkstraAlgorithm(probecg);
		// alg.execute(ProbeUtils.LIBRARY_BLOB);
		// System.out.println(alg.getPath(ProbeUtils.createProbeMethodBySignature(m.getSignature())));

		return probecg;
	}

	/**
	 * The main method of the main class set in the Soot scene is the only entry
	 * point to the call graph.
	 * 
	 * @return
	 */
	private List<SootMethod> entryPoints() {
		List<SootMethod> result = new ArrayList<SootMethod>();
		result.add(Scene.v().getMainMethod());
		if (isAverroes) {
			result.add(Scene.v().getMethod(Names.AVERROES_LIBRARY_CLINIT_METHOD_SIGNATURE));
		}
		return result;
	}

	/**
	 * Convert a soot method to a probe method.
	 * 
	 * @param sootMethod
	 * @return
	 */
	private ProbeMethod probeMethod(SootMethod sootMethod) {
		SootClass sootClass = sootMethod.getDeclaringClass();
		ProbeClass cls = ObjectManager.v().getClass(sootClass.toString());
		return ObjectManager.v().getMethod(cls, sootMethod.getName(), sootMethod.getBytecodeParms());
	}

	public static void addCommonDynamicClass(ClassProvider provider, String className) {
		if (provider.find(className) != null) {
			Scene.v().addBasicClass(className);
		}
	}

	public static void addCommonDynamicClasses(ClassProvider provider) {
		/*
		 * For simulating the FileSystem class, we need the implementation of
		 * the FileSystem, but the classes are not loaded automatically due to
		 * the indirection via native code.
		 */
		addCommonDynamicClass(provider, "java.io.UnixFileSystem");
		addCommonDynamicClass(provider, "java.io.WinNTFileSystem");
		addCommonDynamicClass(provider, "java.io.Win32FileSystem");

		/* java.net.URL loads handlers dynamically */
		addCommonDynamicClass(provider, "sun.net.www.protocol.file.Handler");
		addCommonDynamicClass(provider, "sun.net.www.protocol.ftp.Handler");
		addCommonDynamicClass(provider, "sun.net.www.protocol.http.Handler");
		addCommonDynamicClass(provider, "sun.net.www.protocol.https.Handler");
		addCommonDynamicClass(provider, "sun.net.www.protocol.jar.Handler");
	}
}