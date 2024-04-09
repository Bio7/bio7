package com.eco.bio7.actions;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;
import com.eco.bio7.discrete.Field;
import com.eco.bio7.discrete.Hexagon;
import com.eco.bio7.discrete.MidpointGui;
import com.eco.bio7.discrete.Quad2d;
import com.eco.bio7.image.PointPanel;
import com.eco.bio7.methods.CurrentStates;
import com.eco.bio7.plot.LineChartView;

public class Random extends Action implements IMenuCreator {

	protected final IWorkbenchWindow window;

	private Menu fMenu;

	protected MidpointGui fgui = null;

	public Random(String text, IWorkbenchWindow window) {
		super(text, AS_DROP_DOWN_MENU);
		this.window = window;

		setId("com.eco.bio7.random");

		setImageDescriptor(com.eco.bio7.Bio7Plugin.getImageDescriptor("/icons/maintoolbar/random.png"));
		setMenuCreator(this);
	}

	public void run() {

		IWorkbench wb = PlatformUI.getWorkbench();
		IProgressService ps = wb.getProgressService();
		try {
			ps.busyCursorWhile(new IRunnableWithProgress() {
				public void run(IProgressMonitor pm) {

					Field.chance();
					Quad2d quad2dInstance = Quad2d.getQuad2dInstance();
					if (quad2dInstance != null) {
						Quad2d.getQuad2dInstance().repaint();
					}
					Hexagon hexagonInstance = Hexagon.getHexagonInstance();
					if (hexagonInstance != null) {
						hexagonInstance.repaint();
					}
					if (LineChartView.linechart != null) {
						LineChartView.linechart.deleteDataset();
						LineChartView.linechart.createDataset();
						LineChartView.linechart.renderer();
					}

					PointPanel.doPaint();

				}
			});
		} catch (InvocationTargetException e) {

			e.printStackTrace();
		} catch (InterruptedException e) {

			e.printStackTrace();
		}

	}

	public void dispose() {

	}

	public Menu getMenu(Control parent) {
		if (fMenu != null) {
			fMenu.dispose();
		}
		fMenu = new Menu(parent);
		final Control parent2 = parent;

		MenuItem menuItem = new MenuItem(fMenu, SWT.PUSH);
		menuItem.setText("Random");

		menuItem.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				for (int i = 0; i < Field.getHeight(); i++) {

					for (int u = 0; u < Field.getWidth(); u++) {

						Field.setState(u, i, (int) (Math.random() * (CurrentStates.getStateList().size())));
						Field.setTempState(u, i, (int) (Math.random() * (CurrentStates.getStateList().size())));

						/*Field.setPlant(u, i, null);
						Field.setTempPlant(u, i, null);*/
					}
				}
				Quad2d quad2dInstance = Quad2d.getQuad2dInstance();
				if (quad2dInstance != null) {

					quad2dInstance.repaint();
				}

				Hexagon hexagonInstance = Hexagon.getHexagonInstance();

				if (hexagonInstance != null) {

					hexagonInstance.repaint();
				}

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}

		});
		MenuItem menuItemFractal = new MenuItem(fMenu, SWT.PUSH);
		menuItemFractal.setText("Fractal");

		menuItemFractal.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {

				fgui = new MidpointGui();

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

}