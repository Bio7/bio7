package com.eco.bio7.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IWorkbenchWindow;
import com.eco.bio7.discrete.Field;
import com.eco.bio7.discrete.Hexagon;
import com.eco.bio7.discrete.Quad2d;
import com.eco.bio7.image.PointPanel;

public class ResetField extends Action implements IMenuCreator {

	protected final IWorkbenchWindow window;

	private Menu fMenu;

	public ResetField(String text, IWorkbenchWindow window) {
		super(text, AS_DROP_DOWN_MENU);

		this.window = window;

		setId("com.eco.bio7.empty_field");
		setActionDefinitionId("com.eco.bio7.empty_field");
		setImageDescriptor(com.eco.bio7.Bio7Plugin.getImageDescriptor("icons/maintoolbar/reset_field.png"));
		setMenuCreator(this);
	}

	public void run() {

		for (int i = 0; i < Field.getHeight(); i++) {
			for (int u = 0; u < Field.getWidth(); u++) {
				Field.setState(u, i, 0);
				Field.setTempState(u, i, 0);
				/*Field.setTempPlant(u,i,Field.getSoil(u, i));// 
				Field.setPlant(u,i,Field.getSoil(u, i));// set
				*/
				Quad2d quad2dInstance = Quad2d.getQuad2dInstance();
				if (quad2dInstance != null) {
					Quad2d.getQuad2dInstance().repaint();
				}
				Hexagon hexagonInstance = Hexagon.getHexagonInstance();
				if (hexagonInstance != null) {
					hexagonInstance.repaint();
				}

			}
		}

		PointPanel.doPaint();

	}

	public Menu getMenu(Control parent) {
		if (fMenu != null) {
			fMenu.dispose();
		}
		fMenu = new Menu(parent);
		final Control parent2 = parent;

		MenuItem menuItem = new MenuItem(fMenu, SWT.PUSH);
		menuItem.setText("Reset Field");

		menuItem.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				for (int i = 0; i < Field.getHeight(); i++) {
					for (int u = 0; u < Field.getWidth(); u++) {
						Field.setState(u, i, 0);
						Field.setTempState(u, i, 0);
						Quad2d quad2dInstance = Quad2d.getQuad2dInstance();
						if (quad2dInstance != null) {
							Quad2d.getQuad2dInstance().repaint();
						}
						if (Hexagon.getHexagonInstance() != null) {
							Hexagon.getHexagonInstance().repaint();
						}

					}
				}

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});

		return fMenu;
	}

	public void dispose() {

	}

	public Menu getMenu(Menu parent) {

		return null;
	}

}