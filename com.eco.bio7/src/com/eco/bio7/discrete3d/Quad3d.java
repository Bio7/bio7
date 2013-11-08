package com.eco.bio7.discrete3d;

import ij.ImagePlus;
import ij.WindowManager;
import ij.process.ImageProcessor;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.glu.GLU;
import com.eco.bio7.discrete.Field;
import com.eco.bio7.methods.CurrentStates;
import com.jogamp.opengl.util.Animator;

import static javax.media.opengl.GL2.*; // GL2 constants

public class Quad3d {

	private static float transz = -150.0f;

	private static float transx = 0.0f;

	private static float transy = 0.0f;

	private static float RGB[] = new float[3];

	private static float rotatx = -100.0f;

	private static float rotaty = 100.0f;

	private static float rotatz = 0.0f;

	private static float rotatzz = 0.0f;

	private static int tiefe = 3;// Soil,Plants,

	private static int[][][] raum = new int[Field.getWidth()][Field.getHeight()][tiefe];

	private static float layerdistance = 2.0f;

	private static int prevMouseX;

	private static int prevMouseY;

	private static boolean mouseRButtonDown = false;

	public void raumfueller() {
		for (int z = 0; z < tiefe; z++) {
			for (int y = 0; y < Field.getHeight(); y++) {
				for (int x = 0; x < Field.getWidth(); x++) {
					raum[x][y][z] = (int) (Math.random() * 3);

				}
			}
		}

	}

	static Animator animator = null;

	public static class Renderer implements GLEventListener, KeyListener, MouseListener, MouseMotionListener, MouseWheelListener {

		private ImageProcessor ip;
		private ImagePlus imp;
		GLCanvas canvasGl;

		public Renderer(GLCanvas canvas) {
			canvasGl=canvas;
		}

		public void display(GLAutoDrawable gLDrawable) {
			final GL2 gl = gLDrawable.getGL().getGL2();
			gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
			gl.glLoadIdentity();

			gl.glTranslatef(transx, transy, transz);
			gl.glRotatef(rotatx, rotaty, rotatz, rotatzz);
			imp = WindowManager.getCurrentImage();
			if (imp != null) {

				ip = imp.getProcessor();

				ip = ip.convertToByte(true);

			}

			float disx = -(Field.getWidth() - 1f);
			float disy = Field.getHeight() - 1f;
			float disz = tiefe - 1f;
			for (int z = 0; z < tiefe; z++) {
				for (int y = 0; y < Field.getHeight(); y++) {
					for (int x = 0; x < Field.getWidth(); x++) {

						gl.glTranslatef(disx, disy, disz);

						if (z == 1) {
							DrawCube3(gl, Field.getState(x, y));// draw the
							// value from
							// the array

						} else if (z == 0) {

							if (imp != null) {

								int pixel = ip.getPixel(x, y);
								float red = ip.getColorModel().getRed(pixel);
								float green = ip.getColorModel().getGreen(pixel);
								float blue = ip.getColorModel().getBlue(pixel);

								DrawCubeSoil(gl, red, green, blue);

							}

						}

						disx = 2.0f;
						disy = 0.0f;
						disz = 0.0f;

					}

					disy = -2.0f;
					disx = disx - ((Field.getWidth() * 2));

				}
				disy = disy + ((Field.getHeight() * 2));
				disz = +layerdistance;
			}

			disz = disz + (Field.getHeight() * 2);

			gl.glFlush();

		}

		public void displayChanged(GLDrawable gLDrawable, boolean modeChanged, boolean deviceChanged) {
		}

		public void init(GLAutoDrawable gLDrawable) {
			final GL2 gl = gLDrawable.getGL().getGL2();

			gl.glClearColor(0.438f, 0.599f, 1.0f, 0.5f);
			gl.glClearDepth(1.0f);

			gl.glDepthFunc(GL.GL_LEQUAL);

			gl.glDepthFunc(GL.GL_LEQUAL);
			gl.glEnable(GL.GL_DEPTH_TEST);
			gl.glShadeModel(GL_SMOOTH);
			gl.glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);

			gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
			gl.glEnable(GL.GL_BLEND);
			gl.glEnable(GL.GL_LINE_SMOOTH);

