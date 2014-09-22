package com.eco.bio7.scenebuilder;

import javafx.application.Platform;
import javafx.embed.swt.FXCanvas;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.eco.bio7.scenebuilder.editor.ILinkedWithEditorView;
import com.eco.bio7.scenebuilder.editor.LinkWithEditorPartListener;
import com.eco.bio7.scenebuilder.editor.MultiPageEditor;
import com.oracle.javafx.scenebuilder.kit.editor.panel.css.CssPanelController;

public class CssView extends ViewPart implements ILinkedWithEditorView {

	private FXCanvas canvas;
	private MultiPageEditor pag;
	private Composite composite;
	private Scene scene;
	private IPartListener2 linkWithEditorPartListener = new LinkWithEditorPartListener(this);
	private Action linkWithEditorAction;
	private boolean linkingActive = true;
	private IEditorPart currentEditor;

	public CssView() {
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
				scene = new Scene(root, 300, 300, Color.WHITE);
				canvas.setScene(scene);
			}
		});

		// getSite().getWorkbenchWindow().getPartService().addPartListener(partListener);
		/*
		 * linkWithEditorAction = new Action("Link with Editor",
		 * IAction.AS_CHECK_BOX) {
		 * 
		 * @Override public void run() { toggleLinking(isChecked()); } };
		 */

		getSite().getWorkbenchWindow().getPartService().addPartListener(partListener);
		// getViewSite().getActionBars().getToolBarManager().add(linkWithEditorAction);
		getSite().getPage().addPartListener(linkWithEditorPartListener);
		getSite().getWorkbenchWindow().getPartService().addPartListener(partListener);

	}

	private void updateHierachyView(IEditorPart editor) {

		if (editor instanceof MultiPageEditor) {
			pag = (MultiPageEditor) editor;

			Platform.runLater(new Runnable() {

				@Override
				public void run() {

					if (pag != null) {
						CssPanelController css = new CssPanelController(pag.editorController, null);
						final BorderPane pane = new BorderPane();
						pane.setCenter(css.getPanelRoot());
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

	@Override
	public void editorActivated(IEditorPart activeEditor) {
		if (!linkingActive || !getViewSite().getPage().isPartVisible(this)) {
			return;
		}

		if (currentEditor != activeEditor || currentEditor == null) {
			updateHierachyView(activeEditor);
			currentEditor = activeEditor;
		}
		// do something with content of the editor
	}

	protected void toggleLinking(boolean checked) {
		this.linkingActive = checked;
		if (checked) {
			editorActivated(getSite().getPage().getActiveEditor());
		}
	}

	private IPartListener2 partListener = new IPartListener2() {

		@Override
		public void partActivated(IWorkbenchPartReference partRef) { //
			// updateHierachyView(partRef, false);

		}

		public void partBroughtToTop(IWorkbenchPartReference partRef) { // TODO
			// updateHierachyView(partRef, false);

		}

		public void partClosed(IWorkbenchPartReference partRef) { // TODO
			if (partRef.getId().equals("com.eco.bio7.browser.scenebuilder")) {
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
		}

		public void partDeactivated(IWorkbenchPartReference partRef) { // TODO
																		// //

		}

		@Override
		public void partOpened(IWorkbenchPartReference partRef) {
			// updateHierachyView(partRef, false);
		}

		public void partHidden(IWorkbenchPartReference partRef) { // TODO

		}

		public void partVisible(IWorkbenchPartReference partRef) { // TODO

		}

		public void partInputChanged(IWorkbenchPartReference partRef) { // TODO

		}

	};

	public void dispose() {
		getSite().getWorkbenchWindow().getPartService().removePartListener(partListener);

		super.dispose();
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}

}
