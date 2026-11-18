/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.gdal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Tests a GDAL library in a separate Java process before it is loaded into the
 * application: a library which crashes (e.g., a wrong architecture or missing
 * dependencies) only ends the test process. The test loads the library,
 * registers the drivers and reads the version.
 */
public final class GdalProbe {

	/** The result of a test: ok and the version, else the reason. */
	public record Result(boolean ok, String version, String message) {
	}

	/* A program for the source file mode of java (no class path needed) */
	private static final String PROGRAM = """
			import java.lang.foreign.*;
			import java.lang.invoke.MethodHandle;
			import java.nio.file.Path;

			public class GdalProbeMain {
			    public static void main(String[] args) throws Throwable {
			        Path lib = Path.of(args[0]);
			        Linker linker = Linker.nativeLinker();
			        if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
			            SymbolLookup k = SymbolLookup.libraryLookup("kernel32", Arena.global());
			            MethodHandle set = linker.downcallHandle(k.find("SetDllDirectoryW").orElseThrow(),
			                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
			            byte[] w = (lib.getParent().toString() + "\\0").getBytes(java.nio.charset.StandardCharsets.UTF_16LE);
			            MemorySegment s = Arena.global().allocate(w.length);
			            s.copyFrom(MemorySegment.ofArray(w));
			            int unused = (int) set.invokeExact(s);
			        }
			        SymbolLookup gdal = SymbolLookup.libraryLookup(lib, Arena.global());
			        MethodHandle register = linker.downcallHandle(gdal.find("GDALAllRegister").orElseThrow(), FunctionDescriptor.ofVoid());
			        MethodHandle version = linker.downcallHandle(gdal.find("GDALVersionInfo").orElseThrow(),
			                FunctionDescriptor.of(ValueLayout.ADDRESS.withTargetLayout(MemoryLayout.sequenceLayout(512, ValueLayout.JAVA_BYTE)),
			                        ValueLayout.ADDRESS));
			        register.invokeExact();
			        MemorySegment v = (MemorySegment) version.invokeExact(Arena.global().allocateFrom("RELEASE_NAME"));
			        System.out.println("GDAL_PROBE_VERSION=" + v.getString(0));
			    }
			}
			""";

	private GdalProbe() {
	}

	/** Tests the library (a file or a folder) in a separate process (at most 60 seconds). */
	public static Result test(Path library) {
		Path lib = GdalLibraryFinder.resolve(library);
		if (lib == null) {
			return new Result(false, null, "No GDAL library at " + library);
		}
		Path java = javaExecutable();
		if (java == null) {
			return new Result(false, null, "The test needs the java executable of the Java runtime (not found in " + System.getProperty("java.home") + ")");
		}
		Path dir = null;
		try {
			dir = Files.createTempDirectory("gdalprobe");
			Path source = dir.resolve("GdalProbeMain.java");
			Files.writeString(source, PROGRAM, StandardCharsets.UTF_8);
			ProcessBuilder pb = new ProcessBuilder(List.of(java.toString(), "--enable-native-access=ALL-UNNAMED", source.toString(), lib.toString()));
			pb.redirectErrorStream(true);
			if (GdalLibraryFinder.WINDOWS) {
				/* The dependencies of the library (also found with the folder in the PATH) */
				pb.environment().merge("PATH", lib.getParent().toString(), (old, add) -> add + ";" + old);
			}
			Process p = pb.start();
			byte[] out = p.getInputStream().readAllBytes();
			if (!p.waitFor(60, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				return new Result(false, null, "The test of GDAL didn't finish in 60 seconds");
			}
			String text = new String(out, StandardCharsets.UTF_8);
			for (String line : text.split("\\R")) {
				if (line.startsWith("GDAL_PROBE_VERSION=")) {
					return new Result(true, line.substring("GDAL_PROBE_VERSION=".length()).strip(), "");
				}
			}
			String reason = text.strip();
			if (reason.length() > 2000) {
				reason = reason.substring(0, 2000) + "...";
			}
			/* The reason of the system loader (e.g., a missing dependency, another architecture) */
			String loader = GdalLibraryFinder.loadError(lib);
			if (loader != null) {
				return new Result(false, null, "The library can't be loaded: " + loader);
			}
			return new Result(false, null, "The library could not be loaded (exit code " + p.exitValue() + ")" + (reason.isEmpty() ? "" : ":\n" + reason));
		} catch (IOException e) {
			return new Result(false, null, "The test could not be started: " + e.getMessage());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return new Result(false, null, "The test was interrupted");
		} finally {
			if (dir != null) {
				try (var files = Files.list(dir)) {
					for (Path f : files.toList()) {
						Files.deleteIfExists(f);
					}
					Files.deleteIfExists(dir);
				} catch (IOException e) {
					/* A temporary file stays */
				}
			}
		}
	}

	/* The java launcher of the running runtime */
	private static Path javaExecutable() {
		Path home = Path.of(System.getProperty("java.home", ""));
		for (String name : List.of("java", "java.exe")) {
			Path p = home.resolve("bin").resolve(name);
			if (Files.isExecutable(p)) {
				return p;
			}
		}
		return null;
	}
}
