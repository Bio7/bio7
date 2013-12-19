package com.eco.bio7.scenebuilder;

import javafx.application.Platform;
import javafx.embed.swt.FXCanvas;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.eco.bio7.scenebuilder.editor.MultiPageEditor;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.InspectorPanelController;

public class InspectorPanelView extends ViewPart {

	private FXCanvas canvas;
	private MultiPageEditor pag;
	private Composite composite;
	private Scene scene;

	public InspectorPanelView() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void createPartControl(Composite parent) {
		composite = new Composite(parent, SWT.NONE);
		FillLayout layout = new FillLayout();
		composite.setLayout(layout);

		canvas = new FXCanvas(composite, SWT.NONE);
		canvas.setLayout(new FillLayout());
		final Group root = new Group();
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				Scene s = new Scene(root, 300, 300, Color.WHITE);
				canvas.setScene(s);
			}
		});

		getSite().getWorkbenchWindow().getPartService().addPartListener(partListener);

	}

	private IPartListener2 partListener = new IPartListener2() {

		@Override
		public void partActivated(IWorkbenchPartReference partRef) { //
			// System.out.println(partRef.getId());
			updateHierachyView(partRef, false);

		}

		public void partBroughtToTop(IWorkbenchPartReference partRef) { // TODO
			updateHierachyView(partRef, false);

		}

		public void partClosed(IWorkbenchPartReference partRef) { // TODO
			Platform.runLater(new Runnable() {

				@Override
				public void run() {
					Group root = new Group();
					Scene s = new Scene(root, 300, 300, Color.WHITE);
					if (composite.isDisposed() == false) {
						canvas.setScene(s);
					}
				}
			});
		}

		public void partDeactivated(IWorkbenchPartReference partRef) { // TODO //

		}

		@Override
		public void partOpened(IWorkbenchPartReference partRef) {
			updateHierachyView(partRef, false);
		}

		public void partHidden(IWorkbenchPartReference partRef) { // TODO

		}

		public void partVisible(IWorkbenchPartReference partRef) { // TODO
			// updateHierachyView(partRef);

		}

		public void partInputChanged(IWorkbenchPartReference partRef) { // TODO

		}

	};

	private void updateHierachyView(IWorkbenchPartReference partRef, final boolean closed) {
		if (partRef.getId().equals("com.eco.bio7.browser.scenebuilder")) {
			IEditorPart editor = partRef.getPage().getActiveEditor();
			if (editor instanceof MultiPageEditor) {
				pag = (MultiPageEditor) editor;

				Platform.runLater(new Runnable() {

					@Override
					public void run() {

						if (pag != null) {
							InspectorPanelController h = new InspectorPanelController(pag.editorController);
							final BorderPane pane = new BorderPane();
							pane.setCenter(h.getPanelRoot());
							scene = new Scene(pane);

						}

					}
				});

				Display display = PlatformUI.getWorkbench().getDisplay();
				display.asyncExec(new Runnable() {

					public void run() {
						if (composite.isDisposed() == false) {
							canvas.setScene(scene);

						}
					}
				});

			}
		}
	}

	public void dispose() {
		getSite().getWorkbenchWindow().getPartService().removePartListener(partListener);

		super.dispose();
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}

}
