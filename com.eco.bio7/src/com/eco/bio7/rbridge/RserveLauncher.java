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

package com.eco.bio7.rbridge;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import com.eco.bio7.console.ConsolePageParticipant;
import com.eco.bio7.reditors.REditor;
import com.eco.bio7.worldwind.WorldWindView;

/**
 * Starts Rserve in the R process of the Bio7 console and connects to it.
 * <p>
 * The R process sources a small start script which reports with markers if
 * Rserve is missing, can't be loaded or can't be started. The connection is
 * tried with an increasing delay until a timeout and is accepted only if the
 * server runs in the R process of the console (the process id is compared): an
 * Rserve of another process (e.g., an orphaned R) or a forked Rserve (not built
 * for the cooperative mode) is detected.
 */
public final class RserveLauncher {

	/** The result of a start. */
	public enum Status {
		CONNECTED, R_NOT_RUNNING, RSERVE_MISSING, RSERVE_LOAD_ERROR, PORT_IN_USE, START_ERROR, NOT_COOPERATIVE, WRONG_SERVER, R_EXITED, TIMEOUT, CANCELED
	}

	/**
	 * The result of a start.
	 *
	 * @param status     the status
	 * @param connection the connection (only for CONNECTED)
	 * @param port       the port of Rserve
	 * @param detail     an error message of R or of the connection, or null
	 * @param notes      notes for the console (e.g., another port was used), or
	 *                   empty
	 */
	public record Result(Status status, RConnection connection, int port, String detail, String notes) {

		public boolean isConnected() {
			return status == Status.CONNECTED;
		}

		/** Returns true if the setup dialog (R and Rserve) can help. */
		public boolean offersSetup() {
			return switch (status) {
			case RSERVE_MISSING, RSERVE_LOAD_ERROR, NOT_COOPERATIVE, R_EXITED -> true;
			default -> false;
			};
		}

		/** A message for the user. */
		public String message() {
			String d = detail == null || detail.isBlank() ? "" : detail.strip();
			return switch (status) {
			case CONNECTED -> "Connected to Rserve on port " + port + ".";
			case R_NOT_RUNNING -> "R is not running in the Bio7 console.\nPlease start the native R shell in the console first.";
			case RSERVE_MISSING -> "The Rserve package is not installed in the R of the Bio7 console.\n\n"
					+ "Use 'R and Rserve Setup' to install the cooperative Rserve build.";
			case RSERVE_LOAD_ERROR -> "The Rserve package is installed but can't be loaded:\n" + d + "\n\n"
					+ "It was probably built for another version or architecture of R. Use 'R and Rserve Setup' to install it again.";
			case PORT_IN_USE -> "Port " + port + " is already in use (e.g., by an orphaned Rserve or R process).\n"
					+ "Stop that process or remove the port from the Rserve startup arguments in the preferences.";
			case START_ERROR -> "Rserve could not be started:\n" + d;
			case NOT_COOPERATIVE -> "The installed Rserve is not built for the cooperative mode: it runs each connection in a forked copy of R, "
					+ "so Bio7 and the console would not share their objects.\nThe connection was closed.\n\n"
					+ "Use 'R and Rserve Setup' to install the cooperative Rserve build.";
			case WRONG_SERVER -> "The Rserve on port " + port + " does not belong to the R of the Bio7 console" + (d.isEmpty() ? "" : " (" + d + ")")
					+ ".\nThe connection was closed. Please stop the other R/Rserve process (e.g., with 'Kill All R Processes').";
			case R_EXITED -> "R quit while Rserve was started. Please see the console for the error.";
			case TIMEOUT -> "Rserve did not answer on port " + port + "." + (d.isEmpty() ? "" : "\nLast error: " + d)
					+ "\n\nPlease see the console of R. A firewall can also block the local connection.";
			case CANCELED -> "The connection was canceled.";
			};
		}
	}

	/* Printed by Rserve when it runs: "-- running Rserve in this R session (pid=123), 1 server(s) --" */
	private static final Pattern RUNNING = Pattern.compile("running Rserve in this R session \\(pid=(\\d+)\\)");
	private static final Pattern PORT_ARG = Pattern.compile("(?<![\\w.])port\\s*=\\s*['\"]?(\\d+)");
	private static final String MISSING = "BIO7_RSERVE_MISSING";
	private static final String LOAD_ERROR = "BIO7_RSERVE_LOAD_ERROR:";
	private static final String START_ERROR = "BIO7_RSERVE_ERROR:";
	private static final String INFO = "BIO7_RSERVE_INFO:";

	private RserveLauncher() {
	}

