
/*
 * Original author of this class: Andrew Davison (Pro Java 6 3D Game Development). 
 * Adapted and slightly changed for Bio7.
 * 
 */

package com.eco.bio7.spatial;

import java.awt.event.MouseEvent;
import java.nio.IntBuffer;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;


import static com.jogamp.opengl.GL2.*; // GL2 constants

/**
 * A custom class to enable 3d selection in the Spatial panel.
 * 
 * @author Bio7
 * 
 */
public class SpatialPicking {

	private int buffSize = 1000;
	private int xCursor = 0;
	private int yCursor = 0;
	private IntBuffer selectBuffer;
	private int selection;
	private double mp = 5;// the picking matrix.

	/**
	 * The default constructor (Buffer size =1000, picking area=5).
	 */
	public SpatialPicking() {

	}

	/**
	 * Constructor with arguments of the buffer size and the picking area.
	 * 
	 * @param buffSize the size of the buffer as an integer value.
	 * @param pickArea an integer value for the picking area.
	 */
	public SpatialPicking(int buffSize, int pickArea) {
		this.buffSize = buffSize;
		this.buffSize = pickArea;
	}

	/**
	 * A method to indicate the start of the picking.
	 * 
	 * @param gl
	 *            the GL context.
	 * @param glu
	 *            the GLU context.
	 * @param evt
	 *            the mouse event.
	 */
	public void startPicking(GL2 gl, GLU glu, MouseEvent evt)

	{

		xCursor = evt.getX();
		yCursor = evt.getY();

		/* The selection buffer. */
		//int selectBuf[] = new int[buffSize];
		//selectBuffer = BufferUtil.newIntBuffer(buffSize);
		selectBuffer = com.jogamp.common.nio.Buffers.newDirectIntBuffer(buffSize);
		gl.glSelectBuffer(buffSize, selectBuffer);

		gl.glRenderMode(GL_SELECT); // switch to selection mode

		gl.glInitNames(); // make an empty name stack

		/*
		 * Redefine the viewing volume so it only renders a small area around
		 * the place where the mouse was clicked.
		 */

		// save the original projection matrix
		gl.glMatrixMode(GL_PROJECTION);

		gl.glPushMatrix();
		gl.glLoadIdentity();

		// get the current viewport
		int viewport[] = new int[4];
		gl.glGetIntegerv(GL.GL_VIEWPORT, viewport, 0);

		// Create a x*x pixel picking area near the cursor location.
		glu.gluPickMatrix((double) xCursor, (double) (viewport[3] - yCursor), mp, mp, viewport, 0);

		/*
		 * The y-value uses an 'inverted' yCursor to transform the y-coordinates
		 * origin from the upper left corner into the bottom left corner.
		 */

		/*
		 * Set projection (perspective or orthogonal) exactly as it is in normal
		 * rendering (i.e. duplicate the gluPerspective() call in resizeView()).
		 */
		glu.gluPerspective(45.0f, (float) SpatialStructure.getSpatialStructureInstance().getWidth() / (float) SpatialStructure.getSpatialStructureInstance().getHeight(), 1.0f, 100000.0f);

		gl.glMatrixMode(GL_MODELVIEW); // restore model view
	} // End of startPicking()

	/**
	 * A method to indicate the stop of the picking.
	 * 
	 * @param gl the GL context.
	 */
	public void endPicking(GL2 gl)
	/*
	 * Switch back to normal rendering, and extract 'hit information generated
	 * because of picking.
	 */
	{
		// restore original projection matrix
		gl.glMatrixMode(GL_PROJECTION);
		gl.glPopMatrix();
		gl.glMatrixMode(GL_MODELVIEW);
		gl.glFlush();

		// return to normal rendering mode, and process hits
		int numHits = gl.glRenderMode(GL_RENDER);
		processHits(numHits);

	} // End of endPicking().

	private void processHits(int numHits)
	/*
	 * Display all the hit records, and report the name of the 'thing' that was
	 * picked closest to the viewport.
	 * 
	 * Each hit record contains: - the number of different names for the thing
	 * hit (usually only 1) - minimum and maximum depths of the hit - the names
	 * for the thing hit (stored on the name stack)
	 */
	{
		if (numHits == 0)
			return; // No hits to process.

		// Storage for the name ID closest to the viewport.
		int selectedNameID = -1; // Dummy initial values.
		float smallestZ = -1.0f;

		boolean isFirstLoop = true;
		int offset = 0;

		/*
		 * Iterate through the hit records, saving the smallest z value.
		 */
		for (int i = 0; i < numHits; i++) {

			int numNames = selectBuffer.get(offset);
			offset++;

			// MinZ and maxZ are taken from the Z buffer.
			float minZ = getDepth(offset);
			offset++;

			// Store the smallest z value.
			if (isFirstLoop) {
				smallestZ = minZ;
				isFirstLoop = false;
			} else {
				if (minZ < smallestZ)
					smallestZ = minZ;
			}

			float maxZ = getDepth(offset);
			offset++;

			for (int j = 0; j < numNames; j++) {
				selection = selectBuffer.get(offset);

				if (j == (numNames - 1)) { // If the last one (the top element
					// on the stack).
					if (smallestZ == minZ) // Is this the smallest min z?
						selectedNameID = selection; // Then store it's name ID.
				}

				offset++;
			}

		}

	} // End of processHits()

	private float getDepth(int offset)
	/*
	 * A depth is in the range 0 to 1, but is stored after being multiplied by
	 * 2^32 -1 and rounded to the nearest integer. The number will be negative
	 * due to the multiplication and being stored as an integer.
	 */
	{
		long depth = (long) selectBuffer.get(offset); // Large -ve number
		return (1.0f + ((float) depth / 0x7fffffff));
		// Return as a float between 0 and 1.
	} // End of getDepth().

	/**
	 * A method which returns the selected id from the object.
	 * 
	 * @return an integer value representing the selection.
	 */
	public int getSelection() {
		return selection;
	}

}
