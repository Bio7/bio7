/*******************************************************************************
 * Copyright (c) 2004, 2005 Elias Volanakis and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Elias Volanakis - initial API and implementation
 * M.Austenfeld - Some changes for the Bio7 application
  *******************************************************************************/
package com.eco.bio7.floweditor.shapes;

import com.eco.bio7.floweditor.actions.DebugMode;
import com.eco.bio7.floweditor.actions.ManhattenRouter;
import com.eco.bio7.floweditor.actions.SaveImage;
import com.eco.bio7.floweditor.actions.SetGridSize;
import com.eco.bio7.floweditor.actions.ShortestPath;
import com.eco.bio7.floweditor.model.FlowFileDropTargetListener;
import com.eco.bio7.floweditor.model.Shape;
import com.eco.bio7.floweditor.model.ShapesDiagram;
import com.eco.bio7.floweditor.parts.ShapesEditPartFactory;
import com.eco.bio7.floweditor.parts.ShapesTreeEditPartFactory;
import com.eco.bio7.floweditor.ruler.Bio7RulerProvider;
import com.eco.bio7.floweditor.ruler.DiagramRuler;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.util.TransferDropTargetListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.parts.ScrollableThumbnail;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.KeyHandler;
import org.eclipse.gef.KeyStroke;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.MouseWheelHandler;
import org.eclipse.gef.MouseWheelZoomHandler;
import org.eclipse.gef.SnapToGeometry;
import org.eclipse.gef.SnapToGrid;
import org.eclipse.gef.dnd.TemplateTransferDragSourceListener;
import org.eclipse.gef.dnd.TemplateTransferDropTargetListener;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.gef.requests.SimpleFactory;
import org.eclipse.gef.rulers.RulerProvider;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.AlignmentAction;
import org.eclipse.gef.ui.actions.CopyTemplateAction;
import org.eclipse.gef.ui.actions.DirectEditAction;
import org.eclipse.gef.ui.actions.MatchHeightAction;
import org.eclipse.gef.ui.actions.MatchWidthAction;
import org.eclipse.gef.ui.actions.ToggleGridAction;
import org.eclipse.gef.ui.actions.ToggleRulerVisibilityAction;
import org.eclipse.gef.ui.actions.ToggleSnapToGeometryAction;
import org.eclipse.gef.ui.actions.ZoomInAction;
import org.eclipse.gef.ui.actions.ZoomOutAction;
import org.eclipse.gef.ui.palette.PaletteViewer;
import org.eclipse.gef.ui.palette.PaletteViewerProvider;
import org.eclipse.gef.ui.parts.ContentOutlinePage;
import org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.gef.ui.parts.TreeViewer;
import org.eclipse.gef.ui.rulers.RulerComposite;

/**
 * A graphical editor with flyout palette that can edit .shapes files. The
 * binding between the .shapes file extension and this editor is done in
 * plugin.xml
 * 
 * @author Elias Volanakis
 * 
 *         M.Austenfeld - Changes for the Bio7 application.
 */
public class ShapesEditor extends GraphicalEditorWithFlyoutPalette {

	/** This is the root of the editor's model. */
	public ShapesDiagram diagram;

	/** Palette component, holding the tools and shapes. */
	private static PaletteRoot PALETTE_MODEL;

	public boolean debug = false;

	private ScrollableThumbnail thumbnail;

	private DisposeListener disposeListener;

	private SashForm sash;

	private RulerComposite rulerComp;

	private IPartListener partListener = new PartListener();

	ScalableRootEditPart rootEditPart;

	/** Create a new ShapesEditor instance. This is called by the Workspace. */
	public ShapesEditor() {

		setEditDomain(new DefaultEditDomain(this));

	}

	protected void closeEditor(boolean save) {
		getSite().getPage().closeEditor(this, save);
	}

	private static ShapesDiagram get_active_diagram() {
		IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		ShapesDiagram diagram = null;
		if (editor != null) {
			if (editor instanceof ShapesEditor) {
				diagram = ((ShapesEditor) editor).getModel();
			}
		}
		return diagram;
	}

