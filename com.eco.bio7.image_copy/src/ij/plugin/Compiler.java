package ij.plugin;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.util.*;

import ij.*;
import ij.gui.*;
import ij.io.*;
import ij.util.*;
import ij.plugin.frame.Editor;
import ij.plugin.filter.*;
import ij.text.TextWindow;
import java.awt.event.KeyEvent;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.internal.compiler.batch.Main;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

import com.eco.bio7.image.ScanClassPath;
import com.eco.bio7.javaeditor.Bio7EditorPlugin;

/** Compiles and runs plugins using the javac compiler. */
public class Compiler implements PlugIn, FilenameFilter {

	private static final int TARGET14=0, TARGET15=1, TARGET16=2,  TARGET17=3;
	private static final String[] targets = {"1.4", "1.5", "1.6", "1.7"};
	private static final String TARGET_KEY = "javac.target";
	//private static com.sun.tools.javac.Main javac;
	private static ByteArrayOutputStream output;
	private static String dir, name;
	private static Editor errors;
	private static boolean generateDebuggingInfo;
	private static int target = (int)Prefs.get(TARGET_KEY, TARGET15); 
	private static boolean checkForUpdateDone;
	private static Main comp;
	private static String bundle;
	private static String pathBundle;
	private String pathBundle2;
	private String pathBundle3;
	private static boolean warningInfo = false;
	

	public void run(String arg) {
		if (arg.equals("edit"))
			edit();
		else if (arg.equals("options"))
			showDialog();
		else
			compileAndRun(arg);
	 }
	 
	void edit() {
		if (open("", "Open macro or plugin")) {
			Editor ed = (Editor)IJ.runPlugIn("ij.plugin.frame.Editor", "");
			if (ed!=null) ed.open(dir, name);
		}
	}
	
