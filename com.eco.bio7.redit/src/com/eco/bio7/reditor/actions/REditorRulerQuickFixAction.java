package com.eco.bio7.reditor.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.jface.text.source.IVerticalRulerInfoExtension;
import org.eclipse.jface.text.source.IVerticalRulerListener;
import org.eclipse.jface.text.source.VerticalRulerEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.texteditor.AbstractRulerActionDelegate;
import org.eclipse.ui.texteditor.ITextEditor;

import com.eco.bio7.reditor.code.VerticalRulerListener;
import com.eco.bio7.reditors.REditor;


public class REditorRulerQuickFixAction extends AbstractRulerActionDelegate {
	
	

	/*
	 * @see AbstractRulerActionDelegate#createAction(ITextEditor, IVerticalRulerInfo)
	 */
	protected IAction createAction(ITextEditor editor, IVerticalRulerInfo rulerInfo) {
		int line = rulerInfo.getLineOfLastMouseButtonActivity();
		
		IVerticalRulerListener listener = new VerticalRulerListener();
		
		Object adapted = editor.getAdapter(IVerticalRuler.class);
		if (adapted instanceof IVerticalRulerInfoExtension) {
			((IVerticalRulerInfoExtension)adapted).addVerticalRulerListener(listener);
		}
		
		
		//System.out.println(line);
		if (line > 0) {
			
			

			ITextOperationTarget operation = (ITextOperationTarget) editor.getAdapter(ITextOperationTarget.class);

			final int opCode = ISourceViewer.QUICK_ASSIST;

			if (operation != null && operation.canDoOperation(opCode)) {

				editor.selectAndReveal(10, 5);

				operation.doOperation(opCode);

			}
		}

		return null;
	}
	
}