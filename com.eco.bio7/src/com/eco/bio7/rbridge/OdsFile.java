package com.eco.bio7.rbridge;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Reads and writes spreadsheets of LibreOffice (OpenDocument Spreadsheet,
 * *.ods) without a library: the file is a zip archive with the cells in
 * content.xml. Numbers are read as their exact values (office:value, not the
 * formatted text), dates as ISO dates, booleans as TRUE / FALSE and texts with
 * their lines. Numbers are written as numbers, the other cells as texts.
 */
public final class OdsFile {

	private static final String OFFICE = "urn:oasis:names:tc:opendocument:xmlns:office:1.0";
	private static final String TABLE = "urn:oasis:names:tc:opendocument:xmlns:table:1.0";
	private static final String TEXT = "urn:oasis:names:tc:opendocument:xmlns:text:1.0";
	private static final String MIMETYPE = "application/vnd.oasis.opendocument.spreadsheet";
	/* Repeated empty rows or columns (e.g., the rest of a sheet) are not stored beyond these limits */
	private static final int MAX_ROWS = 1_048_576, MAX_COLUMNS = 16_384;

	/**
	 * A sheet: the name and the texts of the cells (rows, columns; empty cells are
	 * ""), optional the formats and pictures of the cells (null: none) and the
	 * widths of the columns and the heights of the rows in points (null: the
	 * defaults of LibreOffice).
	 */
	public record Sheet(String name, String[][] cells, Format[][] formats, Picture[][] pictures, double[] columnWidths,
			double[] rowHeights) {

		/** A sheet with the texts only. */
		public Sheet(String name, String[][] cells) {
			this(name, cells, null, null, null, null);
		}
	}

	/**
	 * The format of a cell: the background and the text color (RGB 0xrrggbb, -1 =
	 * the default), the font (null and 0 = the default) and the style.
	 */
	public record Format(int background, int foreground, String fontName, double fontSize, boolean bold, boolean italic) {

		/** True if the format has no setting (the default of LibreOffice). */
		public boolean isDefault() {
			return background < 0 && foreground < 0 && fontName == null && fontSize <= 0 && !bold && !italic;
		}
	}

	/** A picture in a cell: a PNG image and its size in points. */
	public record Picture(byte[] png, double width, double height) {
	}

	private static final String STYLE = "urn:oasis:names:tc:opendocument:xmlns:style:1.0";
	private static final String FO = "urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0";
	private static final String DRAW = "urn:oasis:names:tc:opendocument:xmlns:drawing:1.0";
	private static final String SVG = "urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0";
	private static final String XLINK = "http://www.w3.org/1999/xlink";

	private OdsFile() {
	}

	/**
	 * Reads the sheets of a file.
	 *
	 * @param file the *.ods file
	 * @return the sheets (empty rows and columns at the end are removed)
	 * @throws IOException if the file is not an OpenDocument spreadsheet
	 */
	public static List<Sheet> read(File file) throws IOException {
		try (ZipFile zip = new ZipFile(file)) {
			ZipEntry content = zip.getEntry("content.xml");
			if (content == null) {
				throw new IOException("Not an OpenDocument spreadsheet (no content.xml): " + file.getName());
			}
			try (InputStream in = zip.getInputStream(content)) {
				return parse(in);
			}
		} catch (XMLStreamException e) {
			throw new IOException("The content of " + file.getName() + " can't be read: " + e.getMessage(), e);
		}
	}

