/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.rbridge;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;

/**
 * Reads CSV data with the options of the CSV import dialog (opencsv). The
 * preview of the dialog and the import into the table use the same methods.
 */
public final class CsvReading {

	/** The characters read for the preview of large files. */
	public static final int PREVIEW_CHARS = 1 << 20;

	/** The separators which can be detected. */
	private static final char[] SEPARATORS = { ',', ';', '\t', '|' };

	/** The options of the CSV import. */
	public static final class Options {
		public char separator = ',';
		public char quote = '"';
		public char escape = '\\';
		/** The comment character or '\0' (no comments). */
		public char comment = '\0';
		/** The lines skipped at the start (before the comments are removed). */
		public int skipLines;
		public boolean strictQuotes;
		public boolean ignoreLeadingWhiteSpace;
		public boolean keepCarriageReturn;
		public boolean ignoreQuotations;
		public boolean trimValues;
		public boolean skipEmptyRows;
		/** The first row is also used for the column titles of the table. */
		public boolean titlesFromFirstRow;
		public Charset charset = Charset.forName("UTF-8");
	}

	private CsvReading() {
	}

	/**
	 * Reads the text of a file (with a byte order mark removed).
	 *
	 * @param maxChars the maximum characters to read or 0 for the complete file
	 */
	public static String readText(String file, Charset charset, int maxChars) throws IOException {
		if (maxChars <= 0) {
			return stripBom(new String(Files.readAllBytes(Paths.get(file)), charset));
		}
		try (Reader reader = new InputStreamReader(Files.newInputStream(Paths.get(file)), charset)) {
			char[] buffer = new char[maxChars];
			int length = 0;
			int n;
			while (length < maxChars && (n = reader.read(buffer, length, maxChars - length)) > 0) {
				length += n;
			}
			return stripBom(new String(buffer, 0, length));
		}
	}

	private static String stripBom(String text) {
		return text.startsWith("\uFEFF") ? text.substring(1) : text;
	}

	/**
	 * Parses the text into rows.
	 *
	 * @param maxRows the maximum rows or 0 for all rows
	 */
	public static List<String[]> parse(String text, Options o, int maxRows) throws IOException, CsvValidationException {
		String filtered = filterLines(text, o.skipLines, o.comment);
		List<String[]> rows = new ArrayList<String[]>();
		try (CSVReader reader = new CSVReaderBuilder(new StringReader(filtered))
				.withCSVParser(new CSVParserBuilder().withSeparator(o.separator).withQuoteChar(o.quote).withEscapeChar(o.escape)
						.withStrictQuotes(o.strictQuotes).withIgnoreLeadingWhiteSpace(o.ignoreLeadingWhiteSpace)
						.withIgnoreQuotations(o.ignoreQuotations).build())
				.withKeepCarriageReturn(o.keepCarriageReturn).build()) {
			String[] row;
			while ((row = reader.readNext()) != null) {
				if (o.trimValues) {
					for (int i = 0; i < row.length; i++) {
						row[i] = row[i].trim();
					}
				}
				if (o.skipEmptyRows && isEmpty(row)) {
					continue;
				}
				rows.add(row);
				if (maxRows > 0 && rows.size() >= maxRows) {
					break;
				}
			}
		}
		return rows;
	}

	private static boolean isEmpty(String[] row) {
		for (String value : row) {
			if (!value.trim().isEmpty()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Removes the skipped lines at the start and the comment lines (the line
	 * endings of the other lines are kept).
	 */
	static String filterLines(String text, int skipLines, char comment) {
		if (skipLines <= 0 && comment == '\0') {
			return text;
		}
		StringBuilder out = new StringBuilder(text.length());
		int start = 0;
		int line = 0;
		while (start < text.length()) {
			int end = text.indexOf('\n', start);
			end = end < 0 ? text.length() : end + 1;
			if (line >= skipLines && !isComment(text, start, end, comment)) {
				out.append(text, start, end);
			}
			line++;
			start = end;
		}
		return out.toString();
	}

	private static boolean isComment(String text, int start, int end, char comment) {
		if (comment == '\0') {
			return false;
		}
		for (int i = start; i < end; i++) {
			char c = text.charAt(i);
			if (c != ' ' && c != '\t') {
				return c == comment;
			}
		}
		return false;
	}

	/**
	 * Detects the separator (comma, semicolon, tab or pipe) which occurs in the
	 * first lines with the most constant count outside of quotes. Returns '\0' if
	 * no separator was found.
	 */
	public static char detectSeparator(String text, char quote, char comment, int skipLines) {
		String filtered = filterLines(text, skipLines, comment);
		List<String> lines = new ArrayList<String>();
		for (String line : filtered.split("\r?\n", 51)) {
			if (!line.trim().isEmpty() && lines.size() < 50) {
				lines.add(line);
			}
		}
		char best = '\0';
		long bestScore = 0;
		for (char sep : SEPARATORS) {
			Map<Integer, Integer> frequency = new HashMap<Integer, Integer>();
			for (String line : lines) {
				int count = 0;
				boolean quoted = false;
				for (int i = 0; i < line.length(); i++) {
					char c = line.charAt(i);
					if (c == quote) {
						quoted = !quoted;
					} else if (c == sep && !quoted) {
						count++;
					}
				}
				if (count > 0) {
					frequency.merge(count, 1, Integer::sum);
				}
			}
			/* The most frequent count: lines with this count, then the count */
			for (Map.Entry<Integer, Integer> e : frequency.entrySet()) {
				long score = e.getValue() * 100000L + e.getKey();
				if (score > bestScore) {
					bestScore = score;
					best = sep;
				}
			}
		}
		return best;
	}
}
