package com.eco.bio7.actions;

import java.awt.Cursor;
import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.ui.IWorkbenchWindow;
import org.osgi.framework.Bundle;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.collection.Work;
import com.eco.bio7.discrete.Hexagon;
import com.eco.bio7.discrete.Quad2d;
import com.eco.bio7.time.Time;

public class Start extends Action implements IMenuCreator {

	protected final IWorkbenchWindow window;

	private Point storepointquad;

	private Point storepointhex;

	private Quad2d quad2d;

	private Hexagon hex;

	private File file;

	private Menu fMenu;

	public Start(String text, IWorkbenchWindow window) {
		super(text, AS_DROP_DOWN_MENU);
		this.window = window;

		setId("com.eco.bio7.start");
		setActionDefinitionId("com.eco.bio7.start");
		setMenuCreator(this);
		this.setToolTipText("Start/Stop the repeated time based invocation\n of the Java run method if available!");
		setImageDescriptor(com.eco.bio7.Bio7Plugin.getImageDescriptor("/icons/maintoolbar/play_pause.png"));
		this.quad2d = Quad2d.getQuad2dInstance();
		this.hex = Hexagon.getHexagonInstance();

		fileopener();

	}

	public Menu getMenu(Control parent) {
		if (fMenu != null) {
			fMenu.dispose();
		}
		fMenu = new Menu(parent);

		MenuItem menuItem = new MenuItem(fMenu, SWT.PUSH);
		menuItem.setText("Set Time Interval");

		// Image image =
		// PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);

		//menuItem.setImage(Bio7Plugin.getImageDescriptor("/icons/views/raction.png").createImage());
		menuItem.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {

				Work.openView("com.eco.bio7.control");

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}

		});
		return fMenu;
	}

	public Menu getMenu(Menu parent) {
		// TODO Auto-generated method stub
		return null;
	}

	private void fileopener() {

		Bundle bundle = Platform.getBundle("com.eco.bio7");

		URL locationUrl = FileLocator.find(bundle, new Path("icons/bio7new.png"), null);
		URL url = null;
		try {
			url = FileLocator.toFileURL(locationUrl);
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		file = new File(url.getFile());
	}

	public void run() {
		if (Time.isPause()) {
			this.setChecked(true);
			Time.setPause(false);
			startquad();

			starthex();
		} else {
			this.setChecked(false);
			Time.setPause(true);
			stopconditionsquad();
			stopconditionshex();
		}

	}

	private void startquad() {

		this.quad2d = Quad2d.getQuad2dInstance();
		if (quad2d != null) {
			if (quad2d.activeRendering == false) {
				startconditionsquad();
			} else {
				setcursor_quad();
			}
		}

	}

	private void starthex() {
		this.hex = Hexagon.getHexagonInstance();
		if (hex != null) {

			if (hex.active_rendering == false) {
				startconditionshex();
			} else {
				setcursor_hex();
			}

		}
	}

	private void stopconditionsquad() {
		this.quad2d = Quad2d.getQuad2dInstance();
		if (quad2d != null) {
			quad2d.jScrollPane.getVerticalScrollBar().setEnabled(true);
			quad2d.jScrollPane.getHorizontalScrollBar().setEnabled(true);
			SwingUtilities.invokeLater(new Runnable() {// notwendig
				// !!
				public void run() {
					quad2d.jScrollPane.getViewport().setViewPosition(storepointquad);
				}
			});
			quad2d.setCursor(new Cursor(0));
		}

	}

	private void startconditionsquad() {
		this.quad2d = Quad2d.getQuad2dInstance();
		if (quad2d != null) {
			quad2d.offscreenimage = null;

			storepointquad = quad2d.jScrollPane.getViewport().getViewPosition();
			SwingUtilities.invokeLater(new Runnable() {// notwendig
				// !!
				public void run() {
					quad2d.jScrollPane.getViewport().setViewPosition(new Point(0, 0));
				}
			});
			quad2d.jScrollPane.getVerticalScrollBar().setEnabled(false);
			quad2d.jScrollPane.getHorizontalScrollBar().setEnabled(false);

			setcursor_quad();
		}
	}

	private void setcursor_quad() {
		//Cursor c = quad2d.getToolkit().createCustomCursor(new ImageIcon(file.getPath()).getImage(), new Point(5, 5), "Cursor");
		quad2d.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	}

	private void stopconditionshex() {
		this.hex = Hexagon.getHexagonInstance();
		if (hex != null) {

			hex.jScrollPanehex.getVerticalScrollBar().setEnabled(true);
			hex.jScrollPanehex.getHorizontalScrollBar().setEnabled(true);
			SwingUtilities.invokeLater(new Runnable() {// notwendig

				// !!
				public void run() {
					hex.jScrollPanehex.getViewport().setViewPosition(storepointhex);
				}
			});
			hex.setCursor(new Cursor(0));
		}

	}

	private void startconditionshex() {
		this.hex = Hexagon.getHexagonInstance();
		if (hex != null) {
			hex.offscreenimage = null;

			storepointhex = hex.jScrollPanehex.getViewport().getViewPosition();
			SwingUtilities.invokeLater(new Runnable() {// notwendig
				// !!
				public void run() {
					hex.jScrollPanehex.getViewport().setViewPosition(new Point(0, 0));
				}
			});
			hex.jScrollPanehex.getVerticalScrollBar().setEnabled(false);
			hex.jScrollPanehex.getHorizontalScrollBar().setEnabled(false);

			setcursor_hex();
		}

	}

	private void setcursor_hex() {
		Cursor c = hex.getToolkit().createCustomCursor(new ImageIcon(file.getPath()).getImage(), new Point(5, 5), "Cursor");
		hex.setCursor(c);

	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

}