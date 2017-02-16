/*******************************************************************************
 * Copyright (c) 2007-2013 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.batch;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Stack;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import com.eco.bio7.actions.Bio7Action;
import com.eco.bio7.compile.BeanShellInterpreter;
import com.eco.bio7.editors.BeanshellEditor;
import com.eco.bio7.floweditor.model.Connection;
import com.eco.bio7.floweditor.model.EllipticalShape;
import com.eco.bio7.floweditor.model.FlowDecisionShape;
import com.eco.bio7.floweditor.model.Shape;
import com.eco.bio7.floweditor.model.ShapesDiagram;
import com.eco.bio7.floweditor.model.TriangleEndShape;
import com.eco.bio7.floweditor.model.TriangleShape;
import com.eco.bio7.floweditor.shapes.ShapesEditor;

public class BatchModel {

	private static String argument = null;

	private static String folder = null;

	private static boolean openFolder = true;

	private static String filesourceprop = null;

	private static String fileextension = null;

	private static Shape shapetemp = null;

	private static List dia;

	private static String decision = "true";

	private static Shape shapestart;

	private static Shape shapestop;

	private static Shape shapenext;

	private static volatile boolean cancel;

	private static boolean pause;

	private static boolean debug;// The variable switch for the visualization mode!

	private static boolean test = false;// The variable switch for the test mode!

	private static Stack<IFile> s = new Stack<IFile>();

	public static void batch(boolean testRun) {
		/* Set shapenext to default color after test run! */
		// tempTestShape();
		if (debug || test) {
			eraseColours();
		}

		test = testRun;
		debug = geteditor().debug;
		pause = false;
		shapestart = null;
		shapestop = null;
		cancel = false;

		ShapesDiagram diagram = getActiveDiagram();
		dia = diagram.getChildren();

		if (dia.size() > 0) {
			for (int i = 0; i < dia.size(); i++) {// Search a start!
				Shape a = (Shape) dia.get(i);
				if (a instanceof TriangleShape) {
					shapestart = a;
				} else if (a instanceof TriangleEndShape) {

					shapestop = a;

				} else if (a instanceof EllipticalShape) {
					/* Control if the values are numeric! */
					try {
						Integer.parseInt((String) a.getPropertyValue(a.LOOP_COUNT));
					} catch (NumberFormatException e) {
						a.setColor();
						warning("No number defined or value is not an integer value!");

					}

					/* Reset all loops to the default value! */
					a.setPropertyValue(a.LOOP_COUNT, a.getPropertyValue(a.INIT_LOOP));
					List sourceconnectionlist = a.getSourceConnections();
					List targetconnectionlist = a.getTargetConnections();

					/* Control if loop is connected correctly! */
					if (targetconnectionlist.size() != 1 || sourceconnectionlist.size() != 1) {
						a.setColor();
						warning("A loop has to have only one incoming connection and one outgoing connection!");
					}
				}

			}
			if (shapestart == null) {

				warning("No start defined!");
			}

			else if (shapestop == null) {
				warning("No stop defined!");

			}

			else {
				List sourceconnectionlist = shapestart.getSourceConnections();
				List targetconnectionlist = shapestop.getTargetConnections();
				if (targetconnectionlist.size() == 0) {
					warning("The end is not connected for a regular Bio7 flow!");
				}
				if (sourceconnectionlist.size() > 0) {
					Connection targetlist = (Connection) sourceconnectionlist.get(0);
					Shape shapestarter = (Shape) targetlist.getTarget();
					if (!(shapestarter instanceof EllipticalShape)) {
						// Get the ancestor of the start!
						shapenext = shapestarter;
						batchStart();
					} else {
						warning("A loop cannot be the first component in a Bio7 flow!");
					}
				} else {
					warning("The start is not connected!");
				}

			}

		} else {// No children in diagram!
			warning("No regular flow to execute!");
		}

	}

	private static ShapesDiagram getActiveDiagram() {
		IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		ShapesDiagram diagram = ((ShapesEditor) editor).getModel();
		return diagram;
	}

	private static ShapesEditor geteditor() {
		IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (editor != null || editor instanceof ShapesEditor) {
			ShapesEditor shapeeditor = (ShapesEditor) editor;
			return shapeeditor;
		} else {
			return null;
		}

	}

	/**
	 * 
	 */
	public static void batchStart() {
		while (shapenext != null && cancel == false && pause == false) {

			debug = geteditor().debug;
			Shape a = shapenext;
			if (debug) {// Send shape if debug is true!
				a.setColor();
			}

			if (a instanceof TriangleEndShape) {
				System.out.println("Finished Flow!");
				shapenext = null; // End the flow!

				if (debug) {// Send the shape if debug is true!
					a.setDefaultColor();
				}
				if (test) {
					MessageBox messageBox = new MessageBox(new Shell(),

							SWT.ICON_WARNING);
					messageBox.setMessage("No errors detected!");
					messageBox.open();
				}
			} else if (a instanceof FlowDecisionShape) {// If not a Decision!

				handleDecision(a);

			}

			else {

				filesourceprop = (String) a.getPropertyValue(a.FILE_PROP);
				// Get the file property to select the interpreters or compiler,
				// etc.!
				fileextension = (String) a.getPropertyValue(a.FILE_TYPE);
				// Compile or interpret the source in the path!
				if (debug) {
					a.setColor();
				}
				if (test == false) {

					ExecuteBatchFile.intercompile(filesourceprop, fileextension);
					// a.setDefaultColor();
				}

				List sourceconnectionlist = a.getSourceConnections();

				if (sourceconnectionlist.size() > 0) {// When children!

					child(sourceconnectionlist); // Call child method!

				} else {
					warning("Flow not defined with a regular end!");
				}

			}
		}
	}

	private static void child(List sourceconnectionlist) {
		// Only two source connections allowed!

		if (sourceconnectionlist.size() == 1) { // If it has only one child!

			Connection targetlist = (Connection) sourceconnectionlist.get(0);
			Shape target = (Shape) targetlist.getTarget();

			if (target instanceof EllipticalShape) { // If it is a loop!

				// twoConnectHandleLoop(target);// Call method for the
				// ellipse connection!
				target.setColor();
				warning("a figure must follow a loop with a connection!");
			}

			else {// If it is a file!
				shapenext = target;

			}

		}

		else if (sourceconnectionlist.size() == 2) {// Has two children!

			Connection targetlist = (Connection) sourceconnectionlist.get(0);
			Shape target1 = (Shape) targetlist.getTarget();
			Connection targetlist2 = (Connection) sourceconnectionlist.get(1);
			Shape target2 = (Shape) targetlist2.getTarget();

			// Look which shape is the loop!
			if (target1 instanceof EllipticalShape) {
				shapetemp = target2;
				// Call method for the ellipse connection!
				twoConnectHandleLoop(target1);

			} else if (target2 instanceof EllipticalShape) {
				shapetemp = target1;

				twoConnectHandleLoop(target2);

			} else {
				warning("Only two connections are allowed if one of the connection is a loop!");

			}

		}

		else if (sourceconnectionlist.size() > 2) {
			warning("Only connections <=2 are allowed for this component!");

		}

	}

	private static void handleDecision(Shape target) {
		if (debug) {
			target.setColor();
		}
		String inter = (String) target.getPropertyValue(target.FILE_PROP);
		BeanShellInterpreter.doInterpret(inter, null);

		List decisionconnections = target.getSourceConnections();
		if (decisionconnections.size() > 2) {
			target.setColor();
			warning("A decision can only have two connections !");

		} else if (decisionconnections.size() < 2) {
			target.setColor();
			warning("A decision has to have two connections !");
		} else {

			Connection con1 = (Connection) decisionconnections.get(0);
			Connection con2 = (Connection) decisionconnections.get(1);
			String connection1 = con1.getBoolean();
			String connection2 = con2.getBoolean();
			if (!(connection1.equals(decision) && connection2.equals(decision))) {
				if (connection1.equals(decision)) {

					if ((con1.getTarget() instanceof EllipticalShape) == false) {
						Shape targetcon1 = con1.getTarget();
						shapenext = targetcon1;

					} else {// If connected with a Loop!
						target.setColor();
						warning("You connected a Decision with a Loop !");

					}

				} else if (connection2.equals(decision)) {

					if ((con2.getTarget() instanceof EllipticalShape) == false) {
						Shape targetcon2 = con2.getTarget();
						shapenext = targetcon2;

					} else {// If connected with a Loop !
						target.setColor();
						warning("You connected a Decision with a Loop !");

					}
				} else {
					target.setColor();
					warning("The two connections are set to false. No decision possible !");

				}
			} else {
				target.setColor();
				warning("The two connections are set to true. No decision possible !");
			}
		}
	}

	private static void twoConnectHandleLoop(Shape target) {

		Shape eshape = (Shape) target;
		if (debug) {
			target.setColor();
		}
		List sourceconnectionlist = eshape.getSourceConnections();
		List targetconnectionlist = eshape.getTargetConnections();

		/* Not a real condition if connection to the loop is missing then the loop will never be evaluated! */
		if (targetconnectionlist.size() == 1 && sourceconnectionlist.size() == 1) {

			int count = -1;
			try {
				count = Integer.parseInt((String) eshape.getPropertyValue(eshape.LOOP_COUNT));
			} catch (NumberFormatException e) {
				count = -1;

			}

			if (count > 0) {

				Connection eshapetargetlist = (Connection) sourceconnectionlist.get(0);
				Shape eshapetarget = (Shape) eshapetargetlist.getTarget();
				count--;
				if (test == false) {
					eshape.setPropertyValue(eshape.LOOP_COUNT, Integer.toString(count));
				} else {
					/* If test is evaluated all loops will be reset to 0! */
					eshape.setPropertyValue(eshape.LOOP_COUNT, Integer.toString(0));
				}
				shapenext = eshapetarget;

			}

			else if (count == 0) {
				/*
				 * if (debug) { if (test == false) { target.setDefaultColor(); } }
				 */
				// If the loop has finished!
				Shape temp = shapetemp;
				shapetemp = null;
				// Important to delete before flow proceeds!

				eshape.setPropertyValue(eshape.LOOP_COUNT, eshape.getPropertyValue(eshape.INIT_LOOP));
				// Get the init value, reset the loop-> important for nested
				// loops!
				shapenext = temp;

			} else if (count < 0) {
				eshape.setColor();
				warning("No number defined or value is not an integer value!");

			}
		} else {
			target.setColor();
			warning("A loop in Bio7 can only have one incoming and one outgoing connection !");
		}

	}

	private static void warning(String message) {
		Bio7Action.stopFlow();
		MessageBox messageBox = new MessageBox(new Shell(),

				SWT.ICON_WARNING);
		messageBox.setMessage(message);
		messageBox.open();
		if (shapenext != null) {
			shapenext.setColor();
		}

		shapenext = null;
	}

	private static void interCompile(String fileprop, String fileextension) {
		ExecuteBatchFile.intercompile(fileprop, fileextension);
	}

	public static void recursiveBatch(String fileprop) {

		IEditorPart editorStart = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();

		IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

		IPath location = Path.fromOSString(fileprop);

		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IFile iFile = workspace.getRoot().getFile(location);

		IWorkbenchPage page = workbenchWindow.getActivePage();

		try {
			page.openEditor(new FileEditorInput(iFile), PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(iFile.getFullPath().toString()).getId());
		} catch (PartInitException exception) {
			System.out.println(exception.getMessage());
		}
		batch(test);

		IFile ifiletemp = s.pop();

		try {
			page.openEditor(new FileEditorInput(ifiletemp), PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(ifiletemp.getFullPath().toString()).getId());
		} catch (PartInitException exception) {
			System.out.println(exception.getMessage());
		}

	}

	/**
	 * Returns a string representation of the file.
	 * 
	 * @param path
	 * @return
	 */
	public static String fileToString(String path) {// this function returns the
		// File as a String
		FileInputStream fileinput = null;
		try {
			fileinput = new FileInputStream(path);
		} catch (FileNotFoundException e) {

			e.printStackTrace();
		}
		int filetmp = 0;
		try {
			filetmp = fileinput.available();
		} catch (IOException e) {

			e.printStackTrace();
		}
		byte bitstream[] = new byte[filetmp];
		try {
			fileinput.read(bitstream);
		} catch (IOException e) {

			e.printStackTrace();
		}
		String content = new String(bitstream);
		return content;
	}

	public static void resumeFlow() {
		pause = true;
		Bio7Action.flowStart();

	}

	public static void resume() {
		pause = true;
		Bio7Action.flowStart();

	}

	public static void pause() {
		pause = true;
	}

	/**
	 * @return
	 */
	public static boolean isDebug() {
		return debug;
	}

	/**
	 * @param debug
	 */
	public static void setDebug(boolean debug) {
		BatchModel.debug = debug;
	}

	/**
	 * @return
	 */
	public static String getDecision() {
		return decision;
	}

	/**
	 * @param decision
	 */
	public static void setDecision(String decision) {
		BatchModel.decision = decision;
	}

	public static boolean isPause() {
		return pause;
	}

	public static void setPause(boolean pause) {
		BatchModel.pause = pause;
	}

	/**
	 * @return
	 */
	public static boolean isCancel() {
		return cancel;
	}

	/**
	 * @param cancel
	 */
	public static void setCancel(boolean cancel) {
		BatchModel.cancel = cancel;
	}

	/**
	 * @return
	 */
	public static String getArgument() {
		return argument;
	}

	/**
	 * @param argument
	 */
	public static void setArgument(String argument) {
		BatchModel.argument = argument;
	}

	public static Stack<IFile> getS() {
		return s;
	}

	public static String getFolder() {
		return folder;
	}

	public static void setFolder(String folder) {
		BatchModel.folder = folder;
	}

	public static boolean isOpenFolder() {
		return openFolder;
	}

	public static void setOpenFolder(boolean openFolder) {
		BatchModel.openFolder = openFolder;
	}

	public static void eraseColours() {
		ShapesDiagram diagram = getActiveDiagram();
		dia = diagram.getChildren();
		if (dia.size() > 0) {
			for (int i = 0; i < dia.size(); i++) {// Search a start!
				Shape a = (Shape) dia.get(i);
				a.setDefaultColor();
			}
		}
	}

}
