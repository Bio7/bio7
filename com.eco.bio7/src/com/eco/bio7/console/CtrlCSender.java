package com.eco.bio7.console;
import com.sun.jna.platform.win32.Kernel32;    
/*Code from: https://stackoverflow.com/questions/813086/can-i-send-a-ctrl-c-sigint-to-an-application-on-windows?noredirect=1&lq=1
 *Author: https://stackoverflow.com/users/3372187/patimo*/
public class CtrlCSender {

    public static void main(String args[]) {
        int processId = Integer.parseInt(args[0]);
        Kernel32.INSTANCE.AttachConsole(processId);
        Kernel32.INSTANCE.GenerateConsoleCtrlEvent(Kernel32.CTRL_C_EVENT, 0);
    }
}