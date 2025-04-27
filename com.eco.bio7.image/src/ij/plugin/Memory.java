package ij.plugin;
import ij.*;
import ij.gui.*;
import java.io.*;
import ij.util.Tools;


/** This plugin implements the Edit/Options/Memory command. */
public class Memory implements PlugIn {
	String s;
	int index1, index2;
	File f;
	boolean fileMissing;
	boolean sixtyFourBit;

	public void run(String arg) {
		changeMemoryAllocation();
		//IJ.log("setting="+getMemorySetting()/(1024*1024)+"MB");
		//IJ.log("maxMemory="+maxMemory()/(1024*1024)+"MB");
	}

	void changeMemoryAllocation() {
		IJ.maxMemory(); // forces IJ to cache old limit
		int max = (int)(getMemorySetting()/1048576L);
		boolean unableToSet = max==0;
		if (max==0) max = (int)(maxMemory()/1048576L);
		String title = "Memory "+(IJ.is64Bit()?"(64-bit)":"(32-bit)");
		GenericDialog gd = new GenericDialog(title);
		gd.addNumericField("Maximum memory:", max, 0, 6, "MB");
		gd.addNumericField("Parallel threads:", Prefs.getThreads(), 0, 6, "");
		gd.setInsets(12, 0, 0);
		gd.addCheckbox("Keep multiple undo buffers", Prefs.keepUndoBuffers);
		gd.setInsets(12, 0, 0);
		gd.addCheckbox("Run garbage collector on status bar click", !Prefs.noClickToGC);
		gd.addHelp(IJ.URL2+"/docs/menus/edit.html#memory");
		gd.showDialog();
		if (gd.wasCanceled()) return;
		int max2 = (int)gd.getNextNumber();
		Prefs.setThreads((int)gd.getNextNumber());
		Prefs.keepUndoBuffers = gd.getNextBoolean();
		Prefs.noClickToGC = !gd.getNextBoolean();
		if (gd.invalidNumber()) {
			IJ.showMessage("Memory", "The number entered was invalid.");
			return;
		}
		if (unableToSet && max2!=max)
			{showError(); return;}
		if (IJ.isMacOSX() && max2<256)
			max2 = 256;
		else if (max2<32)
			max2 = 32;
		if (max2==max) return;
		int limit = IJ.isWindows()?1600:1700;
		String OSXInfo = "";
		if (max2>=limit && !IJ.is64Bit()) {
			if (!IJ.showMessageWithCancel(title, 
			"Note: setting the memory limit to a value\n"
			+"greater than "+limit+"MB on a 32-bit system\n"
			+"may cause ImageJ to fail to start. The title of\n"
			+"the Edit>Options>Memory & Threads dialog\n"
			+"box changes to \"Memory (64-bit)\" when ImageJ\n"
			+"is running on a 64-bit version of Java."))
				return;
		}
		try {
			String s2 = s.substring(index2);
			if (s2.startsWith("g"))
				s2 = "m"+s2.substring(1);
			String s3 = s.substring(0, index1) + max2 + s2;
			FileOutputStream fos = new FileOutputStream(f);
			PrintWriter pw = new PrintWriter(fos);
			pw.print(s3);
			pw.close();
		} catch (IOException e) {
			String error = e.getMessage();
			if (error==null || error.equals("")) error = ""+e;
			String name = IJ.isMacOSX()?"Info.plist":"ImageJ.cfg";
			String msg = 
				   "Unable to update the file \"" + name + "\".\n"
				+ " \n"
				+ "\"" + error + "\"";
			IJ.showMessage("Memory", msg);
			return;
		}
		String hint = "";
		if (IJ.isWindows() && max2>640 && max2>max)
			hint = "\nDelete the \"ImageJ.cfg\" file, located in the ImageJ folder,\nif ImageJ fails to start.";
		IJ.showMessage("Memory", "The new " + max2 +"MB limit will take effect after ImageJ is restarted."+hint);		
	}
	
	public long getMemorySetting() {
		if (IJ.getApplet()!=null) return 0L;
		long max = 0L;
		if (IJ.isMacOSX()) {
			String appPath = System.getProperty("java.class.path");
			if (appPath==null) return 0L;
			int index = appPath.indexOf(".app/");
			if (index==-1) return 0L;
			appPath = appPath.substring(0,index+5);
			max = getMemorySetting(appPath+"Contents/Info.plist");
		} else
			max = getMemorySetting("ImageJ.cfg");		
		return max;
	}

	void showError() {
		int max = (int)(maxMemory()/1048576L);
		String msg =
			   "ImageJ is unable to change the memory limit. For \n"
			+ "more information, refer to the installation notes at\n \n"
			+ "    "+IJ.URL2+"/docs/install/\n"
			+ " \n";
		if (fileMissing) {
			if (IJ.isMacOSX())
				msg += "The ImageJ application (ImageJ.app) was not found.\n \n";
			else if (IJ.isWindows())
				msg += "ImageJ.cfg not found.\n \n";
			fileMissing = false;
		}
		if (max>0)
			msg += "Current limit: " + max + "MB";
		IJ.showMessage("Memory", msg);
	}

	long getMemorySetting(String file) {
		String path = file.startsWith("/")?file:Prefs.getImageJDir()+file;
		if (IJ.debugMode) IJ.log("getMemorySetting: "+path);
		f = new File(path);
		if (!f.exists()) {
			fileMissing = true;
			return 0L;
		}
		long max = 0L;
		try {
			int size = (int)f.length();
			byte[] buffer = new byte[size];
			FileInputStream in = new FileInputStream(f);
			in.read(buffer, 0, size);
			s = new String(buffer, 0, size, "ISO8859_1");
			in.close();
			index1 = s.indexOf("-mx");
			if (index1==-1) index1 = s.indexOf("-Xmx");
			if (index1==-1) return 0L;
			if (s.charAt(index1+1)=='X') index1+=4; else index1+=3;
			index2 = index1;
			while (index2<s.length()-1 && Character.isDigit(s.charAt(++index2))) {}
			String s2 = s.substring(index1, index2);
			max = (long)Tools.parseDouble(s2, 0.0)*1024*1024;
			if (index2<s.length() && s.charAt(index2)=='g')
				max = max*1024L;
		}
		catch (Exception e) {
			IJ.log(""+e);
			return 0L;
		}
		return max;
	}

	/** Returns the maximum amount of memory this JVM will attempt to use. */
	public long maxMemory() {
			return Runtime.getRuntime().maxMemory();
	}
	
}
