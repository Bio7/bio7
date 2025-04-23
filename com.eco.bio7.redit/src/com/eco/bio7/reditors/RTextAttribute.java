/*package com.eco.bio7.reditors;

import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;

*//**
 * Description of textual attributes such as color and style. Text attributes
 * are considered value objects.
 * <p>
 * Clients usually instantiate object of the class.
 * </p>
 *//*
public class RTextAttribute extends TextAttribute {

	private FontRegistry fontRegistry;

	*//** Foreground color *//*
	private Color foreground;

	*//** Background color *//*
	private Color background;

	*//** The text style *//*
	private int style;

	*//**
	 * Creates a text attribute with the given colors and style.
	 *
	 * @param foreground
	 *            the foreground color, <code>null</code> if none
	 * @param background
	 *            the background color, <code>null</code> if none
	 * @param style
	 *            the style
	 * @param font
	 *            the font, <code>null</code> if none
	 * @since 3.3
	 *//*
	public RTextAttribute(Color foreground, Color background, int style, FontRegistry fontRegistry) {
		super(foreground, background, style);

		this.fontRegistry = fontRegistry;
		this.foreground = foreground;
        this.style=style;
	}
	
	

	public void putFontRegistry(String symbolicName, FontData[] fontData) {
		fontRegistry.put(symbolicName, fontData);
	}

	public Font getFontRegistry(String symbolicName) {
		return fontRegistry.get(symbolicName);
	}

	public Color getForeground() {
		return foreground;
	}

	public void setForeground(Color foreground) {
		this.foreground = foreground;
	}
	
	public void setStyle(int style) {
		this.style = style;
		
	}

}
*/