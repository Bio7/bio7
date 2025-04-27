package ij.plugin;
import ij.*;
import ij.gui.*;
import ij.util.*;
import ij.measure.ResultsTable;
import java.awt.*;
import java.io.*;
import java.util.*;

/** Implements the Plugins/Hotkeys/Create Shortcut and Remove commands. */
public class Hotkeys implements PlugIn {

	private static final String TITLE = "Hotkeys";
	private static String command = "";
	private static String shortcut = "";

	public void run(String arg) {
		if (arg.equals("install") || arg.equals("install2"))
			installHotkey(arg);
		else if (arg.equals("remove"))
			removeHotkey();
		else if (arg.equals("list"))
			listCommands();
		else {
			Executer e = new Executer(arg);
			e.run();
		}
		IJ.register(Hotkeys.class);
	}

	void installHotkey(String arg) {
		boolean byName = arg.equals("install2");
		String[] commands = byName?null:getAllCommands();
		String[] shortcuts = getAvailableShortcuts();
		String nCommands = commands!=null?" ("+commands.length+")":"";
		GenericDialog gd = new GenericDialog("Add Shortcut"+nCommands);
		gd.addChoice("Shortcut:", shortcuts, shortcuts[0]);
		if (byName)
			gd.addStringField("Command:", "", 20);
		else
			gd.addChoice("Command:", commands, command);
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		shortcut = gd.getNextChoice();
		if (byName) {
			command = gd.getNextString();
			Hashtable cmds = Menus.getCommands();
			if (cmds==null || cmds.get(command)==null) {
				String command2 = command;
				if (cmds.get(command)==null)
					command = command+" ";
				if (cmds.get(command)==null) {
					command = command2 + "...";
					if (cmds.get(command)==null) {
						command = command2;
						IJ.error("Command not found:\n \n   "+ "\""+command+"\"");
						return;
					}
				}
			}
		} else {
			command = gd.getNextChoice();
			Hashtable cmds = Menus.getCommands();
			if (command.contains("[") && cmds!=null && cmds.get(command)==null) {
				if (cmds.get(command+"]")!=null)
					command += "]";
			}
		}
		String plugin = "ij.plugin.Hotkeys("+"\""+command+"\")";
		int err = Menus.installPlugin(plugin,Menus.SHORTCUTS_MENU,"*"+command,shortcut,IJ.getInstance());
		switch (err) {
			case Menus.COMMAND_IN_USE:
				IJ.showMessage(TITLE, "The command \"" + command + "\" is already installed.");
				break;
			case Menus.INVALID_SHORTCUT:
				IJ.showMessage(TITLE, "The shortcut must be a single character or F1-F24.");
				break;
			case Menus.SHORTCUT_IN_USE:
				IJ.showMessage("The \""+shortcut+"\" shortcut is in use.");
				break;
			default:
				shortcut = "";
				break;
		}
	}
	
	void removeHotkey() {
		String[] shortcuts = getShortcuts();
		if (shortcuts==null) {
			IJ.showMessage("Remove...", "No shortcuts found.");
			return;
		}
		GenericDialog gd = new GenericDialog("Remove");
		gd.addChoice("Shortcut:", shortcuts, "");
		if (shortcuts.length>1)
			gd.addCheckbox("Remove all "+shortcuts.length+" shortcuts", false);
		gd.addMessage("Shortcuts are not removed\nuntil ImageJ is restarted.");
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		command = gd.getNextChoice();
		boolean removeAll = false;
		if (shortcuts.length>1)
			removeAll = gd.getNextBoolean();
		if (removeAll) {
			boolean ok = IJ.showMessageWithCancel("Remove", "Remove all "+shortcuts.length+" shortcuts?");
			if (!ok)
				return;
			command = "";
		} else {
			shortcuts = new String[1];
			shortcuts[0] = command;
		}
		int count = 0;
		for (int i=0; i<shortcuts.length; i++) {
			int err = Menus.uninstallPlugin(shortcuts[i]);
			if (err==Menus.NORMAL_RETURN)
				count++;
		}
		if (count==0)
			IJ.showStatus("No shortcuts removed");
		else
			IJ.showStatus(count+" shortcut"+(count>1?"s":"")+" removed; ImageJ restart required");
	}
	
	private void listCommands() {
		String[] commands = getAllCommands();
		Hashtable classes = Menus.getCommands();
		ResultsTable rt = new ResultsTable();
		for (int i=0; i<commands.length; i++) {
			rt.incrementCounter();
			rt.addValue("Command", commands[i]);
			rt.addValue("Plugin", (String)classes.get(commands[i]));
		}
		rt.show("Commands");
	}

	String[] getAllCommands() {
		Vector v = new Vector();
		Hashtable commandTable = Menus.getCommands();
		Hashtable shortcuts = Menus.getShortcuts();
		for (Enumeration en=commandTable.keys(); en.hasMoreElements();) {
			String cmd = (String)en.nextElement();
			if (!cmd.startsWith("*") && !cmd.startsWith(" ") && cmd.length()<35 && !shortcuts.contains(cmd))
				v.addElement(cmd);
		}
		String[] list = new String[v.size()];
		v.copyInto((String[])list);
		Arrays.sort(list, String.CASE_INSENSITIVE_ORDER);
		return list;
	}
	
	String[] getAvailableShortcuts() {
		Vector v = new Vector();
		String[] existingShortcuts = (new CommandLister()).getShortcuts();
		for (char c = '0'; c<='9'; c++) {
			String shortcut = ""+c;
			if (!Menus.shortcutInUse(shortcut))
				v.add(shortcut);
		}
		for (char c = 'a'; c<='z'; c++) {
			String shortcut = ""+c;
			if (!Menus.shortcutInUse(shortcut))
				v.add(shortcut);
		}
		for (char c = 'A'; c<='Z'; c++) {
			String shortcut = ""+c;
			if (!Menus.shortcutInUse(shortcut))
				v.add(shortcut);
		}
		for (int i = 1; i<=12; i++) {
			String shortcut = "F"+i;
			if (!Menus.shortcutInUse(shortcut))
				v.add(shortcut);
		}
		String[] list = new String[v.size()];
		v.copyInto((String[])list);
		return list;
	}

	String[] getShortcuts() {
		Vector v = new Vector();
		Hashtable commandTable = Menus.getCommands();
		for (Enumeration en=commandTable.keys(); en.hasMoreElements();) {
			String cmd = (String)en.nextElement();
			if (cmd.startsWith("*"))
				v.addElement(cmd);
		}
		if (v.size()==0)
			return null;
		String[] list = new String[v.size()];
		v.copyInto((String[])list);
		Arrays.sort(list, String.CASE_INSENSITIVE_ORDER);
		return list;
	}
	
}
