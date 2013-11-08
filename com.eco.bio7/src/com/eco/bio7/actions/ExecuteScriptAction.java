package com.eco.bio7.actions;

import java.io.File;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchWindow;

import com.eco.bio7.compile.GroovyInterpreter;
import com.eco.bio7.compile.BeanShellInterpreter;
import com.eco.bio7.compile.PythonInterpreter;

public class ExecuteScriptAction extends Action {

	private File file;
	private String text;

	public ExecuteScriptAction(String text, IWorkbenchWindow window, File file) {
		super(text);
		this.file = file;
		this.text = text;
		setId("com.eco.bio7.execute_bsh_script");

	}

	public void run() {
		if (text.equals("Empty")) {
			System.out.println("No script available!");
		}

		else if (text.equals(".txt")||text.equals(".ijm")) {

			ij.IJ.runMacroFile(file.getAbsolutePath());

		} else {
			if (file.getName().endsWith(".bsh")) {

				BeanShellInterpreter.interpretJob(null, file.toString());

			} else if (file.getName().endsWith(".groovy")) {

				GroovyInterpreter.interpretJob(null, file.toString());

			}
			else if (file.getName().endsWith(".py")) {

				PythonInterpreter.interpretJob(null, file.toString());

			}
			

		}

	}

}