package ij;

import ij.process.*;
import ij.gui.*;
import ij.io.*;
import ij.measure.*;
import ij.plugin.filter.*;
import ij.macro.Interpreter;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.Locale;
import java.util.Hashtable;

/** The class contains static methods that perform macro operations. */
public class Macro {

	public static final String MACRO_CANCELED = "Macro canceled";

	// A table of Thread as keys and String as values, so
	// Macro options are local to each calling thread.
	static private Hashtable table = new Hashtable();
	static boolean abort;

	public static boolean open(String path) {
		if (path == null || path.equals("")) {
			Opener o = new Opener();
			return true;
		}
		Opener o = new Opener();
		ImagePlus img = o.openImage(path);
		if (img == null)
			return false;
		img.show();
		return true;
	}

	public static boolean saveAs(String path) {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp == null)
			return false;
		FileSaver fs = new FileSaver(imp);
		if (path == null || path.equals(""))
			return fs.saveAsTiff();
		if (imp.getStackSize() > 1)
			return fs.saveAsTiffStack(path);
		else
			return fs.saveAsTiff(path);
	}

	public static String getName(String path) {
		int i = path.lastIndexOf('/');
		if (i == -1)
			i = path.lastIndexOf('\\');
		if (i > 0)
			return path.substring(i + 1);
		else
			return path;
	}

	public static String getDir(String path) {
		int i = path.lastIndexOf('/');
		if (i == -1)
			i = path.lastIndexOf('\\');
		if (i > 0)
			return path.substring(0, i + 1);
		else
			return "";
	}

	/** Aborts the currently running macro or any plugin using IJ.run(). */
	public static void abort() {
		// IJ.log("Abort: "+Thread.currentThread().getName());
		abort = true;
		if (Thread.currentThread().getName().endsWith("Macro$")) {
			table.remove(Thread.currentThread());
			/* Changed for Bio7 to get a message instead of the stack trace! */
			try {
				throw new RuntimeException(MACRO_CANCELED);
			} catch (Exception e) {
				System.out.println(MACRO_CANCELED);
			}

		}
	}

	/**
	 * If a command started using run(name, options) is running, and the current
	 * thread is the same thread, returns the options string, otherwise, returns
	 * null.
	 * 
	 * @see ij.gui.GenericDialog
	 * @see ij.io.OpenDialog
	 */
	public static String getOptions() {
		String threadName = Thread.currentThread().getName();

		// IJ.log("getOptions: "+threadName+" "+Thread.currentThread().hashCode()); //ts
		/*Changed for Bio7. Added Eclipse 'Worker' thread name!*/
		if (threadName.startsWith("Run$_") || threadName.contains("Worker")||threadName.startsWith("RMI TCP")) {
			Object options = table.get(Thread.currentThread());
			return options == null ? null : options + " ";
		} else
			return null;
	}

	/** Define a set of Macro options for the current Thread. */
	public static void setOptions(String options) {
		// IJ.log("setOptions: "+Thread.currentThread().getName()+"
		// "+Thread.currentThread().hashCode()+" "+options); //ts
		if (options == null || options.equals(""))
			table.remove(Thread.currentThread());
		else
			table.put(Thread.currentThread(), options);
	}

	/** Define a set of Macro options for a Thread. */
	public static void setOptions(Thread thread, String options) {
		if (null == thread)
			throw new RuntimeException("Need a non-null thread instance");
		if (null == options)
			table.remove(thread);
		else
			table.put(thread, options);
	}

	public static String getValue(String options, String key, String defaultValue) {
		key = trimKey(key);
		if (!options.endsWith(" "))
			options = options + " ";
		key += '=';
		int index = -1;
		do { // Require that key not be preceded by a letter
			index = options.indexOf(key, ++index);
			if (index < 0)
				return defaultValue;
		} while (index != 0 && Character.isLetter(options.charAt(index - 1)));
		options = options.substring(index + key.length(), options.length());
		if (options.charAt(0) == '\'') {
			index = options.indexOf("'", 1);
			if (index < 0)
				return defaultValue;
			else
				return options.substring(1, index);
		} else if (options.charAt(0) == '[') {
			int count = 1;
			index = -1;
			for (int i = 1; i < options.length(); i++) {
				char ch = options.charAt(i);
				if (ch == '[')
					count++;
				else if (ch == ']')
					count--;
				if (count == 0) {
					index = i;
					break;
				}
			}
			if (index < 0)
				return defaultValue;
			else
				return options.substring(1, index);
		} else {
			index = options.indexOf(" ");
			if (index < 0)
				return defaultValue;
			else
				return options.substring(0, index);
		}
	}

	public static String trimKey(String key) {
		if (key==null)
			return key;
		int index = key.indexOf(" ");
		if (index > -1)
			key = key.substring(0, index);
		index = key.indexOf(":");
		if (index > -1)
			key = key.substring(0, index);
		key = key.toLowerCase(Locale.US);
		return key;
	}

	/**
	 * Evaluates 'code' and returns the output, or any error, as a String (e.g.,
	 * Macro.eval("2+2") returns "4").
	 */
	public static String eval(String code) {
		return new Interpreter().eval(code);
	}

}
