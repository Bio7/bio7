package com.eco.bio7.ijmacro.editor.hoover;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.AbstractInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.util.Geometry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Drawable;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GlyphMetrics;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import com.eco.bio7.ijmacro.editor.preferences.template.IJMacroCompletionProcessor;
import com.eco.bio7.ijmacro.editor.preferences.template.IJMacroFunctions;

/**
 * Default implementation of {@link org.eclipse.jface.text.IInformationControl}.
 * <p>
 * Displays textual information in a {@link org.eclipse.swt.custom.StyledText}
 * widget. Before displaying, the information set to this information control is
 * processed by an <code>IInformationPresenter</code>.
 *
 * @since 2.0
 */
public class IJMacroSimpleDefaultInformationControl extends AbstractInformationControl implements DisposeListener {

	/**
	 * An information presenter determines the style presentation of information
	 * displayed in the default information control. The interface can be
	 * implemented by clients.
	 */
	public interface IInformationPresenter {

		/**
		 * Updates the given presentation of the given information and thereby may
		 * manipulate the information to be displayed. The manipulation could be the
		 * extraction of textual encoded style information etc. Returns the manipulated
		 * information.
		 * <p>
		 * <strong>Note:</strong> The given display must only be used for measuring.
		 * </p>
		 *
		 * @param display      the display of the information control
		 * @param hoverInfo    the information to be presented
		 * @param presentation the presentation to be updated
		 * @param maxWidth     the maximal width in pixels
		 * @param maxHeight    the maximal height in pixels
		 *
		 * @return the manipulated information
		 * @deprecated As of 3.2, replaced by
		 *             {@link IJMacroSimpleDefaultInformationControl.IInformationPresenterExtension#updatePresentation(Drawable, String, TextPresentation, int, int)}
		 */
		String updatePresentation(Display display, String hoverInfo, TextPresentation presentation, int maxWidth,
				int maxHeight);
	}

	/**
	 * An information presenter determines the style presentation of information
	 * displayed in the default information control. The interface can be
	 * implemented by clients.
	 *
	 * @since 3.2
	 */
	public interface IInformationPresenterExtension {

		/**
		 * Updates the given presentation of the given information and thereby may
		 * manipulate the information to be displayed. The manipulation could be the
		 * extraction of textual encoded style information etc. Returns the manipulated
		 * information.
		 * <p>
		 * Replaces
		 * {@link IJMacroSimpleDefaultInformationControl.IInformationPresenter#updatePresentation(Display, String, TextPresentation, int, int)}
		 * Implementations should use the font of the given <code>drawable</code> to
		 * calculate the size of the text to be presented.
		 * </p>
		 *
		 * @param drawable     the drawable of the information control
		 * @param hoverInfo    the information to be presented
		 * @param presentation the presentation to be updated
		 * @param maxWidth     the maximal width in pixels
		 * @param maxHeight    the maximal height in pixels
		 *
		 * @return the manipulated information
		 */
		String updatePresentation(Drawable drawable, String hoverInfo, TextPresentation presentation, int maxWidth,
				int maxHeight);
	}

	/**
	 * Inner border thickness in pixels.
	 * 
	 * @since 3.1
	 */
	private static final int INNER_BORDER = 1;

	/** The control's text widget */
	private StyledText fText;
	/** The information presenter, or <code>null</code> if none. */
	private final IInformationPresenter fPresenter;
	/** A cached text presentation */
	private final TextPresentation fPresentation = new TextPresentation();

	/**
	 * Additional styles to use for the text control.
	 * 
	 * @since 3.4, previously called <code>fTextStyle</code>
	 */
	private final int fAdditionalTextStyles;

	protected boolean canBrowse = true;

	protected String informationControlText;

	// Image publicMethodIcon =
	// Bio7REditorPlugin.getImageDescriptor("/icons/methpub_obj.png").createImage();

	private IPreferenceStore store;

