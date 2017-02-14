package com.eco.bio7.image;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JApplet;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.eco.bio7.actions.PlaceholderAction;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.collection.SwingFxSwtView;
import com.eco.bio7.discrete.Quad2d;
import com.eco.bio7.swt.SwtAwt;
import com.eco.bio7.util.Util;

public class PointPanelView extends ViewPart {
	private static PointPanel jp;
	public static final String ID = "com.eco.bio7.points";
	private Composite top = null;
	protected String[] fileList;
	private BufferedImage image;
	private Container contentPane;
	private static JScrollPane scroll;
	private static PointPanelView imj;

	public PointPanelView() {
		imj = this;

	}

	public void createPartControl(Composite parent) {

		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "com.eco.bio7.bio7image");

		top = new Composite(parent, SWT.NONE);
		top.setLayout(new FillLayout());
		DropTarget dt = new DropTarget(top, DND.DROP_DEFAULT | DND.DROP_MOVE);
		dt.setTransfer(new Transfer[] { FileTransfer.getInstance() });
		dt.addDropListener(new DropTargetAdapter() {
			public void drop(DropTargetEvent event) {

				FileTransfer ft = FileTransfer.getInstance();
				if (ft.isSupportedType(event.currentDataType)) {
					fileList = (String[]) event.data;

					SwingUtilities.invokeLater(new Runnable() {
						public void run() {

							loadImage(fileList[0].toString());

						}
					});

				}
			}
		});
		/*try {
			System.setProperty("sun.awt.noerasebackground", "true");
		} catch (NoSuchMethodError error) {
		}

		java.awt.Frame frame = SWT_AWT.new_Frame(top);
		SwtAwt.setSwtAwtFocus(frame, top);
		final sun.awt.EmbeddedFrame ef = (sun.awt.EmbeddedFrame) frame;

		ef.addWindowListener(new WindowAdapter() {
			public void windowActivated(WindowEvent e) {
				ef.synthesizeWindowActivation(true);

			}
		});
		JApplet panel = new JApplet() {
			public void update(java.awt.Graphics g) {
				 Do not erase the background 
				paint(g);
			}
		};

		frame.add(panel);
		JRootPane root = new JRootPane();
		panel.add(root);
		contentPane = root.getContentPane();
		scroll = new JScrollPane();

		jp = new PointPanel();
		jp.getHeight();
		jp.setPreferredSize(new Dimension(jp.getScaledx(), jp.getScaledy()));

		scroll.setViewportView(jp);
		For MacOSX necessary!
		SwingUtilities.invokeLater(new Runnable() {
			// !!
			public void run() {
				contentPane.add(scroll);
			}
		});*/
		scroll = new JScrollPane();
		jp = new PointPanel();
		jp.getHeight();
		jp.setPreferredSize(new Dimension(jp.getScaledx(), jp.getScaledy()));
        jp.setBackground(getSystemColour(parent));
		scroll.setViewportView(jp);
		
		SwingFxSwtView view=new SwingFxSwtView();
		view.embedd(top,scroll);
		
		
		
		initializeToolBar();

	}
	
	public java.awt.Color getSystemColour(Composite parent) {
		Color col = null;
		org.eclipse.swt.graphics.Color colswt = parent.getBackground();
		int r = colswt.getRed();
		int g = colswt.getGreen();
		int b = colswt.getBlue();
		col = new Color(r, g, b);

		return col;
	}

	public void setFocus() {
		// panel.requestFocus();

	}

	public void setstatusline(String message) {
		IActionBars bars = getViewSite().getActionBars();
		bars.getStatusLineManager().setMessage(message);
		

	}

	public static PointPanel getJp() {
		return jp;
	}

	public static PointPanelView getImj() {
		return imj;
	}

	public static JScrollPane getScroll() {
		return scroll;
	}

	private void initializeToolBar() {
		IToolBarManager toolBarManager = getViewSite().getActionBars().getToolBarManager();
		/*PlaceholderAction placeholder=new PlaceholderAction();
		placeholder.setEnabled(false);
		toolBarManager.add(new PlaceholderAction());*/
	}

	public void loadImage(String path) {
		try {
			image = ImageIO.read(new File(path));
		} catch (IOException e1) {
			image = null;
			// e1.printStackTrace();
		}
		if (image != null) {
			PointPanel Jp = PointPanelView.getJp();
			Jp.setPreferredSize(new Dimension((int) (image.getWidth() * Jp.getTransformx()), (int) (image.getHeight() * Jp.getTransformy())));
			PointPanelView.getScroll().setViewportView(Jp);

			Jp.setBuff(image);
			ImageMethods.setFieldSize(image.getWidth(), image.getHeight());
		} else {
			Bio7Dialog.message("Unrecognized file format!");
		}

	}

}
