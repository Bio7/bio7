package com.eco.bio7.compile;



import java.awt.Graphics2D;
import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import com.jogamp.opengl.util.gl2.GLUT;


/**This is the superclass for the dynamic compiled Java class! If the methods (below) are not implemented in the subclass (Model) the methods of
 * this class are executed!*/
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
	 * @param gl
	 * @param glu
	 * @param glut
	 */
	public void run(GL2 gl, GLU glu, GLUT glut) {

	}
	

	/**
	 * The setup OpenGL method with the OpenGL context as parameters!
	 * @param gl
	 * @param glu
	 * @param glut
	 */
	public void setup(GL2 gl, GLU glu, GLUT glut) {

	}

	/**
	 * The draw method for the Points panel with the Graphics context to draw directly on the panel with available Java routines!
	 * @param g
	 */
	public void draw(Graphics2D g) {

	}

}