	/**
	 * Creates a default information control with the given shell as parent. An
	 * information presenter that can handle simple HTML is used to process the
	 * information to be displayed.
	 *
	 * @param parent       the parent shell
	 * @param isResizeable <code>true</code> if the control should be resizable
	 * @since 3.4
	 */

	/**
	 * Creates a default information control with the given shell as parent. An
	 * information presenter that can handle simple HTML is used to process the
	 * information to be displayed.
	 *
	 * @param parent          the parent shell
	 * @param statusFieldText the text to be used in the status field or
	 *                        <code>null</code> to hide the status field
	 * @since 3.4
	 */

	/**
	 * Creates a default information control with the given shell as parent. The
	 * given information presenter is used to process the information to be
	 * displayed.
	 *
	 * @param parent          the parent shell
	 * @param statusFieldText the text to be used in the status field or
	 *                        <code>null</code> to hide the status field
	 * @param presenter       the presenter to be used, or <code>null</code> if no
	 *                        presenter should be used
	 * @since 3.4
	 */
	public IJMacroSimpleDefaultInformationControl(Shell parent, String statusFieldText,
			IInformationPresenter presenter) {
		super(parent, statusFieldText);
		fAdditionalTextStyles = SWT.NONE;
		fPresenter = presenter;
		create();
	}

	/**
	 * Creates a resizable default information control with the given shell as
	 * parent. An information presenter that can handle simple HTML is used to
	 * process the information to be displayed.
	 *
	 * @param parent         the parent shell
	 * @param toolBarManager the manager or <code>null</code> if toolbar is not
	 *                       desired
	 * @since 3.4
	 */

	/**
	 * Creates a resizable default information control with the given shell as
	 * parent. The given information presenter is used to process the information to
	 * be displayed.
	 *
	 * @param parent         the parent shell
	 * @param toolBarManager the manager or <code>null</code> if toolbar is not
	 *                       desired
	 * @param presenter      the presenter to be used, or <code>null</code> if no
	 *                       presenter should be used
	 * @since 3.4
	 */
	public IJMacroSimpleDefaultInformationControl(Shell parent, ToolBarManager toolBarManager,
			IInformationPresenter presenter) {
		super(parent, toolBarManager);
		// store = Bio7REditorPlugin.getDefault().getPreferenceStore();
		fAdditionalTextStyles = SWT.V_SCROLL | SWT.H_SCROLL;
		fPresenter = presenter;
		create();
	}

	/**
	 * Creates a default information control with the given shell as parent. No
	 * information presenter is used to process the information to be displayed.
	 *
	 * @param parent the parent shell
	 */
	public IJMacroSimpleDefaultInformationControl(Shell parent) {
		this(parent, (String) null, null);
		// store = IJMacroPlugin.getDefault().getPreferenceStore();
	}

	/**
	 * Creates a default information control with the given shell as parent. The
	 * given information presenter is used to process the information to be
	 * displayed.
	 *
	 * @param parent    the parent shell
	 * @param presenter the presenter to be used
	 */
	public IJMacroSimpleDefaultInformationControl(Shell parent, IInformationPresenter presenter) {
		this(parent, (String) null, presenter);
		// store = Bio7REditorPlugin.getDefault().getPreferenceStore();
	}

	/**
	 * Creates a default information control with the given shell as parent. The
	 * given information presenter is used to process the information to be
	 * displayed. The given styles are applied to the created styled text widget.
	 *
	 * @param parent     the parent shell
	 * @param shellStyle the additional styles for the shell
	 * @param style      the additional styles for the styled text widget
	 * @param presenter  the presenter to be used
	 * @deprecated As of 3.4, replaced by simpler constructors
	 */

	/**
	 * Creates a default information control with the given shell as parent. The
	 * given information presenter is used to process the information to be
	 * displayed. The given styles are applied to the created styled text widget.
	 *
	 * @param parentShell     the parent shell
	 * @param shellStyle      the additional styles for the shell
	 * @param style           the additional styles for the styled text widget
	 * @param presenter       the presenter to be used
	 * @param statusFieldText the text to be used in the status field or
	 *                        <code>null</code> to hide the status field
	 * @since 3.0
	 * @deprecated As of 3.4, replaced by simpler constructors
	 */

