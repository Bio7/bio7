package com.eco.bio7.image;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.*;
import javax.swing.plaf.metal.*;

import org.eclipse.swt.widgets.Shell;

public class Bio7LinuxTheme extends OceanTheme {
	// public String getName() { return "Moody Blues"; }
    public static boolean isSet=false;
    
	

	// blue shades
	private final ColorUIResource primary1 = new ColorUIResource(new Color(100, 100, 100));
	private final ColorUIResource primary2;
	private final ColorUIResource primary3;

	private final ColorUIResource secondary1 = new ColorUIResource(new Color(100, 100, 100));
	private final ColorUIResource secondary2;
	public final ColorUIResource secondary3;

	// the functions overridden from the base
	// class => DefaultMetalTheme
	
	public static boolean isSet() {
		return isSet;
	}
	/*Do not load twice if already loaded from Bio7 in the ImageJ plugin!*/
	public static void setSet(boolean isSet) {
		Bio7LinuxTheme.isSet = isSet;
	}

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

	Color colBackgr = null;
	private Color colForegr;

	public Bio7LinuxTheme() {

		Shell s = new Shell();

		org.eclipse.swt.graphics.Color colSwtBackgr = s.getBackground();
		int rb = colSwtBackgr.getRed();
		int gb = colSwtBackgr.getGreen();
		int bb = colSwtBackgr.getBlue();
		colBackgr = new Color(rb, gb, bb);
		org.eclipse.swt.graphics.Color colSwtForegr = s.getForeground();
		int rf = colSwtForegr.getRed();
		int gf = colSwtForegr.getGreen();
		int bf = colSwtForegr.getBlue();
		colForegr = new Color(rf, gf, bf);
		primary3 = new ColorUIResource(colBackgr);
		primary2 = new ColorUIResource(colBackgr);
		secondary2 = new ColorUIResource(colBackgr);
		secondary3 = new ColorUIResource(colBackgr);

	}

	public void addCustomEntriesToTable(UIDefaults defaults) {
		defaults.put("Button.gradient", Arrays.asList(new Object[] { new ColorUIResource(colBackgr),
				new ColorUIResource(colBackgr), new ColorUIResource(colBackgr) }));

		List<String> colorKeys = new ArrayList<String>();
		Set<Entry<Object, Object>> entries = UIManager.getLookAndFeelDefaults().entrySet();
		for (Entry entry : entries) {
			if (entry.getValue() instanceof Color) {
				String key = ((String) entry.getKey());
				if (key.endsWith("background") || key.endsWith("Background")) {
					defaults.put(key, Arrays.asList(new Object[] { new ColorUIResource(colBackgr),
							new ColorUIResource(colBackgr), new ColorUIResource(colBackgr) }));
				}

				else if (key.endsWith("foreground") || key.endsWith("Foreground") || key.endsWith("gridColor")
						|| key.endsWith("tickColor")) {
					defaults.put(key, Arrays.asList(new Object[] { new ColorUIResource(colForegr),
							new ColorUIResource(colForegr), new ColorUIResource(colForegr) }));
				}
			}
		}
		defaults.put("ScrollBar.thumb", Arrays.asList(new Object[] { new ColorUIResource(colForegr),
				new ColorUIResource(colForegr), new ColorUIResource(colForegr) }));
		
	}
}