	/**
	 * Starts Rserve in the R process of the console and connects to it.
	 *
	 * @param monitor        the monitor (cancel)
	 * @param rserveArgs     the arguments of run.Rserve of the preferences (e.g.,
	 *                       "port=6312") or empty
	 * @param preferredPort  the port (if the arguments contain no port)
	 * @param timeoutSeconds the time to wait for Rserve
	 * @return the result (the connection is set in RServe by the caller)
	 */
	public static Result start(IProgressMonitor monitor, String rserveArgs, int preferredPort, int timeoutSeconds) {
		ConsolePageParticipant cpp = ConsolePageParticipant.getConsolePageParticipantInstance();
		Process rProcess = cpp == null ? null : cpp.getRProcess();
		if (!"R".equals(ConsolePageParticipant.getInterpreterSelection()) || rProcess == null || !rProcess.isAlive()) {
			return new Result(Status.R_NOT_RUNNING, null, preferredPort, null, "");
		}
		String args = rserveArgs == null ? "" : rserveArgs.trim();
		StringBuilder notes = new StringBuilder();
		/* The port: of the arguments (the user decides) or of the preferences (else a free one) */
		Matcher m = PORT_ARG.matcher(args);
		int port;
		if (m.find()) {
			port = Integer.parseInt(m.group(1));
			if (isListening(port)) {
				return new Result(Status.PORT_IN_USE, null, port, null, "");
			}
		} else {
			port = preferredPort > 0 ? preferredPort : 6311;
			if (isListening(port)) {
				int free = freePort();
				if (free < 0) {
					return new Result(Status.PORT_IN_USE, null, port, null, "");
				}
				notes.append("Port ").append(port).append(" is in use (e.g., by an orphaned Rserve), Rserve uses port ").append(free).append(".\n");
				port = free;
			}
			args = "port=" + port + (args.isEmpty() ? "" : ", " + args);
		}
		File script;
		try {
			script = writeStartScript(args);
		} catch (IOException e) {
			return new Result(Status.START_ERROR, null, port, "The start script could not be written: " + e.getMessage(), notes.toString());
		}
		ConsolePageParticipant.startROutputCapture();
		try {
			String path = script.getAbsolutePath().replace("\\", "/").replace("'", "\\'");
			ConsolePageParticipant.pipeInputToConsole("source('" + path + "')", true, false);
			return waitForRserve(monitor, rProcess, port, timeoutSeconds, notes);
		} finally {
			ConsolePageParticipant.stopROutputCapture();
		}
	}

