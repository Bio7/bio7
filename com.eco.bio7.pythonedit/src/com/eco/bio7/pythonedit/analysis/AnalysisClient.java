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
package com.eco.bio7.pythonedit.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The code analysis of the Python editor: a separate process of the selected
 * Python (bio7_analysis.py) with one JSON request and one JSON response per
 * line. The process is started on the first request and again after a change of
 * the Python or an end.
 */
public final class AnalysisClient {

	private static final AnalysisClient INSTANCE = new AnalysisClient();

	private Process process;
	private Writer writer;
	private String processExecutable;
	private File script;
	private final AtomicLong ids = new AtomicLong();
	private final Map<Long, CompletableFuture<Object>> pending = new ConcurrentHashMap<>();

	private AnalysisClient() {
	}

	public static AnalysisClient get() {
		return INSTANCE;
	}

	/** Returns true if a Python for the analysis is available. */
	public static boolean isAvailable() {
		return PythonEditorSupport.executable() != null;
	}

	/**
	 * Sends a request and waits for the result.
	 *
	 * @return the result (a Map) or null (no Python, a timeout, an error)
	 */
	@SuppressWarnings("unchecked")
	public Map<String, Object> request(String cmd, Map<String, Object> args, long timeoutMillis) {
		String exe = PythonEditorSupport.executable();
		if (exe == null) {
			return null;
		}
		long id = ids.incrementAndGet();
		CompletableFuture<Object> future = new CompletableFuture<>();
		try {
			synchronized (this) {
				ensureProcess(exe);
				pending.put(id, future);
				Map<String, Object> req = new LinkedHashMap<>(args);
				req.put("id", id);
				req.put("cmd", cmd);
				writer.write(Json.write(req));
				writer.write('\n');
				writer.flush();
			}
			Object r = future.get(timeoutMillis, TimeUnit.MILLISECONDS);
			return r instanceof Map<?, ?> m ? (Map<String, Object>) m : null;
		} catch (Exception e) {
			return null;
		} finally {
			pending.remove(id);
		}
	}

	/* Starts the process (again) for the executable */
	private void ensureProcess(String exe) throws IOException {
		if (process != null && process.isAlive() && exe.equals(processExecutable)) {
			return;
		}
		stop();
		if (script == null || !script.exists()) {
			script = File.createTempFile("bio7_analysis", ".py");
			script.deleteOnExit();
			try (InputStream in = AnalysisClient.class.getResourceAsStream("bio7_analysis.py")) {
				Files.copy(in, script.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
		}
		ProcessBuilder pb = new ProcessBuilder(exe, "-u", script.getAbsolutePath());
		pb.environment().put("PYTHONIOENCODING", "utf-8");
		pb.environment().remove("PYTHONHOME");
		pb.environment().remove("PYTHONPATH");
		pb.redirectError(ProcessBuilder.Redirect.DISCARD);
		Process p = pb.start();
		process = p;
		processExecutable = exe;
		writer = new OutputStreamWriter(p.getOutputStream(), StandardCharsets.UTF_8);
		Thread reader = new Thread(() -> read(p), "Python analysis");
		reader.setDaemon(true);
		reader.start();
	}

	private void read(Process p) {
		try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
			String line;
			while ((line = r.readLine()) != null) {
				try {
					if (Json.parse(line) instanceof Map<?, ?> m && m.get("id") instanceof Number n) {
						CompletableFuture<Object> f = pending.get(n.longValue());
						if (f != null) {
							f.complete(m.containsKey("result") ? m.get("result") : null);
						}
					}
				} catch (RuntimeException e) {
					/* Not a response */
				}
			}
		} catch (IOException e) {
			/* The process ended */
		}
		for (CompletableFuture<Object> f : pending.values()) {
			f.complete(null);
		}
	}

	/** Stops the process (e.g., after a change of the Python or new packages). */
	public synchronized void stop() {
		if (process != null) {
			process.destroy();
			process = null;
		}
	}
}
