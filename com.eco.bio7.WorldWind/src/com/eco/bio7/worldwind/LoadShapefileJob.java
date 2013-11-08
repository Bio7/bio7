/*******************************************************************************
 * Copyright (c) 2007-2012 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.worldwind;


import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.util.WWIO;
import gov.nasa.worldwindx.examples.util.OpenStreetMapShapefileLoader;
import gov.nasa.worldwindx.examples.util.ShapefileLoader;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;


public class LoadShapefileJob extends Job {

	protected String[] items;
	private String source;
	private Composite composite_4;

	public LoadShapefileJob(String source,Composite composite) {
		super("Load Shapefile...");
		this.source=source;
		this.composite_4=composite;

	}


	@Override
	protected IStatus run(IProgressMonitor monitor) {
		monitor.beginTask("Load Shapefile..", IProgressMonitor.UNKNOWN);
		
		try
        {
            final List<Layer> layers = this.makeShapefileLayers();
            for (int i = 0; i < layers.size(); i++)
            {
                String name = this.makeDisplayName(this.source);
                layers.get(i).setName(i == 0 ? name : name + "-" + Integer.toString(i));
                layers.get(i).setPickEnabled(false);
                
            }

            Display display = PlatformUI.getWorkbench().getDisplay();
			display.syncExec(new Runnable() {

				public void run() {
                    for (Layer layer : layers)
                    {
                    	LayerCompositeShapefile lc = new LayerCompositeShapefile(composite_4, SWT.NONE, layer);
            			lc.setBounds(10, 10, 260, 60);
            			WorldWindOptionsView.computeScrolledSize();
                        WorldWindOptionsView.insertBeforePlacenames(WorldWindView.getWwd(), layer);
                       // appFrame.layers.add(layer);
                    }

                    //appFrame.layerPanel.update(appFrame.getWwd());
                }
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
		
		

		

		return Status.OK_STATUS;
	}
	
	 protected List<Layer> makeShapefileLayers()
     {
         if (OpenStreetMapShapefileLoader.isOSMPlacesSource(this.source))
         {
             Layer layer = OpenStreetMapShapefileLoader.makeLayerFromOSMPlacesSource(source);
             List<Layer> layers = new ArrayList<Layer>();
             layers.add(layer);
             return layers;
         }
         else
         {
             ShapefileLoader loader = new ShapefileLoader();
             return loader.createLayersFromSource(this.source);
         }
     }

     protected String makeDisplayName(Object source)
     {
         String name = WWIO.getSourcePath(source);
         if (name != null)
             name = WWIO.getFilename(name);
         if (name == null)
             name = "Shapefile";

         return name;
     }
	

}
