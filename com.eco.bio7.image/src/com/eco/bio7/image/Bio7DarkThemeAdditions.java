package com.eco.bio7.image;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.ColorUIResource;

import org.eclipse.swt.widgets.Composite;

public class Bio7DarkThemeAdditions {
	 public static boolean isSet=false;
	 
	public static boolean isSet() {
		return isSet;
	}
	/*Do not load twice in the ImageJ plugin if Bio7 already loaded the LAF extension!*/
	public static void setSet(boolean isSet) {
		Bio7DarkThemeAdditions.isSet = isSet;
	}

	public void applyMacBlackThemeAdditions(Color colBackgr,Color colForegr) {
		
			
			
			//UIManager.getLookAndFeelDefaults().put("Button.border", "Raised");
			UIManager.put("Button.border", new CompoundBorder(new LineBorder(new Color(0, 0, 0)), new EmptyBorder(2, 2, 2, 2)));

			List<String> colorKeys = new ArrayList<String>();
			Set<Entry<Object, Object>> entries = UIManager.getLookAndFeelDefaults().entrySet();
			for (Entry entry : entries) {
				if (entry.getValue() instanceof Color) {
					String key = ((String) entry.getKey());
					if (key.endsWith("background") || key.endsWith("Background")) {
						UIManager.getLookAndFeelDefaults().put(key, Arrays.asList(new Object[] { new ColorUIResource(colBackgr), new ColorUIResource(colBackgr), new ColorUIResource(colBackgr) }));
					} else if (key.endsWith("foreground") || key.endsWith("Foreground") || key.endsWith("gridColor") || key.endsWith("tickColor")) {
						UIManager.getLookAndFeelDefaults().put(key, Arrays.asList(new Object[] { new ColorUIResource(colForegr), new ColorUIResource(colForegr), new ColorUIResource(colForegr) }));
					}
				}
			}
			UIManager.getLookAndFeelDefaults().put("ScrollBar.thumb", Arrays.asList(new Object[] { new ColorUIResource(colForegr), new ColorUIResource(colForegr), new ColorUIResource(colForegr) }));

			UIManager.getLookAndFeelDefaults().put("List.background", colBackgr);
			UIManager.getLookAndFeelDefaults().put("List.foreground", colForegr);
			
			UIManager.getLookAndFeelDefaults().put("List.selectionBackground", colBackgr);
			UIManager.getLookAndFeelDefaults().put("List.selectionForeground", colForegr);

			UIManager.getLookAndFeelDefaults().put("TextField.background", colBackgr);
			UIManager.getLookAndFeelDefaults().put("TextField.foreground", colForegr);

			UIManager.getLookAndFeelDefaults().put("TextField.selectionBackground", colBackgr);
			UIManager.getLookAndFeelDefaults().put("TextField.selectionForeground", colForegr);

			UIManager.getLookAndFeelDefaults().put("TextPane.background", colBackgr);
			UIManager.getLookAndFeelDefaults().put("TextPane.foreground", colForegr);
			
			UIManager.getLookAndFeelDefaults().put("TextArea.background", colBackgr);
			UIManager.getLookAndFeelDefaults().put("TextArea.foreground", colForegr);
			
			UIManager.getLookAndFeelDefaults().put("TextArea.selectionBackground", colBackgr);
			UIManager.getLookAndFeelDefaults().put("TextArea.selectionForeground", colForegr);

			UIManager.getLookAndFeelDefaults().put("TextPane.selectionBackground", colBackgr);
			UIManager.getLookAndFeelDefaults().put("TextPane.inactiveForeground", colForegr);

			UIManager.getLookAndFeelDefaults().put("EditorPane.background", colBackgr);
			UIManager.getLookAndFeelDefaults().put("EditorPane.selectionBackground", colBackgr);
			UIManager.getLookAndFeelDefaults().put("EditorPane.inactiveForeground", colForegr);
			UIManager.getLookAndFeelDefaults().put("Button.background", colBackgr);
			UIManager.getLookAndFeelDefaults().put("Button.foreground", colForegr);
			UIManager.getLookAndFeelDefaults().put("Panel.background", colBackgr);
			UIManager.getLookAndFeelDefaults().put("Panel.foreground", colForegr);
			
			UIManager.getLookAndFeelDefaults().put("TextArea.inactiveBackground", colBackgr);
			UIManager.getLookAndFeelDefaults().put("TextArea.inactiveForeground", colForegr);
			
			UIManager.getLookAndFeelDefaults().put("TextField.inactiveBackground", colBackgr);
			UIManager.getLookAndFeelDefaults().put("TextField.inactiveForeground", colForegr);
			
			UIManager.getLookAndFeelDefaults().put("TextField.selectionBackground", colBackgr);
			UIManager.getLookAndFeelDefaults().put("TextField.selectionForeground", colForegr);
			
			UIManager.getLookAndFeelDefaults().put("TextArea.selectionBackground", colBackgr);
			UIManager.getLookAndFeelDefaults().put("TextArea.selectionForeground", colForegr);
			
			UIManager.getLookAndFeelDefaults().put("TextComponent.selectionBackgroundInactive", colBackgr);
			UIManager.getLookAndFeelDefaults().put("ComboBox.buttonBackground", colBackgr);
			UIManager.getLookAndFeelDefaults().put("ComboBox.buttonHighlight", colBackgr);
		

	}

