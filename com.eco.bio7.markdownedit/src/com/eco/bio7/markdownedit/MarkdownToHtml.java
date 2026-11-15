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
package com.eco.bio7.markdownedit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.options.MutableDataSet;

/**
 * Converts a markdown file (*.md) to HTML in Java, without R: tables,
 * strikethrough, task lists, links, a table of contents ([TOC]) and the YAML
 * header (its title). The page has a readable style (light and dark, following
 * the system) and is written next to the markdown file (relative images and
 * links work).
 */
public final class MarkdownToHtml {

	private static final Pattern YAML = Pattern.compile("\\A---\\s*\\R(.*?)\\R(?:---|\\.\\.\\.)\\s*(\\R|\\z)", Pattern.DOTALL);
	private static final Pattern YAML_TITLE = Pattern.compile("(?m)^title:\\s*[\"']?(.*?)[\"']?\\s*$");

	private static final Parser PARSER;
	private static final HtmlRenderer RENDERER;

	static {
		MutableDataSet options = new MutableDataSet();
		options.set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create(), StrikethroughExtension.create(), AutolinkExtension.create(),
				TaskListExtension.create(), YamlFrontMatterExtension.create(), TocExtension.create()));
		PARSER = Parser.builder(options).build();
		RENDERER = HtmlRenderer.builder(options).build();
	}

	private static final String CSS = String.join("\n", //
			":root { color-scheme: light dark; --fg: #1f2328; --bg: #ffffff; --muted: #59636e; --border: #d1d9e0; --code-bg: #f6f8fa; --link: #0969da; }", //
			"@media (prefers-color-scheme: dark) { :root { --fg: #e6edf3; --bg: #0d1117; --muted: #9198a1; --border: #3d444d; --code-bg: #151b23; --link: #4493f8; } }", //
			"body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Helvetica, Arial, sans-serif; font-size: 16px; line-height: 1.6;", //
			"  color: var(--fg); background: var(--bg); max-width: 860px; margin: 0 auto; padding: 32px 24px 64px; }", //
			"h1, h2, h3, h4, h5, h6 { line-height: 1.25; margin: 1.5em 0 0.6em; font-weight: 600; }", //
			"h1 { font-size: 2em; padding-bottom: 0.3em; border-bottom: 1px solid var(--border); }", //
			"h2 { font-size: 1.5em; padding-bottom: 0.3em; border-bottom: 1px solid var(--border); }", //
			"h3 { font-size: 1.25em; } h4 { font-size: 1em; } h5, h6 { font-size: 0.875em; color: var(--muted); }", //
			"a { color: var(--link); text-decoration: none; } a:hover { text-decoration: underline; }", //
			"code, pre { font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; font-size: 0.875em; }", //
			"code { background: var(--code-bg); padding: 0.2em 0.4em; border-radius: 6px; }", //
			"pre { background: var(--code-bg); padding: 16px; border-radius: 6px; overflow: auto; line-height: 1.45; }", //
			"pre code { background: none; padding: 0; font-size: 1em; }", //
			"blockquote { margin: 0 0 16px; padding: 0 1em; color: var(--muted); border-left: 0.25em solid var(--border); }", //
			"table { border-collapse: collapse; margin: 16px 0; display: block; overflow: auto; }", //
			"th, td { border: 1px solid var(--border); padding: 6px 13px; } th { font-weight: 600; }", //
			"tr:nth-child(2n) { background: var(--code-bg); }", //
			"img { max-width: 100%; } hr { border: 0; height: 1px; background: var(--border); margin: 24px 0; }", //
			"li.task-list-item { list-style: none; } li.task-list-item input { margin: 0 0.4em 0 -1.4em; }", //
			"header.title { margin-bottom: 24px; } header.title h1 { border: 0; margin-top: 0; } header.title p { color: var(--muted); margin: 0; }");

	private MarkdownToHtml() {
	}

	/** The HTML page of a markdown text (the title of the YAML header or the file name). */
	public static String toHtml(String markdown, String fallbackTitle) {
		String title = fallbackTitle;
		String author = null;
		String date = null;
		Matcher y = YAML.matcher(markdown);
		if (y.find()) {
			String yaml = y.group(1);
			Matcher t = YAML_TITLE.matcher(yaml);
			if (t.find()) {
				title = t.group(1);
			}
			author = yamlValue(yaml, "author");
			date = yamlValue(yaml, "date");
		}
		Node doc = PARSER.parse(markdown);
		String body = RENDERER.render(doc);
		StringBuilder sb = new StringBuilder();
		sb.append("<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"utf-8\">\n");
		sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n");
		sb.append("<title>").append(escape(title)).append("</title>\n<style>\n").append(CSS).append("\n</style>\n</head>\n<body>\n");
		if (y.find(0)) {
			sb.append("<header class=\"title\">\n<h1>").append(escape(title)).append("</h1>\n");
			if (author != null) {
				sb.append("<p>").append(escape(author)).append("</p>\n");
			}
			if (date != null) {
				sb.append("<p>").append(escape(date)).append("</p>\n");
			}
			sb.append("</header>\n");
		}
		sb.append(body).append("</body>\n</html>\n");
		return sb.toString();
	}

	/**
	 * Converts a markdown file to an HTML file next to it (same name, *.html).
	 *
	 * @return the HTML file
	 */
	public static Path convert(Path markdownFile) throws IOException {
		String md = Files.readString(markdownFile, StandardCharsets.UTF_8);
		String name = markdownFile.getFileName().toString();
		String base = name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name;
		Path html = markdownFile.resolveSibling(base + ".html");
		Files.writeString(html, toHtml(md, base), StandardCharsets.UTF_8);
		return html;
	}

	private static String yamlValue(String yaml, String key) {
		Matcher m = Pattern.compile("(?m)^" + key + ":\\s*[\"']?(.*?)[\"']?\\s*$").matcher(yaml);
		return m.find() && !m.group(1).isBlank() ? m.group(1) : null;
	}

	private static String escape(String s) {
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}
}
