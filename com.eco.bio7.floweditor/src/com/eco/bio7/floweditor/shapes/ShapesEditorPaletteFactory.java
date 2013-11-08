/*******************************************************************************
 * Copyright (c) 2004, 2005 Elias Volanakis and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Elias Volanakis - initial API and implementation
 *******************************************************************************/
package com.eco.bio7.floweditor.shapes;

import com.eco.bio7.floweditor.model.Connection;
import com.eco.bio7.floweditor.model.EllipticalShape;
import com.eco.bio7.floweditor.model.FlowDecisionShape;
import com.eco.bio7.floweditor.model.RectangularShape;
import com.eco.bio7.floweditor.model.TriangleEndShape;
import com.eco.bio7.floweditor.model.TriangleShape;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.gef.palette.CombinedTemplateCreationEntry;
import org.eclipse.gef.palette.ConnectionCreationToolEntry;
import org.eclipse.gef.palette.MarqueeToolEntry;
import org.eclipse.gef.palette.PaletteContainer;
import org.eclipse.gef.palette.PaletteDrawer;
import org.eclipse.gef.palette.PaletteGroup;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.palette.PaletteSeparator;
import org.eclipse.gef.palette.PanningSelectionToolEntry;
import org.eclipse.gef.palette.ToolEntry;
import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.gef.requests.SimpleFactory;

/**
 * Utility class that can create a GEF Palette.
 * 
 * @see #createPalette()
 * @author Elias Volanakis
 * 
 * M.Austenfeld - Changes for the Bio7 application.
 */
final class ShapesEditorPaletteFactory {

	/** Preference ID used to persist the palette location. */
	private static final String PALETTE_DOCK_LOCATION = "ShapesEditorPaletteFactory.Location";
	/** Preference ID used to persist the palette size. */
	private static final String PALETTE_SIZE = "ShapesEditorPaletteFactory.Size";
	/** Preference ID used to persist the flyout palette's state. */
	private static final String PALETTE_STATE = "ShapesEditorPaletteFactory.State";

	/** Create the "Shapes" drawer. */
	private static PaletteContainer createShapesDrawer() {

		PaletteDrawer componentsDrawer = new PaletteDrawer("Flowchart");

		CombinedTemplateCreationEntry component = new CombinedTemplateCreationEntry(
				"Loop", "Create a Loop", EllipticalShape.class,
				new SimpleFactory(EllipticalShape.class), ImageDescriptor
						.createFromFile(ShapesPlugin.class, "icons/loop.gif"),
				ImageDescriptor.createFromFile(ShapesPlugin.class,
						"icons/loop.gif"));
		componentsDrawer.add(component);

		component = new CombinedTemplateCreationEntry("Stop", "Create a Stop",
				TriangleEndShape.class, new SimpleFactory(
						TriangleEndShape.class), ImageDescriptor
						.createFromFile(ShapesPlugin.class, "icons/stop.gif"),
				ImageDescriptor.createFromFile(ShapesPlugin.class,
						"icons/stop.gif"));
		componentsDrawer.add(component);

		component = new CombinedTemplateCreationEntry("Start",
				"Define a Starting Point", TriangleShape.class,
				new SimpleFactory(TriangleShape.class), ImageDescriptor
						.createFromFile(ShapesPlugin.class, "icons/start.gif"),
				ImageDescriptor.createFromFile(ShapesPlugin.class,
						"icons/start.gif"));
		componentsDrawer.add(component);

		component = new CombinedTemplateCreationEntry("Decision",
				"Create a Decision", FlowDecisionShape.class,
				new SimpleFactory(FlowDecisionShape.class),
				ImageDescriptor.createFromFile(ShapesPlugin.class,
						"icons/ifelse.gif"), ImageDescriptor.createFromFile(
						ShapesPlugin.class, "icons/ifelse.gif"));
		componentsDrawer.add(component);

		return componentsDrawer;
	}

	private static PaletteContainer createShapesDrawer2() {

		PaletteDrawer componentsDrawer = new PaletteDrawer("Extra");

		CombinedTemplateCreationEntry component = new CombinedTemplateCreationEntry(
				"Info", "Create an Information Label", RectangularShape.class,
				new SimpleFactory(RectangularShape.class), ImageDescriptor
						.createFromFile(ShapesPlugin.class, "icons/info.gif"),
				ImageDescriptor.createFromFile(ShapesPlugin.class,
						"icons/info.gif"));
		componentsDrawer.add(component);

		return componentsDrawer;
	}

	/**
	 * Creates the PaletteRoot and adds all palette elements. Use this factory
	 * method to create a new palette for your graphical editor.
	 * 
	 * @return a new PaletteRoot
	 */
	static PaletteRoot createPalette() {
		PaletteRoot palette = new PaletteRoot();
		palette.add(createToolsGroup(palette));
		palette.add(createShapesDrawer());
		palette.add(createShapesDrawer2());
		return palette;
	}

	/** Create the "Tools" group. */
	private static PaletteContainer createToolsGroup(PaletteRoot palette) {
		PaletteGroup toolGroup = new PaletteGroup("Files");

		// Add a selection tool to the group
		ToolEntry tool = new PanningSelectionToolEntry();
		toolGroup.add(tool);
		palette.setDefaultEntry(tool);

		// Add a marquee tool to the group
		toolGroup.add(new MarqueeToolEntry());

		// Add a (unnamed) separator to the group
		toolGroup.add(new PaletteSeparator());

		// Add (solid-line) connection tool
		tool = new ConnectionCreationToolEntry("Connection",
				"Create a connection - two connections per shape allowed",
				new CreationFactory() {
					public Object getNewObject() {
						return null;
					}

					// see ShapeEditPart#createEditPolicies()
					// this is abused to transmit the desired line style
					public Object getObjectType() {
						return Connection.SOLID_CONNECTION;
					}
				}, ImageDescriptor.createFromFile(ShapesPlugin.class,
						"icons/connection_s16.gif"), ImageDescriptor
						.createFromFile(ShapesPlugin.class,
								"icons/connection_s24.gif"));
		toolGroup.add(tool);

		return toolGroup;
	}

	/** Utility class. */
	private ShapesEditorPaletteFactory() {
		// Utility class
	}

}