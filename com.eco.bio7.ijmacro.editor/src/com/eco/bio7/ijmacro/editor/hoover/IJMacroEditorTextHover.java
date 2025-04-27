package com.eco.bio7.ijmacro.editor.hoover;

import java.awt.Color;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.ITextHoverExtension2;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import com.eco.bio7.ijmacro.editors.IJMacroEditor;

public class IJMacroEditorTextHover implements ITextHover, ITextHoverExtension, ITextHoverExtension2 {

	protected String informationControlText = "";
	private boolean hoverMarker;
	private ICompletionProposal[] proposals;
	private static String htmlHelpText = "";
	String message = "";
	private IPreferenceStore store;
	private IJMacroEditor iJMacroEditor;
	private boolean isHex = false;

	public IJMacroEditorTextHover(IJMacroEditor iJMacroEditor) {
		this.iJMacroEditor = iJMacroEditor;

	}

	// R Return information to be shown when the cursor is on the given
	// region
	@Override
	public Object getHoverInfo2(ITextViewer textViewer, IRegion hoverRegion) {
		hoverMarker = false;
		isHex = false;
		int offset = hoverRegion.getOffset();
		int length = 0;
		int minusLength = 0;
		/*
		 * If enabled in the preferences we show proposals when hovering over a marker
		 * in the editor!
		 */

		informationControlText = "";
		IDocument doc = textViewer.getDocument();

		while (true) {
			char c = 0;
			if (offset + length >= 0 && offset + length < doc.getLength()) {

				try {
					c = doc.getChar(offset + length);
				} catch (BadLocationException e) {

					e.printStackTrace();
				}

				if (Character.isLetter(c) == false && (c == '.') == false && Character.isDigit(c) == false
						&& (c == '_') == false) {
					// If we have a method call we avoid something like 'aov(' as return value!
					length--;
					break;
				}
				if (offset + length >= doc.getLength() - 1) {

					break;
				}
				length++;

			} else {
				break;
			}
		}
		while (true) {
			char c = 0;
			if (offset + length >= 0 && offset + length < doc.getLength()) {

				try {
					c = doc.getChar(offset + minusLength);
				} catch (BadLocationException e) {

					e.printStackTrace();
				}

				if (Character.isLetter(c) == false && (c == '.') == false && Character.isDigit(c) == false
						&& (c == '_') == false && (c == '#') == false) {
					break;
				}

				minusLength--;
				if (offset + minusLength < 0) {
					break;
				}
			} else {
				break;
			}
		}
		final int wordOffset = offset + minusLength + 1;
		final int resultedLength = length - minusLength;

		if (resultedLength > 0) {

			try {
				htmlHelpText = textViewer.getDocument().get(wordOffset, resultedLength);

			} catch (BadLocationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			/* If we have a hex color! */
			if (isHexColor(htmlHelpText)) {
				isHex = true;
				return htmlHelpText + "," + wordOffset + "," + resultedLength;
			}

		}

		return htmlHelpText;

	}

	/*
	 * private void createHoverTable(int offset, ICompletionProposal[] proposals,
	 * String message) { Display display = Util.getDisplay(); display.asyncExec(new
	 * Runnable() {
	 * 
	 * public void run() { if (hoverTable != null) {
	 * hoverTable.getShell().dispose(); } StyledText sh =
	 * rEditor.getViewer().getTextWidget(); Font f = sh.getFont();
	 * 
	 * int height = f.getFontData()[0].getHeight(); Point poi =
	 * sh.getLocationAtOffset(offset); poi = sh.toDisplay(poi); int locx = poi.x;
	 * int locy = poi.y; hoverTable = new RHoverQuickFixTable(Util.getShell(),
	 * rEditor, message, proposals);
	 * rEditor.setRHooverPopupShell(hoverTable.getShell()); Rectangle rect = new
	 * Rectangle(locx, locy - height - 150, 300, 200); hoverTable.open(rect); } });
	 * }
	 */

	String readFile(String path, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}

	// just an old version of the API method that returns only strings
	@Override
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		return getHoverInfo2(textViewer, hoverRegion).toString();
	}

	// returns the region object for a given position in the text editor
	@Override
	public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
		Point selection = textViewer.getSelectedRange();
		if (selection.x <= offset && offset < selection.x + selection.y) {
			return new Region(selection.x, selection.y);
		}
		// if no text is selected then we return a region of the size 0 (a
		// single character)
		return new Region(offset, 0);
	}

	/* Displays the hover popup with a toolbar! */
	public IInformationControlCreator getHoverControlCreator() {
		return new IInformationControlCreator() {
			/*
			 * @see org.eclipse.jface.text.IInformationControlCreator#
			 * createInformationControl(org.eclipse.swt.widgets.Shell)
			 */
			public IInformationControl createInformationControl(Shell parent) {

				if (isHex) {
					return new IJMacroColorInformationControl(parent);
				}

				else {

					return new IJMacroDefaultInformationControl(parent);
				}
			}
		};
	}

	public boolean isHexColor(final String hexColorCode) {
		String HEX_PATTERN = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$";
		Pattern pattern = Pattern.compile(HEX_PATTERN);
		Matcher matcher = pattern.matcher(hexColorCode);
		return matcher.matches();

	}

}