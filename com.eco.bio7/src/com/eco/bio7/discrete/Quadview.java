package com.eco.bio7.discrete;

import java.awt.Frame;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Stack;

import javax.swing.JApplet;
import javax.swing.JRootPane;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.collection.SwingFxSwtView;
import com.eco.bio7.jobs.LoadData;
import com.eco.bio7.jobs.LoadWorkspaceJob;
import com.eco.bio7.methods.Compiled;
import com.eco.bio7.methods.CurrentStates;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.rcp.ApplicationWorkbenchWindowAdvisor;
import com.eco.bio7.swt.SwtAwt;
import com.eco.bio7.time.Time;

import javafx.embed.swing.SwingNode;
import javafx.embed.swt.FXCanvas;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Quadview extends ViewPart {
	public static final String ID = "com.eco.bio7.quadgrid";

	private IPartListener partListener;

	private JApplet panel;

	private Composite top;

	private java.awt.Frame frame;

	// private JRootPane root;

	protected String[] fileList;

	private static Quadview quadview;

	private Action transferCounts;

	private Action transferPattern;

	private Scene scene;

	// private Parent root;

	private Stage stage1;

	private Stage stage2;

	private Stage stage3;

	private Stage stage4;

	private Stage primaryStage;

	private FXCanvas canvas;

	private SwingNode swingNode;

	private StackPane pane;

	public Quadview() {

		super();
		quadview = this;

	}

	public void createPartControl(Composite parent) {
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "com.eco.bio7.quadgrid");
		createActions();
		initializeToolBar();
		getViewSite().getPage().addPartListener(new IPartListener() {
			public void partActivated(IWorkbenchPart part) {
				if (part instanceof Quadview) {
					setFocus();

				}
			}

			public void partBroughtToTop(IWorkbenchPart part) {

			}

			public void partClosed(IWorkbenchPart part) {
				if (part instanceof Quadview) {
					Quad2d.getQuad2dInstance().quadviewopenend = false;
				}
			}

			public void partDeactivated(IWorkbenchPart part) {

			}

			public void partOpened(IWorkbenchPart part) {
				if (part instanceof Quadview) {
					Quad2d.getQuad2dInstance().quadviewopenend = true;
				}
			}
		});

		this.top = new Composite(parent, SWT.NONE);
		top.setLayout(new FillLayout());
		DropTarget dt = new DropTarget(top, DND.DROP_DEFAULT | DND.DROP_MOVE);
		dt.setTransfer(new Transfer[] { FileTransfer.getInstance() });
		dt.addDropListener(new DropTargetAdapter() {
			private LoadWorkspaceJob ab;

			public void drop(DropTargetEvent event) {

				FileTransfer ft = FileTransfer.getInstance();
				if (ft.isSupportedType(event.currentDataType)) {
					fileList = (String[]) event.data;
					if (fileList[0].endsWith("exml")) {
						/* The same call as in Spreadview! */
						LoadData.load(fileList[0].toString());

					} else {
						MessageBox messageBox = new MessageBox(new Shell(),

								SWT.ICON_WARNING);
						messageBox.setMessage("File is not an *.exml file!");
						messageBox.open();

					}

				}

			}
		});
		/*
		 * try { System.setProperty("sun.awt.noerasebackground", "true"); }
		 * catch (NoSuchMethodError error) { }
		 * 
		 * frame = SWT_AWT.new_Frame(top); SwtAwt.setSwtAwtFocus(frame, top);
		 * 
		 * panel = new JApplet();
		 * 
		 * frame.add(panel); root = new JRootPane(); panel.add(root);
		 * java.awt.Container contentPane = root.getContentPane();
		 * 
		 * Time.setPause(true);
		 * 
		 * contentPane.add(Quad2d.getQuad2dInstance().jScrollPane);
		 */
		
		
		Time.setPause(true);
		SwingFxSwtView view=new SwingFxSwtView();
		view.embedd(top,Quad2d.getQuad2dInstance().jScrollPane);

		
	}

	public void setFocus() {

	}

	public void setstatusline(String message) {
		IActionBars bars = getViewSite().getActionBars();
		Image im;

		im = new Image(Display.getCurrent(), getClass().getResourceAsStream("/pics/logo.gif"));

		bars.getStatusLineManager().setMessage(im, message);

	}

	public void dispose() {
		if (partListener != null) {
			IWorkbenchPage page = getViewSite().getPage();
			page.removePartListener(partListener);
		}

		super.dispose();
	}

	public static Quadview getQuadview() {
		return quadview;
	}

	public Composite getTop() {
		return top;
	}

	public void setTop(Composite top) {
		this.top = top;
	}

	private void initializeToolBar() {
		IToolBarManager toolbarManager = getViewSite().getActionBars().getToolBarManager();

		toolbarManager.add(transferPattern);
		toolbarManager.add(transferCounts);

	}

	private void createActions() {

		transferPattern = new Action("Transfer pattern to R") {
			public void run() {
				if (RServe.isAliveDialog()) {
					if (RState.isBusy() == false) {
						RState.setBusy(true);
						Job job = new Job("Transfer Pattern...") {
							@Override
							protected IStatus run(IProgressMonitor monitor) {
								monitor.beginTask("Transfer ...", IProgressMonitor.UNKNOWN);

								RConnection c = RServe.getConnection();
								int h = Field.getHeight();
								int w = Field.getWidth();
								String name = "quadgrid";

								int[] pInt = new int[w * h];
								int y = 0;
								int x = 0;
								for (int z = 0; z < h * w; z++) {

									if (x > (w - 1)) {
										y++;
										x = 0;
									}
									pInt[z] = Field.getState(x, y);

									if (x < w) {
										x++;
									}
								}

								/* We transfer the values to R! */

								try {
									c.assign(name, pInt);
								} catch (REngineException e) {

									e.printStackTrace();
								}

								try {
									c.eval("try(" + name + "<-matrix(" + name + "," + w + "," + h + "))");

								} catch (RserveException e) {

									e.printStackTrace();
								}
								pInt = null;
								monitor.done();
								return Status.OK_STATUS;
							}

						};
						job.addJobChangeListener(new JobChangeAdapter() {
							public void done(IJobChangeEvent event) {
								if (event.getResult().isOK()) {

									RState.setBusy(false);
								} else {

									RState.setBusy(false);
								}
							}
						});
						// job.setSystem(true);
						job.schedule();
						Bio7Dialog.message("Transferred data to R!");
					} else {
						Bio7Dialog.message("Rserve is busy!");
					}
				}

			}
		};
		transferPattern.setImageDescriptor(com.eco.bio7.Bio7Plugin.getImageDescriptor("/icons/regelmaessig.gif"));
		transferCounts = new Action("Transfer population count to R") {
			public void run() {
				if (RServe.isAliveDialog()) {
					if (RState.isBusy() == false) {
						RState.setBusy(true);
						Job job = new Job("Transfer Population Count...") {
							@Override
							protected IStatus run(IProgressMonitor monitor) {
								monitor.beginTask("Transfer ...", IProgressMonitor.UNKNOWN);

								RConnection c = RServe.getConnection();
								for (int i = 0; i < CurrentStates.getStateList().size(); i++) {

									CounterModel zahl = (CounterModel) Quad2d.getQuad2dInstance().zaehlerlist.get(i);

									int z[] = zahl.getCounterListasArray();

									try {
										c.assign("State_" + CurrentStates.getStateName(i), z);
									} catch (REngineException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}

								}

								monitor.done();
								return Status.OK_STATUS;
							}

						};
						job.addJobChangeListener(new JobChangeAdapter() {
							public void done(IJobChangeEvent event) {
								if (event.getResult().isOK()) {

									RState.setBusy(false);
								} else {

									RState.setBusy(false);
								}
							}
						});
						// job.setSystem(true);
						job.schedule();
						Bio7Dialog.message("Transferred data to R!");
					} else {
						Bio7Dialog.message("Rserve is busy!");
					}
				}

			}
		};
		transferCounts.setImageDescriptor(com.eco.bio7.Bio7Plugin.getImageDescriptor("/icons/diagramm.gif"));

	}

	
}
