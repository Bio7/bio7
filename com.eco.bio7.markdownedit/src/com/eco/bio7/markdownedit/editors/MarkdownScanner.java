package com.eco.bio7.markdownedit.editors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;

import com.eco.bio7.markdownedit.Activator;

/**
 * The highlighting of the markdown text (not the code chunks): headings (with a
 * larger font for H1 to H3), bold, italic, strikethrough, inline code and code
 * blocks, links, quotes, list markers, math, the YAML header, rules and
 * comments. The markup characters stay visible (it is the source).
 * <p>
 * The tokens {@link #other} (the text) and {@link #head} (headings) hold the
 * colors of the preferences (changed by the preference page).
 */
public class MarkdownScanner extends RuleBasedScanner {

	public Token other;
	public Token head;

	/* Font sizes of the headings H1 to H3 (relative to the editor font) */
	private static final double[] HEADING_SCALE = { 1.5, 1.3, 1.15 };

	private static final Pattern HEADING = Pattern.compile("^ {0,3}(#{1,6})(\\s.*|)$");
	private static final Pattern RULE = Pattern.compile("^ {0,3}([-*_])(\\s*\\1){2,}\\s*$");
	private static final Pattern QUOTE = Pattern.compile("^\\s{0,3}>");
	private static final Pattern LIST = Pattern.compile("^(\\s*)([-*+]|\\d{1,9}[.)])(\\s+)");
	private static final Pattern FENCE = Pattern.compile("^\\s{0,3}(```|~~~)");
	/*
	 * Inline markup, the first match wins (code before emphasis: no emphasis
	 * within code)
	 */
	private static final Pattern INLINE = Pattern.compile(String.join("|", //
			"(?<code>`+)(?<codetext>.+?)\\k<code>", //
			"(?<comment><!--.*?-->)", //
			"(?<math>\\$\\$.+?\\$\\$|(?<![\\w$])\\$(?=\\S)[^$\\n]*?\\S\\$(?![\\w$]))", //
			"(?<link>!?\\[[^\\]\\n]*\\]\\([^)\\n]*\\)|<https?://[^>\\s]+>|https?://[^\\s)>\\]]*[^\\s)>\\].,;:!?'\"])", //
			"(?<bolditalic>\\*\\*\\*(?=\\S).+?(?<=\\S)\\*\\*\\*|(?<![\\w_])___(?=\\S).+?(?<=\\S)___(?![\\w_]))", //
			"(?<bold>\\*\\*(?=\\S).+?(?<=\\S)\\*\\*|(?<![\\w_])__(?=\\S).+?(?<=\\S)__(?![\\w_]))", //
			"(?<italic>(?<![*\\w])\\*(?=[^\\s*]).*?(?<=[^\\s*])\\*(?![*\\w])|(?<![_\\w])_(?=[^\\s_]).*?(?<=[^\\s_])_(?![_\\w]))", //
			"(?<strike>~~(?=\\S).+?(?<=\\S)~~)"));

	private ISourceViewer viewer;
	private final Map<String, Font> fonts = new HashMap<>();

	/* The tokens of the current range */
	private final List<int[]> spans = new ArrayList<>();
	private final List<IToken> spanTokens = new ArrayList<>();
	private int index;
	private int tokenOffset;
	private int tokenLength;

	public MarkdownScanner(ColorManager manager) {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		RGB rgbkey = PreferenceConverter.getColor(store, "colourkey");
		RGB rgbkey1 = PreferenceConverter.getColor(store, "colourkey1");
		other = new Token(new TextAttribute(manager.getColor(rgbkey), null, isBold("BOLD_COLOURKEY")));
		head = new Token(new TextAttribute(manager.getColor(rgbkey1), null, isBold("BOLD_COLOURKEY1")));
	}

	public MarkdownScanner(MarkdownColorProvider rColorProvider) {
	}

	/** The viewer of the editor: its font (the base of the heading fonts) and background. */
	public void setViewer(ISourceViewer viewer) {
		this.viewer = viewer;
		StyledText text = viewer.getTextWidget();
		if (text != null) {
			text.addDisposeListener(_ -> disposeFonts());
		}
	}

	private int isBold(String key) {
		return Activator.getDefault().getPreferenceStore().getBoolean(key) ? SWT.BOLD : SWT.NORMAL;
	}

	/* ------------------------------------------------------------ scanning */

