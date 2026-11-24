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
package com.eco.bio7.pybridge;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.eco.bio7.pyffm.PyRunListener;

/**
 * The feedback of the running Python code: a job for a run (a script, a long
 * console line) in the status bar and the Progress view with the time (its
 * stop button interrupts Python), and a job with a progress bar for each task
 * of bio7.progress, bio7.task and tqdm. Short runs (below RUN_DELAY) are not
 * shown.
 */
public final class PythonJobs implements PyRunListener {

	/* A run is shown after this time (no flickering of short console lines) */
	static final long RUN_DELAY = 500;
	private static final long TASK_DELAY = 250;
	private static final int TICKS = 10000;

	private static volatile RunJob run;
	private static volatile long runStart;
	private static volatile String runLabel;
	private static final Map<String, TaskJob> TASKS = new LinkedHashMap<>();

	@Override
	public void started(String label) {
		begin(label);
	}

	@Override
	public void finished() {
		end();
	}

	/** A run of user code begins (e.g., a script). */
	public static synchronized void begin(String label) {
		if (run != null) {
			run.done = true;
		}
		runStart = System.currentTimeMillis();
		runLabel = label;
		run = new RunJob(label);
		run.schedule(RUN_DELAY);
	}

	/** The run has ended: also its progress tasks. */
	public static synchronized void end() {
		if (run != null) {
			run.done = true;
			/* Not started yet (a short run): never shown */
			run.cancel();
			run = null;
		}
		runLabel = null;
		endTasks();
	}

	/** Returns the label of the running user code or null. */
	public static String runningLabel() {
		return runLabel;
	}

	/** The milliseconds since the run began (0: no run). */
	public static long runningMillis() {
		return runLabel == null ? 0 : System.currentTimeMillis() - runStart;
	}

	/**
	 * The progress of a task (bio7.progress, bio7.task, a tqdm bar): done of total
	 * (total <= 0: unknown) with a text.
	 */
	public static void progress(String key, double done, double total, String text) {
		progress(key, done, total, text, "Python", EmbeddedPython::interrupt);
	}

	/**
	 * The progress of a task of a language (e.g., "R" for bio7_progress of R): the
	 * stop button of the job runs interrupt (e.g., interrupts R).
	 */
	public static synchronized void progress(String key, double done, double total, String text, String language, Runnable interrupt) {
		TaskJob t = TASKS.get(key);
		if (t == null || t.done) {
			/* bio7.progress: the name of the script (the text changes), a task or tqdm: its text */
			boolean plain = key.endsWith("progress") || text == null || text.isBlank();
			t = new TaskJob(plain ? language + " progress" + (runLabel != null && "Python".equals(language) ? ": " + runLabel : "") : text, interrupt);
			TASKS.put(key, t);
			t.schedule(TASK_DELAY);
		}
		t.update(done, total, text);
		java.util.function.BiConsumer<Double, Double> l = LISTENERS.get(key);
		if (l != null) {
			l.accept(done, total);
		}
		if (total > 0 && done >= total) {
			finishTask(key);
		}
	}

	/* The listeners of the progress of a key (e.g., the dialog Experiment) */
	private static final Map<String, java.util.function.BiConsumer<Double, Double>> LISTENERS = new java.util.concurrent.ConcurrentHashMap<>();

	/** A listener of the progress of the key (done, total; null removes it), e.g., "progress" (bio7.progress) or "r:progress" (bio7_progress of R). */
	public static void setListener(String key, java.util.function.BiConsumer<Double, Double> listener) {
		if (listener == null) {
			LISTENERS.remove(key);
		} else {
			LISTENERS.put(key, listener);
		}
	}

	/** A task has ended (e.g., the tqdm bar was closed). */
	public static synchronized void finishTask(String key) {
		TaskJob t = TASKS.remove(key);
		if (t != null) {
			t.done = true;
			t.cancel();
		}
	}

	/** Ends all progress tasks (e.g., after a markdown chunk). */
	static synchronized void endTasks() {
		for (TaskJob t : TASKS.values()) {
			t.done = true;
			t.cancel();
		}
		TASKS.clear();
	}

