package com.eco.bio7.reditor.control;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.AbstractInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.util.Geometry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.PaintObjectEvent;
import org.eclipse.swt.custom.PaintObjectListener;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Drawable;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
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
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.reditor.Bio7REditorPlugin;
import com.eco.bio7.reditor.actions.OpenHelpBrowserAction;
import com.eco.bio7.reditor.actions.OpenWebHelpBrowser;
import com.eco.bio7.reditor.actions.color.OpenColorDialog;
import com.eco.bio7.reditor.actions.color.OpenColorHexDialog;
import com.eco.bio7.reditor.actions.color.OpenColorNameDialog;
import com.eco.bio7.util.Util;

/**
 * Default implementation of {@link org.eclipse.jface.text.IInformationControl}.
 * <p>
 * Displays textual information in a {@link org.eclipse.swt.custom.StyledText}
 * widget. Before displaying, the information set to this information control is
 * processed by an <code>IInformationPresenter</code>.
 *
 * @since 2.0
 */
public class RColorInformationControl extends AbstractInformationControl implements DisposeListener {

	// Image publicMethodIcon =
	// Bio7REditorPlugin.getImageDescriptor("/icons/methpub_obj.png").createImage();

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
		 * @param display
		 *            the display of the information control
		 * @param hoverInfo
		 *            the information to be presented
		 * @param presentation
		 *            the presentation to be updated
		 * @param maxWidth
		 *            the maximal width in pixels
		 * @param maxHeight
		 *            the maximal height in pixels
		 *
		 * @return the manipulated information
		 * @deprecated As of 3.2, replaced by
		 *             {@link RColorInformationControl.IInformationPresenterExtension#updatePresentation(Drawable, String, TextPresentation, int, int)}
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
		 * {@link RColorInformationControl.IInformationPresenter#updatePresentation(Display, String, TextPresentation, int, int)}
		 * Implementations should use the font of the given <code>drawable</code> to
		 * calculate the size of the text to be presented.
		 * </p>
		 *
		 * @param drawable
		 *            the drawable of the information control
		 * @param hoverInfo
		 *            the information to be presented
		 * @param presentation
		 *            the presentation to be updated
		 * @param maxWidth
		 *            the maximal width in pixels
		 * @param maxHeight
		 *            the maximal height in pixels
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

	private int offset;

	private int theLength;

	private String selColorName;

	private Color colTextBackgroundTemp;

	/**
	 * Creates a default information control with the given shell as parent. An
	 * information presenter that can handle simple HTML is used to process the
	 * information to be displayed.
	 *
	 * @param parent
	 *            the parent shell
	 * @param isResizeable
	 *            <code>true</code> if the control should be resizable
	 * @since 3.4
	 */

	/**
	 * Creates a default information control with the given shell as parent. An
	 * information presenter that can handle simple HTML is used to process the
	 * information to be displayed.
	 *
	 * @param parent
	 *            the parent shell
	 * @param statusFieldText
	 *            the text to be used in the status field or <code>null</code> to
	 *            hide the status field
	 * @since 3.4
	 */

	/**
	 * Creates a default information control with the given shell as parent. The
	 * given information presenter is used to process the information to be
	 * displayed.
	 *
	 * @param parent
	 *            the parent shell
	 * @param statusFieldText
	 *            the text to be used in the status field or <code>null</code> to
	 *            hide the status field
	 * @param presenter
	 *            the presenter to be used, or <code>null</code> if no presenter
	 *            should be used
	 * @since 3.4
	 */
	public RColorInformationControl(Shell parent, String statusFieldText, IInformationPresenter presenter) {
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
	 * @param parent
	 *            the parent shell
	 * @param toolBarManager
	 *            the manager or <code>null</code> if toolbar is not desired
	 * @since 3.4
	 */

	/**
	 * Creates a resizable default information control with the given shell as
	 * parent. The given information presenter is used to process the information to
	 * be displayed.
	 *
	 * @param parent
	 *            the parent shell
	 * @param toolBarManager
	 *            the manager or <code>null</code> if toolbar is not desired
	 * @param presenter
	 *            the presenter to be used, or <code>null</code> if no presenter
	 *            should be used
	 * @since 3.4
	 */
	public RColorInformationControl(Shell parent, ToolBarManager toolBarManager, IInformationPresenter presenter) {
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
	 * @param parent
	 *            the parent shell
	 */
	public RColorInformationControl(Shell parent) {
		this(parent, (String) null, null);
	}

	/**
	 * Creates a default information control with the given shell as parent. The
	 * given information presenter is used to process the information to be
	 * displayed.
	 *
	 * @param parent
	 *            the parent shell
	 * @param presenter
	 *            the presenter to be used
	 */
	public RColorInformationControl(Shell parent, IInformationPresenter presenter) {
		this(parent, (String) null, presenter);
	}

	/**
	 * Creates a default information control with the given shell as parent. The
	 * given information presenter is used to process the information to be
	 * displayed. The given styles are applied to the created styled text widget.
	 *
	 * @param parent
	 *            the parent shell
	 * @param shellStyle
	 *            the additional styles for the shell
	 * @param style
	 *            the additional styles for the styled text widget
	 * @param presenter
	 *            the presenter to be used
	 * @deprecated As of 3.4, replaced by simpler constructors
	 */

	/**
	 * Creates a default information control with the given shell as parent. The
	 * given information presenter is used to process the information to be
	 * displayed. The given styles are applied to the created styled text widget.
	 *
	 * @param parentShell
	 *            the parent shell
	 * @param shellStyle
	 *            the additional styles for the shell
	 * @param style
	 *            the additional styles for the styled text widget
	 * @param presenter
	 *            the presenter to be used
	 * @param statusFieldText
	 *            the text to be used in the status field or <code>null</code> to
	 *            hide the status field
	 * @since 3.0
	 * @deprecated As of 3.4, replaced by simpler constructors
	 */

	/**
	 * Creates a default information control with the given shell as parent. The
	 * given information presenter is used to process the information to be
	 * displayed.
	 *
	 * @param parent
	 *            the parent shell
	 * @param textStyles
	 *            the additional styles for the styled text widget
	 * @param presenter
	 *            the presenter to be used
	 * @deprecated As of 3.4, replaced by
	 *             {@link #DefaultInformationControl(Shell, RColorInformationControl.IInformationPresenter)}
	 */
	public RColorInformationControl(Shell parent, int textStyles, IInformationPresenter presenter) {
		this(parent, textStyles, presenter, null);
	}

	/**
	 * Creates a default information control with the given shell as parent. The
	 * given information presenter is used to process the information to be
	 * displayed.
	 *
	 * @param parent
	 *            the parent shell
	 * @param textStyles
	 *            the additional styles for the styled text widget
	 * @param presenter
	 *            the presenter to be used
	 * @param statusFieldText
	 *            the text to be used in the status field or <code>null</code> to
	 *            hide the status field
	 * @since 3.0
	 * @deprecated As of 3.4, replaced by
	 *             {@link #DefaultInformationControl(Shell, String, RColorInformationControl.IInformationPresenter)}
	 */
	public RColorInformationControl(Shell parent, int textStyles, IInformationPresenter presenter,
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
		Font font = JFaceResources.getFontRegistry().get("com.eco.bio7.reditor.reditor.textfont");
		fText = new StyledText(parent, SWT.MULTI | SWT.READ_ONLY | fAdditionalTextStyles);
		fText.setFont(font);
		fText.addListener(SWT.MouseDown, new Listener() {
			@Override
			public void handleEvent(Event event) {

			}

		});
		fText.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent event) {
				fText.setBackground(parent.getBackground());
				//Dispose after background has been set again!!
				colTextBackgroundTemp.dispose();
				//fText.getBackground().dispose();

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

	public Color hex2Rgb(String colorStr) {
		return new Color(fText.getDisplay(), Integer.valueOf(colorStr.substring(1, 3), 16),
				Integer.valueOf(colorStr.substring(3, 5), 16), Integer.valueOf(colorStr.substring(5, 7), 16));
	}

	/*
	 * @see IInformationControl#setInformation(String)
	 */
	public void setInformation(String contentDefault) {
		Font font = JFaceResources.getFontRegistry().get("com.eco.bio7.reditor.reditor.textfont");
		fText.setFont(font);
		String[] contentTemp = contentDefault.split(",");
		String content = contentTemp[0];
		offset = Integer.parseInt(contentTemp[1]);
		theLength = Integer.parseInt(contentTemp[2]);
		Color colRGB = hex2Rgb(content);
		selColorName = null;
		for (int i = 0; i < RColors.hexValues.length; i++) {

			if (RColors.hexValues[i].equals(content)) {
				selColorName = RColors.colorNames[i];
				break;
			}
		}
		if (selColorName == null) {
			fText.setText("Hex : " + content + System.lineSeparator() + "RGB : " + colRGB.getRed() + ","
					+ colRGB.getGreen() + "," + colRGB.getBlue());
		} else {
			fText.setText("Hex : " + content + System.lineSeparator() + "RGB : " + colRGB.getRed() + ","
					+ colRGB.getGreen() + "," + colRGB.getBlue() + System.lineSeparator() + "Name: " + selColorName);
		}
		colRGB.dispose();
		java.awt.Color c = java.awt.Color.decode(content);

		colTextBackgroundTemp = new Color(fText.getDisplay(), c.getRed(), c.getGreen(), c.getBlue());
		fText.setBackground(colTextBackgroundTemp);
        
		/*
		 * Apply a bold style for the first line!
		 */
		/*
		 * StyleRange style1 = new StyleRange(); style1.start = 0; style1.length
		 * =fText.getLine(0).length(); style1.fontStyle = SWT.BOLD;
		 * fText.setStyleRange(style1);
		 */
	}

	/*
	 * static void addImage(Image image, int offset, StyledText styledText, int
	 * length) { StyleRange style = new StyleRange(); style.start = offset;
	 * style.length = 1; style.data = image; Rectangle rect = image.getBounds();
	 * style.metrics = new GlyphMetrics(rect.height, 0, rect.width);
	 * 
	 * Apply a bold style for the first line!
	 * 
	 * 
	 * StyleRange style1 = new StyleRange(); style1.start = 1; style1.length =
	 * length; style1.fontStyle = SWT.BOLD; // fText.setStyleRange(style1);
	 * StyleRange[] str = new StyleRange[2]; str[0] = style; str[1] = style1;
	 * styledText.setStyleRanges(str); }
	 */

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
				OpenColorHexDialog colDialog = new OpenColorHexDialog("Hex Colors",
						PlatformUI.getWorkbench().getActiveWorkbenchWindow(), offset, theLength);
				OpenColorNameDialog colNameDialog = new OpenColorNameDialog("R Colors",
						PlatformUI.getWorkbench().getActiveWorkbenchWindow(), offset, theLength, selColorName);
				tbm.add(colDialog);
				tbm.add(colNameDialog);

				tbm.update(true);

				return new RColorInformationControl(parent, (ToolBarManager) tbm, fPresenter);
			}
		};
	}

}