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
package com.eco.bio7.compile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.collection.BrowserUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordedThread;
import jdk.jfr.consumer.RecordingFile;

/**
 * The profiler of the Java editor (as for R and Python): runs the class of the
 * editor as "Compile Java" does (in the JVM of Bio7) while Java Flight Recorder
 * samples the call stacks (every 2 ms, with line numbers), then shows the report
 * (lines, flame graph, methods) in the browser and the time marks in the editor.
 * <p>
 * JFR samples threads which run Java code or native code: time while a thread
 * waits or sleeps is not counted (a CPU profile).
 */
public final class JavaProfiler {

	/* The sampling period of JFR (ms) */
	private static final int PERIOD_MS = 2;
	/* The thread in which Bio7 runs the compiled code */
	private static final String EXECUTION_THREAD = "DynamicCode-Execution";

	private static volatile boolean running;

	private JavaProfiler() {
	}

	/** Profiles the class of the active Java editor (UI thread). */
	public static void profileActiveEditor() {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		IEditorPart editor = page.getActiveEditor();
		if (editor == null || !(editor.getEditorInput() instanceof IFileEditorInput input) || !input.getFile().getName().endsWith(".java")) {
			Bio7Dialog.message("Please open a Java file in the editor.");
			return;
		}
		if (running) {
			Bio7Dialog.message("A Java class is being profiled (please wait until it has finished).");
			return;
		}
		IFile file = input.getFile();
		for (IEditorPart dirty : page.getDirtyEditors()) {
			dirty.doSave(null);
		}
		Recording recording;
		try {
			recording = new Recording();
			recording.enable("jdk.ExecutionSample").withPeriod(Duration.ofMillis(PERIOD_MS));
			recording.enable("jdk.NativeMethodSample").withPeriod(Duration.ofMillis(PERIOD_MS));
			recording.setToDisk(true);
			recording.start();
		} catch (Throwable t) {
			Bio7Dialog.message("Java Flight Recorder is not available in this Java runtime:\n" + t);
			return;
		}
		running = true;
		JavaProfileMarks.clear(file);
		long t0 = System.nanoTime();
		Job job;
		/* Line numbers in the compiled classes (reset when the job is done) */
		org.codehaus.commons.compiler.jdk.JavaSourceClassLoader.lineNumbers = true;
		try {
			job = new CompileClassAndMultipleClasses().compileClasses(file, file, page);
		} catch (RuntimeException e) {
			org.codehaus.commons.compiler.jdk.JavaSourceClassLoader.lineNumbers = false;
			running = false;
			recording.close();
			throw e;
		}
		job.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				org.codehaus.commons.compiler.jdk.JavaSourceClassLoader.lineNumbers = false;
				double elapsed = (System.nanoTime() - t0) / 1e9;
				Thread t = new Thread(() -> {
					try {
						recording.stop();
						Path jfr = Files.createTempFile("bio7_profile_", ".jfr");
						recording.dump(jfr);
						List<String> code = file.getLocation() == null ? List.of() : Files.readAllLines(file.getLocation().toFile().toPath(), StandardCharsets.UTF_8);
						String json = analyze(jfr, ownClasses(file), file.getName(), code, elapsed,
								event.getResult().isOK() ? null : "The run ended with " + event.getResult().getMessage());
						Files.deleteIfExists(jfr);
						show(file, json);
					} catch (Exception e) {
						System.err.println("The profile could not be evaluated: " + e);
					} finally {
						recording.close();
						running = false;
					}
				}, "Bio7 Java profile");
				t.setDaemon(true);
				t.start();
			}
		});
	}

	/* ---------------- The evaluation ---------------- */

	/* A node of the flame graph: a method (as in the profiles of Python and R) */
	private static final class Node {
		final String name, file;
		final int line;
		final boolean own;
		double ms;
		final Map<String, Node> children = new LinkedHashMap<>();

		Node(String name, String file, int line, boolean own) {
			this.name = name;
			this.file = file;
			this.line = line;
			this.own = own;
		}

		Node child(String key, String name, String file, int line, boolean own) {
			return children.computeIfAbsent(key, k -> new Node(name, file, line, own));
		}

		List<Object> json() {
			List<Object> c = new ArrayList<>();
			children.values().stream().sorted((a, b) -> Double.compare(b.ms, a.ms)).forEach(n -> c.add(n.json()));
			List<Object> l = new ArrayList<>();
			l.add(name);
			l.add(ms);
			l.add(file);
			l.add(line);
			l.add(own);
			l.add(c);
			return l;
		}
	}

	/* A method of the table: self and total time, the first line seen */
	private static final class Method {
		final String name, file;
		final boolean own;
		int line = Integer.MAX_VALUE;
		double self, total;

		Method(String name, String file, boolean own) {
			this.name = name;
			this.file = file;
			this.own = own;
		}
	}

	private static final Pattern PACKAGE = Pattern.compile("^\\s*package\\s+([\\w.]+)\\s*;", Pattern.MULTILINE);
	private static final Pattern TYPE = Pattern.compile("^\\s*(?:public\\s+|final\\s+|abstract\\s+|sealed\\s+|non-sealed\\s+|static\\s+)*(?:class|interface|enum|record)\\s+(\\w+)",
			Pattern.MULTILINE);

	/* The top-level classes of the Java files of the project (fully qualified name -> file name): the own code */
	private static Map<String, String> ownClasses(IFile profiled) {
		Map<String, String> map = new HashMap<>();
		try {
			profiled.getProject().accept(r -> {
				if (r.getType() == IResource.FILE && "java".equals(r.getFileExtension()) && r.getLocation() != null) {
					try {
						String src = Files.readString(r.getLocation().toFile().toPath(), StandardCharsets.UTF_8);
						Matcher p = PACKAGE.matcher(src);
						String pkg = p.find() ? p.group(1) + "." : "";
						Matcher t = TYPE.matcher(src);
						while (t.find()) {
							map.put(pkg + t.group(1), r.getName());
						}
					} catch (IOException e) {
						/* Not read */
					}
				}
				return true;
			});
		} catch (CoreException e) {
			/* Only the file itself */
		}
		return map;
	}

	private static String topLevel(String className) {
		int i = className.indexOf('$');
		return i < 0 ? className : className.substring(0, i);
	}

	private static String libraryOf(String className) {
		return className.startsWith("java.") || className.startsWith("javax.") || className.startsWith("jdk.") || className.startsWith("sun.")
				|| className.startsWith("com.sun.") ? "built-in" : "library";
	}

	/**
	 * The samples of the recording as the data of the report (profile_report.html).
	 *
	 * @param own      the own classes (fully qualified top-level name -> file name)
	 * @param fileName the profiled file (its lines are shown and marked)
	 * @param code     the lines of the file
	 */
	public static String analyze(Path jfr, Map<String, String> own, String fileName, List<String> code, double elapsed, String message) throws IOException {
		double[] incl = new double[code.size() + 2], self = new double[code.size() + 2];
		Node root = new Node(fileName.replaceFirst("\\.java$", ""), "", 0, true);
		Node other = null;
		Map<String, Method> methods = new LinkedHashMap<>();
		int samples = 0, ownSamples = 0;
		double w = PERIOD_MS;
		for (RecordedEvent e : RecordingFile.readAllEvents(jfr)) {
			String type = e.getEventType().getName();
			if (!type.equals("jdk.ExecutionSample") && !type.equals("jdk.NativeMethodSample")) {
				continue;
			}
			RecordedStackTrace st = e.getStackTrace();
			RecordedThread thread = e.getThread("sampledThread");
			if (st == null) {
				continue;
			}
			List<RecordedFrame> frames = st.getFrames();
			/* The outermost frame of the own code (frames: 0 is the innermost) */
			int outer = -1;
			for (int i = frames.size() - 1; i >= 0; i--) {
				if (frames.get(i).isJavaFrame() && own.containsKey(topLevel(frames.get(i).getMethod().getType().getName()))) {
					outer = i;
					break;
				}
			}
			boolean executionThread = thread != null && EXECUTION_THREAD.equals(thread.getJavaName());
			if (outer < 0 && !executionThread) {
				/* Another thread of Bio7 (not the profiled code) */
				continue;
			}
			samples++;
			root.ms += w;
			if (outer < 0) {
				/* The thread of the code, but not in the own code: compiling, loading, the start */
				if (other == null) {
					other = root.child("\u0000other", "<compile, load and start>", "", 0, false);
				}
				other.ms += w;
				continue;
			}
			ownSamples++;
			/* The flame graph from the outermost own frame inwards, the methods, the lines of the file */
			Node node = root;
			Set<String> seenMethods = new HashSet<>();
			Set<Integer> seenLines = new HashSet<>();
			boolean selfLine = false;
			for (int i = outer; i >= 0; i--) {
				RecordedFrame f = frames.get(i);
				if (!f.isJavaFrame()) {
					continue;
				}
				String cls = f.getMethod().getType().getName();
				boolean mine = own.containsKey(topLevel(cls));
				String fn = mine ? own.get(topLevel(cls)) : libraryOf(cls);
				/* A lambda: a hidden class (Outer$$Lambda.0x... or Outer$$Lambda/0x...) */
				int lambda = cls.indexOf("$$Lambda");
				String base = lambda >= 0 ? cls.substring(0, lambda) : cls;
				String simple = base.substring(base.lastIndexOf('.') + 1) + (lambda >= 0 ? ".lambda" : "");
				String name = simple + "." + f.getMethod().getName();
				int line = f.getLineNumber();
				node = node.child(cls + "#" + f.getMethod().getName(), name, fn, mine ? Math.max(0, line) : 0, mine);
				node.ms += w;
				Method m = methods.computeIfAbsent(cls + "#" + f.getMethod().getName() + f.getMethod().getDescriptor(), k -> new Method(name, fn, mine));
				if (mine && line > 0) {
					m.line = Math.min(m.line, line);
				}
				if (seenMethods.add(cls + "#" + f.getMethod().getName() + f.getMethod().getDescriptor())) {
					m.total += w;
				}
				if (i == 0) {
					m.self += w;
				}
				if (mine && fn.equals(fileName) && line > 0 && line <= code.size()) {
					if (seenLines.add(line)) {
						incl[line] += w;
					}
				}
			}
			/* The line itself: the innermost frame of the file */
			for (int i = 0; i <= outer && !selfLine; i++) {
				RecordedFrame f = frames.get(i);
				if (f.isJavaFrame() && fileName.equals(own.get(topLevel(f.getMethod().getType().getName()))) && f.getLineNumber() > 0
						&& f.getLineNumber() <= code.size()) {
					self[f.getLineNumber()] += w;
					selfLine = true;
				}
			}
		}

		Map<String, Object> d = new LinkedHashMap<>();
		d.put("name", fileName);
		d.put("selection", false);
		d.put("elapsed", elapsed);
		d.put("sampled", samples * w / 1000.0);
		d.put("samples", samples);
		d.put("interval", w / 1000.0);
		StringBuilder msg = new StringBuilder("CPU profile of Java Flight Recorder: time while a thread waits or sleeps is not counted.");
		if (samples == 0) {
			msg.append(" No samples: the code ran too briefly (less than about " + PERIOD_MS + " ms).");
		} else if (ownSamples == 0) {
			msg.append(" None of the code of the project ran: please see the console (e.g., a compile error), "
					+ "or the code only started something which runs later (a view, an animation, a timer).");
		} else if (other != null && other.ms > 0) {
			msg.append(String.format(Locale.ROOT, " %.0f ms of the run were compiling, loading and the start.", other.ms));
		}
		if (message != null) {
			msg.append(' ').append(message);
		}
		d.put("message", msg.toString());
		List<Object> source = new ArrayList<>();
		for (int n = 1; n <= code.size(); n++) {
			source.add(List.of(n, incl[n], self[n], code.get(n - 1)));
		}
		d.put("source", source);
		d.put("flame", root.json());
		List<Object> fns = new ArrayList<>();
		for (Method m : methods.values()) {
			List<Object> r = new ArrayList<>();
			r.add(m.name);
			r.add(m.file);
			r.add(m.own && m.line != Integer.MAX_VALUE ? m.line : 0);
			r.add(null);
			r.add(null);
			r.add(m.self);
			r.add(m.total);
			r.add(m.own);
			fns.add(r);
		}
		d.put("functions", fns);
		/* The marks: the lines with at least 1 % of the time (at most 60) */
		double total = samples * w;
		List<Object> marks = new ArrayList<>();
		List<Integer> order = new ArrayList<>();
		for (int n = 1; n <= code.size(); n++) {
			if (incl[n] > 0 && incl[n] >= 0.01 * total) {
				order.add(n);
			}
		}
		order.sort((a, b) -> Double.compare(incl[b], incl[a]));
		for (int n : order.subList(0, Math.min(60, order.size()))) {
			marks.add(List.of(n, incl[n], self[n]));
		}
		d.put("marks", marks);
		d.put("ownSamples", ownSamples);
		return new ObjectMapper().writeValueAsString(d);
	}

	/* The report in the browser and the marks in the editor */
	private static void show(IFile file, String json) throws Exception {
		var n = new ObjectMapper().readTree(json);
		List<JavaProfileMarks.Line> lines = new ArrayList<>();
		for (var l : n.path("marks")) {
			lines.add(new JavaProfileMarks.Line(l.get(0).asInt(), l.get(1).asDouble(), l.get(2).asDouble()));
		}
		double total = n.path("sampled").asDouble() * 1000;
		String title = ("Profile: " + file.getName()).replace("&", "&amp;").replace("<", "&lt;");
		String html = com.eco.bio7.pybridge.debug.PythonProfiler.reportTemplate().replace("__TITLE__", title).replace("__DATA__", json.replace("</", "<\\/"));
		Path out = Files.createTempFile("bio7_profile_", ".html");
		Files.writeString(out, html, StandardCharsets.UTF_8);
		out.toFile().deleteOnExit();
		System.out.println(String.format(Locale.ROOT, "Profile of %s: %.3f s (%d samples)", file.getName(), n.path("elapsed").asDouble(), n.path("samples").asInt()));
		Display.getDefault().asyncExec(() -> {
			BrowserUtil.setLocationWithJS(out.toUri().toString());
			JavaProfileMarks.show(file, lines, total);
		});
	}
}
