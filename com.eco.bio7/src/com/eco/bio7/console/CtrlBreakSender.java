package com.eco.bio7.console;
import com.sun.jna.platform.win32.Kernel32;    

public class CtrlBreakSender {

    public static void main(String args[]) {
        int processId = Integer.parseInt(args[0]);
        Kernel32.INSTANCE.AttachConsole(processId);
        Kernel32.INSTANCE.GenerateConsoleCtrlEvent(Kernel32.CTRL_BREAK_EVENT, 0);
    }
}