package com.eco.bio7.markdownedit.hoover;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import org.eclipse.jface.preference.IPreferenceStore;
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
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.texteditor.spelling.SpellingAnnotation;
import com.eco.bio7.markdownedit.completion.InvocationContext;
import com.eco.bio7.markdownedit.completion.MardownEditorQuickFixProcessor;
import com.eco.bio7.markdownedit.completion.MarkdownEditorHoverTextmarkerInformationControl;
import com.eco.bio7.markdownedit.editors.MarkdownEditor;

public class RMarkdownEditorTextHover implements ITextHover, ITextHoverExtension, ITextHoverExtension2 {

	protected String informationControlText = "";
	// protected RHoverQuickFixTable hoverTable;
	private MarkdownEditor rEditor;
	private MardownEditorQuickFixProcessor rAssist;
	private ICompletionProposal[] proposals;
	private static String htmlHelpText = "";
	private String message = "";
	private boolean hoverMarker;

	public static String getHtmlHelpText() {
		return htmlHelpText;
	}

	public static void setHtmlHelpText(String htmlHelpText) {
		RMarkdownEditorTextHover.htmlHelpText = htmlHelpText;
	}

	public RMarkdownEditorTextHover(MarkdownEditor rEditor, MardownEditorQuickFixProcessor rAssist) {
		this.rEditor = rEditor;
		this.rAssist = rAssist;

		// store = Bio7REditorPlugin.getDefault().getPreferenceStore();
	}

	// R Return information to be shown when the cursor is on the given
	// region
	@Override
	public Object getHoverInfo2(ITextViewer textViewer, IRegion hoverRegion) {
		hoverMarker = false;
		informationControlText = "";
		int offset = hoverRegion.getOffset();

		/*
		 * This function will set the hoverMarker variable to true or false and
		 * calculates the hover popup for markers!
		 */
		informationControlText = openPopupHoverTable(textViewer, offset);
		/* Show no popup if this is not the selected Spelling annotation! */
		if (hoverMarker == false) {
			return null;
		}
		return informationControlText;

	}

	/*
	 * This function looks for the annotation (Marker or Spelling) which matches the
	 * offset to call the QuickAssist in the RAssistProcessor class!
	 */
	private String openPopupHoverTable(ITextViewer textViewer, int offset) {

		IAnnotationModel model = ((SourceViewer) textViewer).getAnnotationModel();

		Iterator<Annotation> iter = model.getAnnotationIterator();
		while (iter.hasNext()) {
			Annotation annotation = iter.next();

			if (annotation.isMarkedDeleted())
				continue;

			if (annotation instanceof SpellingAnnotation) {

				Position pos = model.getPosition(annotation);
				int offsetStart = pos.getOffset();
				int length = pos.getLength();
				int offsetEnd = offsetStart + length;
				if (offset >= offsetStart && offset <= offsetEnd) {
					hoverMarker = true;
					proposals = rAssist.computeQuickAssistProposals(
							new InvocationContext(offsetStart, length, (SourceViewer) textViewer));

					message = annotation.getText();

				}

			}

		}
		return message;
	}

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

	/*
	 * @see org.eclipse.jface.text.ITextHoverExtension#getHoverControlCreator()
	 */
	public IInformationControlCreator getHoverControlCreator() {
		return new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {

				return new MarkdownEditorHoverTextmarkerInformationControl(parent, proposals, message, rEditor);

			}
		};
	}

}