	/**
	 * Creates a default information control with the given shell as parent. The
	 * given information presenter is used to process the information to be
	 * displayed.
	 *
	 * @param parent     the parent shell
	 * @param textStyles the additional styles for the styled text widget
	 * @param presenter  the presenter to be used
	 * @deprecated As of 3.4, replaced by
	 *             {@link #DefaultInformationControl(Shell, IJMacroSimpleDefaultInformationControl.IInformationPresenter)}
	 */
	public IJMacroSimpleDefaultInformationControl(Shell parent, int textStyles, IInformationPresenter presenter) {
		this(parent, textStyles, presenter, null);
	}

	/**
	 * Creates a default information control with the given shell as parent. The
	 * given information presenter is used to process the information to be
	 * displayed.
	 *
	 * @param parent          the parent shell
	 * @param textStyles      the additional styles for the styled text widget
	 * @param presenter       the presenter to be used
	 * @param statusFieldText the text to be used in the status field or
	 *                        <code>null</code> to hide the status field
	 * @since 3.0
	 * @deprecated As of 3.4, replaced by
	 *             {@link #DefaultInformationControl(Shell, String, IJMacroSimpleDefaultInformationControl.IInformationPresenter)}
	 */
	public IJMacroSimpleDefaultInformationControl(Shell parent, int textStyles, IInformationPresenter presenter,
			String statusFieldText) {
		super(parent, statusFieldText);
		fAdditionalTextStyles = textStyles;
		fPresenter = presenter;
		create();

	}

	/*
	 * @see IInformationControl#setInformation(String)
	 */

	protected void createContent(Composite parent) {
		Font font = JFaceResources.getFontRegistry().get("com.eco.bio7.ijmacro.editor.textfont");
		fText = new StyledText(parent, SWT.MULTI | SWT.READ_ONLY | fAdditionalTextStyles);
		fText.setFont(font);
		fText.addListener(SWT.MouseDown, new Listener() {
			@Override
			public void handleEvent(Event event) {

			}

		});
		fText.setForeground(parent.getForeground());
		fText.setBackground(parent.getBackground());
		fText.setWordWrap(true);
		// fText.setFont(JFaceResources.getDialogFont());
		FillLayout layout = (FillLayout) parent.getLayout();
		if (fText.getWordWrap()) {
			// indent does not work for wrapping StyledText, see
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=56342 and
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=115432
			layout.marginHeight = INNER_BORDER;
			layout.marginWidth = INNER_BORDER;
		} else {
			fText.setIndent(INNER_BORDER);
		}
	}

