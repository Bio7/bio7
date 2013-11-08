package com.eco.bio7.console;

import groovy.util.GroovyScriptEngine;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;

import com.eco.bio7.scriptengines.ScriptEngineConnection;

public class IoConsole extends IOConsole{

	public final static String ID = "com.eco.bio.IOConsole";
	private InputStream input=null;
	private IOConsoleOutputStream out;
	private IoConsole iOConsoleInstance;

	/**
	 * Some useful colors.
	 */
	private static final Color RED;
	private static final Color BLUE;
	static {
		Display device = Display.getCurrent();
		RED = new Color(device, 255, 0, 0);
		BLUE = new Color(device, 0, 0, 128);
	}
	
	

	public IoConsole(String title) {
		super(title, null);
        iOConsoleInstance=this;
		out = newOutputStream(); 
		out.setColor(BLUE);
		System.setOut(new PrintStream(out));
		
		

		IOConsoleOutputStream err = newOutputStream(); 
		err.setColor(RED);
		System.setErr(new PrintStream(err));
		
		input = getInputStream();
		System.setIn(input);
		
		Job job = new Job("Transfer to R") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Transfer Data ...", IProgressMonitor.UNKNOWN);
				ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[]{ iOConsoleInstance });
				ConsolePlugin.getDefault().getConsoleManager().showConsoleView(iOConsoleInstance);	
				
				BufferedReader in = new BufferedReader(new InputStreamReader(iOConsoleInstance.getInputStream()));
				String input="";		
				try {
					
					GroovyScriptEngine gse = new GroovyScriptEngine("");
					while(true)
					{
						System.out.print("groovy>");
						//System.out.flush();
						input = in.readLine();
						if(input==null)  break;
						else {					
							
							ScriptEngine gs = ScriptEngineConnection.getScriptingEnginePython();
							try {
								String out=(String)gs.eval(input);
							} catch (ScriptException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
							
							System.out.flush();
							
							
							
						}
					}
				} catch (IOException e) {			
					e.printStackTrace();
					System.out.println(e.getMessage());
					return Status.CANCEL_STATUS;
				} 
				System.out.println();
				

				monitor.done();
				return Status.OK_STATUS;
			}

		};
		job.addJobChangeListener(new JobChangeAdapter() {
			public void done(IJobChangeEvent event) {
				if (event.getResult().isOK()) {

					
				} else {

					
				}
			}
		});
		// job.setSystem(true);
		job.schedule();
		
		
		
		
		
		
		
	}
}

