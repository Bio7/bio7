package com.eco.bio7.actions;

import java.util.List;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import com.eco.bio7.batch.BatchModel;
import com.eco.bio7.floweditor.model.EllipticalShape;
import com.eco.bio7.floweditor.model.Shape;
import com.eco.bio7.floweditor.model.ShapesDiagram;
import com.eco.bio7.floweditor.shapes.ShapesEditor;

public class FlowStopAction extends Action {

	private final IWorkbenchWindow window;

	public FlowStopAction(String text, IWorkbenchWindow window) {
		super(text);
		this.window = window;

		setId("com.eco.bio7.flow_stop");

		setActionDefinitionId("com.eco.bio7.flowstop");

	}

	public void run() {
		BatchModel.setCancel(true);

		IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (editor != null) {
			if (editor instanceof ShapesEditor) {
				ShapesDiagram diagram = ((ShapesEditor) editor).getModel();
				if (diagram != null) {
					List dia = diagram.getChildren();
					if (dia.size() > 0) {

						for (int i = 0; i < dia.size(); i++) {

							Shape a = (Shape) dia.get(i);
                            //Reset the loop values !!!!!
							if (a instanceof EllipticalShape) {
								a.setPropertyValue(a.LOOP_COUNT, a
										.getPropertyValue(a.INIT_LOOP));
							}
						}
					}
				}
			}
		}

	}
}