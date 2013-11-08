package com.eco.bio7.rbridge;

/**
 * @author M. Austenfeld 
 * A class to wrap a Font object for the use with the
 *         XStream library!
 */
public class GridXMLFontObject {
	private int height;
	private int style;
	private String locale;
	private String name;

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getStyle() {
		return style;
	}

	public void setStyle(int style) {
		this.style = style;
	}

	public String getLocale() {
		return locale;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
