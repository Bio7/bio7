package com.eco.bio7.reditor.actions;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.source.IVerticalRulerInfo;

import org.eclipse.ui.texteditor.AbstractRulerActionDelegate;
import org.eclipse.ui.texteditor.ITextEditor;

public class JavaSelectRulerAction extends AbstractRulerActionDelegate {

        /*
         * @see AbstractRulerActionDelegate#createAction(ITextEditor, IVerticalRulerInfo)
         */
        protected IAction createAction(ITextEditor editor, IVerticalRulerInfo rulerInfo) {
        	
        	System.out.println(rulerInfo.getLineOfLastMouseButtonActivity());
        	
			return null;
               
        }
}