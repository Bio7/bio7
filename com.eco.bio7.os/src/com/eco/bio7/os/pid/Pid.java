package com.eco.bio7.os.pid;

public class Pid {

	public long getPidWindows(Process process) {
		long pid = 0;
		pid = process.pid();
		return pid;
	}

	public long getPidUnix(Process process) {
		long pid = 0;
		pid = process.pid();
		return pid;
	}

}
