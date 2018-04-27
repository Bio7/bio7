package com.eco.bio7.rbridge.views;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.List;
import org.eclipse.ui.part.ViewPart;

import com.eco.bio7.image.Util;
import com.eco.bio7.rbridge.RFunctions;
import com.eco.bio7.rbridge.RShellView;
import org.eclipse.swt.events.ExpandAdapter;
import org.eclipse.swt.events.ExpandEvent;

public class RTemplatesView extends ViewPart {

	public static final String ID = "com.eco.bio7.rbridge.views.RTemplatesView"; //$NON-NLS-1$
	private List variableList;
	private List dataList;
	private List mathList;
	private List spatialStatList;
	private List matrixList;
	private List imageList;
	private List statisticsList;
	private ExpandItem spatialStatExpanditem;
	private ExpandBar expandBar;

	public RTemplatesView() {
	}

	/**
	 * Create contents of the view part.
	 * 
	 * @param parent
	 */
	@Override
	public void createPartControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		expandBar = new ExpandBar(container, SWT.NONE);

		container.setLayout(new FillLayout(SWT.HORIZONTAL));
		{

			{
				ExpandItem variablesExpanditem = new ExpandItem(expandBar, SWT.NONE);
				variablesExpanditem.setText("Variables");
				variableList = new List(variablesExpanditem.getParent(), SWT.BORDER | SWT.V_SCROLL);
				variableList.addMouseListener(new MouseAdapter() {
					public void mouseDoubleClick(final MouseEvent e) {
						RShellView rShellView = RShellView.getInstance();
						if (rShellView != null) {
							if (e.button == 3) {

								rShellView.setInDocument(variableList);

								String[] items = variableList.getSelection();

								String t = rShellView.text.getText();
								int pos = rShellView.text.getCaretPosition();
								String res = t.substring(pos);
								String res2 = t.substring(0, pos);
								String fin = res2 + items[0] + res;

								rShellView.text.setText(fin);
							} else {
								String[] items = variableList.getSelection();
								rShellView.text.setText(items[0]);
							}
						}
					}

					public void mouseDown(final MouseEvent e) {

						int index = variableList.getSelectionIndex();
						/* For MacOSX we proof if the result is >0! */
						if (index >= 0) {
							variableList.setToolTipText(RFunctions.getPropsHistInstance().variablesContext[index]);
						}
					}

				});

				variableList.setItems(RFunctions.getPropsHistInstance().variables);
				variablesExpanditem.setControl(variableList);
				variablesExpanditem.setHeight(200);

			}
			{
				ExpandItem dataExpanditem = new ExpandItem(expandBar, SWT.NONE);
				dataExpanditem.setText("Data");
				dataList = new List(dataExpanditem.getParent(), SWT.BORDER | SWT.V_SCROLL);
				dataList.addMouseListener(new MouseAdapter() {
					public void mouseDoubleClick(final MouseEvent e) {
						RShellView rShellView = RShellView.getInstance();
						if (rShellView != null) {
							if (e.button == 3) {
								rShellView.setInDocument(dataList);
							} else {
								String[] items = dataList.getSelection();
								rShellView.text.setText(items[0]);
							}
						}
					}

					public void mouseDown(final MouseEvent e) {

						int index = dataList.getSelectionIndex();
						/* For MacOSX we proof if the result is >0! */
						if (index >= 0) {
							dataList.setToolTipText(RFunctions.getPropsHistInstance().dataContext[index]);
						}
					}
				});
				dataList.setItems(RFunctions.getPropsHistInstance().data);
				dataExpanditem.setControl(dataList);
				dataExpanditem.setHeight(300);
			}
			{
				ExpandItem mathExpanditem = new ExpandItem(expandBar, SWT.NONE);
				mathExpanditem.setText("Math");
				mathList = new List(mathExpanditem.getParent(), SWT.BORDER | SWT.V_SCROLL);
				mathList.addMouseListener(new MouseAdapter() {
					public void mouseDoubleClick(final MouseEvent e) {
						RShellView rShellView = RShellView.getInstance();
						if (rShellView != null) {
							if (e.button == 3) {
								rShellView.setInDocument(mathList);
							} else {
								String[] items = mathList.getSelection();
								rShellView.text.setText(items[0]);
							}
						}
					}

					public void mouseDown(final MouseEvent e) {

						int index = mathList.getSelectionIndex();
						/* For MacOSX we proof if the result is >0! */
						if (index >= 0) {
							mathList.setToolTipText(RFunctions.getPropsHistInstance().mathContext[index]);
						}

					}
				});
				mathList.setItems(RFunctions.getPropsHistInstance().math);
				mathExpanditem.setControl(mathList);
				mathExpanditem.setHeight(300);

			}
			{
				ExpandItem statisticsExpanditem = new ExpandItem(expandBar, SWT.NONE);
				statisticsExpanditem.setText("Statistics");
				statisticsList = new List(statisticsExpanditem.getParent(), SWT.BORDER | SWT.V_SCROLL);
				statisticsList.addMouseListener(new MouseAdapter() {
					public void mouseDoubleClick(final MouseEvent e) {
						RShellView rShellView = RShellView.getInstance();
						if (rShellView != null) {
							if (e.button == 3) {
								rShellView.setInDocument(statisticsList);
							} else {
								String[] items = statisticsList.getSelection();
								rShellView.text.setText(items[0]);
							}
						}
					}

					public void mouseDown(final MouseEvent e) {

						int index = statisticsList.getSelectionIndex();
						/* For MacOSX we proof if the result is >0! */
						if (index >= 0) {
							statisticsList.setToolTipText(RFunctions.getPropsHistInstance().statisticsContext[index]);
						}
					}
				});
				statisticsList.setItems(RFunctions.getPropsHistInstance().statistics);
				statisticsExpanditem.setControl(statisticsList);
				statisticsExpanditem.setHeight(300);
			}
			{
				ExpandItem imageExpanditem = new ExpandItem(expandBar, SWT.NONE);
				imageExpanditem.setText("Image");
				imageList = new List(imageExpanditem.getParent(), SWT.V_SCROLL | SWT.BORDER);
				imageList.addMouseListener(new MouseAdapter() {
					public void mouseDoubleClick(final MouseEvent e) {
						RShellView rShellView = RShellView.getInstance();
						if (rShellView != null) {

							if (e.button == 3) {
								rShellView.setInDocument(imageList);
							} else {
								String[] items = imageList.getSelection();
								rShellView.text.setText(items[0]);
							}
						}

					}

					public void mouseDown(final MouseEvent e) {

						int index = imageList.getSelectionIndex();
						/* For MacOSX we proof if the result is >0! */
						if (index >= 0) {
							imageList.setToolTipText(RFunctions.getPropsHistInstance().imageAnalysisContext[index]);
						}
					}
				});

				imageList.setItems(RFunctions.getPropsHistInstance().imageAnalysis);
				imageExpanditem.setControl(imageList);
				imageExpanditem.setHeight(300);
			}
			{
				ExpandItem matrixExpanditem = new ExpandItem(expandBar, SWT.NONE);
				matrixExpanditem.setText("Matrix");
				matrixList = new List(matrixExpanditem.getParent(), SWT.BORDER | SWT.V_SCROLL);
				matrixList.addMouseListener(new MouseAdapter() {
					public void mouseDoubleClick(final MouseEvent e) {
						RShellView rShellView = RShellView.getInstance();
						if (rShellView != null) {

							if (e.button == 3) {
								rShellView.setInDocument(matrixList);
							} else {
								String[] items = matrixList.getSelection();
								rShellView.text.setText(items[0]);
							}
						}

					}

					public void mouseDown(final MouseEvent e) {

						int index = matrixList.getSelectionIndex();
						/* For MacOSX we proof if the result is >0! */
						if (index >= 0) {
							matrixList.setToolTipText(RFunctions.getPropsHistInstance().matrixContext[index]);
						}
					}
				});
				matrixList.setItems(RFunctions.getPropsHistInstance().matrix);
				matrixExpanditem.setControl(matrixList);
				matrixExpanditem.setHeight(300);
			}
			{
				spatialStatExpanditem = new ExpandItem(expandBar, SWT.NONE);
				spatialStatExpanditem.setText("Spatial Statistics");
				spatialStatList = new List(spatialStatExpanditem.getParent(), SWT.BORDER | SWT.V_SCROLL);
				spatialStatList.addMouseListener(new MouseAdapter() {
					public void mouseDoubleClick(final MouseEvent e) {
						RShellView rShellView = RShellView.getInstance();
						if (rShellView != null) {

							if (e.button == 3) {
								rShellView.setInDocument(spatialStatList);
							} else {
								String[] items = spatialStatList.getSelection();
								rShellView.text.setText(items[0]);
							}
						}
					}

					public void mouseDown(final MouseEvent e) {

						int index = spatialStatList.getSelectionIndex();
						/* For MacOSX we proof if the result is >0! */
						if (index >= 0) {
							spatialStatList.setToolTipText(RFunctions.getPropsHistInstance().spatialStatsContext[index]);
						}

					}
				});
				spatialStatList.setItems(RFunctions.getPropsHistInstance().spatialStats);
				spatialStatExpanditem.setControl(spatialStatList);
				spatialStatExpanditem.setHeight(300);
			}
		}
		/* Add listeners for a dynamic layout! */
		expandBar.addListener(SWT.Resize, event -> Util.getDisplay().asyncExec(() -> {
			/*
			 * The following is done asynchronously to allow the Text's width to be changed
			 * before re-calculating its preferred height.
			 *
			 */
			expandAction(container);

		}));

