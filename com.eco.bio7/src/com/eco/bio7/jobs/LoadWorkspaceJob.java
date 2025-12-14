/*******************************************************************************
 * Copyright (c) 2007-2025 M. Austenfeld
 * 
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************/

package com.eco.bio7.jobs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridItem;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.collection.ResizeArray;
//import com.eco.bio7.compile.Compile;
import com.eco.bio7.compile.CompileClassAndMultipleClasses;
import com.eco.bio7.database.Bio7State;
import com.eco.bio7.database.StateTable;
import com.eco.bio7.discrete.Field;
import com.eco.bio7.discrete.Hexagon;
import com.eco.bio7.discrete.Quad2d;
import com.eco.bio7.info.InfoView;
import com.eco.bio7.methods.Compiled;
import com.eco.bio7.methods.CurrentStates;
import com.eco.bio7.plot.LineChart;
import com.eco.bio7.plot.PieChartView;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.FPSAnimator;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class LoadWorkspaceJob extends WorkspaceJob {

	private String source;

	// private ArrayList<String> li;

	DataDescriptorGrids grid = null;

	public LoadWorkspaceJob(String source) {
		super("Load Pattern");

		this.source = source;

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {

		monitor.beginTask("Load Pattern...", IProgressMonitor.UNKNOWN);
		XStream xs = new XStream(new DomDriver());
		xs.allowTypesByRegExp(new String[] { ".*" });
		xs.setClassLoader(com.eco.bio7.jobs.DataDescriptorGrids.class.getClassLoader());

		try {
			grid = (DataDescriptorGrids) xs.fromXML(new FileReader(source));
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		Quad2d quad = Quad2d.getQuad2dInstance();

		if (quad != null) {

			unsetAllStates();

			ResizeArray.update(0, 0);
			// For a fast loading we are resizing the field to a minimum!

			// Update the size for the field with the stored values!

			ArrayList<String> li = grid.getStateNames();
			for (int i = 0; i < li.size(); i++) {
				final int count = i;
				Bio7State.createState(li.get(i));
				setGridDescription(count);

			}
			CurrentStates.setR(grid.getColorR());
			CurrentStates.setG(grid.getColorG());
			CurrentStates.setB(grid.getColorB());
			CurrentStates.setStateDescriptions(grid.getDescriptions());
			setPlotColors();

			ResizeArray.update(grid.getFieldSizeY(), grid.getFieldSizeX());

			// We adjust the slider value!
			updateSlider(grid);
			int[][] stat = grid.getStates();

			for (int y = 0; y < stat.length; y++) {
				for (int x = 0; x < stat[0].length; x++) {
					Field.setState(x, y, stat[y][x]);
					Field.setTempState(x, y, stat[y][x]);
				}
			}

			repaintFields();
			grid.setStates(null);
			/*
			 * Load and compile a class or classbody dependent on the stored boolean value
			 * form the Java preferences of Bio7 !
			 */
		}
		if (grid.getSource() != null) {

			// if (grid.isClassBody() == false) {
			// Compile.compileClassWithoutJob(grid.getSource(), grid.getFileName());
			String tempDir = System.getProperty("java.io.tmpdir");
			File file = null;
			try {

				file = new File(tempDir + grid.getFileName() + ".java");
				FileOutputStream fos = new FileOutputStream(file);
				fos.write(grid.getSource().getBytes());
				fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			CompileClassAndMultipleClasses cp = new CompileClassAndMultipleClasses();
			try {
				cp.compileAndLoad(file, file.getParent(), grid.getFileName(), null, true);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				// Bio7Dialog.message(e.getMessage());
			}

			callSetup();
			// } else {
			// Compile.compileClassbodyWithoutJob(grid.getSource());
			// callSetup();
			// }

		}

		return Status.OK_STATUS;

	}

	private void setGridDescription(final int count) {
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {
				StateTable stt = StateTable.getDatatableInstance();
				GridItem item = stt.getGrid().getItem(count);
				item.setText(1, grid.getDescriptions().get(count));
			}
		});
	}

	// We adjust the slider value!
	private void updateSlider(final DataDescriptorGrids grid) {

		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {

				InfoView.getScale_1().setSelection(grid.getFieldSizeY());
				InfoView.getLblFieldsizeY().setText("Fieldsize Y -> " + grid.getFieldSizeY());
				InfoView.getScale_2().setSelection(grid.getFieldSizeX());
				InfoView.getLblFieldsizeX().setText("Fieldsize X -> " + grid.getFieldSizeX());
			}
		});

	}

	private void repaintFields() {
		Quad2d.getQuad2dInstance().redraw();
		Hexagon hex = Hexagon.getHexagonInstance();
		if (hex != null) {

			hex.repaint();
		}
	}

	public void setPlotColors() {
		/* Change the colour of the linechart! */
		LineChart.renderer();

		/* Change the colour of the piechart! */
		PieChartView.getPiechart().plotter();

	}

	private void unsetAllStates() {

		// li = CurrentStates.getStateList();

		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {
				Grid grid = StateTable.grid;
				/*
				 * We don't use the list directly but the grid entrys. Since we remove it
				 * iterately (At each iteration step a state will be removed)!
				 */

				for (int i = 0; i < grid.getItemCount(); i++) {

					String state = grid.getItem(i).getText(0);

					StateTable.unsetCell(state);

				}

				grid.removeAll();

			}
		});

	}

	private static void callSetup() {
		if (Compiled.getModel() != null) {

			Class cla = Compiled.getModel().getClass();
			try {
				if (cla.getMethod("setup", null) != null) {

					Display display = PlatformUI.getWorkbench().getDisplay();
					display.syncExec(new Runnable() {

						public void run() {

							Compiled.getModel().setup();
						}
					});
				}

			} catch (SecurityException e) {

			} catch (NoSuchMethodException e) {
				Bio7Dialog.message("No setup method available!");
			}
		} else {

			Bio7Dialog.message("No compiled method available !");
		}
	}

}
