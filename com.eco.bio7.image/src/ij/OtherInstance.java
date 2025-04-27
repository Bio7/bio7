package ij;

import ij.IJ;
import ij.ImageJ;
import ij.Prefs;

import ij.io.OpenDialog;
import ij.io.Opener;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.lang.reflect.Method;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import java.util.Properties;

/*
 * This class tries to contact another instance on the same machine, started
 * by the current user.  If such an instance is found, the arguments are
 * sent to that instance.  If no such an instance is found, listen for clients.
 *
 * No need for extra security, as the stub (and its serialization) contain
 * a hard-to-guess hash code.
 *
 *	@author Johannes Schindelin
 */
public class OtherInstance {
	private static final String DELIMETER = "~!~"; // Separates macro name and argument

	interface ImageJInstance extends Remote {
		void sendArgument(String arg) throws RemoteException;
	}

	static class Implementation implements ImageJInstance {
		int counter = 0;

		public void sendArgument(String cmd) {
			if (IJ.debugMode) IJ.log("SocketServer.sendArgument: \""+ cmd+"\"");
			if (cmd.startsWith("open "))
				(new Opener()).openAndAddToRecent(cmd.substring(5));
			else if (cmd.startsWith("macro ")) {
				String name = cmd.substring(6);
				String name2 = name;
				String arg = null;
				int index = name2.indexOf(DELIMETER);
				if (index!=-1) {
					name = name2.substring(0, index);
					arg = name2.substring(index+DELIMETER.length(), name2.length());
				}
				IJ.runMacroFile(name, arg);
			} else if (cmd.startsWith("run "))
				IJ.run(cmd.substring(4));
			else if (cmd.startsWith("eval ")) {
				String rtn = IJ.runMacro(cmd.substring(5));
				if (rtn!=null)
					System.out.print(rtn);
			} else if (cmd.startsWith("user.dir ")) {
				OpenDialog.setDefaultDirectory(cmd.substring(9));
				System.setProperty("user.dir", cmd.substring(9));
			}
		}
	}

	public static String getStubPath() {
		String display = System.getenv("DISPLAY");
		if (display!=null) {
			display = display.replace(':', '_');
			display = display.replace('/', '_');
		}
		String tmpDir = System.getProperty("java.io.tmpdir");
		tmpDir = IJ.addSeparator(tmpDir);
		return tmpDir + "ImageJ-"
			+ System.getProperty("user.name") + "-"
			+ (display == null ? "" : display + "-")
			+ ImageJ.getPort() + ".stub";
	}

	public static void makeFilePrivate(String path) {
		try {
			File file = new File(path);
			file.deleteOnExit();

			// File.setReadable() is Java 6
			Class[] types = { boolean.class, boolean.class };
			Method m = File.class.getMethod("setReadable", types);
			Object[] arguments = { Boolean.FALSE, Boolean.FALSE };
			m.invoke(file, arguments);
			arguments = new Object[] { Boolean.TRUE, Boolean.TRUE };
			m.invoke(file, arguments);
			types = new Class[] { boolean.class };
			m = File.class.getMethod("setWritable", types);
			arguments = new Object[] { Boolean.FALSE };
			m.invoke(file, arguments);
			return;
		} catch (Exception e) {
			if (IJ.debugMode)
				System.err.println("Java < 6 detected,"
					+ " trying chmod 0600 " + path);
		}
		if (!IJ.isWindows()) {
			try {
				String[] command = {
					"chmod", "0600", path
				};
				Runtime.getRuntime().exec(command);
			} catch (Exception e) {
				if (IJ.debugMode)
					System.err.println("Even chmod failed.");
			}
		}
	}

	public static boolean sendArguments(String[] args) {
		if (!isRMIEnabled())
			return false;
		String file = getStubPath();
		try {
			FileInputStream in = new FileInputStream(file);
			ImageJInstance instance = (ImageJInstance) new ObjectInputStream(in).readObject();
			in.close();
			if (instance==null)
				return false;
			instance.sendArgument("user.dir "+System.getProperty("user.dir"));
			int macros = 0;
			for (int i=0; i<args.length; i++) {
				String arg = args[i];
				if (arg==null)
					continue;
				String cmd = null;
				if (macros==0 && arg.endsWith(".ijm")) {
					cmd = "macro " + arg;
					macros++;
				} else if (arg.startsWith("-macro") && i+1<args.length) {
					String macroArg = i+2<args.length?DELIMETER+args[i+2]:"";
					cmd = "macro " + args[i+1] + macroArg;
					instance.sendArgument(cmd);
					break;
				} else if (arg.startsWith("-eval") && i+1<args.length) {
					cmd = "eval " + args[i+1];
					args[i+1] = null;
				} else if (arg.startsWith("-run") && i+1<args.length) {
					cmd = "run " + args[i+1];
					args[i+1] = null;
				} else if (arg.indexOf("ij.ImageJ")==-1 && !arg.startsWith("-"))
					cmd = "open " + arg;
				if (cmd!=null)
					instance.sendArgument(cmd);
			} // for
			return true;
		} catch (Exception e) {
			if (IJ.debugMode) {
				System.err.println("Client exception: " + e);
				e.printStackTrace();
			}
			new File(file).delete();
		}
		if (!new File(file).exists())
			startServer();
		//IJ.log("sendArguments: return false ");
		return false;
	}

	static ImageJInstance stub;
	static Implementation implementation;

	public static void startServer() {
		if (IJ.debugMode)
			System.err.println("OtherInstance: starting server");
		try {
			implementation = new Implementation();
			stub = (ImageJInstance)UnicastRemoteObject.exportObject(implementation, 0);

			// Write serialized object
			String path = getStubPath();
			FileOutputStream out = new FileOutputStream(path);
			makeFilePrivate(path);
			new ObjectOutputStream(out).writeObject(stub);
			out.close();

			if (IJ.debugMode)
				System.err.println("OtherInstance: server ready");
		} catch (Exception e) {
			if (IJ.debugMode) {
				System.err.println("Server exception: " + e);
				e.printStackTrace();
			}
		}
	}

	private static final String OPTIONS = "prefs.options";
	private static final int RUN_SOCKET_LISTENER=1<<22;

	public static boolean isRMIEnabled() {
		if (System.getProperty("os.name").startsWith("Mac"))
			return true;
		Properties ijProps = loadPrefs();
		if (ijProps==null)
			return true;
		int options = getInt(ijProps, OPTIONS);
		if (options==-1)
			return true;
		return (options&RUN_SOCKET_LISTENER)!=0;
	}
	
	protected static int getInt(Properties props, String key) {
		String s = props.getProperty(key);
		if (s!=null) {
			try {
				return Integer.decode(s).intValue();
			} catch (NumberFormatException e) {IJ.log(""+e);}
		}
		return -1;
	}

	protected static Properties loadPrefs() {
		Properties result = new Properties();
		File file = new File(getPrefsDirectory(), "IJ_Prefs.txt");
		try {
			InputStream in = new BufferedInputStream(new FileInputStream(file));
			result.load(in);
			in.close();
		} catch (IOException e) { /* ignore */ }
		return result;
	}

	protected static String getPrefsDirectory() {
		String env = System.getenv("IJ_PREFS_DIR");
		if (env != null)
			return env;
		else
			return Prefs.getPrefsDir();
	}
}
