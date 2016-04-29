package com.eco.bio7.reditor.code;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;

import com.eco.bio7.reditors.REditor;
import com.eco.bio7.util.Util;

/**
 * A RPopupTable is a table of selectable items that appears in its own shell
 * positioned above its parent shell. It is used for selecting items when
 * editing a Table cell (similar to the list that appears when you open a Combo
 * box).
 *
 * The list will be positioned so that it does not run off the screen and the
 * largest number of items are visible. It may appear above the current cursor
 * location or below it depending how close you are to the edge of the screen.
 *
 * @see <a href="http://www.eclipse.org/swt/">Sample code and further
 *      information</a>
 */
public class RHoverQuickFixTable {
	private Shell shell;
	private Table table;
	private int minimumWidth;
	private String result;
	private REditor rEditor;
	private ICompletionProposal[] proposals;

	/**
	 * Creates a PopupList above the specified shell.
	 * 
	 * @param parent
	 *            a Shell control which will be the parent of the new instance
	 *            (cannot be null)
	 */
	public RHoverQuickFixTable(Shell parent, REditor rEditor, ICompletionProposal[] proposals) {
		this(parent, 0, rEditor, proposals);

	}

	/**
	 * Creates a PopupList above the specified shell.
	 * 
	 * @param parent
	 *            a widget which will be the parent of the new instance (cannot
	 *            be null)
	 * @param style
	 *            the style of widget to construct
	 * 
	 * @since 3.0
	 */
	public RHoverQuickFixTable(Shell parent, int style, REditor rEditor, ICompletionProposal[] proposals) {
		this.rEditor = rEditor;
		this.proposals = proposals;
		int listStyle = SWT.SINGLE | SWT.V_SCROLL;
		if ((style & SWT.H_SCROLL) != 0)
			listStyle |= SWT.H_SCROLL;

		shell = new Shell(parent, checkStyle(style));
		
		MouseTrackAdapter listener = new MouseEnterExitListener();
       
		// list = new List(shell, listStyle);
		table = new Table(shell, listStyle);
		table.setSize(200, 300);
		//table.setLinesVisible(true);
		table.addMouseTrackListener(listener);
		// close dialog if user selects outside of the shell
		shell.addListener(SWT.Deactivate, new Listener() {
			public void handleEvent(Event e) {
				shell.setVisible(false);
			}
		});

		// resize shell when list resizes
		shell.addControlListener(new ControlListener() {
			public void controlMoved(ControlEvent e) {
			}

			public void controlResized(ControlEvent e) {
				Rectangle shellSize = shell.getClientArea();
				table.setSize(shellSize.width, shellSize.height);
			}
		});

		// return list selection on Mouse Up or Carriage Return
		table.addMouseListener(new MouseListener() {
			public void mouseDoubleClick(MouseEvent e) {
				int selection = table.getSelectionIndex();

				Display display = Util.getDisplay();
				display.asyncExec(new Runnable() {

					public void run() {
						proposals[selection].apply(rEditor.getViewer().getDocument());
					}
				});

				shell.setVisible(false);
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
					shell.setVisible(false);
				}
			}
		});
		setItems();
	}

	public Shell getShell() {
		return shell;
	}

	private static int checkStyle(int style) {
		int mask = SWT.LEFT_TO_RIGHT | SWT.RIGHT_TO_LEFT | SWT.TOOL | SWT.NO_FOCUS;
		return style & mask;
	}

	/**
	 * Gets the widget font.
	 * <p>
	 * 
	 * @return the widget font
	 *
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
	 *                disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *                thread that created the receiver</li>
	 *                </ul>
	 */
	public Font getFont() {
		return table.getFont();
	}

	/**
	 * Gets the items.
	 * <p>
	 * This operation will fail if the items cannot be queried from the OS.
	 *
	 * @return the items in the widget
	 *
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
	 *                disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *                thread that created the receiver</li>
	 *                </ul>
	 */
	public String[] getItems() {
		TableItem[] tableItems = table.getItems();
		String it[] = new String[tableItems.length];
		for (int i = 0; i < tableItems.length; i++) {
			it[i] = tableItems[i].getText(1);
		}
		return it;
	}

	/**
	 * Gets the minimum width of the list.
	 *
	 * @return the minimum width of the list
	 */
	public int getMinimumWidth() {
		return minimumWidth;
	}

	/**
	 * Launches the Popup List, waits for an item to be selected and then closes
	 * the PopupList.
	 *
	 * @param rect
	 *            the initial size and location of the PopupList; the dialog
	 *            will be positioned so that it does not run off the screen and
	 *            the largest number of items are visible
	 *
	 * @return the text of the selected item or null if no item is selected
	 */
	public String open(Rectangle rect) {

		Point listSize = table.computeSize(rect.width, SWT.DEFAULT, false);
		Rectangle screenSize = shell.getDisplay().getBounds();

		// Position the dialog so that it does not run off the screen and the
		// largest number of items are visible
		int spaceBelow = screenSize.height - (rect.y + rect.height) - 30;
		int spaceAbove = rect.y - 30;

		int y = 0;
		if (spaceAbove > spaceBelow && listSize.y > spaceBelow) {
			// place popup list above table cell
			if (listSize.y > spaceAbove) {
				listSize.y = spaceAbove;
			} else {
				listSize.y += 2;
			}
			y = rect.y - listSize.y;

		} else {
			// place popup list below table cell
			if (listSize.y > spaceBelow) {
				listSize.y = spaceBelow;
			} else {
				listSize.y += 2;
			}
			y = rect.y + rect.height;
		}

		// Make dialog as wide as the cell
		listSize.x = rect.width;
		// dialog width should not be less than minimumWidth
		if (listSize.x < minimumWidth)
			listSize.x = minimumWidth;

		// Align right side of dialog with right side of cell
		int x = rect.x + rect.width - listSize.x;

		shell.setBounds(x, y, listSize.x, listSize.y);
		/*
		 * Instead of using open the text widget will not loose the
		 * focus(SWT_NO_FOCUS)!
		 */
		shell.setVisible(true);
		// table.setFocus();

		Display display = shell.getDisplay();
		while (!shell.isDisposed() && shell.isVisible()) {
			if (!display.readAndDispatch())
				display.sleep();
		}

		if (!shell.isDisposed()) {

			// String [] strings = list.getSelection ();

			shell.dispose();

		}
		return result;
	}

	/**
	 * Selects an item with text that starts with specified String.
	 * <p>
	 * If the item is not currently selected, it is selected. If the item at an
	 * index is selected, it remains selected. If the string is not matched, it
	 * is ignored.
	 *
	 * @param string
	 *            the text of the item
	 *
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
	 *                disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *                thread that created the receiver</li>
	 *                </ul>
	 */
	public void select(String string) {
		// String[] items = list.getItems();

		TableItem[] tableItems = table.getItems();
		String items[] = new String[tableItems.length];
		for (int i = 0; i < tableItems.length; i++) {
			items[i] = tableItems[i].getText(1);
		}

		// find the first entry in the list that starts with the
		// specified string
		if (string != null) {
			for (int i = 0; i < items.length; i++) {
				if (items[i].startsWith(string)) {

					table.setSelection(i);

					/*
					 * int index = list.indexOf(items[i]); list.select(index);
					 */
					break;
				}
			}
		}
	}

	/**
	 * Sets the widget font.
	 * <p>
	 * When new font is null, the font reverts to the default system font for
	 * the widget.
	 *
	 * @param font
	 *            the new font (or null)
	 * 
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
	 *                disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *                thread that created the receiver</li>
	 *                </ul>
	 */
	public void setFont(Font font) {
		table.setFont(font);
	}

	/**
	 * Sets all items.
	 * <p>
	 * The previous selection is cleared. The previous items are deleted. The
	 * new items are added. The top index is set to 0.
	 *
	 * @param strings
	 *            the array of items
	 *
	 *            This operation will fail when an item is null or could not be
	 *            added in the OS.
	 * 
	 * @exception IllegalArgumentException
	 *                <ul>
	 *                <li>ERROR_NULL_ARGUMENT - if the items array is null</li>
	 *                <li>ERROR_INVALID_ARGUMENT - if an item in the items array
	 *                is null</li>
	 *                </ul>
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
	 *                disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *                thread that created the receiver</li>
	 *                </ul>
	 */
	public void setItems() {
		for (int i = 0; i < proposals.length; i++) {
			TableItem item = new TableItem(table, 0);
			item.setText(proposals[i].getDisplayString());
			item.setImage(proposals[i].getImage());

		}

		// list.setItems(strings);
	}

	/**
	 * Sets the minimum width of the list.
	 *
	 * @param width
	 *            the minimum width of the list
	 */
	public void setMinimumWidth(int width) {
		if (width < 0)
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);

		minimumWidth = width;
	}
	class MouseEnterExitListener extends MouseTrackAdapter {
		  public void mouseEnter(MouseEvent e) {
		       
		  }

		  public void mouseExit(MouseEvent arg0) {
			  shell.setVisible(false);
		  }
		}
}
