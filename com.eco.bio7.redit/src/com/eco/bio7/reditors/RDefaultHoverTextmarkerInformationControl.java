package com.eco.bio7.reditors;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.AbstractInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.util.Geometry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Drawable;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import com.eco.bio7.reditor.actions.OpenHelpBrowserAction;
import com.eco.bio7.reditor.actions.OpenWebHelpBrowser;
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
public class RDefaultHoverTextmarkerInformationControl extends AbstractInformationControl implements DisposeListener {

	/**
	 * An information presenter determines the style presentation of information
	 * displayed in the default information control. The interface can be
	 * implemented by clients.
	 */
	public interface IInformationPresenter {

		/**
		 * Updates the given presentation of the given information and thereby
		 * may manipulate the information to be displayed. The manipulation
		 * could be the extraction of textual encoded style information etc.
		 * Returns the manipulated information.
		 * <p>
		 * <strong>Note:</strong> The given display must only be used for
		 * measuring.
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
		 *             {@link RDefaultHoverTextmarkerInformationControl.IInformationPresenterExtension#updatePresentation(Drawable, String, TextPresentation, int, int)}
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
		 * Updates the given presentation of the given information and thereby
		 * may manipulate the information to be displayed. The manipulation
		 * could be the extraction of textual encoded style information etc.
		 * Returns the manipulated information.
		 * <p>
		 * Replaces
		 * {@link RDefaultHoverTextmarkerInformationControl.IInformationPresenter#updatePresentation(Display, String, TextPresentation, int, int)}
		 * Implementations should use the font of the given
		 * <code>drawable</code> to calculate the size of the text to be
		 * presented.
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

	private Table table;

	private String message;

	private ICompletionProposal[] proposals;

	private REditor rEditor;

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
	 *            the text to be used in the status field or <code>null</code>
	 *            to hide the status field
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
	 *            the text to be used in the status field or <code>null</code>
	 *            to hide the status field
	 * @param presenter
	 *            the presenter to be used, or <code>null</code> if no presenter
	 *            should be used
	 * @since 3.4
	 */
	public RDefaultHoverTextmarkerInformationControl(Shell parent, String statusFieldText, IInformationPresenter presenter) {
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
	 * parent. The given information presenter is used to process the
	 * information to be displayed.
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
	public RDefaultHoverTextmarkerInformationControl(Shell parent, ToolBarManager toolBarManager, IInformationPresenter presenter) {
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
	 * @param proposals 
	 */
	public RDefaultHoverTextmarkerInformationControl(Shell parent, ICompletionProposal[] proposals,String message, REditor rEditor) {
		this(parent, (String) null, null);
		this.proposals=proposals;
		this.rEditor=rEditor;
		this.message=message;
	}
	/**
	 * Creates a default information control with the given shell as parent. No
	 * information presenter is used to process the information to be displayed.
	 *
	 * @param parent
	 *            the parent shell
	 * @param fPresenter2 
	 * @param tbm 
	 * @param proposals 
	 */
	public RDefaultHoverTextmarkerInformationControl(Shell parent, ToolBarManager tbm, IInformationPresenter fPresenter2, ICompletionProposal[] proposals,String message, REditor rEditor) {
		this(parent, tbm, fPresenter2);
		
		this.proposals=proposals;
		this.rEditor=rEditor;
		this.message=message;
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
	public RDefaultHoverTextmarkerInformationControl(Shell parent, IInformationPresenter presenter) {
		this(parent, (String) null, presenter);
	}

	/**
	 * Creates a default information control with the given shell as parent. The
	 * given information presenter is used to process the information to be
	 * displayed. The given styles are applied to the created styled text
	 * widget.
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
	 * displayed. The given styles are applied to the created styled text
	 * widget.
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
	 *            the text to be used in the status field or <code>null</code>
	 *            to hide the status field
	 * @since 3.0
	 * @deprecated As of 3.4, replaced by simpler constructors
	 */

	

	

	/*
	 * @see org.eclipse.jface.text.AbstractInformationControl#createContent(org.
	 * eclipse .swt.widgets.Composite)
	 */
	protected void createContent(Composite parent) {

		table = new Table(parent,SWT.NONE);
		table.setSize(200, 300);
		// table.setLinesVisible(true);
		

		// return list selection on Mouse Up or Carriage Return
		table.addMouseListener(new MouseListener() {
			public void mouseDoubleClick(MouseEvent e) {
				if (proposals != null) {
					int selection = table.getSelectionIndex();

					Display display = Util.getDisplay();
					display.asyncExec(new Runnable() {

						public void run() {
							proposals[selection].apply(rEditor.getViewer().getDocument());
						}
					});

					
				}
				
			}

			public void mouseDown(MouseEvent e) {
			}

			public void mouseUp(MouseEvent e) {

			}
		});
		table.addKeyListener(new KeyListener() {
			public void keyReleased(KeyEvent e) {
			}

			public void keyPressed(KeyEvent e) {
				if (e.character == '\r') {
					//shell.setVisible(false);
				}
			}
		});
		
	}

	

	/*
	 * @see IInformationControl#setInformation(String)
	 */
	public void setInformation(String content) {
		if (proposals != null) {
			for (int i = 0; i < proposals.length; i++) {
				TableItem item = new TableItem(table, 0);
				item.setText(proposals[i].getDisplayString());
				item.setImage(proposals[i].getImage());

			}
		} else {
			TableItem item = new TableItem(table, 0);
			item.setText(content);
			// item.setImage(proposals[i].getImage());
		}
		
		
			
			
		
	}
	

	/*
	 * @see IInformationControl#setVisible(boolean)
	 */
	/*public void setVisible(boolean visible) {
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
	}*/

	/*
	 * @see IInformationControl#computeSizeHint()
	 */
	public Point computeSizeHint() {
		// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=117602
	
		

		return getShell().computeSize(300, SWT.DEFAULT, true);
	}

	/*
	 * @see org.eclipse.jface.text.AbstractInformationControl#computeTrim()
	 */
	/*public Rectangle computeTrim() {
		return Geometry.add(super.computeTrim(), table.computeTrim(0, 0, 0, 0));
	}*/

	/*
	 * @see IInformationControl#setForegroundColor(Color)
	 */
	public void setForegroundColor(Color foreground) {
		super.setForegroundColor(foreground);
		table.setForeground(foreground);
	}

	/*
	 * @see IInformationControl#setBackgroundColor(Color)
	 */
	public void setBackgroundColor(Color background) {
		super.setBackgroundColor(background);
		table.setBackground(background);
	}

	/*
	 * @see IInformationControlExtension#hasContents()
	 */
	public boolean hasContents() {
		return true;
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
				OpenHelpBrowserAction localBrowserHelp = new OpenHelpBrowserAction();
				OpenWebHelpBrowser webHelp= new OpenWebHelpBrowser();
				tbm.add(localBrowserHelp);
				tbm.add(webHelp);
				tbm.update(true);
				return new RDefaultHoverTextmarkerInformationControl(parent, (ToolBarManager) tbm, fPresenter,proposals, message,  rEditor);
			}
		};
	}

}