	void compileAndRun(String path) {
		if (!open(path, "Compile and Run Plugin..."))
			return;
		if (name.endsWith(".class")) {
			runPlugin(name.substring(0, name.length()-1));
			return;
		}
		if (!isJavac()){
			return;
		}
		if (compile(dir+name)){}
		/*IJ.showMessage("Compilation", "Compilation successfull!\nPlease refresh the file folder\n(context menu of the Navigator view)\n" +
				"to access the file!");*/
			//runPlugin(name);
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				MessageBox messageBox = new MessageBox(new Shell(),

				SWT.ICON_WARNING);
				messageBox.setText("Info!");
				messageBox.setMessage("Compilation done!\n\nPlease consult the console output for errors!\n\nIf not compiled with the ImageJ-Canvas menu actions:\n\nPlease refresh the file folder\n(context menu of the Navigator view)\n" +
						"to access the file(s)!");
				messageBox.open();
			}
		});
	}
	
	 /*Changed for Bio7!
	  * 
	  * private void checkForUpdate(String plugin, String currentVersion) {
		                int slashIndex = plugin.lastIndexOf("/");
		                if (slashIndex==-1 || !plugin.endsWith(".jar"))
		                        return;
		                String className = plugin.substring(slashIndex+1, plugin.length()-4);
		                File f = new File(Prefs.getImageJDir()+"plugins"+File.separator+"jars"+File.separator+className+".jar");
		                if (!f.exists() || !f.canWrite()) {
		                        if (IJ.debugMode) IJ.log("checkForUpdate: jar not found ("+plugin+")");
		                        return;
		                }
		                String version = null;
		                try {
		                        Class c = IJ.getClassLoader().loadClass("Compiler");
		                        version = "0.00a";
		                        Method m = c.getDeclaredMethod("getVersion", new Class[0]);
		                        version = (String)m.invoke(null, new Object[0]);
		                }
		                catch (Exception e) {}
		                if (version==null) {
		                        if (IJ.debugMode) IJ.log("checkForUpdate: class not found ("+className+")");
		                        return;
		                }
		                if (version.compareTo(currentVersion)>=0) {
		                        if (IJ.debugMode) IJ.log("checkForUpdate: up to date ("+className+"  "+version+")");
		                        return;
		                }
		                boolean ok = Macro_Runner.downloadJar(plugin);
		                if (IJ.debugMode) IJ.log("checkForUpdate: "+className+" "+version+" "+ok);
		       }*/
	 
	boolean isJavac() {
		/* Changed for Bio7! */
		try {
			// if (javac==null) {
			
			


			output = new ByteArrayOutputStream(4096);
			// javac = new sun.tools.javac.Main(output, "javac");
			// bundle =
			// com.eco.bio7.image.Activator.getDefault().getBundle().getHeaders().get("Bundle-ClassPath").toString();
			comp = new Main(new PrintWriter(output), new PrintWriter(output), false);
			// }
		} catch (NoClassDefFoundError e) {
			IJ.error("This JVM does not include the javac compiler.\n" + "Javac is included with the Windows and Linux\n" + "versions of ImageJ that are bundled with Java.");
			return false;
		}
		return true;

	}

	boolean compile(String path) {
		IJ.showStatus("compiling: " + path);
		// System.getProperty("java.class.path");
		/* Add the classpath ! */
		//String classpath = File.pathSeparator +pathBundle3 + "/R/RserveEngine.jar"+File.pathSeparator +pathBundle3 + "/R/REngine.jar"+File.pathSeparator +pathBundle2 + "/bin" + File.pathSeparator + pathBundle + "/bin" + File.pathSeparator + getClassPath(path);
		File f = new File(path);
		String fileDirectory = File.pathSeparator + f.getParent();
		if (f != null) // add directory containing file to classpath
	      

		// IJ.log("classpath: " + classpath);
		output.reset();
		Vector<String> options = new Vector<String>();
		IPreferenceStore store = Bio7EditorPlugin.getDefault().getPreferenceStore();
		String version = store.getString("compiler_version");
		boolean debug = store.getBoolean("compiler_debug");
		boolean verbose = store.getBoolean("compiler_verbose");
		boolean warnings = store.getBoolean("compiler_warnings");
		options.addElement("-source");
		options.addElement(version);
		options.addElement("-target");
		options.addElement(version);
		options.addElement("-deprecation");
		if (debug) {
			options.addElement("-g");
		} else {
			options.addElement("-g:none");
		}
		if (verbose) {
			options.addElement("-verbose");
		}

		if (warnings == false) {
			options.addElement("-nowarn");
		}
		options.addElement("-classpath");
		/*Add the Bio7 classpath and the path to the file (to compile the dependencies, too)!*/
		options.addElement(new ScanClassPath().scan()+fileDirectory);
		options.addElement(path);
		String[] arguments = new String[options.size()];
		options.copyInto((String[]) arguments);
		// String[] arguments;
		/*
		 * if (generateDebuggingInfo) arguments = new String[] { "-g",
		 * "-deprecation","-1.5", "-classpath", classpath, path }; arguments =
		 * new String[] { "-g", "-deprecation","-1.5", "-classpath", classpath,
		 * path }; else arguments = new String[] { "-deprecation",
		 * "-nowarn","-1.5", "-classpath", classpath, path };
		 */
		/*
		 * arguments = new String[] { "-deprecation", "-nowarn","-1.5",
		 * "-classpath", classpath, path };
		 */
		/* Changed for Bio7! */
		/*
		 * if (IJ.debugMode) { String str = "javac"; for (int i=0;
		 * i<arguments.length; i++) str += " "+arguments[i]; IJ.log(str); }
		 */

		boolean compiled = comp.compile(arguments);
		// boolean compiled = false;//javac.compile(arguments);
		String s = output.toString();
		boolean errors = (!compiled || areErrors(s));
		if (errors)
			//showErrors(s);
		System.out.println(s);
		else
			IJ.showStatus("done");
		return compiled;
	 }
	 
	 // Returns a string containing the Java classpath, 
	 // the path to the directory containing the plugin, 
	 // and paths to any .jar files in the plugins folder.
	 String getClassPath(String path) {
		long start = System.currentTimeMillis();
	 	StringBuffer sb = new StringBuffer();
		sb.append(System.getProperty("java.class.path"));
		File f = new File(path);
		if (f!=null)  // add directory containing file to classpath
			sb.append(File.pathSeparator + f.getParent());
		String pluginsDir = Menus.getPlugInsPath();
		if (pluginsDir!=null)
			addJars(pluginsDir, sb);
		return sb.toString();
	 }
	 
	// Adds .jar files in plugins folder, and subfolders, to the classpath
	void addJars(String path, StringBuffer sb) {
		String[] list = null;
		File f = new File(path);
		if (f.exists() && f.isDirectory())
			list = f.list();
		if (list==null) return;
		if (!path.endsWith(File.separator))
			path += File.separator;
		for (int i=0; i<list.length; i++) {
			File f2 = new File(path+list[i]);
			if (f2.isDirectory())
				addJars(path+list[i], sb);
			else if (list[i].endsWith(".jar")&&(list[i].indexOf("_")==-1||list[i].equals("loci_tools.jar"))) {
				sb.append(File.pathSeparator+path+list[i]);
				if (IJ.debugMode) IJ.log("javac: "+path+list[i]);
			}
		}
	}
	 boolean areErrors(String s) {
		                boolean errors = s!=null && s.length()>0;
		                if(errors && s.indexOf("1 warning")>0 && s.indexOf("[deprecation] show()")>0)
		                        errors = false;
		                //if(errors&&s.startsWith("Note:com.sun.tools.javac")&&s.indexOf("error")==-1)
		                //      errors = false;
		                return errors;
		        }
	
	
	void showErrors(String s) {
		if (errors==null || !errors.isVisible()) {
			errors = (Editor)IJ.runPlugIn("ij.plugin.frame.Editor", "");
			errors.setFont(new Font("Monospaced", Font.PLAIN, 12));
		}
		if (errors!=null)
			errors.display("Errors", s);
		IJ.showStatus("done (errors)");
	}

	 // open the .java source file
	 boolean open(String path, String msg) {
	 	boolean okay;
		String fileName, directory;
	 	if (path.equals("")) {
			if (dir==null) dir = IJ.getDirectory("plugins");
			OpenDialog od = new OpenDialog(msg, dir, name);
			directory = od.getDirectory();
			fileName = od.getFileName();
			okay = fileName!=null;
			String lcName = okay?fileName.toLowerCase(Locale.US):null;
			if (okay) {
				if (msg.startsWith("Compile")) {
					if (!(lcName.endsWith(".java")||lcName.endsWith(".class"))) {
						IJ.error("File name must end with \".java\" or \".class\".");
						okay = false;
					}
				} else if (!(lcName.endsWith(".java")||lcName.endsWith(".txt")||lcName.endsWith(".ijm")||lcName.endsWith(".js"))) {
					IJ.error("File name must end with \".java\", \".txt\" or \".js\".");
					okay = false;
				}
			}
		} else {
			int i = path.lastIndexOf('/');
			if (i==-1)
				i = path.lastIndexOf('\\');
			if (i>0) {
				directory = path.substring(0, i+1);
				fileName = path.substring(i+1);
			} else {
				directory = "";
				fileName = path;
			}
			okay = true;
		}
		if (okay) {
			name = fileName;
			dir = directory;
			Editor.setDefaultDirectory(dir);
		}
		return okay;
	}

	// only show files with names ending in ".java"
	// doesn't work with Windows
	public boolean accept(File dir, String name) {
		return name.endsWith(".java")||name.endsWith(".macro")||name.endsWith(".txt");
	}
	
	// run the plugin using a new class loader
	void runPlugin(String name) {
		name = name.substring(0,name.length()-5); // remove ".java"
		new PlugInExecuter(name);
	}
	
	public void showDialog() {
		validateTarget();
		GenericDialog gd = new GenericDialog("Compile and Run");
		gd.addChoice("Target: ", targets, targets[target]);
		gd.setInsets(15,5,0);
		gd.addCheckbox("Generate debugging info (javac -g)", generateDebuggingInfo);
        gd.addHelp(IJ.URL+"/docs/menus/edit.html#compiler");
		gd.showDialog();
		if (gd.wasCanceled()) return;
		target = gd.getNextChoiceIndex();		
		generateDebuggingInfo = gd.getNextBoolean();
		validateTarget();
	}
	
	void validateTarget() {
		if (target<0 || target>TARGET17)
			target = TARGET15;
		if ((target>TARGET16&&!IJ.isJava17()) || (target>TARGET15&&!IJ.isJava16()))
			target = TARGET15;
		if (!IJ.isJava15())
			target = TARGET14;
		Prefs.set(TARGET_KEY, target);
	}
	
}


