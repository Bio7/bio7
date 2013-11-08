package com.eco.bio7.discrete3d;


import javax.media.opengl.awt.GLCanvas;
import javax.swing.JRootPane;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import com.eco.bio7.actions.MatrixQuad3DAction;
import com.eco.bio7.rcp.ApplicationActionBarAdvisor;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.FPSAnimator;

public class Quad3dview extends ViewPart {
	private static boolean on = true;

	private static FPSAnimator animator;

	public static final String ID = "com.eco.bio7.3d";

	private IPartListener partListener;

	private MatrixQuad3DAction nitrate = new MatrixQuad3DAction();

	public void createPartControl(Composite parent) {
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "com.eco.bio7.quad3d");
		initializeToolBar();

		Composite top = new Composite(parent, SWT.NO_BACKGROUND | SWT.EMBEDDED);
		try {
			System.setProperty("sun.awt.noerasebackground", "true");
		} catch (NoSuchMethodError error) {
		}

		java.awt.Frame frame = SWT_AWT.new_Frame(top);
		java.awt.Panel panel = new java.awt.Panel(new java.awt.BorderLayout()) {
			public void update(java.awt.Graphics g) {

				paint(g);
			}
		};

		JRootPane root = new JRootPane();
		panel.add(root);
		GLCanvas canvas = new GLCanvas();
		canvas.addGLEventListener(new Quad3d.Renderer(canvas));
		frame.add(canvas);
		animator = new FPSAnimator(canvas,60);
		top.setLayout(new RowLayout());
		animator.start();

	}

	private void initializeToolBar() {
		IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();
		tbm.add(ApplicationActionBarAdvisor.getStart3d());

	}

	public void setFocus() {

	}

	public static FPSAnimator getAnimator() {
		return animator;
	}

	public void setAnimator(FPSAnimator animator) {
		this.animator = animator;
	}

	public void init(IViewSite site) {
		try {
			super.init(site);
		} catch (PartInitException e) {

			e.printStackTrace();
		}

		IWorkbenchPage page = site.getPage();
		partListener = new IPartListener() {

			public void partActivated(IWorkbenchPart part) {

			}

			public void partBroughtToTop(IWorkbenchPart part) {

			}

			public void partClosed(IWorkbenchPart part) {

			}

			public void partDeactivated(IWorkbenchPart part) {

			}

			public void partOpened(IWorkbenchPart part) {

			}

		};
		page.addPartListener(partListener);
	}

	public void dispose() {
		animator.stop();
		if (partListener != null) {
			IWorkbenchPage page = getViewSite().getPage();
			page.removePartListener(partListener);
		}

		super.dispose();
	}

	public static boolean isOn() {
		return on;
	}

	public static void setOn(boolean on) {
		Quad3dview.on = on;
	}

}