	private static List<Sheet> parse(InputStream in) throws XMLStreamException {
		XMLInputFactory factory = XMLInputFactory.newInstance();
		/* No external entities (a file from somewhere) */
		factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
		XMLStreamReader r = factory.createXMLStreamReader(in);
		List<Sheet> sheets = new ArrayList<>();
		String sheetName = null;
		List<List<String>> rows = null;
		List<String> row = null;
		/* Empty rows and cells are added only if data follows (the rest of a sheet is often "repeated" empty cells) */
		long pendingRows = 0, pendingCells = 0;
		int rowRepeat = 1, cellRepeat = 1;
		/* The value of the current cell (null: from its text) */
		String value = null;
		StringBuilder text = null;
		int paragraphs = 0;
		/* Inside of a comment of a cell (its text is not the value) */
		int annotation = 0;
		while (r.hasNext()) {
			int event = r.next();
			if (event == XMLStreamConstants.START_ELEMENT) {
				String ns = r.getNamespaceURI(), local = r.getLocalName();
				if (OFFICE.equals(ns) && local.equals("annotation")) {
					annotation++;
				} else if (annotation > 0) {
					continue;
				} else if (TABLE.equals(ns) && local.equals("table")) {
					sheetName = r.getAttributeValue(TABLE, "name");
					rows = new ArrayList<>();
					pendingRows = 0;
				} else if (rows != null && TABLE.equals(ns) && local.equals("table-row")) {
					row = new ArrayList<>();
					pendingCells = 0;
					rowRepeat = repeat(r.getAttributeValue(TABLE, "number-rows-repeated"));
				} else if (row != null && TABLE.equals(ns) && (local.equals("table-cell") || local.equals("covered-table-cell"))) {
					cellRepeat = repeat(r.getAttributeValue(TABLE, "number-columns-repeated"));
					value = cellValue(r);
					text = new StringBuilder();
					paragraphs = 0;
				} else if (text != null && TEXT.equals(ns)) {
					if (local.equals("p")) {
						if (paragraphs++ > 0) {
							text.append('\n');
						}
					} else if (local.equals("s")) {
						int n = repeat(r.getAttributeValue(TEXT, "c"));
						text.append(" ".repeat(Math.min(n, 1000)));
					} else if (local.equals("tab")) {
						text.append('\t');
					} else if (local.equals("line-break")) {
						text.append('\n');
					}
				}
			} else if ((event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA) && text != null && annotation == 0) {
				text.append(r.getText());
			} else if (event == XMLStreamConstants.END_ELEMENT) {
				String ns = r.getNamespaceURI(), local = r.getLocalName();
				if (OFFICE.equals(ns) && local.equals("annotation")) {
					annotation--;
				} else if (annotation > 0) {
					continue;
				} else if (row != null && TABLE.equals(ns) && (local.equals("table-cell") || local.equals("covered-table-cell"))) {
					String cell = value != null ? value : text.toString();
					if (cell.isEmpty()) {
						pendingCells += cellRepeat;
					} else {
						for (long i = 0; i < pendingCells && row.size() < MAX_COLUMNS; i++) {
							row.add("");
						}
						pendingCells = 0;
						for (int i = 0; i < cellRepeat && row.size() < MAX_COLUMNS; i++) {
							row.add(cell);
						}
					}
					text = null;
					value = null;
				} else if (rows != null && row != null && TABLE.equals(ns) && local.equals("table-row")) {
					if (row.isEmpty()) {
						pendingRows += rowRepeat;
					} else {
						for (long i = 0; i < pendingRows && rows.size() < MAX_ROWS; i++) {
							rows.add(new ArrayList<>());
						}
						pendingRows = 0;
						for (int i = 0; i < rowRepeat && rows.size() < MAX_ROWS; i++) {
							rows.add(row);
						}
					}
					row = null;
				} else if (rows != null && TABLE.equals(ns) && local.equals("table")) {
					int columns = 0;
					for (List<String> l : rows) {
						columns = Math.max(columns, l.size());
					}
					String[][] cells = new String[rows.size()][columns];
					for (int i = 0; i < rows.size(); i++) {
						List<String> l = rows.get(i);
						for (int j = 0; j < columns; j++) {
							cells[i][j] = j < l.size() ? l.get(j) : "";
						}
					}
					sheets.add(new Sheet(sheetName != null ? sheetName : "Sheet " + (sheets.size() + 1), cells));
					rows = null;
				}
			}
		}
		r.close();
		return sheets;
	}