	@Override
	public void setRange(IDocument document, int offset, int length) {
		spans.clear();
		spanTokens.clear();
		index = 0;
		tokenOffset = offset;
		tokenLength = 0;
		int end = offset + length;
		try {
			Styles st = new Styles();
			int firstLine = document.getLineOfOffset(offset);
			int lastLine = document.getLineOfOffset(Math.max(offset, end - (length > 0 ? 1 : 0)));
			int yamlEnd = yamlEnd(document);
			boolean inCode = insideFence(document, firstLine);
			int pos = offset;
			for (int line = firstLine; line <= lastLine; line++) {
				IRegion r = document.getLineInformation(line);
				String text = document.get(r.getOffset(), r.getLength());
				int lineStart = r.getOffset();
				List<Object[]> lineSpans = new ArrayList<>();
				if (line <= yamlEnd) {
					lineSpans.add(new Object[] { 0, text.length(), st.meta });
				} else if (FENCE.matcher(text).find()) {
					inCode = !inCode;
					lineSpans.add(new Object[] { 0, text.length(), st.code });
				} else if (inCode) {
					lineSpans.add(new Object[] { 0, text.length(), st.code });
				} else {
					styleLine(text, st, lineSpans);
				}
				for (Object[] s : lineSpans) {
					int from = Math.max(lineStart + (int) s[0], pos);
					int to = Math.min(lineStart + (int) s[1], end);
					if (to <= from) {
						continue;
					}
					if (from > pos) {
						add(pos, from, other);
					}
					add(from, to, (IToken) s[2]);
					pos = to;
				}
			}
			if (pos < end) {
				add(pos, end, other);
			}
		} catch (BadLocationException e) {
			spans.clear();
			spanTokens.clear();
			add(offset, end, other);
		}
	}

	private void add(int from, int to, IToken token) {
		if (to > from) {
			spans.add(new int[] { from, to - from });
			spanTokens.add(token);
		}
	}

	@Override
	public IToken nextToken() {
		if (index >= spans.size()) {
			tokenOffset += tokenLength;
			tokenLength = 0;
			return Token.EOF;
		}
		int[] s = spans.get(index);
		tokenOffset = s[0];
		tokenLength = s[1];
		return spanTokens.get(index++);
	}

	@Override
	public int getTokenOffset() {
		return tokenOffset;
	}

	@Override
	public int getTokenLength() {
		return tokenLength;
	}

	/* The styles of a line outside of code */
	private void styleLine(String text, Styles st, List<Object[]> out) {
		Matcher h = HEADING.matcher(text);
		if (h.matches()) {
			out.add(new Object[] { 0, text.length(), st.heading(h.group(1).length()) });
			return;
		}
		if (RULE.matcher(text).matches()) {
			out.add(new Object[] { 0, text.length(), st.meta });
			return;
		}
		if (!st.formatting) {
			return;
		}
		if (QUOTE.matcher(text).find()) {
			out.add(new Object[] { 0, text.length(), st.quote });
			return;
		}
		int from = 0;
		Matcher l = LIST.matcher(text);
		if (l.find()) {
			int ms = l.start(2);
			int me = l.end(2);
			out.add(new Object[] { ms, me, st.marker });
			from = me;
		}
		Matcher m = INLINE.matcher(text);
		m.region(from, text.length());
		while (m.find()) {
			IToken t;
			if (m.group("code") != null) {
				t = st.code;
			} else if (m.group("comment") != null) {
				t = st.meta;
			} else if (m.group("math") != null) {
				t = st.code;
			} else if (m.group("link") != null) {
				t = st.link;
			} else if (m.group("bolditalic") != null) {
				t = st.boldItalic;
			} else if (m.group("bold") != null) {
				t = st.bold;
			} else if (m.group("italic") != null) {
				t = st.italic;
			} else {
				t = st.strike;
			}
			out.add(new Object[] { m.start(), m.end(), t });
		}
	}

	/* The last line of the YAML header (---) at the start of the document, -1 if none */
	private static int yamlEnd(IDocument doc) throws BadLocationException {
		if (doc.getNumberOfLines() < 2 || !line(doc, 0).trim().equals("---")) {
			return -1;
		}
		int n = Math.min(doc.getNumberOfLines(), 500);
		for (int i = 1; i < n; i++) {
			String t = line(doc, i).trim();
			if (t.equals("---") || t.equals("...")) {
				return i;
			}
		}
		return -1;
	}

	/*
	 * A line within a fenced code block (``` without {r}, an R chunk is its own
	 * partition): the fences above are counted
	 */
	private static boolean insideFence(IDocument doc, int line) throws BadLocationException {
		boolean in = false;
		for (int i = 0; i < line; i++) {
			if (FENCE.matcher(line(doc, i)).find()) {
				in = !in;
			}
		}
		return in;
	}

