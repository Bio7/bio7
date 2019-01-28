/*
 * package com.eco.bio7.actions;
 * 
 * import org.eclipse.jface.action.Action; import
 * org.eclipse.ui.IWorkbenchWindow;
 * 
 * import com.eco.bio7.Bio7Plugin; import com.eco.bio7.discrete3d.Quad3dview;
 * 
 * public class Start3d extends Action {
 * 
 * private final IWorkbenchWindow window;
 * 
 * public Start3d(String text, IWorkbenchWindow window) { super(text,
 * AS_CHECK_BOX); this.window = window; setId("com.eco.bio7.start3d");
 * setImageDescriptor(Bio7Plugin.getImageDescriptor(
 * "/icons/maintoolbar/play_pause.png"));
 * 
 * }
 * 
 * public void run() { if (Quad3dview.isOn() == true) {
 * Quad3dview.getAnimator().stop(); Quad3dview.setOn(false); } else if
 * (Quad3dview.isOn() == false) { Quad3dview.getAnimator().start();
 * Quad3dview.setOn(true); }
 * 
 * }
 * 
 * }
 */