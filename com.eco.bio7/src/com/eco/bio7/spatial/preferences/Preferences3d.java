package com.eco.bio7.spatial.preferences;

import org.eclipse.jface.preference.IPreferenceStore;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.spatial.SpatialStructure;

public class Preferences3d {

	private IPreferenceStore store;

	public Preferences3d() {
		store = Bio7Plugin.getDefault().getPreferenceStore();
	}

	public void storeSplitViewCameraLocation(double x, double y, double z) {

		Bio7PrefConverterSpatial.setValue(store, "splitCameraLoc", new Point3d((int) x, (int) y, (int) z));

	}

	public void storeDefaultSplitViewCameraLocation(int x, int y, int z) {

		Bio7PrefConverterSpatial.setDefault(store, "splitCameraLoc", new Point3d(0, 0, 0));

	}

	public void storeSplitViewCameraLookAt(double x, double y, double z) {

		Bio7PrefConverterSpatial.setValue(store, "splitCameraLookAt", new Point3d((int) x, (int) y, (int) z));

	}

	public void storeDefaultSplitViewCameraLookAt(int x, int y, int z) {

		Bio7PrefConverterSpatial.setDefault(store, "splitCameraLookAt", new Point3d(0, 0, 0));

	}

	/* Will be called in the constructor of the SpatialStructure class! */
	public void getSplitViewCameraValues() {
		Point3d p = Bio7PrefConverterSpatial.getPoint(store, "splitCameraLoc");
		Point3d p2 = Bio7PrefConverterSpatial.getPoint(store, "splitCameraLookAt");
		SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
		if (grid != null) {
			grid.setSplitCameraValues(p.x, p.y, p.z, p2.x, p2.y, p2.z);

		}

	}

	public boolean isFixedFps() {

		return store.getBoolean("fps");

	}

	/**
	 * *************The interface *.obj model
	 * preferences*************************************************
	 */

	public void storeModelLocation(double x, double y, double z) {

		Bio7PrefConverterSpatial.setValue(store, "modelLocation", new Point3d((int) x, (int) y, (int) z));

	}

	public void storeModelRotation(double x, double y, double z) {

		Bio7PrefConverterSpatial.setValue(store, "modelRotation", new Point3d((int) x, (int) y, (int) z));

	}

	public void storeModelScale(double x, double y, double z) {

		Bio7PrefConverterSpatial.setValue(store, "modelScale", new Point3d((int) x, 0, 0));

	}

	/**
	 * Stores the position, rotation and scale of the model (with decimals).
	 */
	public void storeModelTransform(double[] position, double[] rotation, double scale) {
		store.setValue("modelTransform", position[0] + "," + position[1] + "," + position[2] + "," + rotation[0] + ","
				+ rotation[1] + "," + rotation[2] + "," + scale);
	}

	/* The stored position, rotation and scale (7 values) or null! */
	private double[] getModelTransform() {
		String value = store.getString("modelTransform");
		if (value == null || value.isEmpty()) {
			return null;
		}
		String[] parts = value.split(",");
		if (parts.length != 7) {
			return null;
		}
		double[] v = new double[7];
		try {
			for (int i = 0; i < 7; i++) {
				v[i] = Double.parseDouble(parts[i].trim());
			}
		} catch (NumberFormatException e) {
			return null;
		}
		return v;
	}

	public void getModelValues() {
		SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
		double[] transform = getModelTransform();
		if (grid != null && transform != null && transform[6] > 0) {
			grid.objModel1Position = new double[] { transform[0], transform[1], transform[2] };
			grid.objModel1Rotation = new double[] { transform[3], transform[4], transform[5] };
			grid.objModel1Scale = transform[6];
			return;
		}
		Point3d p = Bio7PrefConverterSpatial.getPoint(store, "modelLocation");
		Point3d p2 = Bio7PrefConverterSpatial.getPoint(store, "modelRotation");
		Point3d p3 = Bio7PrefConverterSpatial.getPoint(store, "modelScale");

		if (grid != null) {
			/* Avoid scale value <1. The Model will not be shown! */
			if (p3.x < 1) {
				p3.x = 1;
			}
			grid.setModelValues(p.x, p.y, p.z, p2.x, p2.y, p2.z, p3.x);

		}

	}

	/* Set a default fps! */
	public void setFixedFps(boolean fixedFps) {

		store.setValue("fps", fixedFps);

	}

}