			/*
			 * gl.glLightfv(GL.GL_LIGHT1, GL.GL_DIFFUSE, new float[] { 1.0f,
			 * 1.0f, 1.0f, 1.0f }, 0); gl.glLightfv(GL.GL_LIGHT1, GL.GL_AMBIENT,
			 * new float[] { 0.5f, 0.5f, 0.5f, 1.0f }, 0);
			 * gl.glLightfv(GL.GL_LIGHT1, GL.GL_POSITION, new float[] { -50.f,
			 * 50.0f, 100.0f, 1.0f }, 0); gl.glEnable(GL.GL_LIGHT1);
			 * gl.glEnable(GL.GL_LIGHTING); gl.glEnable(GL.GL_COLOR_MATERIAL);
			 */
			gl.glColorMaterial(GL.GL_FRONT, GL_AMBIENT_AND_DIFFUSE);
			
			
			
			
			//((Component) gLDrawable).addKeyListener(this);
			canvasGl.addKeyListener(this);
			canvasGl.addMouseListener(this);
			canvasGl.addMouseMotionListener(this);
			canvasGl.addMouseWheelListener(this);
			
			
			
			

		}

		public void reshape(GLAutoDrawable gLDrawable, int x, int y, int width, int height) {
			final GL2 gl = gLDrawable.getGL().getGL2();
			final GLU glu = new GLU();

			if (height <= 0)
				height = 1;
			final float h = (float) width / (float) height;
			gl.glViewport(0, 0, width, height);
			gl.glMatrixMode(GL_PROJECTION);
			gl.glLoadIdentity();
			glu.gluPerspective(45.0f, h, 1.0, 1500.0f);
			gl.glMatrixMode(GL_MODELVIEW);
			gl.glLoadIdentity();
		}

		public void keyPressed(KeyEvent e) {
			int keyCode = e.getKeyCode();
			switch (keyCode) {
			case KeyEvent.VK_LEFT:
				transx--;
				break;
			case KeyEvent.VK_RIGHT:
				transx++;
				break;
			case KeyEvent.VK_UP:
				transy--;
				break;
			case KeyEvent.VK_DOWN:
				transy++;
				break;

			default:
				break;
			}

		}

		public void keyReleased(KeyEvent e) {
		}

		public void keyTyped(KeyEvent e) {

		}

		public void mousePressed(MouseEvent e) {
			prevMouseX = e.getX();
			prevMouseY = e.getY();
			if ((e.getModifiers() & e.BUTTON3_MASK) != 0) {
				mouseRButtonDown = true;
			}
		}

		public void mouseReleased(MouseEvent e) {
			if ((e.getModifiers() & e.BUTTON3_MASK) != 0) {
				mouseRButtonDown = false;
			}
		}

		public void mouseClicked(MouseEvent e) {
		}

		// Methods required for the implementation of MouseMotionListener
		public void mouseDragged(MouseEvent e) {
			int x = e.getX();
			int y = e.getY();
			Dimension size = e.getComponent().getSize();

			float thetaY = 360.0f * ((float) (x - prevMouseX) / (float) size.width);
			float thetaX = 360.0f * ((float) (prevMouseY - y) / (float) size.height);

			prevMouseX = x;
			prevMouseY = y;

			rotatx -= thetaX;
			transz += thetaY;
		}

		public void mouseMoved(MouseEvent e) {

		}

		public void mouseWheelMoved(MouseWheelEvent e) {
			layerdistance = layerdistance + e.getWheelRotation();

		}

		public void displayChanged(GLAutoDrawable arg0, boolean arg1, boolean arg2) {

		}

		public void mouseEntered(MouseEvent arg0) {

		}

		public void mouseExited(MouseEvent arg0) {

		}

		@Override
		public void dispose(GLAutoDrawable arg0) {
			// TODO Auto-generated method stub
			
		}

	}

	private static void DrawCube3(GL2 gl, int rg) {
		int v = rg;
		if (CurrentStates.getStateList().size() > v) {
			RGB = CurrentStates.getRGBGL(v);

			gl.glBegin(GL_QUADS); // Draw A Quad

			gl.glColor3f(RGB[0], RGB[1], RGB[2]);
			gl.glVertex3f(1.0f, 1.0f, 1.0f);
			gl.glVertex3f(-1.0f, 1.0f, 1.0f);
			gl.glVertex3f(-1.0f, -1.0f, 1.0f);
			// (Front)
			gl.glVertex3f(1.0f, -1.0f, 1.0f);

			gl.glEnd();
		}
	}

	private static void DrawCubeSoil(GL2 gl, float re, float gr, float bl) {
		float r = 0;
		float g = 0;
		float b = 0;

		r = (float) (re / 255);
		g = (float) (gr / 255);
		b = (float) (bl / 255);

		gl.glBegin(GL_QUADS);

		gl.glColor3f(r, g, b);
		gl.glVertex3f(1.0f, 1.0f, 1.0f);
		gl.glVertex3f(-1.0f, 1.0f, 1.0f);
		gl.glVertex3f(-1.0f, -1.0f, 1.0f);
		gl.glVertex3f(1.0f, -1.0f, 1.0f);

		gl.glEnd();
	}

}
