package com.eco.bio7.reditors;

import java.awt.Color;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
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
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.ui.texteditor.spelling.SpellingAnnotation;
import org.rosuda.REngine.Rserve.RConnection;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.rbridge.RStrObjectInformation;
import com.eco.bio7.reditor.Bio7REditorPlugin;
import com.eco.bio7.reditor.code.InvocationContext;
import com.eco.bio7.reditor.code.RAssistProcessor;
import com.eco.bio7.reditor.code.RHoverQuickFixTable;
import com.eco.bio7.reditor.control.RColorInformationControl;
import com.eco.bio7.reditor.control.RColors;
import com.eco.bio7.reditor.control.RDefaultHoverTextmarkerInformationControl;
import com.eco.bio7.reditor.control.RDefaultInformationControl;

public class REditorTextHover implements ITextHover, ITextHoverExtension, ITextHoverExtension2 {

	protected String informationControlText = "";
	protected RHoverQuickFixTable hoverTable;
	private boolean hoverMarker;
	private REditor rEditor;
	private RAssistProcessor rAssist;
	private ICompletionProposal[] proposals;
	private static String htmlHelpText = "";
	String message = "";
	private IPreferenceStore store;
	private boolean isHex = false;

	public static String getHtmlHelpText() {
		return htmlHelpText;
	}

	public static void setHtmlHelpText(String htmlHelpText) {
		REditorTextHover.htmlHelpText = htmlHelpText;
	}