	public void applyWinBlackThemeAdditions(Color colBackgr,Color colForegr) {
		
			

			
			
			UIManager.getLookAndFeelDefaults().put("ScrollBar.thumb", Arrays.asList(new Object[] { new ColorUIResource(colForegr), new ColorUIResource(colForegr), new ColorUIResource(colForegr) }));


			UIManager.getLookAndFeelDefaults().put("List.background", colBackgr);
			UIManager.getLookAndFeelDefaults().put("List.foreground", colForegr);
			
			UIManager.getLookAndFeelDefaults().put("List.selectionBackground", colBackgr);
			UIManager.getLookAndFeelDefaults().put("List.selectionForeground", colForegr);

			UIManager.getLookAndFeelDefaults().put("TextField.background", colBackgr);
			UIManager.getLookAndFeelDefaults().put("TextField.foreground", colForegr);

			UIManager.getLookAndFeelDefaults().put("TextField.selectionBackground", colBackgr);
			UIManager.getLookAndFeelDefaults().put("TextField.selectionForeground", colForegr);

			UIManager.getLookAndFeelDefaults().put("TextPane.background", colBackgr);
			UIManager.getLookAndFeelDefaults().put("TextPane.foreground", colForegr);

			UIManager.getLookAndFeelDefaults().put("TextPane.selectionBackground", colBackgr);
			UIManager.getLookAndFeelDefaults().put("TextPane.inactiveForeground", colForegr);

			UIManager.getLookAndFeelDefaults().put("EditorPane.background", colBackgr);
			UIManager.getLookAndFeelDefaults().put("EditorPane.selectionBackground", colBackgr);
			UIManager.getLookAndFeelDefaults().put("EditorPane.inactiveForeground", colForegr);

		

	}

