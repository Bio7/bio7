package com.eco.bio7.rcp;

import java.awt.Color;
import java.util.Arrays;

import javax.swing.UIDefaults;
import javax.swing.plaf.*;
import javax.swing.plaf.metal.*;

import org.eclipse.swt.widgets.Shell;

import com.eco.bio7.util.Util;

public class Bio7LinuxTheme extends OceanTheme {
	// public String getName() { return "Moody Blues"; }

	// blue shades
	Color colorSwt = Util.getSWTBackgroundToAWT();
	private final ColorUIResource primary1 = new ColorUIResource(colorSwt);
	private final ColorUIResource primary2;
	private final ColorUIResource primary3;

	private final ColorUIResource secondary1 = new ColorUIResource(colorSwt);
	private final ColorUIResource secondary2;
	public final ColorUIResource secondary3;

	// the functions overridden from the base
	// class => DefaultMetalTheme

	protected ColorUIResource getPrimary1() {
		return primary1;
	}

	protected ColorUIResource getPrimary2() {
		return primary2;
	}

	protected ColorUIResource getPrimary3() {
		return primary3;
	}

	protected ColorUIResource getSecondary1() {
		return secondary1;
	}

	protected ColorUIResource getSecondary2() {
		return secondary2;
	}

	protected ColorUIResource getSecondary3() {
		return secondary3;
	}

	Color col = null;

	public Bio7LinuxTheme() {

		col = Util.getSWTBackgroundToAWT();
		primary3 = new ColorUIResource(col);
		primary2 = new ColorUIResource(col);
		secondary2 = new ColorUIResource(col);
		secondary3 = new ColorUIResource(col);

	}

	public void addCustomEntriesToTable(UIDefaults defaults) {
		defaults.put("Button.gradient", Arrays.asList(new Object[] { new Double(0.3), new Double(0.0), new ColorUIResource(col), new ColorUIResource(Color.WHITE), new ColorUIResource(col) }));
		defaults.put("CheckBox.gradient", Arrays.asList(new Object[] { new Double(0.3), new Double(0.0), new ColorUIResource(col), new ColorUIResource(Color.WHITE), new ColorUIResource(col) }));
		defaults.put("CheckBoxMenuItem.gradient", Arrays.asList(new Object[] { new Double(0.3), new Double(0.0), new ColorUIResource(221, 232, 243), new ColorUIResource(Color.WHITE), new ColorUIResource(184, 207, 229) }));
		defaults.put("MenuBar.gradient", Arrays.asList(new Object[] { new Double(1.0), new Double(0.0), new ColorUIResource(col), new ColorUIResource(218, 218, 218), new ColorUIResource(218, 218, 218) }));
		defaults.put("RadioButton.gradient", Arrays.asList(new Object[] { new Double(0.3), new Double(0.0), new ColorUIResource(col), new ColorUIResource(col), new ColorUIResource(col) }));
		defaults.put("RadioButtonMenuItem.gradient", Arrays.asList(new Object[] { new Double(0.3), new Double(0.0), new ColorUIResource(221, 232, 243), new ColorUIResource(col), new ColorUIResource(184, 207, 229) }));
		defaults.put("ScrollBar.gradient", Arrays.asList(new Object[] { new Double(0.3), new Double(0.0), new ColorUIResource(col), new ColorUIResource(Color.WHITE), new ColorUIResource(col) }));
		defaults.put("Slider.gradient", Arrays.asList(new Object[] { new Double(0.3), new Double(0.2), new ColorUIResource(col), new ColorUIResource(Color.WHITE), new ColorUIResource(col) }));
		defaults.put("ToggleButton.gradient", Arrays.asList(new Object[] { new Double(0.3), new Double(0.0), new ColorUIResource(221, 232, 243), new ColorUIResource(col), new ColorUIResource(184, 207, 229) }));
		defaults.put("InternalFrame.activeTitleGradient", Arrays.asList(new Object[] { new Double(0.3), new Double(0.0), new ColorUIResource(221, 232, 243), new ColorUIResource(col), new ColorUIResource(184, 207, 229) }));

		defaults.put("Button.rollover", Boolean.TRUE);

		defaults.put("TabbedPane.selected", new ColorUIResource(200, 221, 242));
		defaults.put("TabbedPane.unselectedBackground", secondary3);
	}
}