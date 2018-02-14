package com.eco.bio7.database;

import java.lang.reflect.InvocationTargetException;
import javax.swing.SwingUtilities;
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridColumn;
import org.eclipse.nebula.widgets.grid.GridEditor;
import org.eclipse.nebula.widgets.grid.GridItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.discrete.Hexagon;
import com.eco.bio7.discrete.Quad2d;
import com.eco.bio7.image.ImageMethods;
import com.eco.bio7.image.PointPanel;
import com.eco.bio7.init.ButtonContainer;
import com.eco.bio7.methods.CurrentStates;
import com.eco.bio7.plot.LineChart;
import com.eco.bio7.plot.LineChartView;
import com.eco.bio7.plot.Pie;

public class StateTable {

	public static Grid grid;

	public Grid getGrid() {
		return grid;
	}

	public void setGrid(Grid grid) {
		this.grid = grid;
	}

	private static StateTable stateTableInstance;

	protected boolean checkedEvent = false;

	private GridColumn column;

	private GridColumn column1;

	private GridColumn column2;

	private GridColumn column3;

	protected int index;

	public StateTable(final Composite parent) {
		stateTableInstance = this;

		createTable(parent);
		setCell("State");

	}

	public static StateTable getDatatableInstance() {
		return stateTableInstance;
	}