	public void applyLinuxBlackThemeAdditions(Color colBackgr,Color colForegr) {

		UIManager.getLookAndFeelDefaults().put("Panel.background", colBackgr);
		UIManager.getLookAndFeelDefaults().put("Panel.foreground", colForegr);
		UIManager.getLookAndFeelDefaults().put("Button.background", colBackgr);
		UIManager.getLookAndFeelDefaults().put("Button.foreground", colForegr);
		UIManager.getLookAndFeelDefaults().put("Button.border", "Raised");

		List<String> colorKeys = new ArrayList<String>();
		Set<Entry<Object, Object>> entries = UIManager.getLookAndFeelDefaults().entrySet();
		for (Entry entry : entries) {
			if (entry.getValue() instanceof Color) {
				String key = ((String) entry.getKey());
				if (key.endsWith("background") || key.endsWith("Background")) {
					UIManager.getLookAndFeelDefaults().put(key, Arrays.asList(new Object[] { new ColorUIResource(colBackgr), new ColorUIResource(colBackgr), new ColorUIResource(colBackgr) }));
				} else if (key.endsWith("foreground") || key.endsWith("Foreground") || key.endsWith("gridColor") || key.endsWith("tickColor")) {
					UIManager.getLookAndFeelDefaults().put(key, Arrays.asList(new Object[] { new ColorUIResource(colForegr), new ColorUIResource(colForegr), new ColorUIResource(colForegr) }));
				}
			}
		}
		UIManager.getLookAndFeelDefaults().put("ScrollBar.thumb", Arrays.asList(new Object[] { new ColorUIResource(colForegr), new ColorUIResource(colForegr), new ColorUIResource(colForegr) }));

		UIManager.getLookAndFeelDefaults().put("List.background", colBackgr);
		UIManager.getLookAndFeelDefaults().put("List.foreground", colForegr);
		
		UIManager.getLookAndFeelDefaults().put("List.selectionBackground", colBackgr);
		UIManager.getLookAndFeelDefaults().put("List.selectionForeground", colForegr);

		UIManager.getLookAndFeelDefaults().put("TextField.background", colBackgr);
		UIManager.getLookAndFeelDefaults().put("TextField.foreground", colForegr);

		UIManager.getLookAndFeelDefaults().put("TextField.selectionBackground", colBackgr);
		UIManager.getLookAndFeelDefaults().put("TextField.selectionForeground", colForegr);

		UIManager.getLookAndFeelDefaults().put("TextPane.background", colBackgr);
		UIManager.getLookAndFeelDefaults().put("TextPane.foreground", colForegr);
		
		UIManager.getLookAndFeelDefaults().put("TextArea.background", colBackgr);
		UIManager.getLookAndFeelDefaults().put("TextArea.foreground", colForegr);
		
		UIManager.getLookAndFeelDefaults().put("TextArea.selectionBackground", colBackgr);
		UIManager.getLookAndFeelDefaults().put("TextArea.selectionForeground", colForegr);

		UIManager.getLookAndFeelDefaults().put("TextPane.selectionBackground", colBackgr);
		UIManager.getLookAndFeelDefaults().put("TextPane.inactiveForeground", colForegr);

		UIManager.getLookAndFeelDefaults().put("EditorPane.background", colBackgr);
		UIManager.getLookAndFeelDefaults().put("EditorPane.selectionBackground", colBackgr);
		UIManager.getLookAndFeelDefaults().put("EditorPane.inactiveForeground", colForegr);
		UIManager.getLookAndFeelDefaults().put("Button.background", colBackgr);
		UIManager.getLookAndFeelDefaults().put("Button.foreground", colForegr);
		UIManager.getLookAndFeelDefaults().put("Panel.background", colBackgr);
		UIManager.getLookAndFeelDefaults().put("Panel.foreground", colForegr);
		
		UIManager.getLookAndFeelDefaults().put("TextArea.inactiveBackground", colBackgr);
		UIManager.getLookAndFeelDefaults().put("TextArea.inactiveForeground", colForegr);
		
		UIManager.getLookAndFeelDefaults().put("TextField.inactiveBackground", colBackgr);
		UIManager.getLookAndFeelDefaults().put("TextField.inactiveForeground", colForegr);
		
		UIManager.getLookAndFeelDefaults().put("TextField.selectionBackground", colBackgr);
		UIManager.getLookAndFeelDefaults().put("TextField.selectionForeground", colForegr);
		
		UIManager.getLookAndFeelDefaults().put("TextArea.selectionBackground", colBackgr);
		UIManager.getLookAndFeelDefaults().put("TextArea.selectionForeground", colForegr);
		
		UIManager.getLookAndFeelDefaults().put("TextComponent.selectionBackgroundInactive", colBackgr);
		UIManager.getLookAndFeelDefaults().put("ComboBox.buttonBackground", colBackgr);
		UIManager.getLookAndFeelDefaults().put("ComboBox.buttonHighlight", colBackgr);

	}

}