	/* The value of a cell from its attributes (numbers exact) or null (the text of the cell) */
	private static String cellValue(XMLStreamReader r) {
		String type = r.getAttributeValue(OFFICE, "value-type");
		if (type == null) {
			return null;
		}
		switch (type) {
		case "float":
		case "percentage":
		case "currency":
			String v = r.getAttributeValue(OFFICE, "value");
			return v != null ? number(v) : null;
		case "date":
			return r.getAttributeValue(OFFICE, "date-value");
		case "time":
			return r.getAttributeValue(OFFICE, "time-value");
		case "boolean":
			String b = r.getAttributeValue(OFFICE, "boolean-value");
			return b != null ? (Boolean.parseBoolean(b) ? "TRUE" : "FALSE") : null;
		default:
			return null;
		}
	}

	/* A number without a needless ".0" (e.g., 3 instead of 3.0) */
	private static String number(String v) {
		try {
			double d = Double.parseDouble(v);
			if (d == Math.rint(d) && Math.abs(d) < 1e15) {
				return String.valueOf((long) d);
			}
			return v;
		} catch (NumberFormatException e) {
			return v;
		}
	}

	private static int repeat(String s) {
		if (s == null) {
			return 1;
		}
		try {
			return Math.max(1, Integer.parseInt(s.trim()));
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	/**
	 * Writes sheets to a file (numbers as numbers, the other cells as texts; empty
	 * rows and columns at the end are not written). The names of the sheets are
	 * made unique and valid for LibreOffice.
	 *
	 * @param file   the *.ods file
	 * @param sheets the sheets
	 * @throws IOException if the file can't be written
	 */
	public static void write(File file, List<Sheet> sheets) throws IOException {
		/* The styles of the cells, columns and rows (the automatic styles are written before the cells) */
		java.util.Map<Format, String> cellStyles = new java.util.LinkedHashMap<>();
		java.util.Map<Double, String> columnStyles = new java.util.LinkedHashMap<>(), rowStyles = new java.util.LinkedHashMap<>();
		Set<String> fonts = new java.util.TreeSet<>();
		int[][] extents = new int[sheets.size()][];
		for (int k = 0; k < sheets.size(); k++) {
			Sheet sheet = sheets.get(k);
			extents[k] = extent(sheet);
			for (int i = 0; i < extents[k][0]; i++) {
				for (int j = 0; j < extents[k][1]; j++) {
					Format f = format(sheet, i, j);
					if (f != null) {
						cellStyles.computeIfAbsent(f, _ -> "ce" + (cellStyles.size() + 1));
						if (f.fontName() != null) {
							fonts.add(f.fontName());
						}
					}
				}
			}
			if (sheet.columnWidths() != null) {
				for (int j = 0; j < Math.min(extents[k][1], sheet.columnWidths().length); j++) {
					columnStyles.computeIfAbsent(round(sheet.columnWidths()[j]), _ -> "co" + (columnStyles.size() + 1));
				}
			}
			if (sheet.rowHeights() != null) {
				for (int i = 0; i < Math.min(extents[k][0], sheet.rowHeights().length); i++) {
					rowStyles.computeIfAbsent(round(sheet.rowHeights()[i]), _ -> "ro" + (rowStyles.size() + 1));
				}
			}
		}
		List<byte[]> pictures = new ArrayList<>();
		try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file)); ZipOutputStream zip = new ZipOutputStream(out)) {
			/* The first entry: the mimetype, not compressed (required by the format) */
			byte[] mime = MIMETYPE.getBytes(StandardCharsets.US_ASCII);
			ZipEntry m = new ZipEntry("mimetype");
			m.setMethod(ZipEntry.STORED);
			m.setSize(mime.length);
			m.setCompressedSize(mime.length);
			CRC32 crc = new CRC32();
			crc.update(mime);
			m.setCrc(crc.getValue());
			zip.putNextEntry(m);
			zip.write(mime);
			zip.closeEntry();
			zip.putNextEntry(new ZipEntry("content.xml"));
			Writer w = new OutputStreamWriter(zip, StandardCharsets.UTF_8);
			w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<office:document-content xmlns:office=\"" + OFFICE + "\" xmlns:table=\"" + TABLE
					+ "\" xmlns:text=\"" + TEXT + "\" xmlns:style=\"" + STYLE + "\" xmlns:fo=\"" + FO + "\" xmlns:draw=\"" + DRAW + "\" xmlns:svg=\"" + SVG
					+ "\" xmlns:xlink=\"" + XLINK + "\" office:version=\"1.2\">");
			/* The fonts and the automatic styles */
			w.write("<office:font-face-decls>");
			for (String font : fonts) {
				w.write("<style:font-face style:name=\"" + escape(font) + "\" svg:font-family=\"" + escape("'" + font + "'") + "\"/>");
			}
			w.write("</office:font-face-decls><office:automatic-styles>");
			for (java.util.Map.Entry<Double, String> e : columnStyles.entrySet()) {
				w.write("<style:style style:name=\"" + e.getValue() + "\" style:family=\"table-column\"><style:table-column-properties style:column-width=\""
						+ points(e.getKey()) + "\"/></style:style>");
			}
			for (java.util.Map.Entry<Double, String> e : rowStyles.entrySet()) {
				w.write("<style:style style:name=\"" + e.getValue() + "\" style:family=\"table-row\"><style:table-row-properties style:row-height=\""
						+ points(e.getKey()) + "\" style:use-optimal-row-height=\"false\"/></style:style>");
			}
			for (java.util.Map.Entry<Format, String> e : cellStyles.entrySet()) {
				Format f = e.getKey();
				w.write("<style:style style:name=\"" + e.getValue() + "\" style:family=\"table-cell\">");
				if (f.background() >= 0) {
					w.write("<style:table-cell-properties fo:background-color=\"" + color(f.background()) + "\"/>");
				}
				StringBuilder t = new StringBuilder();
				if (f.foreground() >= 0) {
					t.append(" fo:color=\"").append(color(f.foreground())).append('"');
				}
				if (f.fontName() != null) {
					t.append(" style:font-name=\"").append(escape(f.fontName())).append('"');
				}
				if (f.fontSize() > 0) {
					String size = points(f.fontSize());
					t.append(" fo:font-size=\"").append(size).append("\" style:font-size-asian=\"").append(size).append("\" style:font-size-complex=\"")
							.append(size).append('"');
				}
				if (f.bold()) {
					t.append(" fo:font-weight=\"bold\" style:font-weight-asian=\"bold\" style:font-weight-complex=\"bold\"");
				}
				if (f.italic()) {
					t.append(" fo:font-style=\"italic\" style:font-style-asian=\"italic\" style:font-style-complex=\"italic\"");
				}
				if (t.length() > 0) {
					w.write("<style:text-properties" + t + "/>");
				}
				w.write("</style:style>");
			}
			w.write("</office:automatic-styles><office:body><office:spreadsheet>");
			Set<String> used = new HashSet<>();
			for (int k = 0; k < sheets.size(); k++) {
				Sheet sheet = sheets.get(k);
				w.write("<table:table table:name=\"" + escape(uniqueName(sheet.name(), used)) + "\">");
				String[][] cells = sheet.cells();
				int rows = extents[k][0], columns = extents[k][1];
				/* The columns (with their widths) */
				for (int j = 0; j < Math.max(1, columns); j++) {
					String style = sheet.columnWidths() != null && j < sheet.columnWidths().length && j < columns
							? columnStyles.get(round(sheet.columnWidths()[j]))
							: null;
					w.write(style != null ? "<table:table-column table:style-name=\"" + style + "\"/>" : "<table:table-column/>");
				}
				for (int i = 0; i < rows; i++) {
					String rowStyle = sheet.rowHeights() != null && i < sheet.rowHeights().length ? rowStyles.get(round(sheet.rowHeights()[i])) : null;
					w.write(rowStyle != null ? "<table:table-row table:style-name=\"" + rowStyle + "\">" : "<table:table-row>");
					for (int j = 0; j < columns; j++) {
						String s = i < cells.length && j < cells[i].length && cells[i][j] != null ? cells[i][j] : "";
						Format f = format(sheet, i, j);
						Picture picture = picture(sheet, i, j);
						String frame = null;
						if (picture != null) {
							pictures.add(picture.png());
							int n = pictures.size();
							frame = "<draw:frame draw:z-index=\"" + n + "\" draw:name=\"Image " + n + "\" svg:x=\"0pt\" svg:y=\"0pt\" svg:width=\""
									+ points(picture.width()) + "\" svg:height=\"" + points(picture.height()) + "\"><draw:image xlink:href=\"Pictures/image" + n
									+ ".png\" xlink:type=\"simple\" xlink:show=\"embed\" xlink:actuate=\"onLoad\"/></draw:frame>";
						}
						writeCell(w, s, f != null ? cellStyles.get(f) : null, frame);
					}
					w.write("</table:table-row>");
				}
				if (rows == 0) {
					w.write("<table:table-row><table:table-cell/></table:table-row>");
				}
				w.write("</table:table>");
			}
			w.write("</office:spreadsheet></office:body></office:document-content>");
			w.flush();
			zip.closeEntry();
			/* The pictures */
			for (int n = 0; n < pictures.size(); n++) {
				zip.putNextEntry(new ZipEntry("Pictures/image" + (n + 1) + ".png"));
				zip.write(pictures.get(n));
				zip.closeEntry();
			}
			/* The manifest (with the pictures) */
			StringBuilder manifest = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
					+ "<manifest:manifest xmlns:manifest=\"urn:oasis:names:tc:opendocument:xmlns:manifest:1.0\" manifest:version=\"1.2\">\n"
					+ " <manifest:file-entry manifest:full-path=\"/\" manifest:version=\"1.2\" manifest:media-type=\"" + MIMETYPE + "\"/>\n"
					+ " <manifest:file-entry manifest:full-path=\"content.xml\" manifest:media-type=\"text/xml\"/>\n");
			for (int n = 0; n < pictures.size(); n++) {
				manifest.append(" <manifest:file-entry manifest:full-path=\"Pictures/image").append(n + 1).append(".png\" manifest:media-type=\"image/png\"/>\n");
			}
			manifest.append("</manifest:manifest>\n");
			zip.putNextEntry(new ZipEntry("META-INF/manifest.xml"));
			zip.write(manifest.toString().getBytes(StandardCharsets.UTF_8));
			zip.closeEntry();
		}
	}

	/* The used rows and columns: the cells with a text, a format or a picture */
	private static int[] extent(Sheet sheet) {
		String[][] cells = sheet.cells();
		int rows = 0, columns = 0;
		int nr = cells.length;
		if (sheet.formats() != null) {
			nr = Math.max(nr, sheet.formats().length);
		}
		if (sheet.pictures() != null) {
			nr = Math.max(nr, sheet.pictures().length);
		}
		for (int i = 0; i < nr; i++) {
			int nc = Math.max(i < cells.length ? cells[i].length : 0, Math.max(
					sheet.formats() != null && i < sheet.formats().length && sheet.formats()[i] != null ? sheet.formats()[i].length : 0,
					sheet.pictures() != null && i < sheet.pictures().length && sheet.pictures()[i] != null ? sheet.pictures()[i].length : 0));
			for (int j = nc - 1; j >= 0; j--) {
				boolean used = (i < cells.length && j < cells[i].length && !empty(cells[i][j])) || format(sheet, i, j) != null
						|| picture(sheet, i, j) != null;
				if (used) {
					rows = i + 1;
					columns = Math.max(columns, j + 1);
					break;
				}
			}
		}
		return new int[] { rows, columns };
	}

	/* The format of a cell (null: none or the default) */
	private static Format format(Sheet sheet, int i, int j) {
		Format[][] f = sheet.formats();
		Format format = f != null && i < f.length && f[i] != null && j < f[i].length ? f[i][j] : null;
		return format == null || format.isDefault() ? null : format;
	}

	private static Picture picture(Sheet sheet, int i, int j) {
		Picture[][] p = sheet.pictures();
		Picture picture = p != null && i < p.length && p[i] != null && j < p[i].length ? p[i][j] : null;
		return picture == null || picture.png() == null || picture.png().length == 0 ? null : picture;
	}

	private static double round(double v) {
		return Math.round(v * 100) / 100.0;
	}

	private static String points(double v) {
		return java.math.BigDecimal.valueOf(round(Math.max(0, v))).stripTrailingZeros().toPlainString() + "pt";
	}

	private static String color(int rgb) {
		return String.format("#%06x", rgb & 0xffffff);
	}

	private static void writeCell(Writer w, String s, String style, String frame) throws IOException {
		String attr = style != null ? " table:style-name=\"" + style + "\"" : "";
		String inner = frame != null ? frame : "";
		String t = s.trim();
		if (t.isEmpty()) {
			w.write(inner.isEmpty() ? "<table:table-cell" + attr + "/>" : "<table:table-cell" + attr + ">" + inner + "</table:table-cell>");
			return;
		}
		double d = Double.NaN;
		try {
			d = Double.parseDouble(t);
		} catch (NumberFormatException e) {
			/* A text */
		}
		if (Double.isFinite(d) && !t.endsWith("d") && !t.endsWith("D") && !t.endsWith("f") && !t.endsWith("F")) {
			w.write("<table:table-cell" + attr + " office:value-type=\"float\" office:value=\"" + t + "\">" + inner + "<text:p>" + escape(t)
					+ "</text:p></table:table-cell>");
			return;
		}
		w.write("<table:table-cell" + attr + " office:value-type=\"string\">" + inner);
		for (String line : s.split("\n", -1)) {
			w.write("<text:p>" + escapeText(line) + "</text:p>");
		}
		w.write("</table:table-cell>");
	}

	/* A name of a sheet without the characters which LibreOffice doesn't allow, unique */
	private static String uniqueName(String name, Set<String> used) {
		String base = (name == null || name.isBlank() ? "Sheet" : name).replaceAll("[\\[\\]*?:/\\\\']", "_").trim();
		if (base.isEmpty()) {
			base = "Sheet";
		}
		String n = base;
		for (int i = 2; !used.add(n.toLowerCase()); i++) {
			n = base + " " + i;
		}
		return n;
	}

	private static boolean empty(String s) {
		return s == null || s.isBlank();
	}

	private static String escape(String s) {
		StringBuilder b = new StringBuilder(s.length());
		for (char c : s.toCharArray()) {
			switch (c) {
			case '&' -> b.append("&amp;");
			case '<' -> b.append("&lt;");
			case '>' -> b.append("&gt;");
			case '"' -> b.append("&quot;");
			default -> {
				/* Control characters are not allowed in XML */
				if (c >= 0x20 || c == '\t' || c == '\n' || c == '\r') {
					b.append(c);
				}
			}
			}
		}
		return b.toString();
	}

	/* The text of a paragraph: tabs and runs of spaces as elements (else they are lost) */
	private static String escapeText(String s) {
		String e = escape(s).replace("\r", "");
		StringBuilder b = new StringBuilder();
		int i = 0;
		while (i < e.length()) {
			char c = e.charAt(i);
			if (c == '\t') {
				b.append("<text:tab/>");
				i++;
			} else if (c == ' ' && (i == 0 || i + 1 < e.length() && e.charAt(i + 1) == ' ')) {
				int j = i;
				while (j < e.length() && e.charAt(j) == ' ') {
					j++;
				}
				b.append("<text:s text:c=\"").append(j - i).append("\"/>");
				i = j;
			} else {
				b.append(c);
				i++;
			}
		}
		return b.toString();
	}
}