	/*
	 * @see IInformationControl#setInformation(String)
	 */
	public void setInformation(String content) {
		/*From the ImageJMacroCompletion get the hashmap with editor defined functions (key) and the comments (value)!*/
		String commentsEditorFunctions=null;
		if(IJMacroCompletionProcessor.mapFunctionAndContext!=null) {
			commentsEditorFunctions = IJMacroCompletionProcessor.mapFunctionAndContext.get(content);
		}
		/*Remove the API template placeholder chars added in class IJMacroCompletionProcessor!*/
		content = content.replace("${", "");
		content = content.replace("}", "");
		content = content.replace(";", "");
		Font font = JFaceResources.getFontRegistry().get("com.eco.bio7.ijmacro.editor.textfont");
		fText.setFont(font);
		String context = null;

		/*Get all API function context!*/
		String[] functions = IJMacroFunctions.functions.split(System.lineSeparator());
		for (int i = 0; i < functions.length; i++) {
			String[] finalContent = functions[i].split("####");
			String temp = finalContent[0];

			if (content.equals(temp)) {
				context = finalContent[0] + "\n\n" + finalContent[1];
				break;
			}

		}
		/*Here the editor defined comments as context!*/
		if (commentsEditorFunctions != null) {
			context = commentsEditorFunctions;
			/*Remove the comments prefix and suffix!*/

			if (context.startsWith("//")) {
				context = context.replace("//", "");
			}

			context = context.replace("/*", "");

			context = context.replace("*/", "");

		}

		if (context == null) {
			context = content;
		}

		informationControlText = null;
		if (fPresenter == null) {

			if (informationControlText != null) {
				if (informationControlText.isEmpty() == false) {
					fText.setText(informationControlText);

				} else {
					fText.setText(context);
				}
			} else {
				fText.setText(context);
			}

			// fText.setText(content);
		} else {
			fPresentation.clear();

			int maxWidth = -1;
			int maxHeight = -1;
			Point constraints = getSizeConstraints();
			if (constraints != null) {
				maxWidth = constraints.x;
				maxHeight = constraints.y;
				if (fText.getWordWrap()) {
					maxWidth -= INNER_BORDER * 2;
					maxHeight -= INNER_BORDER * 2;
				} else {
					maxWidth -= INNER_BORDER; // indent
				}
				Rectangle trim = computeTrim();
				maxWidth -= trim.width;
				maxHeight -= trim.height;
				maxWidth -= fText.getCaret().getSize().x; // StyledText adds a
															// border at the end
															// of the line for
															// the caret.
			}
			if (isResizable())
				maxHeight = Integer.MAX_VALUE;

			if (fPresenter instanceof IInformationPresenterExtension)
				content = ((IInformationPresenterExtension) fPresenter).updatePresentation(fText, content,
						fPresentation, maxWidth, maxHeight);
			else
				content = fPresenter.updatePresentation(getShell().getDisplay(), content, fPresentation, maxWidth,
						maxHeight);

			if (content != null) {
				fText.setText(content);
				TextPresentation.applyTextPresentation(fPresentation, fText);
			} else {
				fText.setText(""); //$NON-NLS-1$
			}
		}
		/*
		 * int length = fText.getLine(0).length(); fText.replaceTextRange(0, 0,
		 * "\uFFFC"); int offset = 0;
		 */

		/*
		 * addImage(publicMethodIcon, offset, fText, length); // use a verify listener
		 * to dispose the images fText.addVerifyListener(event -> { if (event.start ==
		 * event.end) return; String text = fText.getText(event.start, event.end - 1);
		 * int index = text.indexOf('\uFFFC'); while (index != -1) { StyleRange style =
		 * fText.getStyleRangeAtOffset(event.start + index); if (style != null) { Image
		 * image = (Image) style.data;
		 * 
		 * if (image != null) image.dispose();
		 * 
		 * } index = text.indexOf('\uFFFC', index + 1); } });
		 * 
		 * // draw images on paint event fText.addPaintObjectListener(event -> {
		 * StyleRange style = event.style; Image image = (Image) style.data; if
		 * (!image.isDisposed()) { int x = event.x; int y = event.y + event.ascent -
		 * style.metrics.ascent; event.gc.drawImage(image, x, y); } });
		 * fText.addListener(SWT.Dispose, event -> { StyleRange[] styles =
		 * fText.getStyleRanges(); for (int i = 0; i < styles.length; i++) { StyleRange
		 * style = styles[i]; if (style.data != null) { Image image = (Image)
		 * style.data; if (image != null) image.dispose(); } } });
		 */

		/*
		 * Apply a bold style for the first line!
		 */
		/*
		 * StyleRange style1 = new StyleRange(); style1.start = 0; style1.length
		 * =fText.getLine(0).length(); style1.fontStyle = SWT.BOLD;
		 * fText.setStyleRange(style1);
		 */
	}

	static void addImage(Image image, int offset, StyledText styledText, int length) {
		StyleRange style = new StyleRange();
		style.start = offset;
		style.length = 1;
		style.data = image;
		Rectangle rect = image.getBounds();
		style.metrics = new GlyphMetrics(rect.height, 0, rect.width);
		/*
		 * Apply a bold style for the first line!
		 */

		StyleRange style1 = new StyleRange();
		style1.start = 1;
		style1.length = length;
		style1.fontStyle = SWT.BOLD;
		// fText.setStyleRange(style1);
		StyleRange[] str = new StyleRange[2];
		str[0] = style;
		str[1] = style1;
		styledText.setStyleRanges(str);
	}

