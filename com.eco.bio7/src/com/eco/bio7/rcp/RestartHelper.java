package com.eco.bio7.rcp;

import java.io.File;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Starts Bio7 again after the former instance has ended (Windows; a small Java
 * program without other classes, started with javaw.exe of Bio7's Java):
 * waits until the former process has ended, ends it if it hangs in its exit,
 * waits until the workspace is unlocked and starts the launcher.
 * <p>
 * Arguments: the process id, the lock file of the workspace (or "-"), the
 * command (the launcher and its arguments).
 */
public final class RestartHelper {

	/* The former process: ended after this time if its exit hangs */
	private static final long EXIT_WAIT_SECONDS = 30;
	/* The workspace: started anyway after this time (the new Bio7 then reports the locked workspace) */
	private static final long LOCK_WAIT_MILLIS = 30_000;

	private RestartHelper() {
	}

	public static void main(String[] args) throws Exception {
		if (args.length < 3) {
			return;
		}
		long pid = Long.parseLong(args[0]);
		String lockFile = args[1];
		List<String> command = new ArrayList<>(List.of(args).subList(2, args.length));

		Optional<ProcessHandle> former = ProcessHandle.of(pid);
		if (former.isPresent()) {
			try {
				former.get().onExit().get(EXIT_WAIT_SECONDS, TimeUnit.SECONDS);
			} catch (java.util.concurrent.TimeoutException e) {
				/* The exit hangs (the workbench is closed and saved already): ended */
				former.get().destroyForcibly();
				former.get().onExit().get(10, TimeUnit.SECONDS);
			}
		}
		if (!"-".equals(lockFile)) {
			waitForUnlock(Path.of(lockFile));
		}
		ProcessBuilder builder = new ProcessBuilder(command);
		builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
		builder.redirectError(ProcessBuilder.Redirect.DISCARD);
		File dir = new File(command.get(0)).getParentFile();
		if (dir != null && dir.isDirectory()) {
			builder.directory(dir);
		}
		builder.start();
	}

	/* Waits until the lock of the workspace can be taken (the former Bio7 released it) */
	private static void waitForUnlock(Path lock) throws InterruptedException {
		long end = System.currentTimeMillis() + LOCK_WAIT_MILLIS;
		while (System.currentTimeMillis() < end) {
			if (!lock.toFile().exists()) {
				return;
			}
			try (FileChannel ch = FileChannel.open(lock, StandardOpenOption.WRITE); FileLock l = ch.tryLock()) {
				if (l != null) {
					return;
				}
			} catch (Exception e) {
				/* Still locked (or not accessible yet) */
			}
			Thread.sleep(200);
		}
	}
}