	private void createTable(final Composite parent) {
		grid = new Grid(parent, SWT.BORDER | SWT.V_SCROLL | SWT.SCROLL_LINE | SWT.MULTI | SWT.FULL_SELECTION | SWT.VIRTUAL);
		grid.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				checkedEvent = event.detail == SWT.CHECK ? true : false;

				// GridItem item = (GridItem) event.item;

			}
		});
		// grid.setRowsResizeable(true);
		grid.setHeaderVisible(true);
		// grid.setRowHeaderVisible(true);

		// grid.setCellSelectionEnabled(true);

		grid.addListener(SWT.MouseDown, new Listener() {
			public void handleEvent(Event event) {

				Rectangle clientArea = grid.getClientArea();
				Point pt = new Point(event.x, event.y);
				index = grid.getTopIndex();
				while (index < grid.getItemCount()) {
					boolean visible = false;
					GridItem item = grid.getItem(index);

					for (int i = 2; i < 4; i++) {
						Rectangle rect = item.getBounds(i);

						if (rect.contains(pt)) {

							/*
							 * Proof if a checked event occured, else selection event causes an error!
							 */

							if (checkedEvent) {

								if (i == 2) { // Check the items correct
												// checkbox!
									if (grid.getItem(index).getChecked(i) == true) {

										for (int j = 0; j < grid.getItemCount(); j++) {
											if (j != index) {
												grid.getItem(j).setChecked(2, false);
												Quad2d.getQuad2dInstance().setValue(index);
												Hexagon hex = Hexagon.getHexagonInstance();
												if (hex != null) {
													hex.value = index;
												}
												PointPanel.setPlantIndexPanel(index);

											}
										}

									}

									else if (grid.getItem(index).getChecked(i) == false) {

									}

								}

							}

						}
						if (!visible && rect.intersects(clientArea)) {
							visible = true;
						}
					}
					if (!visible)
						return;
					index++;
				}
			}
		});

		/*
		 * grid.addListener(SWT.MouseDoubleClick, new Listener() { public void
		 * handleEvent(Event event) {
		 * 
		 * Rectangle clientArea = grid.getClientArea(); Point pt = new Point(event.x,
		 * event.y); index = grid.getTopIndex(); while (index < grid.getItemCount()) {
		 * 
		 * GridItem item = grid.getItem(index);
		 * 
		 * if (grid.getItem(index).getBounds(2).contains(pt)) { ColorDialog dialog = new
		 * ColorDialog(parent.getShell(), SWT.APPLICATION_MODAL);
		 * 
		 * RGB color = dialog.open(); grid.getItem(index).setBackground(2, new
		 * Color(Display.getCurrent(), color));
		 * 
		 * }
		 * 
		 * index++; } } });
		 */
		final GridEditor editor = new GridEditor(grid);
		editor.horizontalAlignment = SWT.LEFT;
		editor.grabHorizontal = true;
		grid.addListener(SWT.MouseDown, new Listener() {
			public void handleEvent(Event event) {
				Rectangle clientArea = grid.getClientArea();
				Point pt = new Point(event.x, event.y);
				int index = grid.getTopIndex();
				while (index < grid.getItemCount()) {
					boolean visible = false;
					final GridItem item = grid.getItem(index);
					for (int i = 0; i < grid.getColumnCount(); i++) {
						Rectangle rect = item.getBounds(i);
						if (rect.contains(pt)) {
							final int column = i;
							final int itemNr = index;
							if (i == 1) {
								final Text text = new Text(grid, SWT.NONE);

								Listener textListener = new Listener() {
									public void handleEvent(final Event e) {
										switch (e.type) {
										case SWT.FocusOut:
											item.setText(column, text.getText());
											// System.out.println(CurrentStates.getStateDescriptions().size());
											CurrentStates.getStateDescriptions().set(itemNr, text.getText());
											text.dispose();
											break;
										case SWT.Traverse:
											switch (e.detail) {
											case SWT.TRAVERSE_RETURN:
												item.setText(column, text.getText());
												// System.out.println(itemNr);
												CurrentStates.getStateDescriptions().set(itemNr, text.getText());
												// FALL THROUGH
											case SWT.TRAVERSE_ESCAPE:
												text.dispose();
												e.doit = false;
											}
											break;
										}
									}
								};
								text.addListener(SWT.FocusOut, textListener);
								text.addListener(SWT.Traverse, textListener);
								editor.setEditor(text, item, i);
								text.setText(item.getText(i));
								text.selectAll();
								text.setFocus();
								return;
							}
						}
						if (!visible && rect.intersects(clientArea)) {
							visible = true;
						}
					}
					if (!visible)
						return;
					index++;
				}
			}
		});
		column = new GridColumn(grid, SWT.CENTER);
		column.setText("Name");
		column.setWidth(100);
		column.setResizeable(true);
		column1 = new GridColumn(grid, SWT.CENTER);
		column1.setText("Description");
		column1.setWidth(100);
		column1.setResizeable(true);
		column2 = new GridColumn(grid, SWT.CHECK | SWT.CENTER);
		column2.setText("Select");
		column2.setWidth(100);
		column2.setResizeable(true);
		column3 = new GridColumn(grid, SWT.CHECK | SWT.CENTER);
		column3.setText("Delete");
		column3.setWidth(100);
		column3.setResizeable(true);
		column3.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {

				for (int j = 0; j < grid.getItemCount(); j++) {
					grid.getItem(j).setChecked(3, true);
				}

			}

		});

		/* Resize column width if shell changes! */
		parent.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				if (grid.isDisposed() == false) {
					Rectangle area = parent.getClientArea();
					Point preferredSize = grid.computeSize(SWT.DEFAULT, SWT.DEFAULT);
					int width = area.width - 2 * grid.getBorderWidth();
					if (preferredSize.y > area.height + grid.getHeaderHeight()) {
						// Subtract the scrollbar width from the total column width
						// if a vertical scrollbar will be required
						Point vBarSize = grid.getVerticalBar().getSize();
						width -= vBarSize.x;
					}
					Point oldSize = grid.getSize();
					if (oldSize.x > area.width) {
						// table is getting smaller so make the columns
						// smaller first and then resize the table to
						// match the client area width
						column1.setWidth(width / 4);
						column2.setWidth(width / 4);
						column3.setWidth(width / 4);
						column.setWidth(width / 4);
						grid.setSize(area.width, area.height);
					} else {
						// table is getting bigger so make the table
						// bigger first and then make the columns wider
						// to match the client area width
						grid.setSize(area.width, area.height);
						column1.setWidth(width / 4);
						column2.setWidth(width / 4);
						column3.setWidth(width / 4);
						column.setWidth(width / 4);
					}
				}
			}
		});

	}

	public void addObjects(String ob) {
		CurrentStates.addState(ob);// The Soil as Value 0!
		// State.data(ob); // The Object creation!
		if (LineChartView.linechart != null) {
			updateCharts();
		}
	}

	public void deleteTable() {

		grid.removeAll();

	}

	public static void setInGrid(final String result) {
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {

				GridItem gi = new GridItem(grid, SWT.NONE);// Create new item(s)

				gi.setText(0, result);

				gi.setText(1, result);

				gi.setChecked(2, false);

				gi.setChecked(3, false);

			}
		});

	}

	public static void setCell(final String name) {

		String species = name;
		if (CurrentStates.stateExistent(species) == false) {
			setInGrid(name);
			/* Add the state! */
			CurrentStates.addState(species);
			CurrentStates.getStateDescriptions().add("");

			/* Create the button for the selection! */
			CurrentStates.getIndexStateName(name);

			ButtonContainer.getinstance().addButton(species);
			/* Create the counter for the new state! */
			Quad2d.getQuad2dInstance().createzaehler();
			/* Important to wrap, if a lot of states will be set! */
			if (LineChartView.linechart != null) {
				try {
					SwingUtilities.invokeAndWait(new Runnable() {
						public void run() {
							updateCharts();
						}
					});
				} catch (InterruptedException e) {

					e.printStackTrace();
				} catch (InvocationTargetException e) {

					e.printStackTrace();
				}
			}

		} else {
			Bio7Dialog.message("A state with this name already exists!");
		}
	}

	public static void unsetCell(String name) {

		String species = name;

		if (CurrentStates.getIndexStateName(species) > -1) {

			ButtonContainer.getinstance().removeButton(species);

			/* Remove the state at first! */

			CurrentStates.removeState(species);
			/* Update the counter! */
			Quad2d.getQuad2dInstance().createzaehler();
			try {
				SwingUtilities.invokeAndWait(new Runnable() {
					public void run() {
						updateCharts();
					}

				});
			} catch (InterruptedException e) {

				e.printStackTrace();
			} catch (InvocationTargetException e) {

				e.printStackTrace();
			}
			/* The datatable needs to be repainted! */
			repaintFields();
		}
		/* When we have removed all states one state is created as dummy data! */
		/*
		 * if (Plantspecies.getSpeciesList().size() == 0) { setcell("0"); }
		 */
		/* Delete the points in the Points panel!!! */
		ImageMethods.deletePoints();
		/* Reset - Species have to be selected again for the Points panel! */
		resetActivePlant();

	}

	private static void resetActivePlant() {
		Quad2d.getQuad2dInstance().setValue(0);
		Hexagon hex = Hexagon.getHexagonInstance();
		if (hex != null) {
			hex.value = 0;
		}
		PointPanel.setPlantIndexPanel(0);
	}

	private static void repaintFields() {
		Quad2d.getQuad2dInstance().repaint();
		Hexagon hex = Hexagon.getHexagonInstance();
		if (hex != null) {
			hex.repaint();
		}
	}

	private static void updateCharts() {
		LineChart.update();
		Pie.update();

	}

}