	/*
	 * @see IInformationControl#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		if (visible) {
			if (fText.getWordWrap()) {
				Point currentSize = getShell().getSize();
				getShell().pack(true);
				Point newSize = getShell().getSize();
				if (newSize.x > currentSize.x || newSize.y > currentSize.y)
					setSize(currentSize.x, currentSize.y); // restore previous
															// size
			} else {
				/* Changed for Bio7! Calculate the shell size from the longest line! */
				int count = fText.getLineCount();
				int linNr = 0;
				int width = 0;
				for (int i = 0; i < count; i++) {
					String line = fText.getLine(i);

					if (line.length() > width) {
						width = line.length();
						linNr = i;

					}

				}

				int offsetStart = fText.getOffsetAtLine(linNr);
				int offsetEnd = fText.getOffsetAtLine(count - 1);
				Point locEndLine = fText.getLocationAtOffset(offsetStart + width);
				Point locLastLine = fText.getLocationAtOffset(offsetEnd);
				if ((locLastLine.y + 40) <= 400) {
					setSize(locEndLine.x + 40, locLastLine.y + 100);
				} else {
					setSize(locEndLine.x + 40, 400);
				}
			}
		}

		super.setVisible(visible);
	}

	/*
	 * @see IInformationControl#computeSizeHint()
	 */
	public Point computeSizeHint() {
		// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=117602
		int widthHint = SWT.DEFAULT;
		Point constraints = getSizeConstraints();
		if (constraints != null && fText.getWordWrap())
			widthHint = constraints.x;

		return getShell().computeSize(widthHint, SWT.DEFAULT, true);
	}

	/*
	 * @see org.eclipse.jface.text.AbstractInformationControl#computeTrim()
	 */
	public Rectangle computeTrim() {
		return Geometry.add(super.computeTrim(), fText.computeTrim(0, 0, 0, 0));
	}

	/*
	 * @see IInformationControl#setForegroundColor(Color)
	 */
	public void setForegroundColor(Color foreground) {
		super.setForegroundColor(foreground);
		fText.setForeground(foreground);
	}

	/*
	 * @see IInformationControl#setBackgroundColor(Color)
	 */
	public void setBackgroundColor(Color background) {
		super.setBackgroundColor(background);
		fText.setBackground(background);
	}

	/*
	 * @see IInformationControlExtension#hasContents()
	 */
	public boolean hasContents() {
		return fText.getCharCount() > 0;
	}

	/**
	 * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
	 * @since 3.0
	 * @deprecated As of 3.2, no longer used and called
	 */
	public void widgetDisposed(DisposeEvent event) {
	}

	/*
	 * @see org.eclipse.jface.text.IInformationControlExtension5#
	 * getInformationPresenterControlCreator()
	 * 
	 * @since 3.4
	 */
	public IInformationControlCreator getInformationPresenterControlCreator() {
		return new IInformationControlCreator() {
			/*
			 * @see org.eclipse.jface.text.IInformationControlCreator#
			 * createInformationControl(org.eclipse.swt.widgets.Shell)
			 */
			public IInformationControl createInformationControl(Shell parent) {

				ToolBarManager tbm = new ToolBarManager(SWT.FLAT);
				/*
				 * OpenTemplatePreferences openTemplatePreferences = new
				 * OpenTemplatePreferences(); OpenHelpBrowserAction localBrowserHelp = new
				 * OpenHelpBrowserAction(); OpenWebHelpBrowser webHelp = new
				 * OpenWebHelpBrowser(); tbm.add(localBrowserHelp); tbm.add(webHelp);
				 * tbm.add(openTemplatePreferences); tbm.update(true);
				 */
				return new IJMacroSimpleDefaultInformationControl(parent, (ToolBarManager) tbm, fPresenter);
			}
		};
	}

}