class PlugInExecuter implements Runnable {
	private String plugin;
	private Thread thread;

	/** Create a new object that runs the specified plugin
		in a separate thread. */
	PlugInExecuter(String plugin) {
		this.plugin = plugin;
		thread = new Thread(this, plugin);
		thread.setPriority(Math.max(thread.getPriority()-2, Thread.MIN_PRIORITY));
		thread.start();
	}

	public void run() {
		IJ.resetEscape();
		IJ.runPlugIn("ij.plugin.ClassChecker", "");
		runCompiledPlugin(plugin);
	}
	
	void runCompiledPlugin(String className) {
		if (IJ.debugMode) IJ.log("runCompiledPlugin: "+className);
		IJ.resetClassLoader();
		ClassLoader loader = IJ.getClassLoader();
		Object thePlugIn = null;
		try { 
			thePlugIn = (loader.loadClass(className)).newInstance(); 
			if (thePlugIn instanceof PlugIn)
 				((PlugIn)thePlugIn).run("");
 			else if (thePlugIn instanceof PlugInFilter)
				new PlugInFilterRunner(thePlugIn, className, "");
		}
		catch (ClassNotFoundException e) {
			if (className.indexOf('_')!=-1)
				IJ.error("Plugin or class not found: \"" + className + "\"\n(" + e+")");
		}
		catch (NoClassDefFoundError e) {
			String err = e.getMessage();
			int index = err!=null?err.indexOf("wrong name: "):-1;
			if (index>-1 && !className.contains(".")) {
				String className2 = err.substring(index+12, err.length()-1);
				className2 = className2.replace("/", ".");
				runCompiledPlugin(className2);
				return;
			}
			if (className.indexOf('_')!=-1)
				IJ.error("Plugin or class not found: \"" + className + "\"\n(" + e+")");
		}
		catch (Exception e) {
			//IJ.error(""+e);
			IJ.handleException(e); //Marcel Boeglin 2013.09.01
			//Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e); //IDE output
		}
	}
	
}

