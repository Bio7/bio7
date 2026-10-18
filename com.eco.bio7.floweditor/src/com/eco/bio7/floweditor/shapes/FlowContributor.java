package com.eco.bio7.floweditor.shapes;

import java.util.function.Function;

import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.RetargetAction;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.internal.GEFMessages;
import org.eclipse.gef.ui.actions.ActionBarContributor;
import org.eclipse.gef.ui.actions.AlignmentRetargetAction;
import org.eclipse.gef.ui.actions.DeleteRetargetAction;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.gef.ui.actions.MatchHeightRetargetAction;
import org.eclipse.gef.ui.actions.MatchWidthRetargetAction;
import org.eclipse.gef.ui.actions.RedoRetargetAction;
import org.eclipse.gef.ui.actions.UndoRetargetAction;
import org.eclipse.gef.ui.actions.ZoomInRetargetAction;
import org.eclipse.gef.ui.actions.ZoomOutRetargetAction;

public class FlowContributor extends ActionBarContributor {

	/* The zoom levels of the active editor in the tool bar! */
	private final ZoomDropDownAction zoomLevels = new ZoomDropDownAction();

	public FlowContributor() {
	}

	public void dispose() {
		zoomLevels.dispose();
		super.dispose();
	}

	protected void buildActions() {

		addRetargetAction(new UndoRetargetAction());
		addRetargetAction(new RedoRetargetAction());
		addRetargetAction(new DeleteRetargetAction());
		addRetargetAction(new ZoomInRetargetAction());
		addRetargetAction(new ZoomOutRetargetAction());

		addRetargetAction(new AlignmentRetargetAction(PositionConstants.LEFT));
		addRetargetAction(new AlignmentRetargetAction(PositionConstants.CENTER));
		addRetargetAction(new AlignmentRetargetAction(PositionConstants.RIGHT));
		addRetargetAction(new AlignmentRetargetAction(PositionConstants.TOP));
		addRetargetAction(new AlignmentRetargetAction(PositionConstants.MIDDLE));
		addRetargetAction(new AlignmentRetargetAction(PositionConstants.BOTTOM));

		addRetargetAction(new ZoomInRetargetAction());
		addRetargetAction(new ZoomOutRetargetAction());

		addRetargetAction(new MatchWidthRetargetAction());
		addRetargetAction(new MatchHeightRetargetAction());

		addRetargetAction(new RetargetAction(
				GEFActionConstants.TOGGLE_RULER_VISIBILITY,
				GEFMessages.ToggleRulerVisibility_Label, IAction.AS_CHECK_BOX));

		addRetargetAction(new RetargetAction(
				GEFActionConstants.TOGGLE_SNAP_TO_GEOMETRY,
				GEFMessages.ToggleSnapToGeometry_Label, IAction.AS_CHECK_BOX));

		addRetargetAction(new RetargetAction(
				GEFActionConstants.TOGGLE_GRID_VISIBILITY,
				GEFMessages.ToggleGrid_Label, IAction.AS_CHECK_BOX));
		setBio7Icons();
	}

	public void setActiveEditor(IEditorPart editor) {
		super.setActiveEditor(editor);
		zoomLevels.setZoomManager(editor == null ? null : editor.getAdapter(ZoomManager.class));
		/* Undo and redo take the images of the actions of the editor! */
		setBio7Icons();
	}

	private void setBio7Icons() {
		setBio7Icons(this::getAction);
	}