	private static Result waitForRserve(IProgressMonitor monitor, Process rProcess, int port, int timeoutSeconds, StringBuilder notes) {
		long deadline = System.nanoTime() + Math.max(5, timeoutSeconds) * 1_000_000_000L;
		long delay = 100;
		String lastError = null;
		boolean infoChecked = false;
		while (System.nanoTime() < deadline) {
			if (monitor.isCanceled() || RConnectionJob.isCanceled()) {
				return new Result(Status.CANCELED, null, port, null, notes.toString());
			}
			if (!rProcess.isAlive()) {
				return new Result(Status.R_EXITED, null, port, null, notes.toString());
			}
			String out = ConsolePageParticipant.getROutputCapture();
			if (out.contains(MISSING)) {
				return new Result(Status.RSERVE_MISSING, null, port, null, notes.toString());
			}
			String loadError = markerText(out, LOAD_ERROR);
			if (loadError != null) {
				return new Result(Status.RSERVE_LOAD_ERROR, null, port, loadError, notes.toString());
			}
			String startError = markerText(out, START_ERROR);
			if (startError != null) {
				/* "Unable to start Rserve server": the port could not be opened */
				Status status = startError.contains("Unable to start Rserve server") ? Status.PORT_IN_USE : Status.START_ERROR;
				return new Result(status, null, port, startError, notes.toString());
			}
			if (!infoChecked) {
				String info = markerText(out, INFO);
				if (info != null) {
					infoChecked = true;
					String warning = builtForOtherR(info);
					if (warning != null) {
						notes.append(warning).append('\n');
					}
				}
			}
			Matcher running = RUNNING.matcher(out);
			Long sessionPid = running.find() ? Long.valueOf(running.group(1)) : null;
			/* Try to connect (also without the message: older versions of Rserve may not print it) */
			if (sessionPid != null || out.contains(INFO)) {
				try {
					RConnection con = new RConnection("127.0.0.1", port);
					return verify(con, rProcess, sessionPid, port, notes);
				} catch (RserveException e) {
					lastError = e.getMessage();
				}
			}
			monitor.subTask("Waiting for Rserve on port " + port);
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return new Result(Status.CANCELED, null, port, null, notes.toString());
			}
			delay = Math.min(delay * 2, 1000);
		}
		return new Result(Status.TIMEOUT, null, port, lastError, notes.toString());
	}

	/* The connection must belong to the R process of the console */
	private static Result verify(RConnection con, Process rProcess, Long sessionPid, int port, StringBuilder notes) {
		long serverPid;
		try {
			serverPid = con.eval("Sys.getpid()").asInteger();
		} catch (Exception e) {
			/* Can't be checked: accept the connection */
			notes.append("The process of Rserve could not be checked: ").append(e.getMessage()).append('\n');
			return new Result(Status.CONNECTED, con, port, null, notes.toString());
		}
		long expected = sessionPid != null ? sessionPid : rProcess.pid();
		if (serverPid == expected) {
			return new Result(Status.CONNECTED, con, port, null, notes.toString());
		}
		boolean windows = RLocator.os().equals("Windows");
		/* Without the message of Rserve on Windows: R may be a child of the started process (bin/Rterm.exe) */
		if (sessionPid == null && windows && rProcess.toHandle().descendants().anyMatch(h -> h.pid() == serverPid)) {
			return new Result(Status.CONNECTED, con, port, null, notes.toString());
		}
		Optional<Long> parent = ProcessHandle.of(serverPid).flatMap(ProcessHandle::parent).map(ProcessHandle::pid);
		con.close();
		if (parent.isPresent() && parent.get() == expected) {
			/* A forked Rserve: stop the server loop in R (the console would block) */
			interruptR(expected);
			return new Result(Status.NOT_COOPERATIVE, null, port, null, notes.toString());
		}
		return new Result(Status.WRONG_SERVER, null, port, "R process " + expected + ", Rserve process " + serverPid, notes.toString());
	}

	/* Sends SIGINT to R (Linux, Mac) which stops the server loop of Rserve */
	private static void interruptR(long pid) {
		if (RLocator.os().equals("Windows")) {
			return;
		}
		try {
			new ProcessBuilder("kill", "-INT", Long.toString(pid)).start();
		} catch (IOException e) {
			/* The user can interrupt R in the console */
		}
	}

	/* "version=1.8-15;built=R 4.4.1; aarch64-apple-darwin20; ...;r=4.6.0" -> a warning or null */
	static String builtForOtherR(String info) {
		Matcher built = Pattern.compile("built=R (\\d+\\.\\d+)").matcher(info);
		Matcher r = Pattern.compile("r=(\\d+\\.\\d+)").matcher(info);
		if (built.find() && r.find() && !built.group(1).equals(r.group(1))) {
			return "Warning: the Rserve package was built for R " + built.group(1) + " (this is R " + r.group(1)
					+ "). If the connection fails, install it again ('R and Rserve Setup' in the R preferences).";
		}
		return null;
	}

	/* The text of a marker line (to the end of the line) or null */
	static String markerText(String out, String marker) {
		int i = out.indexOf(marker);
		if (i < 0) {
			return null;
		}
		int end = out.indexOf('\n', i);
		/* Wait for the complete line */
		if (end < 0) {
			return null;
		}
		return out.substring(i + marker.length(), end).strip();
	}

	/* The script which is sourced by the R process of the console (the markers are not echoed) */
	static File writeStartScript(String runArgs) throws IOException {
		String r = """
				(function() {
				  cat('\\n')
				  if (!nzchar(system.file(package = 'Rserve'))) {
				    cat('BIO7_RSERVE_MISSING\\n')
				    return(invisible(FALSE))
				  }
				  ok <- tryCatch({
				    suppressPackageStartupMessages(library(Rserve))
				    TRUE
				  }, error = function(e) {
				    cat('BIO7_RSERVE_LOAD_ERROR:', gsub('[\\r\\n]+', ' ', conditionMessage(e)), '\\n')
				    FALSE
				  })
				  if (!ok) return(invisible(FALSE))
				  d <- utils::packageDescription('Rserve')
				  cat('BIO7_RSERVE_INFO: version=', d$Version, ';built=', d$Built, ';r=', as.character(getRversion()), '\\n', sep = '')
				  tryCatch(Rserve::run.Rserve(%s),
				    error = function(e) cat('BIO7_RSERVE_ERROR:', gsub('[\\r\\n]+', ' ', conditionMessage(e)), '\\n'))
				  invisible(TRUE)
				})()
				""".formatted(runArgs);
		File dir = new File(System.getProperty("java.io.tmpdir"), "bio7-rserve");
		dir.mkdirs();
		File file = new File(dir, "bio7_rserve_start.R");
		Files.writeString(file.toPath(), r, StandardCharsets.UTF_8);
		file.deleteOnExit();
		return file;
	}

	/** Returns true if a server listens on the local port. */
	public static boolean isListening(int port) {
		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 300);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/* A free local port or -1 */
	private static int freePort() {
		try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
			return socket.getLocalPort();
		} catch (IOException e) {
			return -1;
		}
	}

	/**
	 * Stops the R process of the Bio7 console and closes the Rserve connection
	 * (e.g., before Rserve is installed again on Windows, where the loaded library
	 * can't be replaced).
	 */
	public static void stopConsoleR() {
		RConnection con = RServe.getConnection();
		if (con != null) {
			con.close();
			RServe.setConnection(null);
			REditor.setConnection(null);
			WorldWindView.setRConnection(null);
		}
		RServe.setRrunning(false);
		ConsolePageParticipant cpp = ConsolePageParticipant.getConsolePageParticipantInstance();
		if (cpp != null && cpp.getRProcess() != null) {
			cpp.getRProcess().destroy();
			cpp.setRProcess(null);
		}
	}

	/** Returns true if R runs in the Bio7 console. */
	public static boolean isConsoleRRunning() {
		ConsolePageParticipant cpp = ConsolePageParticipant.getConsolePageParticipantInstance();
		return cpp != null && cpp.getRProcess() != null && cpp.getRProcess().isAlive();
	}
}