	public REditorTextHover(REditor rEditor, RAssistProcessor rAssist) {
		this.rEditor = rEditor;
		this.rAssist = rAssist;
		store = Bio7REditorPlugin.getDefault().getPreferenceStore();
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
		if (store.getBoolean("SHOW_HOVERPOPUP")) {
			/*
			 * This function will set the hoverMarker variable to true or false and
			 * calculates the hover popup for markers!
			 */
			informationControlText = openPopupHoverTable(textViewer, offset);
		} else {
			hoverMarker = false;
		}

		/* Test if a QuickFix is available! */
		// triggerQuickFixFromOffset(offset);
		if (hoverMarker == false) {
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

					if (Character.isLetter(c) == false && (c == '.') == false && Character.isDigit(c) == false && (c == ':') == false && (c == '_') == false && (c == '#') == false) {
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

					if (Character.isLetter(c) == false && (c == '.') == false && Character.isDigit(c) == false && (c == ':') == false && (c == '_') == false && (c == '#') == false) {
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
					// System.out.println(htmlHelpText);
				} catch (BadLocationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				/* If we have detected a color name we produce a color display! */
				for (int i = 0; i < RColors.colorNames.length; i++) {
					if (RColors.colorNames[i].equals(htmlHelpText)) {
						isHex = true;
						/*
						 * Display display = PlatformUI.getWorkbench().getDisplay();
						 * display.syncExec(new Runnable() { public void run() {
						 * textViewer.getTextWidget().setSelectionRange(wordOffset, resultedLength); }
						 * });
						 */
						return RColors.hexValues[i] + "," + wordOffset + "," + resultedLength;
					}
				}
				/* If we have a hex color! */
				if (isHexColor(htmlHelpText)) {
					isHex = true;
					/*
					 * Display display = PlatformUI.getWorkbench().getDisplay();
					 * display.syncExec(new Runnable() { public void run() {
					 * textViewer.getTextWidget().setSelectionRange(wordOffset, resultedLength); }
					 * });
					 */
					return htmlHelpText + "," + wordOffset + "," + resultedLength;
				}

				RConnection c = REditor.getRserveConnection();
				if (c != null) {
					if (RState.isBusy() == false) {
						RState.setBusy(true);
						Display display = PlatformUI.getWorkbench().getDisplay();
						display.syncExec(new Runnable() {
							public void run() {
								RStrObjectInformation rObjectInfo = new RStrObjectInformation();

								informationControlText = rObjectInfo.getRHelpInfo(htmlHelpText, c,RStrObjectInformation.PACKAGE_LABEL);
                             
								if (informationControlText == null || informationControlText.startsWith(RStrObjectInformation.ERROR_CHECK)) {
									/* If we would like to show the str objects! */
									if (store.getBoolean("SHOW_HOVERPOPUP_STR")) {

										informationControlText = rObjectInfo.getRStrObjectInfo(htmlHelpText, c);

									}
								}
							}
						});
						RState.setBusy(false);
					}
				}
			} else {
				informationControlText = "";
			}
		}
		return informationControlText;

	}

	/*
	 * This function looks for the annotation (Marker or Spelling) which matches the
	 * offset to call the QuickAssist in the RAssistProcessor class!
	 */
	private String openPopupHoverTable(ITextViewer textViewer, int offset) {

		/* Delete the previous hoover popup table! */
		/*
		 * Display dis = Util.getDisplay(); dis.asyncExec(new Runnable() {
		 * 
		 * public void run() { if (hoverTable != null) { if
		 * (hoverTable.getShell().isDisposed() == false) {
		 * 
		 * hoverTable.getShell().setVisible(false); } } } });
		 */

		IAnnotationModel model = ((SourceViewer) textViewer).getAnnotationModel();

		Iterator<Annotation> iter = model.getAnnotationIterator();
		while (iter.hasNext()) {
			Annotation annotation = iter.next();
			if (annotation.isMarkedDeleted())
				continue;

			if (annotation instanceof MarkerAnnotation) {
				IMarker marker = ((MarkerAnnotation) annotation).getMarker();
				try {
					if (marker.exists()) {

						/*
						 * There is a warning or error else null is returned, see RAssistProcessor!
						 */
						if (marker.getAttribute(IMarker.TEXT) != null && marker.getAttribute(IMarker.CHAR_START) != null && marker.getAttribute(IMarker.CHAR_END) != null) {
							/* Get String error code or text is 'NA'! */
							// System.out.println(offset+"
							// "+(int)marker.getAttribute(IMarker.CHAR_START));
							int offsetStart = (int) marker.getAttribute(IMarker.CHAR_START);
							int offsetEnd = (int) marker.getAttribute(IMarker.CHAR_END);
							if (offset >= offsetStart && offset <= offsetEnd) {
								/*
								 * Use the RAssistProcessor to get the available proposals!
								 */
								hoverMarker = true;
								proposals = rAssist.computeQuickAssistProposals(new InvocationContext(offsetStart, offsetEnd - offsetStart, rEditor.getViewer()));
								try {
									message = (String) marker.getAttribute(IMarker.MESSAGE);
								} catch (Exception e) {
									// TODO Auto-generated catch block
									//e.printStackTrace();
									System.out.println("Marker not found!");
								}
								/*
								 * if (proposals != null) {
								 * 
								 * 
								 * Here we call the table just with an info! The proposals are set to null!
								 * 
								 * //createHoverTable(offset, proposals, null); } else {
								 * 
								 * if (message != null) {
								 * 
								 * //createHoverTable(offset, null, message); } }
								 */
							}

						}
					}
				} catch (CoreException e) {

					e.printStackTrace();
				}
			}
			/* If this is a spelling problem! */
			else if (annotation instanceof SpellingAnnotation) {
				Position pos = model.getPosition(annotation);
				int offsetStart = pos.getOffset();
				int length = pos.getLength();
				int offsetEnd = offsetStart + length;
				if (offset >= offsetStart && offset <= offsetEnd) {
					hoverMarker = true;
					proposals = rAssist.computeQuickAssistProposals(new InvocationContext(offsetStart, length, rEditor.getViewer()));
					message = annotation.getText();

				}
			}

			// System.out.println(annotation.getText());
		}
		return message;
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
			public IInformationControl createInformationControl(Shell parent) {
				if (hoverMarker) {
					return new RDefaultHoverTextmarkerInformationControl(parent, proposals, message, rEditor);
				} else if (isHex) {
					return new RColorInformationControl(parent);
				} else {
					return new RDefaultInformationControl(parent);
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

	public static Color hex2Rgb(String colorStr) {
		return new Color(Integer.valueOf(colorStr.substring(1, 3), 16), Integer.valueOf(colorStr.substring(3, 5), 16), Integer.valueOf(colorStr.substring(5, 7), 16));
	}

}