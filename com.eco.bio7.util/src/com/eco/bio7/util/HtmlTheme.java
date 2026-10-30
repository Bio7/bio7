/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************/
package com.eco.bio7.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;

import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.browser.Browser;

/**
 * The CSS of the local HTML pages in the Bio7 browser (e.g., the R help) for
 * the active theme: the CSS of a custom theme (its colors, set by the theme
 * engine of Bio7), the dark CSS of the dark default theme or none (the light
 * default theme: the CSS of the page).
 */
public final class HtmlTheme {

	private static final String DARK_CSS = "javafx/Bio7BrowserDarkHTML.css";

	private static volatile String themeCss;
	private static String writtenCss;
	private static File cssFile;

	private HtmlTheme() {
	}

	/** Sets the CSS of the active custom theme (null: the CSS of the default themes). */
	public static void setThemeCss(String css) {
		themeCss = css;
	}

	/** The CSS for the local HTML pages of the active theme or null (the CSS of the page). */
	public static String getCss() {
		String css = themeCss;
		if (css != null) {
			return css;
		}
		return Util.isThemeBlack() ? darkCss() : null;
	}

	/**
	 * The CSS of the active theme as a file (e.g., the stylesheet for the R help
	 * created by Rd2HTML) or null if the page keeps its CSS.
	 */
	public static synchronized String getCssPath() {
		String css = getCss();
		if (css == null) {
			return null;
		}
		try {
			if (cssFile == null || !cssFile.isFile() || !css.equals(writtenCss)) {
				if (cssFile == null) {
					cssFile = File.createTempFile("bio7-html-theme", ".css");
					cssFile.deleteOnExit();
				}
				Files.writeString(cssFile.toPath(), css, StandardCharsets.UTF_8);
				writtenCss = css;
			}
			return cssFile.getAbsolutePath().replace("\\", "/");
		} catch (IOException e) {
			return null;
		}
	}

	/** Adds the CSS of the active theme to the page of the browser (after the CSS of the page). */
	public static void apply(Browser browser) {
		String css = getCss();
		if (css == null || browser == null || browser.isDisposed()) {
			return;
		}
		/* Base64: no problems with quotes, line breaks or special characters */
		String encoded = Base64.getEncoder().encodeToString(css.getBytes(StandardCharsets.UTF_8));
		browser.execute("var bio7Style = document.createElement('style');"
				+ "bio7Style.innerHTML = decodeURIComponent(escape(window.atob('" + encoded + "')));"
				+ "document.head.appendChild(bio7Style);");
	}

	private static String darkCss() {
		try {
			URL url = Platform.getBundle("com.eco.bio7.themes").getEntry(DARK_CSS);
			try (InputStream in = url.openStream()) {
				return new String(in.readAllBytes(), StandardCharsets.UTF_8);
			}
		} catch (IOException | RuntimeException e) {
			return "body { background: #1E1E1E; color: #CCCCCC; } a { color: grey; }";
		}
	}
}
