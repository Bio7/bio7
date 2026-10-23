package com.eco.bio7.compile;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;

/**
 * The Java versions of Bio7 in one place: the version of the bundled JDK
 * (Adoptium Temurin, see the feature com.eco.bio7.jdk), the default version of
 * the dynamic Java compiler, the target of the ImageJ plugin compiler and the
 * language level of the JDT parser (AST).
 * <p>
 * For a new Java version change {@link #JDK} (and replace the JDKs in
 * com.eco.bio7.jdk). The JDT parser follows automatically as soon as the JDT
 * of the target platform supports the version.
 */
public final class JavaVersions {

	/** The Java version of the bundled JDK (Adoptium Temurin). */
	public static final int JDK = 27;

	/**
	 * The default Java version of the dynamic Java compiler (javac of the bundled
	 * JDK, see the Java preferences).
	 */
	public static final int COMPILER_VERSION = JDK;

	/** The default target of the ImageJ plugin compiler ("Compile and Run"). */
	public static final int IMAGEJ_COMPILER_TARGET = JDK;

	/**
	 * The language level of the JDT parser, e.g., AST.JLS27 for Java 27. If the
	 * JDT of the target platform does not support {@link #JDK} yet (JDT 3.47: Java
	 * 26 at most, there is no AST.JLS27), the latest level of the JDT is used.
	 */
	public static final int AST_LEVEL = Math.min(JDK, AST.getJLSLatest());

	/**
	 * The compliance of the JDT parser ({@link #JDK} or the latest Java version
	 * which the JDT supports).
	 */
	public static final String JDT_COMPLIANCE = JavaCore.isSupportedJavaVersion(Integer.toString(JDK)) ? Integer.toString(JDK)
			: JavaCore.latestSupportedJavaVersion();

	/* The versions before Java 10 (the version numbers of the old scheme)! */
	private static final String[] LEGACY_VERSIONS = { "1.9", "1.8", "1.7", "1.6", "1.5", "1.4" };

	private JavaVersions() {
	}

	/**
	 * The Java versions which can be selected for the dynamic Java compiler
	 * (label and value), from {@link #JDK} down to Java 1.4.
	 */
	public static String[][] compilerVersions() {
		String[][] versions = new String[JDK - 10 + 1 + LEGACY_VERSIONS.length][];
		int i = 0;
		for (int v = JDK; v >= 10; v--) {
			versions[i++] = new String[] { Integer.toString(v), Integer.toString(v) };
		}
		for (String v : LEGACY_VERSIONS) {
			versions[i++] = new String[] { v, v };
		}
		return versions;
	}
}