	static String time(long millis) {
		long s = millis / 1000;
		return s < 60 ? s + " s" : s < 3600 ? String.format(Locale.ROOT, "%d:%02d min", s / 60, s % 60) : String.format(Locale.ROOT, "%d:%02d h", s / 3600, (s / 60) % 60);
	}

	/* The stop button of a job: interrupts Python (as Ctrl+C) */
	private static IStatus waitOrInterrupt(IProgressMonitor monitor, PythonJob job) {
		while (!job.done) {
			if (monitor.isCanceled()) {
				job.interrupt.run();
				/* Python ends the run with a KeyboardInterrupt (the job ends with it) */
				long until = System.currentTimeMillis() + 5000;
				while (!job.done && System.currentTimeMillis() < until) {
					sleep(50);
				}
				return Status.CANCEL_STATUS;
			}
			job.refresh(monitor);
			sleep(100);
		}
		return Status.OK_STATUS;
	}

	private static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private abstract static class PythonJob extends Job {
		volatile boolean done;
		/* The stop button: interrupts the language (Python: as Ctrl+C) */
		Runnable interrupt = EmbeddedPython::interrupt;

		PythonJob(String name) {
			super(name);
			setPriority(Job.SHORT);
		}

		abstract void refresh(IProgressMonitor monitor);

		@Override
		public boolean belongsTo(Object family) {
			return family == PythonJobs.class;
		}
	}

	/* The run of a script or a console line: the time */
	private static final class RunJob extends PythonJob {
		private final long start = System.currentTimeMillis();
		private String shown = "";

		RunJob(String label) {
			super("Python: " + label);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (done) {
				return Status.OK_STATUS;
			}
			monitor.beginTask(getName(), IProgressMonitor.UNKNOWN);
			IStatus s = waitOrInterrupt(monitor, this);
			monitor.done();
			return s;
		}

		@Override
		void refresh(IProgressMonitor monitor) {
			String t = "Running " + time(System.currentTimeMillis() - start) + " (stop: interrupt)";
			if (!t.equals(shown)) {
				shown = t;
				monitor.subTask(t);
			}
		}
	}

	/* A task with progress: determinate when the total is known */
	private static final class TaskJob extends PythonJob {
		private volatile double done_, total;
		private volatile String text;
		private boolean begun;
		private int sent;
		private String shown = "";

		TaskJob(String name, Runnable interrupt) {
			super(name);
			if (interrupt != null) {
				this.interrupt = interrupt;
			}
		}

		void update(double d, double t, String txt) {
			done_ = d;
			total = t;
			if (txt != null && !txt.isBlank()) {
				text = txt;
			}
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (done) {
				return Status.OK_STATUS;
			}
			IStatus s = waitOrInterrupt(monitor, this);
			monitor.done();
			return s;
		}

		@Override
		void refresh(IProgressMonitor monitor) {
			double t = total, d = done_;
			if (!begun) {
				/* Not begun: the bar is indeterminate until the total is known */
				if (t <= 0) {
					String s = (text != null ? text + ": " : "") + (long) d;
					if (!s.equals(shown)) {
						shown = s;
						monitor.subTask(s);
					}
					return;
				}
				monitor.beginTask(getName(), TICKS);
				begun = true;
			}
			int ticks = t > 0 ? (int) Math.min(TICKS, Math.round(TICKS * d / t)) : sent;
			if (ticks > sent) {
				monitor.worked(ticks - sent);
				sent = ticks;
			}
			String s = (text != null && !text.equals(getName()) ? text + ": " : "") + (t > 0 ? fmt(d) + " / " + fmt(t) + String.format(Locale.ROOT, " (%.0f %%)", 100 * d / t) : fmt(d));
			if (!s.equals(shown)) {
				shown = s;
				monitor.subTask(s);
			}
		}

		private static String fmt(double v) {
			return v == Math.rint(v) ? String.valueOf((long) v) : String.format(Locale.ROOT, "%.1f", v);
		}
	}
}