		expandBar.addExpandListener(new ExpandAdapter() {
			@Override
			public void itemExpanded(ExpandEvent e) {
				Util.getDisplay().asyncExec(() -> {
					expandAction(container);

				});
			}

			public void itemCollapsed(ExpandEvent e) {
				Util.getDisplay().asyncExec(() -> {
					expandAction(container);

				});
			}
		});

		createActions();
		initializeToolBar();
		initializeMenu();
	}

	/**
	 * This method calculates the height until the max values is reached!
	 * 
	 * @param container
	 */
	private void expandAction(Composite container) {
		int size = container.getSize().y;
		ExpandItem[] items = expandBar.getItems();
		for (int i = items.length - 1; i >= 0; i--) {
			int height = size - items[i].getControl().getBounds().y;
			if (height >= 300) {
				items[i].setHeight(300);
			} else {
				items[i].setHeight(height);

			}

		}
	}

	/**
	 * Create the actions.
	 */
	private void createActions() {
		// Create the actions
	}

	/**
	 * Initialize the toolbar.
	 */
	private void initializeToolBar() {
		IToolBarManager toolbarManager = getViewSite().getActionBars().getToolBarManager();
	}

	/**
	 * Initialize the menu.
	 */
	private void initializeMenu() {
		IMenuManager menuManager = getViewSite().getActionBars().getMenuManager();
	}

	@Override
	public void setFocus() {
		// Set the focus
	}
}
