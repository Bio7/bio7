package ij.gui;

import ij.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.JPanel;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import com.eco.bio7.image.Util;

/**
 * A modal dialog box with a one line message and "Yes", "No" and "Cancel"
 * buttons.
 */
/*Changed for Bio7 using JFace!*/
public class YesNoCancelDialog {

	private boolean cancelPressed, yesPressed;

	private MessageDialog dialog;

	public YesNoCancelDialog(Frame parent, String title, String msg) {
		this(parent, title, msg, "  Yes  ", "  No  ");
	}

	public YesNoCancelDialog(Frame parent, String title, String msg, String yesLabel, String noLabel) {
		//if (msg.contains("[NON_BLOCKING]")) {
				//	setModal(false);
				//	msg = msg.replace("[NON_BLOCKING]", "");
				//}
		Display dis = Util.getDisplay();
		dis.syncExec(new Runnable() {

			public void run() {

				if (msg.startsWith("Save")) {
					dialog = new org.eclipse.jface.dialogs.MessageDialog(Util.getShell(), title, null, msg, org.eclipse.jface.dialogs.MessageDialog.CONFIRM,
							new String[] { "Save", "Don't Save", "Cancel" }, 0);
				} else {
					dialog = new org.eclipse.jface.dialogs.MessageDialog(Util.getShell(), title, null, msg, org.eclipse.jface.dialogs.MessageDialog.CONFIRM,
							new String[] { yesLabel, noLabel, "Cancel" }, 0);
				}
				int result = dialog.open();

				switch (result) {
				case 0:
					yesPressed = true;
					break;
				case 1:

					break;
				case 2:
					cancelPressed = true;
					break;

				default:
					break;
				}
			}
		});

	}

	/** Returns true if the user dismissed dialog by pressing "Cancel". */
	public boolean cancelPressed() {
		return cancelPressed;
	}

	/** Returns true if the user dismissed dialog by pressing "Yes". */
	public boolean yesPressed() {
		return yesPressed;
	}

	void closeDialog() {
		//dispose();
	}
}
/*public class YesNoCancelDialog extends Dialog implements ActionListener, KeyListener, WindowListener {
	private Button yesB, noB, cancelB;
	private boolean cancelPressed, yesPressed;
	private boolean firstPaint = true;

	public YesNoCancelDialog(Frame parent, String title, String msg) {
		this(parent, title, msg, "  Yes  ", "  No  ");
	}

	public YesNoCancelDialog(Frame parent, String title, String msg, String yesLabel, String noLabel) {
		super(parent, title, true);
		setLayout(new BorderLayout());
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));
		MultiLineLabel message = new MultiLineLabel(msg);
		message.setFont(new Font("Dialog", Font.PLAIN, 14));
		panel.add(message);
		add("North", panel);

		panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.RIGHT, 15, 8));
		if (msg.startsWith("Save")) {
			yesB = new Button("  Save  ");
			noB = new Button("Don't Save");
			cancelB = new Button("  Cancel  ");
		} else {
			yesB = new Button(yesLabel);
			noB = new Button(noLabel);
			cancelB = new Button(" Cancel ");
		}
		yesB.addActionListener(this);
		noB.addActionListener(this);
		cancelB.addActionListener(this);
		yesB.addKeyListener(this);
		noB.addKeyListener(this);
		cancelB.addKeyListener(this);
		if (IJ.isWindows() || Prefs.dialogCancelButtonOnRight) {
			panel.add(yesB);
			panel.add(noB);
			panel.add(cancelB);

		} else {

			panel.add(noB);
			panel.add(cancelB);
			panel.add(yesB);
		}
		if (IJ.isMacintosh())
			setResizable(false);
		add("South", panel);
		addWindowListener(this);
		GUI.scale(this);
		pack();
		yesB.requestFocusInWindow();
		GUI.centerOnImageJScreen(this);
		 * Changed for Bio7! AWT must be colored per component. All JPanels will get the
		 * SWT background from the definition of the CanvasView (UIManager)!
		 
		if (Util.getOS().equals("Linux")||Util.getOS().equals("Mac")) {
			setBackground(Util.getSWTBackgroundToAWT());
			setForeground(Util.getSWTForegroundToAWT());
		}
		show();
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == cancelB)
			cancelPressed = true;
		else if (e.getSource() == yesB)
			yesPressed = true;
		closeDialog();
	}

	*//** Returns true if the user dismissed dialog by pressing "Cancel". */
/*
public boolean cancelPressed() {
return cancelPressed;
}

*//** Returns true if the user dismissed dialog by pressing "Yes". *//*
																		public boolean yesPressed() {
																		return yesPressed;
																		}
																		
																		void closeDialog() {
																		dispose();
																		}
																		
																		public void keyPressed(KeyEvent e) {
																		int keyCode = e.getKeyCode();
																		IJ.setKeyDown(keyCode);
																		if (keyCode == KeyEvent.VK_ENTER) {
																		if (cancelB.isFocusOwner()) {
																		cancelPressed = true;
																		closeDialog();
																		} else if (noB.isFocusOwner()) {
																		closeDialog();
																		} else {
																		yesPressed = true;
																		closeDialog();
																		}
																		} else if (keyCode == KeyEvent.VK_Y || keyCode == KeyEvent.VK_S) {
																		yesPressed = true;
																		closeDialog();
																		} else if (keyCode == KeyEvent.VK_N || keyCode == KeyEvent.VK_D) {
																		closeDialog();
																		} else if (keyCode == KeyEvent.VK_ESCAPE || keyCode == KeyEvent.VK_C) {
																		cancelPressed = true;
																		closeDialog();
																		IJ.resetEscape();
																		}
																		}
																		
																		public void keyReleased(KeyEvent e) {
																		int keyCode = e.getKeyCode();
																		IJ.setKeyUp(keyCode);
																		}
																		
																		public void keyTyped(KeyEvent e) {
																		}
																		
																		public void paint(Graphics g) {
																		super.paint(g);
																		if (firstPaint) {
																		yesB.requestFocus();
																		firstPaint = false;
																		}
																		}
																		
																		public void windowClosing(WindowEvent e) {
																		cancelPressed = true;
																		closeDialog();
																		}
																		
																		public void windowActivated(WindowEvent e) {
																		}
																		
																		public void windowOpened(WindowEvent e) {
																		}
																		
																		public void windowClosed(WindowEvent e) {
																		}
																		
																		public void windowIconified(WindowEvent e) {
																		}
																		
																		public void windowDeiconified(WindowEvent e) {
																		}
																		
																		public void windowDeactivated(WindowEvent e) {
																		}
																		
																		}
																		*/