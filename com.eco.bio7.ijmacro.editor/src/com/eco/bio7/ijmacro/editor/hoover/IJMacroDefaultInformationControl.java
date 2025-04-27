package com.eco.bio7.ijmacro.editor.hoover;

import org.eclipse.jface.action.ToolBarManager;
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
import com.eco.bio7.ijmacro.editor.IJMacroEditorPlugin;
import com.eco.bio7.ijmacro.editor.actions.OpenHelpBrowserAction;
import com.eco.bio7.ijmacro.editor.preferences.template.IJMacroFunctions;

import ij.IJ;
import ij.macro.Interpreter;
import ij.macro.Variable;
import ij.measure.ResultsTable;

/**
 * Default implementation of {@link org.eclipse.jface.text.IInformationControl}.
 * <p>
 * Displays textual information in a {@link org.eclipse.swt.custom.StyledText}
 * widget. Before displaying, the information set to this information control is
 * processed by an <code>IInformationPresenter</code>.
 *
 * @since 2.0
 */
public class IJMacroDefaultInformationControl extends AbstractInformationControl implements DisposeListener {

	Image publicMethodIcon = IJMacroEditorPlugin.getImageDescriptor("/icons/methpub_obj.png").createImage();

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
		 *             {@link IJMacroDefaultInformationControl.IInformationPresenterExtension#updatePresentation(Drawable, String, TextPresentation, int, int)}
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
		 * {@link IJMacroDefaultInformationControl.IInformationPresenter#updatePresentation(Display, String, TextPresentation, int, int)}
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

	private String htmlHelpText;

	protected boolean canBrowse = true;

	private ToolBarManager toolBarManager;

	private String contentFromHoover;

	private StringBuffer content = null;

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
	public IJMacroDefaultInformationControl(Shell parent, String statusFieldText, IInformationPresenter presenter) {
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
	public IJMacroDefaultInformationControl(Shell parent, ToolBarManager toolBarManager,
			IInformationPresenter presenter) {
		super(parent, toolBarManager);
		this.toolBarManager = toolBarManager;
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
	public IJMacroDefaultInformationControl(Shell parent) {
		this(parent, (String) null, null);
	}

	/**
	 * Creates a default information control with the given shell as parent. The
	 * given information presenter is used to process the information to be
	 * displayed.
	 *
	 * @param parent    the parent shell
	 * @param presenter the presenter to be used
	 */
	public IJMacroDefaultInformationControl(Shell parent, IInformationPresenter presenter) {
		this(parent, (String) null, presenter);
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
	 *             {@link #DefaultInformationControl(Shell, IJMacroDefaultInformationControl.IInformationPresenter)}
	 */
	public IJMacroDefaultInformationControl(Shell parent, int textStyles, IInformationPresenter presenter) {
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
	 *             {@link #DefaultInformationControl(Shell, String, IJMacroDefaultInformationControl.IInformationPresenter)}
	 */
	public IJMacroDefaultInformationControl(Shell parent, int textStyles, IInformationPresenter presenter,
			String statusFieldText) {
		super(parent, statusFieldText);
		fAdditionalTextStyles = textStyles;
		fPresenter = presenter;
		create();
	}

	/*
	 * @see org.eclipse.jface.text.AbstractInformationControl#createContent(org.
	 * eclipse .swt.widgets.Composite)
	 */
	protected void createContent(Composite parent) {
		Font font = JFaceResources.getFontRegistry().get("com.eco.bio7.ijmacro.editor.textfont");
		fText = new StyledText(parent, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY | fAdditionalTextStyles);
		fText.setFont(font);
		fText.addListener(SWT.MouseDown, new Listener() {
			@Override
			public void handleEvent(Event event) {

			}

		});
		fText.setForeground(parent.getForeground());
		fText.setBackground(parent.getBackground());
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
	public void setInformation(String contentFromHoover) {
		StringBuffer con = new StringBuffer();

		/* The given String for the browser only! */
		this.contentFromHoover = contentFromHoover;
		/* Here we split the functions array for our hoover popup! */
		String[] functions = IJMacroFunctions.functions.split(System.lineSeparator());

		for (int i = 0; i < functions.length; i++) {
			String finals = functions[i];
			/*Extract functions with and without parentheses!*/
			int subSplitChar = finals.indexOf('#');
			String methodNameToSplitChar = finals.substring(0, subSplitChar);
			int positionToParentheses = methodNameToSplitChar.indexOf('(');
			String finals2;
			if (positionToParentheses > -1) {
				finals2 = finals.substring(0, positionToParentheses);
			} else {
				finals2 = methodNameToSplitChar;
			}

			if (finals2.equals(contentFromHoover)) {
				con.append(contentFromHoover + System.lineSeparator() + System.lineSeparator());
				String[] temp = finals.split("####");
				// String parsedStr = temp[1];//.replaceAll("(.{70})", "$1\n");
				con.append(temp[0]);
				con.append(System.lineSeparator());
				con.append(System.lineSeparator() + temp[1]);
				con.append(System.lineSeparator());
				con.append(System.lineSeparator());

			}

		}
		/* Return if we have no function match we display possible debugger (if in debug mode) variables!*/
		if (con.length() == 0) {
			if (Interpreter.getInstance() != null) {
				if (Interpreter.getInstance().stack != null) {
					Variable[] variables = Interpreter.getInstance().stack;
					if (variables != null) {
						for (int i = 0; i < variables.length; i++) {
							if (variables[i] != null) {								
								if (variables[i].getType() == Variable.ARRAY) {
									int symIndex = variables[i].symTabIndex;
									String arrName = Interpreter.getInstance().pgm.table[symIndex].str;									
									if (arrName.equals(contentFromHoover)) {
										Variable[] elements = variables[i].getArray();										
										con.append("Index          Value" + System.lineSeparator()
												+ System.lineSeparator());
										for (int jj = 0; jj < elements.length; jj++) {

											Variable element = elements[jj];
											if (element.getType() == Variable.STRING) {
												String valueStr = elements[jj].getString();
												valueStr = valueStr.replaceAll("\n", "\\\\n");
												valueStr = "\"" + valueStr + "\""; //show it's a string
												con.append("  " + jj + "               " + valueStr);
												con.append(System.lineSeparator());
											} else if (element.getType() == Variable.VALUE) {
												double v = elements[jj].getValue();
												String valueStr;
												if ((int) v == v) {
													valueStr = IJ.d2s(v, 0);
													con.append("  " + jj + "               " + valueStr);
													con.append(System.lineSeparator());
												} else {
													valueStr = ResultsTable.d2s(v, 4);
													con.append("  " + jj + "               " + valueStr);
													con.append(System.lineSeparator());
												}
											}
										}
									}

								} else if (variables[i].getType() == Variable.STRING) {
									int symIndex = variables[i].symTabIndex;
									String arrName = Interpreter.getInstance().pgm.table[symIndex].str;
									if (arrName.equals(contentFromHoover)) {
										con.append("String" + System.lineSeparator() + System.lineSeparator());
										con.append(variables[i].getString());
										break;
									}

								} else if (variables[i].getType() == Variable.VALUE) {
									int symIndex = variables[i].symTabIndex;
									String arrName = Interpreter.getInstance().pgm.table[symIndex].str;
									//System.out.println(arrName + "  " + contentFromHoover);
									if (arrName.equals(contentFromHoover)) {
										con.append("Numeric" + System.lineSeparator() + System.lineSeparator());
										con.append(String.valueOf(variables[i].getValue()));
										break;
									}

								}
							}

						}
					}
				}
			}

		}
		/*if (con == null) {
			return;
		}*/

		String content = con.toString();
		if (content.isEmpty()) {
			content = contentFromHoover;
		}
		if (fPresenter == null) {
			fText.setText(content.toString());
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
		int length = fText.getLine(0).length();
		fText.replaceTextRange(0, 0, "\uFFFC");
		int offset = 0;

		addImage(publicMethodIcon, offset, fText, length);
		// use a verify listener to dispose the images
		fText.addVerifyListener(event -> {
			if (event.start == event.end)
				return;
			String text = fText.getText(event.start, event.end - 1);
			int index = text.indexOf('\uFFFC');
			while (index != -1) {
				StyleRange style = fText.getStyleRangeAtOffset(event.start + index);
				if (style != null) {
					Image image = (Image) style.data;
					if (image != null)
						image.dispose();
				}
				index = text.indexOf('\uFFFC', index + 1);
			}
		});

		// draw images on paint event
		fText.addPaintObjectListener(event -> {
			StyleRange style = event.style;
			Image image = (Image) style.data;
			if (!image.isDisposed()) {
				int x = event.x;
				int y = event.y + event.ascent - style.metrics.ascent;
				event.gc.drawImage(image, x, y);
			}
		});
		fText.addListener(SWT.Dispose, event -> {
			StyleRange[] styles = fText.getStyleRanges();
			for (int i = 0; i < styles.length; i++) {
				StyleRange style = styles[i];
				if (style.data != null) {
					Image image = (Image) style.data;
					if (image != null)
						image.dispose();
				}
			}
		});

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
				OpenHelpBrowserAction localBrowserHelp = new OpenHelpBrowserAction(
						"https://imagej.nih.gov/ij/developer/macro/functions.html#" + contentFromHoover);
				tbm.add(localBrowserHelp);
				tbm.update(true);
				return new IJMacroDefaultInformationControl(parent, (ToolBarManager) tbm, fPresenter);
			}
		};
	}

}