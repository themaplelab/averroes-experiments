package soot.coffi;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.raw.HeaderItem;

import soot.RefType;
import soot.ResolutionFailedException;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootFieldRef;
import soot.SootMethod;
import soot.Type;
import soot.options.Options;
import ca.uwaterloo.averroes.properties.AverroesProperties;
import ca.uwaterloo.averroes.soot.Hierarchy;
import ca.uwaterloo.averroes.util.BytecodeUtils;
import ca.uwaterloo.averroes.util.DexUtils;

/**
 * A class that holds the values of library methods and fields found in the constant pool of application classes. The
 * class is in this specific package name because it accesses some package-private classes (e.g.,
 * {@link CONSTANT_Fieldref_info}.
 * 
 * @author karim
 * 
 */
public class AverroesApplicationConstantPool {

	private Set<SootClass> applicationClasses;
	private Set<SootMethod> libraryMethods;
	private Set<SootField> libraryFields;

	private Hierarchy hierarchy;

	/**
	 * Initialize this constant pool with all the library methods and fields in the constant pool of any application
	 * class.
	 * 
	 * @param hierarchy
	 */
	public AverroesApplicationConstantPool(Hierarchy hierarchy) {
		applicationClasses = new HashSet<SootClass>();
		libraryMethods = new HashSet<SootMethod>();
		libraryFields = new HashSet<SootField>();

		this.hierarchy = hierarchy;

		initialize();
	}

	/**
	 * Get the set of library methods that appear in the constant pool of any application class.
	 * 
	 * @return
	 */
	public Set<SootMethod> getLibraryMethods() {
		return libraryMethods;
	}

	/**
	 * Get the set of library fields that appear in the constant pool of any application class.
	 * 
	 * @return
	 */
	public Set<SootField> getLibraryFields() {
		return libraryFields;
	}

	/**
	 * Get the set of classes that are referenced by name in the constant pool of any application class.
	 * 
	 * @return
	 */
	public Set<SootClass> getApplicationClasses() {
		return applicationClasses;
	}

	/**
	 * Check if the given field is a library field referenced by the application.
	 * 
	 * @param field
	 * @return
	 */
	public boolean isLibraryFieldInApplicationConstantPool(SootField field) {
		return getLibraryFields().contains(field);
	}

	/**
	 * Check if the given method is a library method referenced by the application.
	 * 
	 * @param method
	 * @return
	 */
	public boolean isLibraryMethodInApplicationConstantPool(SootMethod method) {
		return getLibraryMethods().contains(method);
	}

	/**
	 * Initialize the application constant pool.
	 */
	private void initialize() {
		findApplicationClassesReferencedByName();
		findLibraryMethodsInApplicationConstantPool();
		findLibraryFieldsInApplicationConstantPool();
	}

	/**
	 * Get the Coffi class corresponding to the given Soot class. From this coffi class, we can get the constant pool
	 * and all the related info.
	 * 
	 * @param cls
	 * @return
	 */
	private ClassFile getCoffiClass(SootClass cls) {
		SootMethod anyMethod = cls.methodIterator().next();
		CoffiMethodSource methodSource = (CoffiMethodSource) anyMethod.getSource();
		return methodSource.coffiClass;
	}

	/**
	 * Get the referenced library methods in an application class.
	 * 
	 * @param applicationClass
	 * @return
	 */
	private Set<SootMethod> findLibraryMethodsInConstantPool(SootClass applicationClass) {
		Set<SootMethod> result = new HashSet<SootMethod>();

		/*
		 * This is only useful if the application class has any methods. Some classes will not have any methods in them,
		 * e.g., org.jfree.data.xml.DatasetTags which is an interface that has some final constants only.
		 */
		if (applicationClass.getMethodCount() > 0) {
			ClassFile coffiClass = getCoffiClass(applicationClass);
			cp_info[] constantPool = coffiClass.constant_pool;

			for (cp_info constantPoolEntry : constantPool) {
				if (constantPoolEntry instanceof ICONSTANT_Methodref_info) {
					ICONSTANT_Methodref_info methodInfo = (ICONSTANT_Methodref_info) constantPoolEntry;

					// Get the method declaring class
					CONSTANT_Class_info c = (CONSTANT_Class_info) constantPool[methodInfo.getClassIndex()];
					String className = ((CONSTANT_Utf8_info) (constantPool[c.name_index])).convert();
					className = className.replace('/', '.');
					// TODO why is that?
					if (className.charAt(0) == '[') {
						className = "java.lang.Object";
					}

					// Get the method name, parameter types, and return type
					CONSTANT_NameAndType_info i = (CONSTANT_NameAndType_info) constantPool[methodInfo
							.getNameAndTypeIndex()];
					String methodName = ((CONSTANT_Utf8_info) (constantPool[i.name_index])).convert();
					String methodDescriptor = ((CONSTANT_Utf8_info) (constantPool[i.descriptor_index])).convert();
					SootMethod method = BytecodeUtils.makeSootMethod(className, methodName, methodDescriptor);

					// If the resolved method is in the library, add it to the result
					if (hierarchy.isLibraryMethod(method)) {
						result.add(method);
					}
				}
			}
		}

		return result;
	}