	private static String line(IDocument doc, int i) throws BadLocationException {
		IRegion r = doc.getLineInformation(i);
		return doc.get(r.getOffset(), r.getLength());
	}

	/* ------------------------------------------------------------ styles */

	/* The tokens of a range: the colors of the preferences, the font of the editor */
	private final class Styles {
		final boolean formatting;
		final boolean largeHeadings;
		final IToken code;
		final IToken link;
		final IToken quote;
		final IToken meta;
		final IToken marker;
		final IToken bold;
		final IToken italic;
		final IToken boldItalic;
		final IToken strike;
		private final TextAttribute text;
		private final TextAttribute heading;
		private final IToken[] headings = new IToken[6];

		Styles() {
			IPreferenceStore store = Activator.getDefault().getPreferenceStore();
			formatting = store.getBoolean("MARKDOWN_FORMATTING");
			largeHeadings = store.getBoolean("MARKDOWN_LARGE_HEADINGS");
			text = (TextAttribute) other.getData();
			heading = (TextAttribute) head.getData();
			Color fg = text.getForeground();
			RGB bgRgb = background();
			RGB fgRgb = fg != null ? fg.getRGB() : (dark(bgRgb) ? new RGB(230, 230, 230) : new RGB(0, 0, 0));
			boolean dark = dark(bgRgb);
			Color gray = new Color(blend(bgRgb, fgRgb, 0.55));
			Color codeColor = dark ? new Color(206, 145, 120) : new Color(163, 21, 21);
			Color codeBg = new Color(blend(bgRgb, fgRgb, 0.07));
			Color linkColor = dark ? new Color(110, 170, 240) : new Color(30, 100, 190);
			code = new Token(new TextAttribute(codeColor, codeBg, SWT.NORMAL));
			link = new Token(new TextAttribute(linkColor, null, TextAttribute.UNDERLINE));
			quote = new Token(new TextAttribute(gray, null, SWT.ITALIC));
			meta = new Token(new TextAttribute(gray, null, SWT.NORMAL));
			marker = new Token(new TextAttribute(heading.getForeground(), null, SWT.BOLD));
			bold = new Token(new TextAttribute(fg, null, SWT.BOLD));
			italic = new Token(new TextAttribute(fg, null, SWT.ITALIC));
			boldItalic = new Token(new TextAttribute(fg, null, SWT.BOLD | SWT.ITALIC));
			strike = new Token(new TextAttribute(gray, null, TextAttribute.STRIKETHROUGH));
		}

		IToken heading(int level) {
			IToken t = headings[level - 1];
			if (t == null) {
				Font font = largeHeadings && level <= HEADING_SCALE.length ? headingFont(HEADING_SCALE[level - 1]) : null;
				t = new Token(new TextAttribute(heading.getForeground(), null, SWT.BOLD, font));
				headings[level - 1] = t;
			}
			return t;
		}
	}

	private RGB background() {
		StyledText t = viewer != null ? viewer.getTextWidget() : null;
		if (t != null && !t.isDisposed()) {
			return t.getBackground().getRGB();
		}
		return com.eco.bio7.util.Util.isDarkTheme() ? new RGB(30, 30, 30) : new RGB(255, 255, 255);
	}

	private static boolean dark(RGB bg) {
		return bg.red + bg.green + bg.blue < 384;
	}

	private static RGB blend(RGB b, RGB f, double t) {
		return new RGB((int) Math.round(b.red + (f.red - b.red) * t), (int) Math.round(b.green + (f.green - b.green) * t),
				(int) Math.round(b.blue + (f.blue - b.blue) * t));
	}

	/* A bold font larger than the editor font (follows the font and the zoom of the editor) */
	private Font headingFont(double scale) {
		StyledText t = viewer != null ? viewer.getTextWidget() : null;
		if (t == null || t.isDisposed()) {
			return null;
		}
		FontData base = t.getFont().getFontData()[0];
		int height = (int) Math.round(base.getHeight() * scale);
		String key = base.getName() + "/" + height;
		Font f = fonts.get(key);
		if (f == null || f.isDisposed()) {
			f = new Font(t.getDisplay(), new FontData(base.getName(), height, SWT.BOLD));
			fonts.put(key, f);
		}
		return f;
	}

	private void disposeFonts() {
		for (Font f : fonts.values()) {
			if (!f.isDisposed()) {
				f.dispose();
			}
		}
		fonts.clear();
	}
}
