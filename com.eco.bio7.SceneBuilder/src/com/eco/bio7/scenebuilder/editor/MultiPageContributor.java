package com.eco.bio7.scenebuilder.editor;

import org.eclipse.jface.action.IAction;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.ide.IDEActionFactory;
import org.eclipse.ui.part.MultiPageEditorActionBarContributor;
import com.eco.bio7.scenebuilder.xmleditor.XMLEditor;

/**
 * Manages the installation/deinstallation of global actions for multi-page editors.
 * Responsible for the redirection of global actions to the active editor.
 * Multi-page contributor replaces the contributors for the individual editors in the multi-page editor.
 */
public class MultiPageContributor extends MultiPageEditorActionBarContributor {
    private IEditorPart activeEditorPart;

    /**
     * Creates a multi-page contributor.
     */
    public MultiPageContributor() {
        super();
    }

    /**
     * Returns the action registed with the given text editor.
     * @return IAction or null if editor is null.
     */
    protected IAction getAction(XMLEditor editor, String actionID) {
        return (editor == null ? null : editor.getAction(actionID));
    }

    
    public void setActivePage(IEditorPart part) {
        if (activeEditorPart == part)
            return;

        activeEditorPart = part;

        IActionBars actionBars = getActionBars();
        if (actionBars != null) {

        	XMLEditor editor = (part instanceof XMLEditor) ? (XMLEditor) part
                    : null;

            actionBars.setGlobalActionHandler(ActionFactory.DELETE.getId(),
                    getAction(editor, ActionFactory.DELETE.getId()));
            actionBars.setGlobalActionHandler(ActionFactory.UNDO.getId(),
                    getAction(editor, ActionFactory.UNDO.getId()));
            actionBars.setGlobalActionHandler(ActionFactory.REDO.getId(),
                    getAction(editor, ActionFactory.REDO.getId()));
            actionBars.setGlobalActionHandler(ActionFactory.CUT.getId(),
                    getAction(editor, ActionFactory.CUT.getId()));
            actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(),
                    getAction(editor, ActionFactory.COPY.getId()));
            actionBars.setGlobalActionHandler(ActionFactory.PASTE.getId(),
                    getAction(editor, ActionFactory.PASTE.getId()));
            actionBars.setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(),
                    getAction(editor, ActionFactory.SELECT_ALL.getId()));
            actionBars.setGlobalActionHandler(ActionFactory.FIND.getId(),
                    getAction(editor, ActionFactory.FIND.getId()));
            actionBars.setGlobalActionHandler(
                    IDEActionFactory.BOOKMARK.getId(), getAction(editor,
                            IDEActionFactory.BOOKMARK.getId()));
            actionBars.setGlobalActionHandler(
                    IDEActionFactory.ADD_TASK.getId(), getAction(editor,
                            IDEActionFactory.ADD_TASK.getId()));
            actionBars.updateActionBars();
        }
    }
}