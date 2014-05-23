package com.eco.bio7.discrete;

import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Stack;

import javax.swing.JApplet;
import javax.swing.JRootPane;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.eco.bio7.jobs.LoadWorkspaceJob;
import com.eco.bio7.swt.SwtAwt;

public class Hexview extends ViewPart {

	public static final String ID = "com.eco.bio7.hexgrid";

	private static Hexview hexview;

	protected String[] fileList;


	public Hexview() {

		super();
		hexview = this;

	}

	public void createPartControl(Composite parent) {
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "com.eco.bio7.quadgrid");
		if (Hexagon.getHexagonInstance() == null) {

			new Hexagon();

		}

		getViewSite().getPage().addPartListener(new IPartListener() {
			public void partActivated(IWorkbenchPart part) {

			}

			public void partBroughtToTop(IWorkbenchPart part) {

			}

			public void partClosed(IWorkbenchPart part) {
				if (part instanceof Hexview) {

					Hexagon.setHexagon_instance();
				}
			}

			public void partDeactivated(IWorkbenchPart part) {

			}

			public void partOpened(IWorkbenchPart part) {
				if (part instanceof Hexview) {

					Hexagon.getHexagonInstance().hexviewopenend = true;
				}
			}
		});
		Composite top = new Composite(parent, SWT.NO_BACKGROUND | SWT.EMBEDDED);
		DropTarget dt = new DropTarget(top, DND.DROP_DEFAULT | DND.DROP_MOVE);
		dt.setTransfer(new Transfer[] { FileTransfer.getInstance() });
		dt.addDropListener(new DropTargetAdapter() {
			private LoadWorkspaceJob ab;

			public void drop(DropTargetEvent event) {

				FileTransfer ft = FileTransfer.getInstance();
				if (ft.isSupportedType(event.currentDataType)) {
					fileList = (String[]) event.data;
					/*if (fileList[0].endsWith("yap")) {
						int size = get_size(fileList[0].toString());
						ab = new LoadWorkspaceJob(fileList[0].toString(), size);
						ab.setUser(true);
						ab.schedule();
					} else {
						MessageBox messageBox = new MessageBox(new Shell(),

						SWT.ICON_WARNING);
						messageBox.setMessage("File is not an *.yap file!");
						messageBox.open();

					}*/

				}

			}
		});
		java.awt.Frame frame = SWT_AWT.new_Frame(top);
		SwtAwt.setSwtAwtFocus(frame, top);
		JApplet panel = new JApplet() {
		      public void update(java.awt.Graphics g) {
		        /* Do not erase the background */
		        paint(g);
		      }
		    };

		frame.add(panel);
		JRootPane root = new JRootPane();
		panel.add(root);
		java.awt.Container contentPane = root.getContentPane();

		contentPane.add(Hexagon.getHexagonInstance().jScrollPanehex);

	}
	

	public void setContentDescription() {

	}

	public void setstatusline(String message) {
		IActionBars bars = getViewSite().getActionBars();
		Image im;

		im = new Image(Display.getCurrent(), getClass().getResourceAsStream("/pics/logo.gif"));

		bars.getStatusLineManager().setMessage(im, message);

	}

	public void setFocus() {

	}

	public static Hexview getHexview() {
		return hexview;
	}

	public void dispose() {

		super.dispose();
	}

}
