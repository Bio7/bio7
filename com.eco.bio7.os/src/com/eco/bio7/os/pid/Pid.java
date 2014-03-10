package com.eco.bio7.os.pid;

import java.lang.reflect.Field;

import com.sun.jna.Pointer;

public class Pid {

	public int getPidWindows(Process process) {
		int pid = 0;
		if (process.getClass().getName().equals("java.lang.Win32Process") || process.getClass().getName().equals("java.lang.ProcessImpl")) {
			/* determine the pid on windows plattforms */
			try {
				Field f = process.getClass().getDeclaredField("handle");
				f.setAccessible(true);
				long handl = f.getLong(process);

				Kernel32 kernel = Kernel32.INSTANCE;
				W32API.HANDLE handle = new W32API.HANDLE();
				handle.setPointer(Pointer.createConstant(handl));
				pid = kernel.GetProcessId(handle);
			} catch (Throwable e) {
			}
		}
		return pid;
	}

	public int getPidUnix(Process process) {
		int pid = 0;
		if (process.getClass().getName().equals("java.lang.UNIXProcess")) {
			/* get the PID on unix/linux systems */
			try {
				Field f = process.getClass().getDeclaredField("pid");
				f.setAccessible(true);
				pid = (int) f.get(process);
			} catch (Throwable e) {
			}
		}
		return pid;
	}

}
