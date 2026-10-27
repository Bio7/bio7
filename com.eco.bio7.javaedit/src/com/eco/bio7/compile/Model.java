package com.eco.bio7.compile;

import java.awt.Graphics2D;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.glu.GLU;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import com.jogamp.opengl.util.gl2.GLUT;

/**
 * This is the superclass for the dynamic compiled Java class! If the methods
 * (below) are not implemented in the subclass (Model) the methods of this class
 * are executed!
 */
public abstract class Model {

	/**
	 * The setup method to initialize values etc.!
	 */
	public void setup() {
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				MessageBox messageBox = new MessageBox(new Shell(),

				SWT.ICON_WARNING);
				messageBox.setText("Info!");
				messageBox.setMessage("No compiled setup method defined!");
				messageBox.open();
			}
		});
	}

	/**
	 * The main method which is called from the calculation thread!
	 */
	public void run() {

	}

	/**
	 * The main OpenGL method with the OpenGL context as parameters!
	 * 
	 * @param gl
	 * @param glu
	 * @param glut
	 */
	public void run(GL2 gl, GLU glu, GLUT glut) {

	}

	/**
	 * The setup OpenGL method with the OpenGL context as parameters!
	 * 
	 * @param gl
	 * @param glu
	 * @param glut
	 */
	public void setup(GL2 gl, GLU glu, GLUT glut) {

	}

	/**
	 * The OpenGL profile of the 3D view for this model, e.g., GLProfile.GL4 or
	 * GLProfile.GL3 (the names of com.jogamp.opengl.GLProfile).
	 * <p>
	 * null (the default) renders the model in the Bio7 scene: the newest
	 * compatibility profile with the fixed function pipeline and, e.g., OpenGL 4
	 * on Windows and Linux (OpenGL 2.1 on macOS). The model can use the methods
	 * with the GL2 context and the methods with the GLAutoDrawable (e.g.,
	 * drawable.getGL().getGL4bc() if available).
	 * <p>
	 * Another profile (e.g., GLProfile.GL4: OpenGL 4.1 core on macOS) creates a
	 * new context for the model without the Bio7 scene: only setup(GLAutoDrawable)
	 * and run(GLAutoDrawable) are called and the model clears and draws the view
	 * with an own camera.
	 * 
	 * @return the name of the profile or null
	 */
	public String getGLProfile() {
		return null;
	}

	/**
	 * The setup OpenGL method with the drawable of the 3D view (see
	 * getGLProfile), e.g., to create shaders and buffers. Called after
	 * setup(GL2, GLU, GLUT) in the Bio7 scene!
	 * 
	 * @param drawable the drawable (drawable.getGL() is the context)
	 */
	public void setup(GLAutoDrawable drawable) {

	}

	/**
	 * The main OpenGL method with the drawable of the 3D view (see getGLProfile).
	 * Called after run(GL2, GLU, GLUT) in the Bio7 scene with the camera of the
	 * scene (the modelview and projection matrix, in GLSL, e.g.,
	 * gl_ModelViewProjectionMatrix with "#version 400 compatibility"). After the
	 * method the shader program, the vertex array and the buffers are unbound!
	 * 
	 * @param drawable the drawable (drawable.getGL() is the context)
	 */
	public void run(GLAutoDrawable drawable) {

	}

	/**
	 * The draw method for the Points panel with the Graphics context to draw
	 * directly on the panel with available Java routines!
	 * 
	 * @param g
	 */
	public void draw(Graphics2D g) {

	}

	/**
	 * A method to allow closing operations with a compiled Model class
	 * called,e.g., from the CustomView implementation.
	 */
	public void close() {

	}

}