	/*
	 * The Bio7 icons instead of the GEF and Eclipse icons (with the icon color of
	 * the preferences). The toolbar actions take the images of the actions of the
	 * editor (when the editor is activated), so the icons are also set for the
	 * actions of the editor!
	 */
	static void setBio7Icons(Function<String, IAction> actions) {
		setIcon(actions, ActionFactory.DELETE.getId(), "delete");
		setIcon(actions, ActionFactory.UNDO.getId(), "undo");
		setIcon(actions, ActionFactory.REDO.getId(), "redo");
		setIcon(actions, GEFActionConstants.ALIGN_LEFT, "alignleft");
		setIcon(actions, GEFActionConstants.ALIGN_CENTER, "aligncenter");
		setIcon(actions, GEFActionConstants.ALIGN_RIGHT, "alignright");
		setIcon(actions, GEFActionConstants.ALIGN_TOP, "aligntop");
		setIcon(actions, GEFActionConstants.ALIGN_MIDDLE, "alignmid");
		setIcon(actions, GEFActionConstants.ALIGN_BOTTOM, "alignbottom");
		setIcon(actions, GEFActionConstants.MATCH_WIDTH, "matchwidth");
		setIcon(actions, GEFActionConstants.MATCH_HEIGHT, "matchheight");
		setIcon(actions, GEFActionConstants.ZOOM_IN, "zoom_in");
		setIcon(actions, GEFActionConstants.ZOOM_OUT, "zoom_out");
	}

	private static void setIcon(Function<String, IAction> actions, String id, String name) {
		IAction action = actions.apply(id);
		if (action == null) {
			return;
		}
		action.setImageDescriptor(ShapesPlugin.getImageDescriptor("icons/gef/" + name + ".svg"));
		/* The disabled and hover images are computed from the image! */
		action.setDisabledImageDescriptor(null);
		action.setHoverImageDescriptor(null);
	}

	protected void declareGlobalActionKeys() {

	}

	public void contributeToToolBar(IToolBarManager tbm) {

		tbm.add(getAction(ActionFactory.DELETE.getId()));
		tbm.add(getAction(ActionFactory.UNDO.getId()));
		tbm.add(getAction(ActionFactory.REDO.getId()));

		tbm.add(new Separator());

		tbm.add(new Separator());
		tbm.add(getAction(GEFActionConstants.ALIGN_LEFT));
		tbm.add(getAction(GEFActionConstants.ALIGN_CENTER));
		tbm.add(getAction(GEFActionConstants.ALIGN_RIGHT));
		tbm.add(new Separator());
		tbm.add(getAction(GEFActionConstants.ALIGN_TOP));
		tbm.add(getAction(GEFActionConstants.ALIGN_MIDDLE));
		tbm.add(getAction(GEFActionConstants.ALIGN_BOTTOM));

		tbm.add(new Separator());
		tbm.add(getAction(GEFActionConstants.MATCH_WIDTH));
		tbm.add(getAction(GEFActionConstants.MATCH_HEIGHT));

		tbm.add(new Separator());
		/*
		 * The zoom levels as drop down button (a native combo in the main tool bar
		 * shifts the gradient of the tool bar on macOS)!
		 */
		ActionContributionItem zoomItem = new ActionContributionItem(zoomLevels);
		zoomItem.setMode(ActionContributionItem.MODE_FORCE_TEXT);
		tbm.add(zoomItem);
		tbm.add(getActionRegistry().getAction(GEFActionConstants.ZOOM_IN));
		tbm.add(getActionRegistry().getAction(GEFActionConstants.ZOOM_OUT));

	}

	/**
	 * @see org.eclipse.ui.part.EditorActionBarContributor#contributeToMenu(IMenuManager)
	 */
	public void contributeToMenu(IMenuManager menubar) {
		super.contributeToMenu(menubar);
		MenuManager viewMenu = new MenuManager("Flowaction");
		viewMenu.add(getAction(GEFActionConstants.ZOOM_IN));
		viewMenu.add(getAction(GEFActionConstants.ZOOM_OUT));
		viewMenu.add(new Separator());
		viewMenu.add(getAction(GEFActionConstants.TOGGLE_RULER_VISIBILITY));
		viewMenu.add(getAction(GEFActionConstants.TOGGLE_GRID_VISIBILITY));
		viewMenu.add(getAction(GEFActionConstants.TOGGLE_SNAP_TO_GEOMETRY));
		viewMenu.add(new Separator());
		viewMenu.add(getAction(GEFActionConstants.MATCH_WIDTH));
		viewMenu.add(getAction(GEFActionConstants.MATCH_HEIGHT));

		menubar.add(viewMenu);
	}

}