	/**
	 * Find all the classes whose name is referenced in the constant pool of application classes.
	 * 
	 * @throws IOException
	 */
	private void findApplicationClassesReferencedByName() {
		applicationClasses = new HashSet<SootClass>();

		// If we're processing an android apk, process the global string constant pool
		if (Options.v().src_prec() == Options.src_prec_apk) {
			applicationClasses.addAll(findAndroidApplicationClassesReferencedByName());
		} else {
			// Add the classes whose name appear in the constant pool of application classes
			for (SootClass applicationClass : hierarchy.getApplicationClasses()) {
				applicationClasses.addAll(findApplicationClassesReferencedByName(applicationClass));
			}
		}
	}

	/**
	 * Search the android string constant pool for application class names.
	 * 
	 * @return
	 */
	private Set<SootClass> findAndroidApplicationClassesReferencedByName() {
		Set<SootClass> result = new HashSet<SootClass>();
		try {
			DexBackedDexFile dex = DexFileFactory.loadDexFile(AverroesProperties.getApkLocation(), 17);
			int stringCount = dex.readSmallUint(HeaderItem.STRING_COUNT_OFFSET);
			for (int i = 0; i < stringCount; i++) {
				try {
					Type tpe = Util.v().jimpleTypeOfFieldDescriptor(dex.getString(i));

					if (tpe instanceof RefType) {
						SootClass sc = ((RefType) tpe).getSootClass();

						// Ignore the R class and its inner classes here
						if (hierarchy.isApplicationClass(sc) && !AverroesProperties.isAndroidRClassOrInnerClass(sc)) {
							result.add(sc);
						}
					}
				} catch (RuntimeException e) {
					// eat it, some entries won't be for class names
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;
	}

	/**
	 * Get the classes referenced by name in the constant pool of an application class.
	 * 
	 * @param applicationClass
	 */
	private Set<SootClass> findApplicationClassesReferencedByName(SootClass applicationClass) {
		Set<SootClass> result = new HashSet<SootClass>();

		/*
		 * This is only useful if the application class has any methods. Some classes will not have any methods in them,
		 * e.g., org.jfree.data.xml.DatasetTags which is an interface that has some final constants only.
		 */
		if (applicationClass.getMethodCount() > 0) {
			ClassFile coffiClass = getCoffiClass(applicationClass);
			cp_info[] constantPool = coffiClass.constant_pool;

			for (cp_info constantPoolEntry : constantPool) {
				if (constantPoolEntry instanceof CONSTANT_String_info) {
					CONSTANT_String_info stringInfo = (CONSTANT_String_info) constantPoolEntry;

					// Get the class name
					CONSTANT_Utf8_info s = (CONSTANT_Utf8_info) constantPool[stringInfo.string_index];
					String className = s.convert();

					if (hierarchy.isApplicationClass(className)) {
						result.add(hierarchy.getClass(className));
					}
				}
			}
		}

		return result;
	}

	/**
	 * Find all the library methods referenced from the constant pool of application classes.
	 * 
	 * @return
	 */
	private void findLibraryMethodsInApplicationConstantPool() {
		libraryMethods = new HashSet<SootMethod>();

		// If we're processing an android apk, process the global method constant pool
		if (Options.v().src_prec() == Options.src_prec_apk) {
			libraryMethods.addAll(findLibraryMethodsInAndroidApplicationConstantPool());
		} else {
			// Add the library methods that appear in the constant pool of application classes
			for (SootClass applicationClass : hierarchy.getApplicationClasses()) {
				libraryMethods.addAll(findLibraryMethodsInConstantPool(applicationClass));
			}
		}
	}

	/**
	 * Search the android string constant pool for application class names.
	 * 
	 * @return
	 */
	private Set<SootMethod> findLibraryMethodsInAndroidApplicationConstantPool() {
		Set<SootMethod> result = new HashSet<SootMethod>();
		try {
			DexBackedDexFile dex = DexFileFactory.loadDexFile(AverroesProperties.getApkLocation(), 17);
			int methodCount = dex.readSmallUint(HeaderItem.METHOD_COUNT_OFFSET);
			for (int i = 0; i < methodCount; i++) {
				if (DexUtils.isArrayClone(dex, i) == false) {
					SootMethod method = DexUtils.asSootMethod(dex, i);

					// If the resolved method is in the library, add it to the result
					if (hierarchy.isLibraryMethod(method)) {
						result.add(method);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;
	}

	/**
	 * Get the referenced library fields in an application class.
	 * 
	 * @param applicationClass
	 * @return
	 */
	private Set<SootField> findLibraryFieldsInConstantPool(SootClass applicationClass) {
		Set<SootField> result = new HashSet<SootField>();

		/*
		 * This is only useful if the application class has any methods. Some classes will not have any methods in them,
		 * e.g., org.jfree.data.xml.DatasetTags which is an interface that has some final constants only.
		 */
		if (applicationClass.getMethodCount() > 0) {
			ClassFile coffiClass = getCoffiClass(applicationClass);
			cp_info[] constantPool = coffiClass.constant_pool;

			for (cp_info constantPoolEntry : constantPool) {
				if (constantPoolEntry instanceof CONSTANT_Fieldref_info) {
					CONSTANT_Fieldref_info fieldInfo = (CONSTANT_Fieldref_info) constantPoolEntry;

					// Get the field declaring class
					CONSTANT_Class_info c = (CONSTANT_Class_info) constantPool[fieldInfo.class_index];
					String className = ((CONSTANT_Utf8_info) (constantPool[c.name_index])).convert();
					className = className.replace('/', '.');
					// TODO why is that?
					if (className.charAt(0) == '[') {
						className = "java.lang.Object";
					}
					SootClass cls = Scene.v().getSootClass(className);

					// Get the field name, and type
					CONSTANT_NameAndType_info i = (CONSTANT_NameAndType_info) constantPool[fieldInfo.name_and_type_index];
					String fieldName = ((CONSTANT_Utf8_info) (constantPool[i.name_index])).convert();
					String fieldDescriptor = ((CONSTANT_Utf8_info) (constantPool[i.descriptor_index])).convert();
					Type fieldType = Util.v().jimpleTypeOfFieldDescriptor(fieldDescriptor);

					// Get the field ref and resolve it to a Soot field
					SootFieldRef fieldRef = Scene.v().makeFieldRef(cls, fieldName, fieldType, false);
					SootField field;

					/*
					 * We have to do this ugly code. Try first and see if the field is not static. If it is static, then
					 * create a new fieldRef in the catch and resolve it again with isStatic = true.
					 */
					try {
						field = fieldRef.resolve();
					} catch (ResolutionFailedException e) {
						fieldRef = Scene.v().makeFieldRef(cls, fieldName, fieldType, true);
					}
					field = fieldRef.resolve();

					// If the resolved field is in the library, add it to the result
					if (hierarchy.isLibraryField(field)) {
						result.add(field);
					}
				}
			}
		}

		return result;
	}

	/**
	 * Search the android string constant pool for application class names.
	 * 
	 * @return
	 */
	private Set<SootField> findLibraryFieldsInAndroidApplicationConstantPool() {
		Set<SootField> result = new HashSet<SootField>();
		try {
			DexBackedDexFile dex = DexFileFactory.loadDexFile(AverroesProperties.getApkLocation(), 17);
			int fieldCount = dex.readSmallUint(HeaderItem.FIELD_COUNT_OFFSET);
			for (int i = 0; i < fieldCount; i++) {
				SootField field = DexUtils.asSootField(dex, i);

				// If the resolved field is in the library, add it to the result
				if (hierarchy.isLibraryField(field)) {
					result.add(field);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;
	}

	/**
	 * Get all the library fields referenced from the constant pool of application classes.
	 * 
	 * @return
	 */
	private void findLibraryFieldsInApplicationConstantPool() {
		libraryFields = new HashSet<SootField>();

		// If we're processing an android apk, process the global field constant pool
		if (Options.v().src_prec() == Options.src_prec_apk) {
			libraryFields.addAll(findLibraryFieldsInAndroidApplicationConstantPool());
		} else {
			// Add the library methods that appear in the constant pool of application classes
			for (SootClass applicationClass : hierarchy.getApplicationClasses()) {
				libraryFields.addAll(findLibraryFieldsInConstantPool(applicationClass));
			}
		}
	}
}