abstract class CompilerTool {
	public static class JavaxCompilerTool extends CompilerTool {
		protected static Class charsetC;
		protected static Class diagnosticListenerC;
		protected static Class javaFileManagerC;
		protected static Class toolProviderC;

		public boolean compile(List sources, List options, StringWriter log) {
			try {
				Object javac = getJavac();

				Class[] getStandardFileManagerTypes = new Class[] { diagnosticListenerC, Locale.class, charsetC };
				Method getStandardFileManager = javac.getClass().getMethod("getStandardFileManager", getStandardFileManagerTypes);
				Object fileManager = getStandardFileManager.invoke(javac, new Object[] { null, null, null });

				Class[] getJavaFileObjectsFromStringsTypes = new Class[] { Iterable.class };
				Method getJavaFileObjectsFromStrings = fileManager.getClass().getMethod("getJavaFileObjectsFromStrings", getJavaFileObjectsFromStringsTypes);
				Object compilationUnits = getJavaFileObjectsFromStrings.invoke(fileManager, new Object[] { sources });

				Class[] getTaskParamTypes = new Class[] { Writer.class, javaFileManagerC, diagnosticListenerC, Iterable.class, Iterable.class, Iterable.class };
				Method getTask = javac.getClass().getMethod("getTask", getTaskParamTypes);
				Object task = getTask.invoke(javac, new Object[] { log, fileManager, null, options, null, compilationUnits });

				Method call = task.getClass().getMethod("call", new Class[0]);
				Object result = call.invoke(task, new Object[0]);

				return Boolean.TRUE.equals(result);
			} catch (Exception e) {
				PrintWriter printer = new PrintWriter(log);
				e.printStackTrace(printer);
				printer.flush();
			}
			return false;
		}

		protected Object getJavac() throws Exception {
			if (charsetC == null) {
				charsetC = Class.forName("java.nio.charset.Charset");
			}
			if (diagnosticListenerC == null) {
				diagnosticListenerC = Class.forName("javax.tools.DiagnosticListener");
			}
			if (javaFileManagerC == null) {
				javaFileManagerC = Class.forName("javax.tools.JavaFileManager");
			}
			if (toolProviderC == null) {
				toolProviderC = Class.forName("javax.tools.ToolProvider");
			}

			Method get = toolProviderC.getMethod("getSystemJavaCompiler", new Class[0]);
			return get.invoke(null, new Object[0]);
		}
	}

	public static class LegacyCompilerTool extends CompilerTool {
		protected static Class javacC;

		boolean areErrors(String s) {
			boolean errors = s != null && s.length() > 0;
			if (errors && s.indexOf("1 warning") > 0 && s.indexOf("[deprecation] show()") > 0)
				errors = false;
			// if(errors&&s.startsWith("Note:com.sun.tools.javac")&&s.indexOf("error")==-1)
			// errors = false;
			return errors;
		}

		public boolean compile(List sources, List options, StringWriter log) {
			try {
				final String[] args = new String[sources.size() + options.size()];
				int argsIndex = 0;
				for (int optionsIndex = 0; optionsIndex < options.size(); optionsIndex++) {
					args[argsIndex++] = (String) options.get(optionsIndex);
				}

				for (int sourcesIndex = 0; sourcesIndex < sources.size(); sourcesIndex++) {
					args[argsIndex++] = (String) sources.get(sourcesIndex);
				}

				Object javac = getJavac();
				Class[] compileTypes = new Class[] { String[].class, PrintWriter.class };
				Method compile = javacC.getMethod("compile", compileTypes);

				PrintWriter printer = new PrintWriter(log);
				Object result = compile.invoke(javac, new Object[] { args, printer });
				printer.flush();

				return Integer.valueOf(0).equals(result) | areErrors(log.toString());
			} catch (Exception e) {
				e.printStackTrace(new PrintWriter(log));
			}
			return false;
		}

		protected Object getJavac() throws Exception {
			if(javacC == null){
				javacC = Class.forName("com.sun.tools.javac.Main");
			}
			return javacC.newInstance();
		}
	}

	public static CompilerTool getDefault() {
		CompilerTool javax = new JavaxCompilerTool();
		if (javax.isSupported())
			return javax;
		
		CompilerTool legacy = new LegacyCompilerTool();
		if (legacy.isSupported())
			return legacy;

		return null;
	}

	public abstract boolean compile(List sources, List options, StringWriter log);

	protected abstract Object getJavac() throws Exception;

	public boolean isSupported() {
		try {
			return null != getJavac();
		} catch (Exception e) {
			return false;
		}
	}
}
