/*
 * Copyright (C) 2014 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwindx.examples;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.globes.EarthFlat;
import gov.nasa.worldwind.layers.IconLayer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.SkyColorLayer;
import gov.nasa.worldwind.layers.SkyGradientLayer;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.pick.PickedObjectList;
import gov.nasa.worldwind.render.UserFacingIcon;
import gov.nasa.worldwind.view.orbit.FlatOrbitView;

import java.awt.BorderLayout;
import java.awt.Dimension;

/**
 * Shows how to detect picked icons. Place the cursor over the icons to see the response printed to the console.
 *
 * @author Patrick Murris
 * @version $Id: IconPicking.java 1940 2014-04-16 00:57:28Z tgaskins $
 * @see gov.nasa.worldwind.globes.FlatGlobe
 * @see EarthFlat
 * @see FlatOrbitView
 */
public class IconPicking extends ApplicationTemplate
{
    public static class AppFrame extends ApplicationTemplate.AppFrame
    {
        public AppFrame()
        {
            super(true, true, false);

            IconLayer layer = new IconLayer();
            layer.setPickEnabled(true);
            layer.setAllowBatchPicking(false);
            layer.setRegionCulling(true);

            UserFacingIcon icon = new UserFacingIcon("src/images/32x32-icon-nasa.png",
                new Position(Angle.fromRadians(0), Angle.fromRadians(0), 0));
            icon.setSize(new Dimension(24, 24));
            layer.addIcon(icon);

            icon = new UserFacingIcon("src/images/32x32-icon-nasa.png",
                new Position(Angle.fromRadians(0.1), Angle.fromRadians(0.0), 0));
            icon.setSize(new Dimension(24, 24));
            layer.addIcon(icon);

            icon = new UserFacingIcon("src/images/32x32-icon-nasa.png",
                new Position(Angle.fromRadians(0.0), Angle.fromRadians(0.1), 0));
            icon.setSize(new Dimension(24, 24));
            layer.addIcon(icon);

            icon = new UserFacingIcon("src/images/32x32-icon-nasa.png",
                new Position(Angle.fromRadians(0.1), Angle.fromRadians(0.1), 0));
            icon.setSize(new Dimension(24, 24));
            layer.addIcon(icon);

            icon = new UserFacingIcon("src/images/32x32-icon-nasa.png",
                new Position(Angle.fromRadians(0), Angle.fromDegrees(180), 0));
            icon.setSize(new Dimension(24, 24));
            layer.addIcon(icon);

            ApplicationTemplate.insertAfterPlacenames(this.getWwd(), layer);
            // Change atmosphere SkyGradientLayer for SkyColorLayer
            LayerList layers = this.getWwd().getModel().getLayers();
            for (int i = 0; i < layers.size(); i++)
            {
                if (layers.get(i) instanceof SkyGradientLayer)
                    layers.set(i, new SkyColorLayer());
            }
            this.getLayerPanel().update(this.getWwd());

            this.getWwd().addSelectListener(new SelectListener()
            {
                @Override
                public void selected(SelectEvent event)
                {
                    if (event.getEventAction().equals(SelectEvent.ROLLOVER))
                    {
                        PickedObjectList pol = event.getObjects();
                        System.out.println(" Picked Objects Size " + pol.size());
                        for (PickedObject po : pol)
                        {
                            System.out.println(" Class " + po.getObject().getClass().getName() + "  isTerrian=" + po.isTerrain());
                        }
                    }
                }
            });
            this.getWwd().getSceneController().setDeepPickEnabled(true);
            // Add flat world projection control panel
            this.getLayerPanel().add(new FlatWorldPanel(this.getWwd()), BorderLayout.SOUTH);
        }
    }

    public static void main(String[] args)
    {
        // Adjust configuration values before instantiation
        Configuration.setValue(AVKey.GLOBE_CLASS_NAME, EarthFlat.class.getName());
        Configuration.setValue(AVKey.VIEW_CLASS_NAME, FlatOrbitView.class.getName());
        Configuration.setValue(AVKey.INITIAL_ALTITUDE, 27e6);
        Configuration.setValue(AVKey.INITIAL_LATITUDE, 0);
        Configuration.setValue(AVKey.INITIAL_LONGITUDE, 88);
        ApplicationTemplate.start("World Wind Flat World", AppFrame.class);
    }
}