	public void eraseColours() {
		ShapesDiagram diagram = get_active_diagram();
		if (diagram != null) {
			List dia = diagram.getChildren();
			if (dia.size() > 0) {
				for (int i = 0; i < dia.size(); i++) {// Search a start!
					Shape a = (Shape) dia.get(i);
					a.setDefaultColor();
				}
			}
		}
	}

	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		eraseColours();
		if (part.getSite().getWorkbenchWindow().getActivePage() == null)
			return;
		super.selectionChanged(part, selection);
	}

	/**
	 * Configure the graphical viewer before it receives contents.
	 * <p>
	 * This is the place to choose an appropriate RootEditPart and
	 * EditPartFactory for your editor. The RootEditPart determines the behavior
	 * of the editor's "work-area". For example, GEF includes zoomable and
	 * scrollable root edit parts. The EditPartFactory maps model elements to
	 * edit parts (controllers).
	 * </p>
	 * 
	 * @see org.eclipse.gef.ui.parts.GraphicalEditor#configureGraphicalViewer()
	 */
	// The composite for the ruler
	protected void createGraphicalViewer(Composite parent) {

		rulerComp = new RulerComposite(parent, SWT.NONE);
		super.createGraphicalViewer(rulerComp);
		rulerComp.setGraphicalViewer((ScrollingGraphicalViewer) getGraphicalViewer());
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "com.eco.bio7.flow");
	}

	protected void configureGraphicalViewer() {
		super.configureGraphicalViewer();

		ScrollingGraphicalViewer viewer = (ScrollingGraphicalViewer) getGraphicalViewer();
		viewer.setEditPartFactory(new ShapesEditPartFactory());
		viewer.setRootEditPart(new ScalableFreeformRootEditPart());

		getGraphicalViewer().setKeyHandler(new GraphicalViewerKeyHandler(getGraphicalViewer()).setParent(getCommonKeyHandler()));

		viewer.addDropTargetListener(new FlowFileDropTargetListener(viewer));
		// configure the context menu provider
		ContextMenuProvider cmProvider = new ShapesEditorContextMenuProvider(viewer, getActionRegistry());
		viewer.setContextMenu(cmProvider);
		getSite().registerContextMenu(cmProvider, viewer);

		rootEditPart = new ScalableRootEditPart();
		viewer.setRootEditPart(rootEditPart);
		viewer.setProperty(SnapToGeometry.PROPERTY_SNAP_ENABLED, true);
		viewer.setProperty(MouseWheelHandler.KeyGenerator.getKey(SWT.MOD1), MouseWheelZoomHandler.SINGLETON);
		ZoomManager manager = rootEditPart.getZoomManager();

		double[] zoomLevels = new double[] { 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 0.75, 1.0, 1.5, 2.0, 2.5, 3.0, 4.0, 5.0, 10.0, 20.0 };
		manager.setZoomLevels(zoomLevels);

		ArrayList zoomContributions = new ArrayList();
		zoomContributions.add(ZoomManager.FIT_ALL);
		zoomContributions.add(ZoomManager.FIT_HEIGHT);
		zoomContributions.add(ZoomManager.FIT_WIDTH);
		manager.setZoomLevelContributions(zoomContributions);

		IAction action = new ZoomInAction(manager);
		getActionRegistry().registerAction(action);

		action = new ZoomOutAction(manager);
		getActionRegistry().registerAction(action);
		loadProperties();// the rulers visibility
		
		
		
		// Actions
		IAction showRulers = new ToggleRulerVisibilityAction(getGraphicalViewer());
		getActionRegistry().registerAction(showRulers);

		IAction snapAction = new ToggleSnapToGeometryAction(getGraphicalViewer());
		getActionRegistry().registerAction(snapAction);

		IAction showGrid = new ToggleGridAction(getGraphicalViewer());
		getActionRegistry().registerAction(showGrid);

		Listener listener = new Listener() {
			public void handleEvent(Event event) {
				handleActivationChanged(event);
			}
		};
		getGraphicalControl().addListener(SWT.Activate, listener);
		getGraphicalControl().addListener(SWT.Deactivate, listener);
		getGraphicalViewer().setProperty(SnapToGrid.PROPERTY_GRID_SPACING,new Dimension(5, 5));
		createActions();

	}

	public void dispose() {
		getSite().getWorkbenchWindow().getPartService().removePartListener(partListener);
		partListener = null;

		super.dispose();
	}

	public void loadProperties() {

		DiagramRuler ruler = getModel().getRuler(PositionConstants.WEST);
		RulerProvider provider = null;
		if (ruler != null) {
			provider = new Bio7RulerProvider(ruler);
		}
		getGraphicalViewer().setProperty(RulerProvider.PROPERTY_VERTICAL_RULER, provider);
		ruler = getModel().getRuler(PositionConstants.NORTH);
		provider = null;
		if (ruler != null) {
			provider = new Bio7RulerProvider(ruler);
		}
		getGraphicalViewer().setProperty(RulerProvider.PROPERTY_HORIZONTAL_RULER, provider);
		getGraphicalViewer().setProperty(RulerProvider.PROPERTY_RULER_VISIBILITY, new Boolean(getModel().getRulerVisibility()));

	}

	public GraphicalViewer getviewer() {// get the Graphical viewer

		GraphicalViewer view = getGraphicalViewer();
		return view;
	}

	protected void handleActivationChanged(Event event) {
		IAction copy = null;
		if (event.type == SWT.Deactivate)
			copy = getActionRegistry().getAction(ActionFactory.COPY.getId());
		if (getEditorSite().getActionBars().getGlobalActionHandler(ActionFactory.COPY.getId()) != copy) {
			getEditorSite().getActionBars().setGlobalActionHandler(ActionFactory.COPY.getId(), copy);
			getEditorSite().getActionBars().updateActionBars();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.gef.ui.parts.GraphicalEditor#commandStackChanged(java.util
	 * .EventObject)
	 */
	public void commandStackChanged(EventObject event) {
		firePropertyChange(IEditorPart.PROP_DIRTY);
		super.commandStackChanged(event);
	}

	protected Control getGraphicalControl() {
		return rulerComp;
	}

	private KeyHandler sharedKeyHandler;

	protected KeyHandler getCommonKeyHandler() {
		if (sharedKeyHandler == null) {
			sharedKeyHandler = new KeyHandler();
			sharedKeyHandler.put(KeyStroke.getPressed(SWT.DEL, 127, 0), getActionRegistry().getAction("Delete"));
		}
		return sharedKeyHandler;
	}

	private void createOutputStream(OutputStream os) throws IOException {
		ObjectOutputStream oos = new ObjectOutputStream(os);
		oos.writeObject(getModel());
		oos.close();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette#
	 * createPaletteViewerProvider()
	 */
	protected PaletteViewerProvider createPaletteViewerProvider() {
		return new PaletteViewerProvider(getEditDomain()) {
			protected void configurePaletteViewer(PaletteViewer viewer) {
				super.configurePaletteViewer(viewer);
				// create a drag source listener for this palette viewer
				// together with an appropriate transfer drop target listener,
				// this will enable
				// model element creation by dragging a
				// CombinatedTemplateCreationEntries
				// from the palette into the editor
				// @see ShapesEditor#createTransferDropTargetListener()
				viewer.addDragSourceListener(new TemplateTransferDragSourceListener(viewer));
			}
		};
	}

	/**
	 * Create a transfer drop target listener. When using a
	 * CombinedTemplateCreationEntry tool in the palette, this will enable model
	 * element creation by dragging from the palette.
	 * 
	 * @see #createPaletteViewerProvider()
	 */
	private TransferDropTargetListener createTransferDropTargetListener() {
		return new TemplateTransferDropTargetListener(getGraphicalViewer()) {
			protected CreationFactory getFactory(Object template) {
				return new SimpleFactory((Class) template);
			}
		};
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.ISaveablePart#doSave(org.eclipse.core.runtime.IProgressMonitor
	 * )
	 */
	public void doSave(IProgressMonitor monitor) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			createOutputStream(out);
			IFile file = ((IFileEditorInput) getEditorInput()).getFile();
			file.setContents(new ByteArrayInputStream(out.toByteArray()), true, false, // dont
					// keep
					// history
					monitor); // progress monitor
			getCommandStack().markSaveLocation();
		} catch (CoreException ce) {
			ce.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

	}

	protected void setSite(IWorkbenchPartSite site) {
		super.setSite(site);
		getSite().getWorkbenchWindow().getPartService().addPartListener(partListener);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.ISaveablePart#doSaveAs()
	 */
	public void doSaveAs() {
		// Show a SaveAs dialog
		Shell shell = getSite().getWorkbenchWindow().getShell();
		SaveAsDialog dialog = new SaveAsDialog(shell);
		dialog.setOriginalFile(((IFileEditorInput) getEditorInput()).getFile());
		dialog.open();

		IPath path = dialog.getResult();
		if (path != null) {

			final IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
			try {
				new ProgressMonitorDialog(shell).run(false, false, new WorkspaceModifyOperation() {
					public void execute(final IProgressMonitor monitor) {
						try {
							ByteArrayOutputStream out = new ByteArrayOutputStream();
							createOutputStream(out);
							file.create(new ByteArrayInputStream(out.toByteArray()), true,

							monitor);
						} catch (CoreException ce) {
							ce.printStackTrace();
						} catch (IOException ioe) {
							ioe.printStackTrace();
						}
					}
				});

				setInput(new FileEditorInput(file));
				getCommandStack().markSaveLocation();
			} catch (InterruptedException ie) {

				ie.printStackTrace();
			} catch (InvocationTargetException ite) {
				ite.printStackTrace();
			}
		}
	}

	public Object getAdapter(Class type) {
		if (type == IContentOutlinePage.class) {
			return new ShapesOutlinePage(new TreeViewer());
		} else if (type == ZoomManager.class) {
			return ((ScalableRootEditPart) getGraphicalViewer().getRootEditPart()).getZoomManager();

		}
		return super.getAdapter(type);

	}

	public ShapesDiagram getModel() {

		return diagram;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette#getPaletteRoot
	 * ()
	 */
	protected PaletteRoot getPaletteRoot() {

		if (PALETTE_MODEL == null)
			PALETTE_MODEL = ShapesEditorPaletteFactory.createPalette();
		return PALETTE_MODEL;
	}

	private void handleLoadException(Exception e) {
		System.err.println("** Load failed. Using default model. **");
		e.printStackTrace();
		diagram = new ShapesDiagram();

	}

	/**
	 * Set up the editor's inital content (after creation).
	 * 
	 * @see org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette#initializeGraphicalViewer()
	 */
	protected void initializeGraphicalViewer() {
		super.initializeGraphicalViewer();
		GraphicalViewer viewer = getGraphicalViewer();
		viewer.setContents(getModel()); // set the contents of this editor

		// listen for dropped parts
		viewer.addDropTargetListener(createTransferDropTargetListener());

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.ISaveablePart#isSaveAsAllowed()
	 */
	public boolean isSaveAsAllowed() {
		return true;
	}

	protected void createActions() {
		super.createActions();
		ActionRegistry registry = getActionRegistry();
		IAction action;

		Action manhatten;
		Action shortest;
		Action saveimage;
		Action debugmode;
		Action setGridSize;
		
		manhatten = new ManhattenRouter("Manhatten Router", PlatformUI.getWorkbench().getActiveWorkbenchWindow());

		registry.registerAction(manhatten);

		shortest = new ShortestPath("Shortest Path", PlatformUI.getWorkbench().getActiveWorkbenchWindow());

		registry.registerAction(shortest);

		saveimage = new SaveImage("Save Image", PlatformUI.getWorkbench().getActiveWorkbenchWindow());

		registry.registerAction(saveimage);

		debugmode = new DebugMode("Debug Mode", PlatformUI.getWorkbench().getActiveWorkbenchWindow());
		
		setGridSize = new SetGridSize("Size Layout Grid", PlatformUI.getWorkbench().getActiveWorkbenchWindow());

		registry.registerAction(debugmode);
		registry.registerAction(setGridSize);

		action = new CopyTemplateAction(this);
		registry.registerAction(action);

		action = new MatchWidthAction(this);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new MatchHeightAction(this);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new DirectEditAction((IWorkbenchPart) this);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new AlignmentAction((IWorkbenchPart) this, PositionConstants.LEFT);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new AlignmentAction((IWorkbenchPart) this, PositionConstants.RIGHT);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new AlignmentAction((IWorkbenchPart) this, PositionConstants.TOP);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new AlignmentAction((IWorkbenchPart) this, PositionConstants.BOTTOM);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new AlignmentAction((IWorkbenchPart) this, PositionConstants.CENTER);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new AlignmentAction((IWorkbenchPart) this, PositionConstants.MIDDLE);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.EditorPart#setInput(org.eclipse.ui.IEditorInput)
	 */
	protected void setInput(IEditorInput input) {
		super.setInput(input);
		try {
			IFile file = ((IFileEditorInput) input).getFile();
			ObjectInputStream in = new ObjectInputStream(file.getContents());
			diagram = (ShapesDiagram) in.readObject();
			in.close();
			setPartName(file.getName());
		} catch (IOException e) {
			handleLoadException(e);
		} catch (CoreException e) {
			handleLoadException(e);
		} catch (ClassNotFoundException e) {
			handleLoadException(e);
		}
	}

	private final class PartListener implements IPartListener {

		public void partActivated(IWorkbenchPart part) {
			if (part != ShapesEditor.this)
				return;
			if (!((IFileEditorInput) getEditorInput()).getFile().exists()) {

			}
		}

		public void partBroughtToTop(IWorkbenchPart part) {
		}

		public void partClosed(IWorkbenchPart part) {
		}

		public void partDeactivated(IWorkbenchPart part) {
		}

		public void partOpened(IWorkbenchPart part) {
		}
	}

	/**
	 * Creates an outline pagebook for this editor.
	 */
	public class ShapesOutlinePage extends ContentOutlinePage {
		/**
		 * Create a new outline page for the shapes editor.
		 * 
		 * @param viewer
		 *            a viewer (TreeViewer instance) used for this outline page
		 * @throws IllegalArgumentException
		 *             if editor is null
		 */
		public ShapesOutlinePage(EditPartViewer viewer) {
			super(viewer);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.ui.part.IPage#createControl(org.eclipse.swt.widgets.Composite
		 * )
		 */
		public void createControl(Composite parent) {
			sash = new SashForm(parent, SWT.VERTICAL);

			getViewer().createControl(sash);
			// create outline viewer page

			// configure outline viewer
			getViewer().setEditDomain(getEditDomain());
			getViewer().setEditPartFactory(new ShapesTreeEditPartFactory());
			// configure & add context menu to viewer
			ContextMenuProvider cmProvider = new ShapesEditorContextMenuProvider(getViewer(), getActionRegistry());
			getViewer().setContextMenu(cmProvider);
			getSite().registerContextMenu("com.eco.bio7.floweditor.shapes.outline.contextmenu", cmProvider, getSite().getSelectionProvider());
			// hook outline viewer
			getSelectionSynchronizer().addViewer(getViewer());
			// initialize outline viewer with model
			getViewer().setContents(getModel());
			Canvas canvas = new Canvas(sash, SWT.BORDER);

			LightweightSystem lws = new LightweightSystem(canvas);

			thumbnail = new ScrollableThumbnail((Viewport) ((ScalableRootEditPart) getGraphicalViewer().getRootEditPart()).getFigure());
			thumbnail.setSource(((ScalableRootEditPart) getGraphicalViewer().getRootEditPart()).getLayer(LayerConstants.PRINTABLE_LAYERS));

			lws.setContents(thumbnail);

			disposeListener = new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {

					if (thumbnail != null) {
						thumbnail.deactivate();
						thumbnail = null;
					}
				}

			};

			getGraphicalViewer().getControl().addDisposeListener(disposeListener);

			// show outline viewer
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.ui.part.IPage#dispose()
		 */

		public void dispose() {
			if (getGraphicalViewer().getControl() != null && !getGraphicalViewer().getControl().isDisposed())
				getGraphicalViewer().getControl().removeDisposeListener(disposeListener);

			// unhook outline viewer
			getSelectionSynchronizer().removeViewer(getViewer());
			// dispose
			super.dispose();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.ui.part.IPage#getControl()
		 */
		public Control getControl() {
			return sash;
		}

		/**
		 * @see org.eclipse.ui.part.IPageBookViewPage#init(org.eclipse.ui.part.IPageSite)
		 */
		public void init(IPageSite pageSite) {
			super.init(pageSite);

			ActionRegistry registry = getActionRegistry();
			IActionBars bars = pageSite.getActionBars();
			String id = ActionFactory.UNDO.getId();
			bars.setGlobalActionHandler(id, registry.getAction(id));
			id = ActionFactory.REDO.getId();
			bars.setGlobalActionHandler(id, registry.getAction(id));
			id = ActionFactory.DELETE.getId();

		}

	}

}