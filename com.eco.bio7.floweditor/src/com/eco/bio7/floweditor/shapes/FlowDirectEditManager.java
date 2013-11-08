package com.eco.bio7.floweditor.shapes;

import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.tools.CellEditorLocator;
import org.eclipse.gef.tools.DirectEditManager;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import com.eco.bio7.floweditor.model.EllipticalShape;
import com.eco.bio7.floweditor.model.Shape;

public class FlowDirectEditManager extends DirectEditManager {

	private Shape shapemodel;

	public FlowDirectEditManager(GraphicalEditPart source, Class editorType, CellEditorLocator locator) {
		super(source, editorType, locator);
		shapemodel = (Shape) source.getModel();
	}

	protected void initCellEditor() {

		getCellEditor().setValue(shapemodel.getText());

		Text text = (Text) getCellEditor().getControl();
		text.selectAll();
	}

	protected CellEditor createCellEditorOn(Composite composite) {

		CellEditor t;
		/* Allow line wrap only for the label! */
		if (shapemodel instanceof EllipticalShape) {
			t = new TextCellEditor(composite, SWT.SINGLE);
		} else {
			t = new TextCellEditor(composite, SWT.MULTI | SWT.WRAP);
		}
		return t;
	}

}
