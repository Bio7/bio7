/*******************************************************************************
 * Copyright (c) 2007-2025 M. Austenfeld
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

/*
 *This class uses and also consists of slightly changed sources of Andrew Davison and his book "Pro Java 6 3D Game Development"
 *http://www.apress.com/book/view/1590598172.
 *
 *The Arcball rotation interface sources were created by 
 *Pepijn Van Eeckhoudt.
 */

package com.eco.bio7.spatial;

import static com.jogamp.opengl.GL.GL_GREATER;
import static com.jogamp.opengl.GL2ES1.GL_ALPHA_TEST;
import static com.jogamp.opengl.GL2ES1.GL_LIGHT_MODEL_TWO_SIDE;
import static com.jogamp.opengl.GL2ES1.GL_POINT_SMOOTH;
import static com.jogamp.opengl.fixedfunc.GLLightingFunc.GL_DIFFUSE;
import static com.jogamp.opengl.fixedfunc.GLLightingFunc.GL_LIGHT0;
import static com.jogamp.opengl.fixedfunc.GLLightingFunc.GL_LIGHT1;
import static com.jogamp.opengl.fixedfunc.GLLightingFunc.GL_LIGHT2;
import static com.jogamp.opengl.fixedfunc.GLLightingFunc.GL_LIGHT3;
import static com.jogamp.opengl.fixedfunc.GLLightingFunc.GL_LIGHTING;
import static com.jogamp.opengl.fixedfunc.GLLightingFunc.GL_POSITION;
import static com.jogamp.opengl.fixedfunc.GLLightingFunc.GL_SMOOTH;
import static com.jogamp.opengl.fixedfunc.GLMatrixFunc.GL_MODELVIEW;
import static com.jogamp.opengl.fixedfunc.GLMatrixFunc.GL_PROJECTION;

import java.awt.Font;
import java.awt.image.BufferedImage;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.compile.Model;
import com.eco.bio7.loader3d.ModelDimensions;
import com.eco.bio7.loader3d.MeshModel;
import com.eco.bio7.methods.Compiled;
import com.eco.bio7.spatial.preferences.Preferences3d;
import com.eco.bio7.util.Util;
import com.jogamp.nativewindow.swt.SWTAccessor;
import com.jogamp.nativewindow.util.Point;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.newt.swt.NewtCanvasSWT;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.awt.TextRenderer;
import com.jogamp.opengl.util.gl2.GLUT;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;

public class SpatialStructure implements KeyListener, MouseListener {
	private static SpatialStructure SpatialStructureInstance = null;
	private initRenderer renderer;
	private NewtCanvasSWT canvas;
	private com.eco.bio7.spatial.FPSAnimator animator;
	private GLU glu;
	private GL2 gl;
	private com.jogamp.opengl.util.gl2.GLUT glut;
	private int canvasHeight, canvasWidth;
	private float scaleValue = 1.0f; // scale for the terrain.
	private boolean keys[] = new boolean[256];
	private int prevMouseX = 0;
	private int prevMouseY = 0;
	private float sizeX = 1000;
	private float sizeY = 1000;
	private float sizeZ = 1000;
	private int mouseButton;
	private float transz = 0.0f;
	private float transx = 0.0f;
	private float transy = 0.0f;
	private float rotatx = 0.0f;
	private float rotaty = 0.0f;
	private float rotatz = 0.0f;
	private boolean doRotateZ;
	private boolean doRotateX;
	private boolean doRotateY;
	public ImagePlus imp;
	public ImageProcessor ip;
	public SpatialLoader space = null;
	private IPreferenceStore store;
	private int dox;
	private int doy;
	private static final int DEFAULT_VIEW_DISTANCE = 2000;
	private int doz = DEFAULT_VIEW_DISTANCE;
	// The texture size the view distance was fitted to (0 = no texture)
	private float fitTextureWidth = 0;
	private float fitTextureHeight = 0;
	private float fitTextureDepth = 0;
	/* The volume rendering of ImageJ stacks! */
	private final VolumeRenderer volume = new VolumeRenderer();
	/* The iso surface of ImageJ stacks! */
	private final IsoSurface isoSurface = new IsoSurface();
	private Runnable modelListener;
	/* The clipping of the volume and the iso surface! */
	private final Clipping clipping = new Clipping();
	private boolean clippingDrag = false;
	/* The measurements of the iso surface and the volume */
	private final Measurement3d measurement;
	{
		/* The mask (cuts) of the clipping is applied to the data! */
		isoSurface.setClipping(clipping);
		volume.setClipping(clipping);
		measurement = new Measurement3d(isoSurface, volume, clipping);
	}
	private int doxv;
	private int doyv = 0;
	private int dozv;
	/* The model of a mesh file (OBJ, STL, PLY or OFF, see the Model tab) */
	public MeshModel model = null;
	/* The error of the last load of the model or null */
	String modelError;
	private static boolean reloadModel;// For the model reload!
	private static boolean walkthrough = false;
	private static long period = 1000000000L;
	static double SPEED = 2.0; // For camera movement
	private final static double lookAtDist = 100.0;
	private final static double Z_POS = 9.0;
	private final static double angleIncrement = 5.0;
	private final static double heightStep = 1.0;
	private double xCameraPosition, yCameraPosition, zCameraPosition;
	private double xLookAt, yLookAt, zLookAt;
	public double xCamSplit = 0, yCamSplit = 0, zCamSplit = 0;
	public double xSplitLookAt = 0, ySplitLookAt = 0, zSplitLookAt = 0;
	private double xStep, zStep;
	private double viewAngle;
	private float viewAngleY;
	private double yStep;
	private boolean mousePressed;
	private boolean forward;
	private boolean backward;
	private boolean up;
	private boolean down;
	public boolean heightMap;
	private boolean loadModel;
	private boolean splitView = false;
	public static HeightMaps hMap;
	public Textures textures;
	private boolean fromOpenGl;
	private long timeBefore = 0;
	private long timeAfter = 0;
	private long timeDiff = 0;
	private long timeCounter = 0;
	private int frameCounter;
	private long fpsTimeAfter;
	private long fpsTimeCounter;
	private long fpsTimeBefore;
	public static boolean step;
	private Matrix4f LastRot = new Matrix4f();
	public Matrix4f ThisRot = new Matrix4f();
	private final Object matrixLock = new Object();
	private float[] matrix = new float[16];
	private ArcBall arcBall;
	public boolean light1 = true;
	public boolean light2;
	public boolean light3;
	public boolean light4;
	public float lightPos1[] = { 0.0f, 1000.0f, 1000.0f, 1.0f };
	public float lightPos2[] = { 1000.0f, 1000.0f, -2000.0f, 1.0f };
	public float lightPos3[] = { 0.0f, 1000.0f, 1000.0f, 1.0f };
	public float lightPos4[] = { 1000.0f, 1000.0f, -2000.0f, 1.0f };
	public boolean showAxes = true;
	public boolean showQuad = true;
	public float gridSize = 50;
	public boolean showGrid = true;
	public boolean layerYZGrid = false;
	public boolean layerXZGrid = false;
	public boolean layerXYGrid = true;
	/* The grids at the far sides of the box (z max, y max, x max) */
	public boolean layerXYGridMax = false;
	public boolean layerXZGridMax = false;
	public boolean layerYZGridMax = false;
	/* A plot view: the grids at all sides of the box (a cage) */
	public boolean plotView = false;
	/* The grid planes of the plot view (behind the data) */
	public boolean plotGridXY = false;
	public boolean plotGridXZ = true;
	public boolean plotGridYZ = true;
	/* The axis orientation overlay in the top right corner */
	public boolean showAxisGizmo = true;
	private final AxisGizmo axisGizmo = new AxisGizmo();
	/* The pixels per point of the canvas (HiDPI, updated when the canvas is resized) */
	private volatile float canvasPixelScale = 1;
	public boolean hold; // Halt the execution;
	public long fpsTimeDiff2;
	public int fpsTimeDiff;
	public int tempFrameCounter;
	public boolean showFramerate = false;
	public long timeDiffTemp;
	public long ufpsTimeCounter;
	public long timeDiffCounter;
	public long timeCounterTemp;
	public long upscount;
	public long upsTimeCounter;
	protected boolean showUpdateFramerate = false;
	public boolean flythrough = false;
	private SpatialScene scene;
	public boolean setup = false;
	public boolean localLight = true;
	protected boolean customCamera = false;
	public boolean stereo = false;
	private static double customCameraCoordinates[] = { 0, 0, 0, 0, 0, 0 };
	private int anaglyphMiddle = -10, anaglyphOffset = 0;
	public boolean backgroundColour = false;
	public float colorBackground[] = { 0.438f, 0.599f, 1.0f, 0.5f };
	public float colorLines[] = { 0.438f, 0.599f, 1.0f, 0.5f };
	public double objModel1Position[] = { 0, 0, 0 };// The *.obj model position
	public double objModel1Rotation[] = { 0, 0, 0 };// The *.obj model rotation
	public double objModel1Scale = 1;// The scale of the model
	/* The window size! */
	private int width;
	private int height;
	protected boolean takeScreenshot = false;
	public boolean renderToImageJ;
	public ImageStack s;
	public ImagePlus plus;
	public boolean createBufferedImage;
	private SpatialView view;
	private static GLAutoDrawable drawable_;
	private TextRenderer textRenderer;
	private boolean stopMovement = false;
	protected boolean lightenedHeightMap = true;
	protected boolean lightenedObjModel = true;
	private Preferences3d pref3d;
	public int splitOriginX;
	public int splitOriginY;
	public int splitHeight;
	public int splitWidth;
	/* The vertical speed of the flythrough (see the Camera tab) */
	public double heightSpeed = 0.1;
	private boolean isSplitPanelDrawing;
	public double spatialExtent = 100000.0f;// The perspective variable
	private static int renderImageTo = 500;
	private static int multiplyDragCameraSpeed = 1;
	private int fixedFps = 60;
	public GLWindow glWindow;
	private AxisLabelsRenderer axisLabelsRenderer;
	/* The renderer of a compiled model with an own profile (without the Bio7 scene)! */
	private final ModelRenderer modelRenderer = new ModelRenderer();
	/* The profile of the compiled model (null = the Bio7 scene) */
	private volatile String modelProfile;
	/* A profile which is not available (the Bio7 scene is used) */
	private volatile String rejectedProfile;
	private volatile boolean profileSwitchPending;

	SpatialStructure(SpatialView view) {
		this.view = view;
		SpatialStructureInstance = this;
		renderer = new initRenderer();
		arcBall = new ArcBall(800.0f, 600.0f);

		/*
		 * GLProfile glprofile = GLProfile.getDefault(); canvas = new GLCanvas(new
		 * GLCapabilities(glprofile)); canvas.setSize(new Dimension(canvasWidth,
		 * canvasHeight));
		 */

		GLProfile glprofile = getSceneProfile();

		glWindow = GLWindow.create(new GLCapabilities(glprofile));
		// canvas = new GLCanvas(new GLCapabilities(glprofile));

		canvas = new NewtCanvasSWT(view.top, SWT.NO_BACKGROUND, glWindow);
		canvas.setSize(canvasWidth, canvasHeight);
		/* The HiDPI scale (NEWT sizes are pixels on some platforms) for the overlays! */
		canvas.addListener(SWT.Resize, _ -> updateCanvasPixelScale());
		/*
		 * Workaround for HiDPI scaling on Windows: NewtCanvasSWT sizes the NEWT window
		 * with the SWT client area in points but NEWT on Windows expects pixels. Our
		 * listeners are called after the NewtCanvasSWT listeners and correct the size!
		 */
		if (Util.getOS().equals("Windows")) {
			Listener hiDpiSizeListener = new Listener() {
				public void handleEvent(Event event) {
					updateHiDpiWindowSize();
				}
			};
			canvas.addListener(SWT.Resize, hiDpiSizeListener);
			canvas.addListener(SWT.Paint, hiDpiSizeListener);
		}
		// canvas.setData(gldata);

		// glWindow.setSize(new Dimension(canvasWidth, canvasHeight));

		glWindow.addGLEventListener(renderer);

		canvas.setFocus();
		glWindow.addKeyListener(this);
		glWindow.addMouseListener(this);
		// canvas.addMouseMotionListener(this);
		// canvas.addMouseWheelListener(this);

		/* Create the custom preferences store! */
		pref3d = new Preferences3d();

		store = Bio7Plugin.getDefault().getPreferenceStore();
		/* Default value for the fps Animator! */
		store.setDefault("fps", true);
		store.setDefault("spatialExtent", 100000.0);
		/* The plot view (the grids at all sides of the box) from the preferences! */
		store.setDefault("plotView", false);
		plotView = store.getBoolean("plotView");
		store.setDefault("showAxisGizmo", true);
		showAxisGizmo = store.getBoolean("showAxisGizmo");
		/* Get the split view coordinates from the preferences! */
		pref3d.getSplitViewCameraValues();

		/* Get the model coordinates from the preferences! */
		pref3d.getModelValues();
		/* Load the default spatial extent of the space! */
		spatialExtent = store.getDouble("spatialExtent");
		hMap = new HeightMaps();
		textures = new Textures(hMap);
		textures.createNewTexture = true;

		initViewerPosn();
	}

	/**
	 * Returns the OpenGL profile of the Bio7 scene: the newest compatibility
	 * profile with the fixed function pipeline (e.g., OpenGL 4 on Windows and
	 * Linux, OpenGL 2.1 on macOS) or the default profile (see the preference
	 * "openglMaxCompatible").
	 * 
	 * @return the profile
	 */
	public static GLProfile getSceneProfile() {
		IPreferenceStore prefs = Bio7Plugin.getDefault().getPreferenceStore();
		prefs.setDefault("openglMaxCompatible", true);
		if (prefs.getBoolean("openglMaxCompatible")) {
			try {
				GLProfile profile = GLProfile.getMaxFixedFunc(true);
				if (profile != null && profile.isGL2()) {
					return profile;
				}
			} catch (GLException e) {
				System.out.println("No compatibility profile available: " + e.getMessage());
			}
		}
		return GLProfile.getDefault();
	}

	/* The profile requested by the compiled model (null = the Bio7 scene)! */
	private String requestedProfile() {
		Model eco = Compiled.getModel();
		if (eco == null) {
			return null;
		}
		String profile;
		try {
			profile = eco.getGLProfile();
		} catch (RuntimeException e) {
			e.printStackTrace();
			return null;
		}
		return profile != null && !profile.equals(rejectedProfile) ? profile : null;
	}

	/*
	 * Called in the OpenGL thread: true if the compiled model requests another
	 * profile (the OpenGL window is created again in the SWT thread)!
	 */
	private boolean profileChanged() {
		if (profileSwitchPending) {
			return true;
		}
		String requested = requestedProfile();
		if (java.util.Objects.equals(requested, modelProfile)) {
			return false;
		}
		profileSwitchPending = true;
		Display.getDefault().asyncExec(() -> switchProfile(requested));
		return true;
	}

	/* Creates the OpenGL window with the profile (null = the Bio7 scene)! */
	private void switchProfile(String profileName) {
		try {
			if (canvas.isDisposed()) {
				return;
			}
			GLProfile profile;
			if (profileName == null) {
				profile = getSceneProfile();
			} else {
				try {
					profile = GLProfile.get(profileName);
				} catch (GLException e) {
					/* Not available: the Bio7 scene */
					rejectedProfile = profileName;
					System.out.println("The OpenGL profile " + profileName + " of the model is not available: " + e.getMessage());
					return;
				}
			}
			GLWindow oldWindow = glWindow;
			if (animator != null) {
				animator.remove(oldWindow);
			}
			GLWindow window = GLWindow.create(new GLCapabilities(profile));
			window.addGLEventListener(profileName == null ? renderer : modelRenderer);
			window.addKeyListener(this);
			window.addMouseListener(this);
			modelProfile = profileName;
			glWindow = window;
			canvas.setNEWTChild(window);
			oldWindow.destroy();
			if (animator != null) {
				animator.add(window);
			}
			System.out.println("3D view: OpenGL profile " + profile.getName()
					+ (profileName == null ? " (Bio7 scene)" : " (model without the Bio7 scene)"));
		} finally {
			profileSwitchPending = false;
		}
	}

	/**
	 * Creates the OpenGL context of the Bio7 scene again, e.g., after the
	 * preference "openglMaxCompatible" was changed (not if a compiled model has
	 * an own profile).
	 */
	public void recreateSceneContext() {
		if (modelProfile == null && !profileSwitchPending) {
			profileSwitchPending = true;
			Display.getDefault().asyncExec(() -> switchProfile(null));
		}
	}

	/**
	 * Returns true if the compiled model renders with an own profile without the
	 * Bio7 scene (see Model.getGLProfile).
	 * 
	 * @return true if the model has an own profile
	 */
	public boolean isModelProfile() {
		return modelProfile != null;
	}

	/* Unbinds the shader program, the vertex array and the buffers of a model! */
	private static void resetModelState(GL gl) {
		if (gl.isGL2ES2()) {
			GL2ES2 es2 = gl.getGL2ES2();
			es2.glUseProgram(0);
			if (gl.isFunctionAvailable("glBindVertexArray")) {
				gl.getGL2ES3().glBindVertexArray(0);
			}
		}
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
		gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
		gl.glActiveTexture(GL.GL_TEXTURE0);
	}

	/*
	 * The renderer of a compiled model with an own profile (see
	 * Model.getGLProfile): only the model draws (without the Bio7 scene)!
	 */
	private class ModelRenderer implements GLEventListener {

		public void init(GLAutoDrawable drawable) {
			drawable.getGL().setSwapInterval(0);
			drawable_ = drawable;
			Model eco = Compiled.getModel();
			if (eco != null) {
				try {
					eco.setup(drawable);
				} catch (RuntimeException e) {
					e.printStackTrace();
				}
			}
		}

		public void display(GLAutoDrawable drawable) {
			if (profileChanged()) {
				return;
			}
			/* The step timing of the control panel (canStep)! */
			renderer.getFps(drawable);
			Model eco = Compiled.getModel();
			if (eco == null) {
				return;
			}
			try {
				if (setup) {
					setup = false;
					eco.setup(drawable);
				}
				if (fromOpenGl) {
					eco.run(drawable);
				}
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
		}

		public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
			drawable.getGL().glViewport(0, 0, width, Math.max(1, height));
		}

		public void dispose(GLAutoDrawable drawable) {
		}
	}

	private void updateCanvasPixelScale() {
		if (canvas.isDisposed()) {
			return;
		}
		Rectangle points = canvas.getClientArea();
		Rectangle pixels = SWTAccessor.getClientAreaInPixels(canvas);
		if (points.width > 0 && pixels.width > 0) {
			canvasPixelScale = pixels.width / (float) points.width;
		}
	}

	/* Resize the NEWT window to the client area of the canvas in pixels! */
	private void updateHiDpiWindowSize() {
		/*
		 * Only resize when the NEWT window is embedded as a child. During a reparent
		 * operation the NEWT window is temporarily a top-level window and a resize would
		 * show it in the taskbar!
		 */
		if (canvas.isDisposed() || glWindow.isFullscreen() || !glWindow.isNativeValid() || glWindow.getParent() == null) {
			return;
		}
		Rectangle pixelArea = SWTAccessor.getClientAreaInPixels(canvas);
		if (pixelArea.width > 0 && pixelArea.height > 0 && (glWindow.getWidth() != pixelArea.width || glWindow.getHeight() != pixelArea.height)) {
			glWindow.setSize(pixelArea.width, pixelArea.height);
		}
	}

	public class initRenderer implements GLEventListener {

		public void init(GLAutoDrawable drawable) {

			gl = drawable.getGL().getGL2();
			/* Switch off to wait for the monitor to refresh! */
			gl.setSwapInterval(0);
			glu = new GLU();
			glut = new GLUT();
			drawable_ = drawable;
			width = drawable.getSurfaceWidth();
			height = drawable.getSurfaceHeight();

			setSplitScreenSize(0, 0, width / 3, height / 3);
			/*
			 * Reload the textures and objects in the custom method(avoid crashes)thread!
			 */
			GlSetup();
			Font font = new Font("Verdana", Font.BOLD, 12);
			textRenderer = new TextRenderer(font, true, false);
			space = new SpatialLoader(gl);
			scene = new SpatialScene();
			/* A new context: the axis numbers stay enabled, a (stored) plot view with the numbers */
			boolean numbers = axisLabelsRenderer != null && axisLabelsRenderer.isEnabled();
			axisLabelsRenderer = new AxisLabelsRenderer(0, 1000, 0, 1000, 0, 1000);
			axisLabelsRenderer.setEnabled(numbers || plotView);
			/* A new context: the volume texture is created again! */
			volume.contextLost();
			isoSurface.contextLost();
			textures.createTexture(gl,drawable);
			addLight(gl);
			/* Load the.obj model from the path! */
			if (loadModel) {
				space.loadObjModel();// loads the model from an object file!
				notifyModelListener();
			}

			gl.glClearColor(colorBackground[0], colorBackground[1], colorBackground[2], colorBackground[3]);
			gl.glClearDepth(1.0f);

			gl.glDepthFunc(GL2.GL_LEQUAL);
			gl.glEnable(GL2.GL_DEPTH_TEST);
			gl.glShadeModel(GL_SMOOTH);
			gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_NICEST);

			gl.glEnable(GL2.GL_BLEND);
			gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
			/* Enable the alpha test */
			gl.glEnable(GL_ALPHA_TEST);
			gl.glAlphaFunc(GL_GREATER, 0); // Render if alpha > 0

			gl.glLightModelf(GL_LIGHT_MODEL_TWO_SIDE, 0);

			// Set the lines are antialiased
			gl.glEnable(GL_POINT_SMOOTH);
			gl.glEnable(GL2.GL_LINE_SMOOTH);

			gl.glHint(GL2.GL_LINE_SMOOTH_HINT, GL2.GL_NICEST);
			gl.glLineWidth(1.0f);

			if (animator == null) {
				/* Default fps to avoid unnecessary loops! */
				if (pref3d.isFixedFps()) {

					Display display = PlatformUI.getWorkbench().getDisplay();
					display.syncExec(new Runnable() {

						public void run() {
							fixedFps = store.getInt("fixedFps");
						}
					});
					animator = new com.eco.bio7.spatial.FPSAnimator(drawable, fixedFps, true);

				} else {
					animator = new com.eco.bio7.spatial.FPSAnimator(drawable, 60, true);
				}
				//

				animator.start();

			}

		}

		private void addLight(GL2 gl)

		{

			gl.glEnable(GL_LIGHTING);
			gl.glEnable(GL_LIGHT0);

			float[] whiteLight = { 1.0f, 1.0f, 1.0f, 1.0f }; // bright white

			gl.glLightfv(GL_LIGHT0, GL_DIFFUSE, whiteLight, 0);

			gl.glLightfv(GL_LIGHT1, GL_DIFFUSE, whiteLight, 0);
			gl.glLightfv(GL_LIGHT2, GL_DIFFUSE, whiteLight, 0);
			gl.glLightfv(GL_LIGHT3, GL_DIFFUSE, whiteLight, 0);

			lightPosition(gl);
		}

		public void switchLight(GL2 gl) {
			if (light1) {
				gl.glEnable(GL_LIGHT0);
			} else {
				gl.glDisable(GL_LIGHT0);
			}
			if (light2) {
				gl.glEnable(GL_LIGHT1);
			} else {
				gl.glDisable(GL_LIGHT1);
			}
			if (light3) {
				gl.glEnable(GL_LIGHT2);
			} else {
				gl.glDisable(GL_LIGHT2);
			}
			if (light4) {
				gl.glEnable(GL_LIGHT3);
			} else {
				gl.glDisable(GL_LIGHT3);
			}

		}

		private void lightPosition(GL2 gl) {
			gl.glLightfv(GL_LIGHT0, GL_POSITION, lightPos1, 0);
			gl.glLightfv(GL_LIGHT1, GL_POSITION, lightPos2, 0);
			gl.glLightfv(GL_LIGHT2, GL_POSITION, lightPos3, 0);
			gl.glLightfv(GL_LIGHT3, GL_POSITION, lightPos4, 0);
		}

		public void upsrate(GLAutoDrawable drawable) {

			textRenderer.beginRendering(drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
			textRenderer.setColor(0, 0, 0, 1);

			textRenderer.draw("Step Fps: " + timeCounterTemp, 0, 20);

			textRenderer.endRendering();
		}

		/* The rotation is handled here! */
		void reset() {
			synchronized (matrixLock) {
				LastRot.setIdentity(); // Reset Rotation
				ThisRot.setIdentity(); // Reset Rotation
			}
		}

		void startDrag(Point MousePt) {
			synchronized (matrixLock) {
				LastRot.set(ThisRot);
			}
			arcBall.click(MousePt);
		}

		void drag(Point MousePt) {
			Quat4f ThisQuat = new Quat4f();

			arcBall.drag(MousePt, ThisQuat);
			// Rotation as quaternion
			synchronized (matrixLock) {
				ThisRot.setRotation(ThisQuat);

				ThisRot.mul(ThisRot, LastRot);

			}
		}

		public void display(GLAutoDrawable drawable) {
			/* The compiled model requests an own profile (without the Bio7 scene)! */
			if (profileChanged()) {
				return;
			}
			gl = drawable.getGL().getGL2();
			gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
			if (backgroundColour) {

				gl.glClearColor(colorBackground[0], colorBackground[1], colorBackground[2], colorBackground[3]);

				backgroundColour = false;

			}
			synchronized (matrixLock) {
				ThisRot.get(matrix);
			}

			if (localLight == false) {
				lightPosition(gl);

				switchLight(gl);
			}

			width = drawable.getSurfaceWidth();
			height = drawable.getSurfaceHeight();

			arcBall.setBounds((float) width, (float) height);

			gl.glScalef(scaleValue, scaleValue, scaleValue);

			if (splitView) {

				getFps(drawable);
				gl.glViewport(0, 0, width, height);

				gl.glLoadIdentity();
				isSplitPanelDrawing = false;
				views(drawable, 1);

				gl.glEnable(GL2.GL_SCISSOR_TEST);
				gl.glScissor(splitOriginX, splitOriginY, splitWidth, splitHeight);

				gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
				gl.glDisable(GL2.GL_SCISSOR_TEST);

				gl.glViewport(splitOriginX, splitOriginY, splitWidth, splitHeight);

				gl.glLoadIdentity();
				isSplitPanelDrawing = true;
				views(drawable, 2);
				/* Render the results to an image or to images! */
				// renderImages();

			}

			else {// The view - not splitted!

				if (stereo) {

					gl.glColorMask(true, false, false, true);
					gl.glViewport(anaglyphMiddle - anaglyphOffset, 0, width, height);
					gl.glScissor(anaglyphMiddle - anaglyphOffset, 0, width, height);
					gl.glLoadIdentity();
					views(drawable, 0);

					gl.glColorMask(false, true, true, true);
					gl.glViewport(0, 0, width, height);
					gl.glScissor(0, 0, width, height);
					gl.glLoadIdentity();
					views(drawable, 0);

				} else {
					gl.glColorMask(true, true, true, true);
					gl.glViewport(0, 0, width, height);
					gl.glLoadIdentity();

					views(drawable, 0);
				}
				/* Render the results to an image or to images! */
				// renderImages();

				getFps(drawable);

			}
			/* The axis orientation overlay in the top right corner (like in Blender)! */
			if (showAxisGizmo) {
				axisGizmo.render(gl, width, height, canvasPixelScale);
			}

		}

		/*
		 * private void renderImages() {
		 * 
		 * If true takes a screenshot! if (takeScreenshot) { if
		 * (CanvasView.getCanvas_view() != null) { takeScreenshot(); takeScreenshot =
		 * false; } }
		 * 
		 * if (createBufferedImage) { if (CanvasView.getCanvas_view() != null) {
		 * 
		 * BufferedImage im = null;// = Screenshot.readToBufferedImage(width, height);
		 * imp = new ImagePlus("Spatial", im); ip = imp.getProcessor(); s = new
		 * ImageStack(width, height);
		 * 
		 * s.addSlice(null, ip); s.addSlice(null, ip); plus = new ImagePlus("Stack", s);
		 * // We make it visible! plus.show(); // imp.show();
		 * 
		 * createBufferedImage = false; } }
		 * 
		 * if (renderToImageJ) {
		 * 
		 * renderToImageJ();
		 * 
		 * } }
		 */

		private void views(GLAutoDrawable drawable, int view) {

			viewPerspective(view);// According to selected view mode change
			// the camera!

			gl.glTranslatef(transx, transy, transz);
			gl.glPushMatrix();
			gl.glMultMatrixf(matrix, 0);

			gl.glRotatef(rotatx, 1.0f, 0.0f, 0.0f);

			gl.glRotatef(rotaty, 0.0f, 1.0f, 0.0f);

			gl.glRotatef(rotatz, 0.0f, 0.0f, 1.0f);
			gl.glScalef(scaleValue, scaleValue, scaleValue);// here you can
			/*
			 * In octant mode rotate around the center of the grid axes (with a texture
			 * around the center of the image)!
			 */
			double[] data = getTextureExtent() == null ? dataBounds : null;
			if (!walkthrough && data != null) {
				/* The data of a model (e.g., a script) in the center of the view, scaled to the default size */
				double scale = getDataScale();
				gl.glScaled(scale, scale, scale);
				gl.glTranslated(-(data[0] + data[1]) / 2, -(data[2] + data[3]) / 2, -(data[4] + data[5]) / 2);
			} else if (!walkthrough && !HeightMaps.isCartesian()) {
				float centerZ;
				float[] stack = getStackExtent();
				if (stack != null) {
					centerZ = stack[2] / 2.0f;
				} else {
					centerZ = getTextureExtent() != null ? 0 : sizeZ / 2.0f;
				}
				gl.glTranslatef(-getAxisSizeX() / 2.0f, -getAxisSizeY() / 2.0f, -centerZ);
			}
			if (stereo) {
				gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
				gl.glDepthMask(true);
			}

			if (localLight) {
				lightPosition(gl);
				switchLight(gl);
			}

			if (textures.createNewTexture) {

				textures.createTexture(gl,drawable);
				textures.createNewTexture = false;
			}
			fitViewToTexture();
			drawSpace(gl,drawable);

			/* Load the.obj model from the path! */
			if (reloadModel) {
				space.loadObjModel();
				reloadModel = false;
				notifyModelListener();
			}
			if (loadModel && model != null) {
				gl.glPushMatrix();
				gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

				gl.glTranslated(objModel1Position[0], objModel1Position[1], objModel1Position[2]);
				gl.glRotated(objModel1Rotation[0], 1, 0, 0);
				gl.glRotated(objModel1Rotation[1], 0, 1, 0);
				gl.glRotated(objModel1Rotation[2], 0, 0, 1);
				gl.glScaled(objModel1Scale, objModel1Scale, objModel1Scale);
				gl.glPushAttrib(GL2.GL_ENABLE_BIT);
				/* The scale also scales the normals (too bright or too dark lighting)! */
				gl.glEnable(GL2.GL_NORMALIZE);
				/* Switch if the model should be affected by the light! */
				if (lightenedObjModel == false) {
					gl.glDisable(GL_LIGHTING);
				} else {
					gl.glEnable(GL_LIGHTING);
				}
				model.draw(gl);
				gl.glPopAttrib();
				gl.glPopMatrix();

			}

			/* We enable a smooth movement! */
			if (walkthrough) {
				if (forward) {
					xCameraPosition += xStep * SPEED;
					zCameraPosition += zStep * SPEED;
					if (flythrough) {
						yCameraPosition += heightSpeed * Math.sin(Math.toRadians(viewAngleY));
						yLookAt += heightSpeed * Math.sin(Math.toRadians(viewAngleY));
					}

					verifyPosition();
				} else if (backward) {
					xCameraPosition -= xStep * SPEED;
					zCameraPosition -= zStep * SPEED;
					verifyPosition();
				} else if (up) {

					// stars
					// ceiling
					yCameraPosition += heightStep + heightSpeed;
					yLookAt += heightStep + heightSpeed;

					verifyPosition();
				} else if (down) {
					if ((yCameraPosition - heightStep) > 0) { // stay above
						// floor
						yCameraPosition -= heightStep + heightSpeed;
						yLookAt -= heightStep + heightSpeed;
					}
					verifyPosition();
				}
			}

			if (setup) {
				GlSetup();
				setup = false;
			}

			ecoMain();

			/*
			 * The iso surface and the transparent volume after the opaque objects, with
			 * the clipping (box and plane) and the slice of the cutting plane!
			 */
			boolean cartesian = HeightMaps.isCartesian();
			double[] bounds = clippingBounds(cartesian);
			/* For the mouse tools (eraser, plane) of the clipping */
			int[] dataSize = isoSurface.isDisplayed() ? isoSurface.getDataSize() : volume.getDataSize();
			if (dataSize != null) {
				clipping.setDataSize(dataSize[0], dataSize[1], dataSize[2]);
			}
			clipping.setView(gl, bounds, HeightMaps.isFlipVertically());
			if (clipping.isSlice() && bounds != null) {
				volume.renderSlice(gl, cartesian, clipping.planePolygon(bounds));
			}
			/* A cut mesh is closed (no clip planes needed)! */
			boolean cutMesh = clipping.isCutMesh();
			if (cutMesh) {
				isoSurface.render(gl, cartesian);
			}
			clipping.begin(gl, bounds);
			if (!cutMesh) {
				isoSurface.render(gl, cartesian);
			}
			volume.render(gl, cartesian);
			clipping.end(gl);
			if (bounds != null && clipping.isPlaneEnabled()) {
				clipping.drawOutline(gl, bounds);
			}
			measurement.render(gl, bounds, HeightMaps.isFlipVertically(), cartesian,
					glWindow.getSurfaceWidth() / (float) Math.max(1, glWindow.getWidth()));

			gl.glPopMatrix();
			gl.glFlush();

		}

		private void viewPerspective(int view) {
			if (walkthrough) {
				LastRot.setIdentity(); // Reset Rotation
				ThisRot.setIdentity(); // Reset Rotation
				ThisRot.get(matrix);
				rotatx = -90.0f;
				rotaty = 0.0f;
				rotatz = 0.0f;
				transx = 0.0f;
				transy = 0.0f;
				if (localLight) {
					lightPosition(gl);

					switchLight(gl);
				}
				/* Two viewports */
				if (view == 0) {
					glu.gluLookAt(xCameraPosition, yCameraPosition, zCameraPosition, xLookAt, yLookAt, zLookAt, 0, 1,
							0);
				} else if (view == 1) {
					glu.gluLookAt(xCameraPosition, yCameraPosition, zCameraPosition, xLookAt, yLookAt, zLookAt, 0, 1,
							0);
				}
				/* One viewport */
				else {// The overview!
					if (customCamera) {

						glu.gluLookAt(customCameraCoordinates[0], customCameraCoordinates[1],
								customCameraCoordinates[2], customCameraCoordinates[3], customCameraCoordinates[4],
								customCameraCoordinates[5], 0, 1, 0);
					} else {

						glu.gluLookAt(xCamSplit, yCamSplit, zCamSplit, xSplitLookAt, ySplitLookAt, zSplitLookAt, 0, 1,
								0);
					}
				}

			} else {// If it is not walk-through.

				if (view == 1) {
					glu.gluLookAt(dox, doy, doz, 0, 0, 0, 0, 1, 0);

				} else if (view == 2) {
					if (customCamera) {

						glu.gluLookAt(customCameraCoordinates[0], customCameraCoordinates[1],
								customCameraCoordinates[2], customCameraCoordinates[3], customCameraCoordinates[4],
								customCameraCoordinates[5], 0, 1, 0);
					} else {
						glu.gluLookAt(xCamSplit, yCamSplit, zCamSplit, xSplitLookAt, ySplitLookAt, zSplitLookAt, 0, 1,
								0);
					}

				} else {// Normal mode!

					if (customCamera == false) {// Normal camera without any
						// mode!
						glu.gluLookAt(dox, doy, doz, 0, 0, 0, 0, 1, 0);
					} else {
						// Custom camera without split!
						glu.gluLookAt(customCameraCoordinates[0], customCameraCoordinates[1],
								customCameraCoordinates[2], customCameraCoordinates[3], customCameraCoordinates[4],
								customCameraCoordinates[5], 0, 1, 0);
					}
				}
			}
		}

		private void drawQuad() {
			if (showQuad) {
				scene.drawQuadLines(gl, getSpatialStructureInstance());
			}
		}

		/*
		 * private void takeScreenshot() { BufferedImage im = null;//
		 * Screenshot.readToBufferedImage(width, height); ImagePlus imp = new
		 * ImagePlus("Spatial", im); imp.show();
		 * 
		 * }
		 */

		/*
		 * private void renderToImageJ() {
		 * 
		 * if (CanvasView.getCanvas_view() != null) {
		 * 
		 * if (s.getWidth() == width && s.getHeight() == height) { if (s.getSize() <
		 * renderImageTo) { BufferedImage im = null;//
		 * Screenshot.readToBufferedImage(width, height);
		 * 
		 * ImagePlus imp = new ImagePlus("Spatial", im);
		 * 
		 * ImageProcessor ip = imp.getProcessor();
		 * 
		 * s.addSlice(null, ip); StackWindow sw = (StackWindow)
		 * WindowManager.getCurrentWindow(); if (sw != null) { sw.updateSliceSelector();
		 * } else { renderToImageJ = false; Options3d.setRenderImageJFrames(true);
		 * 
		 * }
		 * 
		 * } else { renderToImageJ = false; Options3d.setRenderImageJFrames(true);
		 * 
		 * }
		 * 
		 * } else { renderToImageJ = false; Options3d.setRenderImageJFrames(true);
		 * 
		 * } } else { renderToImageJ = false; Options3d.setRenderImageJFrames(true);
		 * 
		 * } }
		 */

		public void GlSetup() {

			Model eco = Compiled.getModel();
			if (eco != null) {
				try {
					eco.setup(gl, glu, glut);
					eco.setup(drawable_);
				} catch (RuntimeException e) {

					e.printStackTrace();
				}
				resetModelState(gl);
			}

		}

		private void ecoMain() {
			if (fromOpenGl) {
				Model eco = Compiled.getModel();
				if (eco != null) {
					try {
						eco.run(gl, glu, glut);
						eco.run(drawable_);
					} catch (RuntimeException e) {
						eco = null;
						e.printStackTrace();
					}
					resetModelState(gl);

				}
			}
		}

		/* Some frame rate calculations! */
		public void getFps(GLAutoDrawable drawable) {
			fpsTimeBefore = System.nanoTime();
			timeBefore = fpsTimeBefore;
			/** **************************************** */

			timeDiff = timeBefore - timeAfter;

			upsTimeCounter += timeDiff;

			if (upsTimeCounter >= 1000000000L) {
				timeCounterTemp = upscount;
				upscount = 0;
				upsTimeCounter = 0;

			}

			if (timeCounter >= period) { //
				if (hold == true) {
					step = false;
				} else {
					step = true;
					upscount++;

				}

				timeCounter = 0;

			} else {
				timeCounter += timeDiff;
				step = false;

			}

			/* The text is not drawn with the profile of a model (no fixed function)! */
			boolean scene = modelProfile == null;
			if (showUpdateFramerate && scene) {
				upsrate(drawable);
			}
			/** ************************************************** */
			if (showFramerate && scene) {
				textRenderer.beginRendering(drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
				textRenderer.setColor(0, 0, 0, 1);

				fpsTimeDiff2 = fpsTimeBefore - fpsTimeAfter;

				if (fpsTimeCounter >= 1000000000L) { //

					textRenderer.draw("Fps: " + frameCounter, 0, 5);
					tempFrameCounter = frameCounter;
					fpsTimeCounter = 0;
					frameCounter = 0;

				} else {
					fpsTimeCounter += fpsTimeDiff2;
					frameCounter++;
					textRenderer.draw("Fps: " + tempFrameCounter, 0, 5);
				}

				textRenderer.endRendering();
			}
			fpsTimeAfter = System.nanoTime();
			/** ********************************** */
			timeAfter = fpsTimeAfter;
		}

		public void framerate(GLAutoDrawable drawable) {

		}

		public void reshape(GLAutoDrawable drawable, int xstart, int ystart, int width, int height) {

			final GL2 gl = drawable.getGL().getGL2();
			final GLU glu = new GLU();
			setSplitScreenSize(0, 0, width / 3, height / 3);
			if (height <= 0)
				height = 1;
			float h = (float) width / (float) height;
			gl.glViewport(0, 0, width, height);
			gl.glMatrixMode(GL_PROJECTION);
			gl.glLoadIdentity();
			glu.gluPerspective(45.0f, h, 1.0, spatialExtent);
			gl.glMatrixMode(GL_MODELVIEW);
			gl.glLoadIdentity();

		}

		public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
		}

		@Override
		public void dispose(GLAutoDrawable arg0) {
			// TODO Auto-generated method stub

		}

	}

	public BufferedImage getImage() {
		BufferedImage im = null;// Screenshot.readToBufferedImage(width, height);
		return im;
	}

	public void keyReleased(KeyEvent evt) {
		/*
		 * Changed for newt. Enabled auto repeat, see:
		 * https://jogamp.org/deployment/webstart/javadoc/jogl/javadoc/com/jogamp/newt/
		 * event/KeyListener.html#keyReleased(com.jogamp.newt.event.KeyEvent)!
		 */
		if (evt.isAutoRepeat()) {
			return;
		}
		SpatialEvents.setKeyReleased(true);
		SpatialEvents.setKeyReleaseEvent(evt);

		keys[evt.getKeyCode()] = false;
		/* Release the movement keys for the walkthrough! */
		doRotateX = false;
		doRotateY = false;
		doRotateZ = false;
		forward = false;
		backward = false;
		up = false;
		down = false;
	}

	public void keyPressed(KeyEvent evt) {
		SpatialEvents.setKeyPressed(true);
		SpatialEvents.setKeyPressEvent(evt);

		if (walkthrough == false) {
			int keyCode = evt.getKeyCode();
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
			case KeyEvent.VK_X:
				doRotateX = true;
				break;
			case KeyEvent.VK_Y:
				doRotateY = true;
				break;
			case KeyEvent.VK_Z:
				doRotateZ = true;
				break;
			case KeyEvent.VK_T:

				break;
			case KeyEvent.VK_A:
				dox++;
				break;
			case KeyEvent.VK_S:
				doy++;
				break;
			case KeyEvent.VK_D:
				doz = doz + 10;
				break;
			case KeyEvent.VK_F:
				doxv = doxv + 10;
				break;
			case KeyEvent.VK_G:
				doyv = doyv + 10;
				break;
			case KeyEvent.VK_H:
				dozv = dozv + 10;
			case KeyEvent.VK_ESCAPE:
				view.recreateGLCanvas();
				if (view.getFullscreen() != null) {
					view.getFullscreen().exit();
				}
				view.setFullscreen(null);

				break;
			case KeyEvent.VK_F2:
				if (view.getFullscreen() == null) {
					view.createFullscreen(0);
				}
				break;
			case KeyEvent.VK_F3:
				if (view.getFullscreen() == null) {
					view.createFullscreen(1);
				}
				break;

			default:

				break;
			}
		} else {
			processKey(evt);// The walkthrough keys!
		}
	}

	/**
	 * Resets the camera and the display settings of the 3D view to the defaults
	 * (like at the first start): the rotation, the zoom and the translation, the
	 * camera modes (walkthrough, split view, stereo), the size of the space, the grids, axes, box, plot
	 * view and the axis orientation overlay, the lights, the colors, the step
	 * rate, the settings of the height map and the fitting to the data of a model
	 * (see setDataBounds). The loaded data (texture, volume, iso surface, model)
	 * is kept.
	 */
	public void resetToDefaults() {
		synchronized (matrixLock) {
			LastRot.setIdentity();
			ThisRot.setIdentity();
		}
		transx = 0;
		transy = 0;
		transz = 0;
		rotatx = 0;
		rotaty = 0;
		rotatz = 0;
		scaleValue = 1.0f;
		dox = 0;
		doy = 0;
		doxv = 0;
		doyv = 0;
		walkthrough = false;
		flythrough = false;
		SPEED = 2.0;
		heightSpeed = 0.1;
		splitView = false;
		stereo = false;
		customCamera = false;
		initViewerPosn();
		/* The camera distance fitted to the displayed data */
		doz = getDefaultViewDistance();
		/* The time: one step per second */
		period = 1000000000L;
		hold = false;
		/* The check boxes of the camera tab (split view, custom camera, ...) */
		Options3d.updateCameraModes();
		showFramerate = false;
		showUpdateFramerate = false;
		/* The lights */
		light1 = true;
		light2 = false;
		light3 = false;
		light4 = false;
		localLight = true;
		setLightPos1(0, 1000, 1000);
		setLightPos2(1000, 1000, -2000);
		setLightPos3(0, 1000, 1000);
		setLightPos4(1000, 1000, -2000);
		/* The size of the space (Size X, Y, Z: -1000 to +1000), without the bounds of the data of a model */
		dataBounds = null;
		sizeX = 1000;
		sizeY = 1000;
		sizeZ = 1000;
		/* The grids, axes, box and overlays */
		showAxes = true;
		showQuad = true;
		gridSize = 50;
		showGrid = true;
		layerXYGrid = true;
		layerXZGrid = false;
		layerYZGrid = false;
		layerXYGridMax = false;
		layerXZGridMax = false;
		layerYZGridMax = false;
		plotView = false;
		plotGridXY = false;
		plotGridXZ = true;
		plotGridYZ = true;
		showAxisGizmo = true;
		if (axisLabelsRenderer != null) {
			axisLabelsRenderer.setEnabled(false);
		}
		if (scene != null) {
			scene.setColorCubeLines(new float[] { 0, 0, 0, 1 });
		}
		colorBackground = new float[] { 0.438f, 0.599f, 1.0f, 0.5f };
		backgroundColour = true;
		/* The height map (Cartesian mode like at the start) */
		heightMap = false;
		lightenedHeightMap = true;
		lightenedObjModel = true;
		HeightMaps.cartesian = true;
		HeightMaps.setInverse(false);
		HeightMaps.setFlipVertically(false);
		HeightMaps.setDynamicMap(true);
		HeightMaps.setWireframe(false);
		HeightMaps.setGraphicsCardSwitch(false);
		if (hMap != null) {
			hMap.detail = 16;
			hMap.setScale(1);
		}
	}

	private void initViewerPosn()
	/*
	 * Specify the camera (player) position, the x- and z- step distance, and the
	 * position being looked at.
	 */
	{
		xCameraPosition = 0;
		yCameraPosition = 1;
		zCameraPosition = Z_POS;

		viewAngle = -90.0;
		xStep = Math.cos(Math.toRadians(viewAngle));
		zStep = Math.sin(Math.toRadians(viewAngle));

		xLookAt = xCameraPosition + (lookAtDist * xStep);
		yLookAt = 0;
		zLookAt = zCameraPosition + (lookAtDist * zStep);
	}

	private void processKey(KeyEvent e)

	{
		int keyCode = e.getKeyCode();

		if (keyCode == KeyEvent.VK_LEFT) {
			if (e.isControlDown()) {
				xCameraPosition += zStep * SPEED;
				zCameraPosition -= xStep * SPEED;
			} else {
				viewAngle -= angleIncrement;
				xStep = Math.cos(Math.toRadians(viewAngle));
				zStep = Math.sin(Math.toRadians(viewAngle));

			}
		} else if (keyCode == KeyEvent.VK_RIGHT) {
			if (e.isControlDown()) {
				xCameraPosition -= zStep * SPEED;
				zCameraPosition += xStep * SPEED;
			} else {
				viewAngle += angleIncrement;
				xStep = Math.cos(Math.toRadians(viewAngle));
				zStep = Math.sin(Math.toRadians(viewAngle));

			}
		} else if (keyCode == KeyEvent.VK_UP) {
			forward = true;

		} else if (keyCode == KeyEvent.VK_DOWN) {
			backward = true;

		} else if (keyCode == KeyEvent.VK_PAGE_UP) {

			up = true;

		} else if (keyCode == KeyEvent.VK_PAGE_DOWN) {
			down = true;

		} else if (keyCode == KeyEvent.VK_ESCAPE) {
			view.recreateGLCanvas();
			view.getFullscreen().exit();
			view.setFullscreen(null);

		} else if (keyCode == KeyEvent.VK_F2) {
			if (view.getFullscreen() == null) {
				view.createFullscreen(0);
			}

		} else if (keyCode == KeyEvent.VK_F3) {
			if (view.getFullscreen() == null) {
				view.createFullscreen(1);
			}

		}

		verifyPosition();

	}

	private void verifyPosition() {

		xLookAt = xCameraPosition + (xStep * lookAtDist);
		zLookAt = zCameraPosition + (zStep * lookAtDist);
	}

	public void keyTyped(KeyEvent evt) {
		SpatialEvents.setKeyTyped(true);
		SpatialEvents.setKeyTypedEvent(evt);

	}

	public void mouseExited(MouseEvent evt) {
		SpatialEvents.setMouseExited(true);
		SpatialEvents.setMouseExitedEvent(evt);

	}

	public void mousePressed(MouseEvent evt) {
		SpatialEvents.setMousePressed(true);
		SpatialEvents.setPressEvent(evt);

		mousePressed = true;
		prevMouseX = evt.getX();
		prevMouseY = evt.getY();
		mouseButton = evt.getButton();
		mouseButton = evt.getButton();
		/* A click on the number of a measured object selects the object (NEWT: pixel units)! */
		if (evt.getButton() == 1 && !walkthrough && measurement.selectLabelAt(evt.getX(), evt.getY())) {
			clippingDrag = false;
			renderer.startDrag(new Point(evt.getX(), evt.getY()));
			return;
		}
		/* The mouse tools of the clipping (eraser, plane) with the left mouse button (NEWT: pixel units)! */
		clippingDrag = evt.getButton() == 1 && !walkthrough
				&& clipping.toolPressed(evt.getX(), evt.getY(), evt.isShiftDown());
		if (clippingDrag) {
			return;
		}
		/* Important for the rotation! */
		renderer.startDrag(new Point(evt.getX(), evt.getY()));

	}

	public void mouseDragged(MouseEvent e) {
		SpatialEvents.setMouseDragged(true);
		SpatialEvents.setDragEvent(e);
		if (clippingDrag) {
			clipping.toolDragged(e.getX(), e.getY());
			return;
		}
		if (stopMovement == false) {
			int x = e.getX();
			int y = e.getY();
			if (walkthrough == false) {
				if (mouseButton == 1) {

					renderer.drag(new Point(e.getX(), e.getY()));

				}

				else if (mouseButton == 2) {

					// Dimension size = e.getComponent().getSize();

					float thetaY = 360.0f * ((float) (x - prevMouseX) / (float) glWindow.getWidth());
					float thetaX = 360.0f * ((float) (prevMouseY - y) / (float) glWindow.getHeight());

					prevMouseX = x;
					prevMouseY = y;

					transx += thetaY;
					transy += thetaX;

				} else if (mouseButton == 3) {
					// Dimension size = e.getComponent().getSize();

					float thetaY = 360.0f * ((float) (x - prevMouseX) / (float) glWindow.getWidth());
					float thetaX = 360.0f * ((float) (prevMouseY - y) / (float) glWindow.getHeight());

					prevMouseX = x;
					prevMouseY = y;

					/* Faster for large distances (e.g., a large texture)! */
					float distanceSpeed = Math.max(1.0f, doz / (float) DEFAULT_VIEW_DISTANCE);
					doz += thetaY * multiplyDragCameraSpeed * distanceSpeed;// adjust camera speed for the distance to the object!

				}
			} else {
				/* Handle walkthrough! */

				// Dimension size = e.getComponent().getSize();

				float thetaWalkY = 360.0f * ((float) (x - prevMouseX) / (float) glWindow.getWidth());
				float thetaWalkX = 360.0f * ((float) (prevMouseY - y) / (float) glWindow.getHeight());
				prevMouseX = x;
				prevMouseY = y;

				viewAngle += thetaWalkY;
				viewAngleY += thetaWalkX;
				xStep = Math.cos(Math.toRadians(viewAngle));
				zStep = Math.sin(Math.toRadians(viewAngle));

				yStep = Math.tan(Math.toRadians(viewAngleY));

				xLookAt = xCameraPosition + (xStep * lookAtDist);
				yLookAt = yCameraPosition + (yStep * lookAtDist);
				zLookAt = zCameraPosition + (zStep * lookAtDist);
				if (viewAngleY >= 90) {
					viewAngleY = 90;
					yStep = Math.tan(Math.toRadians(90));
					yLookAt = yCameraPosition + (yStep * lookAtDist);

				} else if (viewAngleY <= -90) {
					viewAngleY = -90;
					yStep = Math.tan(Math.toRadians(-90));
					yLookAt = yCameraPosition + (yStep * lookAtDist);
				}

			}
		}
	}

	public void mouseEntered(MouseEvent evt) {
		SpatialEvents.setMouseEntered(true);
		SpatialEvents.setMouseEnteredEvent(evt);

	}

	public void mouseClicked(MouseEvent evt) {
		if (evt.getButton() == 2) {
			SpatialEvents.setMouseWheelClicked(true);
			SpatialEvents.setWheelClickEvent(evt);

		}
		if (evt.getButton() == 3) {
			SpatialEvents.setRightMouseClicked(true);
			SpatialEvents.setRightClickEvent(evt);

		} else if (evt.getClickCount() == 1) {
			SpatialEvents.setMouseClicked(true);
			SpatialEvents.setClickEvent(evt);
		} else if (evt.getClickCount() == 2) {
			SpatialEvents.setMouseDoubleClicked(true);
			SpatialEvents.setDoubleClickEvent(evt);

		} else if (evt.getClickCount() == 3) {
			SpatialEvents.setMouseTripleClicked(true);
			SpatialEvents.setTripleClickEvent(evt);
		}
	}

	public void mouseReleased(MouseEvent evt) {
		mousePressed = false;
		if (clippingDrag) {
			clippingDrag = false;
			clipping.toolReleased();
		}
		SpatialEvents.setMouseDragged(false);
		SpatialEvents.setDragEvent(null);

	}

	public NewtCanvasSWT getCanvas() {
		return canvas;
	}

	public void mouseMoved(MouseEvent e) {
		SpatialEvents.setMouseMoved(true);
		SpatialEvents.setMouseMoveEvent(e);
	}

	public void mouseWheelMoved(MouseEvent e) {
		SpatialEvents.setMouseWheelMoved(true);
		SpatialEvents.setMouseWheelEvent(e);

	}

	public com.eco.bio7.spatial.FPSAnimator getLoop() {
		return animator;
	}

	public initRenderer getRenderer() {
		return renderer;
	}

	public float getSizeX() {
		return sizeX;
	}

	public void setSizeX(float sizeX) {
		this.sizeX = sizeX;
	}

	public float getSizeY() {
		return sizeY;
	}

	public void setSizeY(float sizeY) {
		this.sizeY = sizeY;
	}

	public float getSizeZ() {
		return sizeZ;
	}

	/**
	 * Returns the size (width, height) of the displayed texture (or of a displayed
	 * volume if no texture is displayed) or null if nothing is displayed.
	 * 
	 * @return the texture width and height or null
	 */
	public float[] getTextureExtent() {
		if (textures != null && textures.showTexture && textures.tex != null) {
			return new float[] { textures.tex.getWidth(), textures.tex.getHeight() };
		}
		float[] stack = getStackExtent();
		if (stack != null) {
			return new float[] { stack[0], stack[1] };
		}
		return null;
	}

	/*
	 * The size (width, height, depth) of a displayed volume or iso surface of an
	 * ImageJ stack if no texture is displayed, else null!
	 */
	private float[] getStackExtent() {
		if (textures != null && textures.showTexture && textures.tex != null) {
			return null;
		}
		if (volume.isDisplayed()) {
			return new float[] { volume.getWidth(), volume.getHeight(), volume.getDepth() };
		}
		if (isoSurface.isDisplayed()) {
			return new float[] { isoSurface.getWidth(), isoSurface.getHeight(), isoSurface.getDepth() };
		}
		return null;
	}

	/**
	 * Returns the clipping (box, cutting plane and slice) of the volume and the iso
	 * surface.
	 * 
	 * @return the clipping
	 */
	public Clipping getClipping() {
		return clipping;
	}

	/**
	 * Returns the measurements of the iso surface and the volume.
	 * 
	 * @return the measurements
	 */
	public Measurement3d getMeasurement() {
		return measurement;
	}

	/**
	 * Returns the bounds (x0, x1, y0, y1, z0, z1) of the box fitted to the
	 * displayed rendering: the extent of a volume or iso surface, of a texture
	 * (with the range of the heights of a height map) or else the size of the
	 * space (Size X, Y, Z).
	 * 
	 * @return the bounds
	 */
	public double[] getSceneBounds() {
		boolean cartesian = HeightMaps.isCartesian();
		double[] stack = clippingBounds(cartesian);
		if (stack != null) {
			return stack;
		}
		float[] extent = getTextureExtent();
		if (extent != null) {
			double w = extent[0], h = extent[1];
			double z0 = 0, z1 = 0;
			float[] heights = hMap != null ? hMap.getHeightRange() : null;
			if (heights != null) {
				z0 = heights[0];
				z1 = heights[1];
			}
			return cartesian ? new double[] { -w / 2, w / 2, -h / 2, h / 2, z0, z1 }
					: new double[] { 0, w, 0, h, z0, z1 };
		}
		double[] data = dataBounds;
		if (data != null) {
			return data.clone();
		}
		return cartesian ? new double[] { -sizeX, sizeX, -sizeY, sizeY, -sizeZ, sizeZ }
				: new double[] { 0, sizeX, 0, sizeY, 0, sizeZ };
	}

	/* The bounds of the data of a model (see setDataBounds) or null */
	private volatile double[] dataBounds;

	/**
	 * Sets the bounds of the data of a model (e.g., the spheres of a script) for
	 * the box, the grids, the plot view with the axis numbers and the camera (if
	 * no texture, volume or iso surface is displayed). The data is moved into the
	 * center of the view.
	 * 
	 * @param bounds x0, x1, y0, y1, z0, z1 or null (the size of the space)
	 */
	public void setDataBounds(double[] bounds) {
		if (bounds != null && bounds.length != 6) {
			throw new IllegalArgumentException("The bounds need six values: x0, x1, y0, y1, z0, z1");
		}
		dataBounds = bounds != null ? bounds.clone() : null;
		/* The camera distance fitted to the data */
		doz = getDefaultViewDistance();
	}

	/**
	 * Returns the scale of the data of a model in the view: the largest side of
	 * the bounds (see setDataBounds) is displayed with the size 1000 (like the
	 * default space), so that small and large data fit the camera.
	 * 
	 * @return the scale (1 without bounds)
	 */
	public double getDataScale() {
		double[] b = dataBounds;
		if (b == null) {
			return 1;
		}
		double size = Math.max(b[1] - b[0], Math.max(b[3] - b[2], b[5] - b[4]));
		return size > 0 ? 1000 / size : 1;
	}

	/**
	 * Returns the bounds of the data of a model (see setDataBounds) or null.
	 * 
	 * @return x0, x1, y0, y1, z0, z1 or null
	 */
	public double[] getDataBounds() {
		double[] b = dataBounds;
		return b != null ? b.clone() : null;
	}

	/* The extent of a displayed stack (x0, x1, y0, y1, z0, z1) or null! */
	private double[] clippingBounds(boolean cartesian) {
		if (getStackExtent() == null) {
			return null;
		}
		double ax = getAxisSizeX(), ay = getAxisSizeY(), az = getAxisSizeZ();
		return cartesian ? new double[] { -ax, ax, -ay, ay, -az, az } : new double[] { 0, ax, 0, ay, 0, az };
	}

	/**
	 * Returns the iso surface renderer for ImageJ stacks.
	 * 
	 * @return the iso surface
	 */
	public IsoSurface getIsoSurface() {
		return isoSurface;
	}

	/**
	 * A listener which is called (in the OpenGL thread) when an *.obj model has
	 * been loaded.
	 * 
	 * @param listener the listener
	 */
	public void setModelListener(Runnable listener) {
		this.modelListener = listener;
	}

	private void notifyModelListener() {
		Runnable listener = modelListener;
		if (listener != null) {
			listener.run();
		}
	}

	/**
	 * Returns a description of the loaded *.obj model.
	 * 
	 * @return the description
	 */
	public String getModelInfo() {
		MeshModel m = model;
		if (m == null) {
			if (modelError != null) {
				return "The model can't be loaded: " + modelError;
			}
			return loadModel ? "The model can't be loaded (see the error log)" : "No model loaded";
		}
		ModelDimensions dims = m.getModelDims();
		String size = String.format("%.4g x %.4g x %.4g", dims.getWidth(), dims.getHeight(), dims.getDepth());
		return m.getName() + " (" + m.getFormat() + "): " + m.getReport() + ", size " + size
				+ (loadModel ? "" : " (hidden)");
	}

	/**
	 * Returns the volume renderer for ImageJ stacks.
	 * 
	 * @return the volume renderer
	 */
	public VolumeRenderer getVolumeRenderer() {
		return volume;
	}

	/**
	 * Returns the length of the x axis. If a texture is displayed the axis is as
	 * long as the texture width (Cartesian: from -width/2 to +width/2).
	 * 
	 * @return the length of the x axis
	 */
	public float getAxisSizeX() {
		float[] extent = getTextureExtent();
		if (extent == null) {
			return sizeX;
		}
		return HeightMaps.isCartesian() ? extent[0] / 2 : extent[0];
	}

	/**
	 * Returns the length of the z axis. If a texture is displayed the axis is as
	 * long as the longer texture side (Cartesian: from -side/2 to +side/2).
	 * 
	 * @return the length of the z axis
	 */
	public float getAxisSizeZ() {
		float[] extent = getTextureExtent();
		if (extent == null) {
			return sizeZ;
		}
		/* A stack: as long as the depth of the stack! */
		float[] stack = getStackExtent();
		float side = stack != null ? stack[2] : Math.max(extent[0], extent[1]);
		return HeightMaps.isCartesian() ? side / 2 : side;
	}

	/**
	 * Returns the length of the y axis. If a texture is displayed the axis is as
	 * long as the texture height (Cartesian: from -height/2 to +height/2).
	 * 
	 * @return the length of the y axis
	 */
	public float getAxisSizeY() {
		float[] extent = getTextureExtent();
		if (extent == null) {
			return sizeY;
		}
		return HeightMaps.isCartesian() ? extent[1] / 2 : extent[1];
	}

	public void setSizeZ(float sizeZ) {
		this.sizeZ = sizeZ;
	}

	public static SpatialStructure getSpatialStructureInstance() {
		return SpatialStructureInstance;
	}

	public float getTransz() {
		return transz;
	}

	public void setTransz(float transz) {
		this.transz = transz;
	}

	public float getTransx() {
		return transx;
	}

	public void setTransx(float transx) {
		this.transx = transx;
	}

	public float getTransy() {
		return transy;
	}

	public void setTransy(float transy) {
		this.transy = transy;
	}

	public float getRotatx() {
		return rotatx;
	}

	public void setRotatx(float rotatx) {
		this.rotatx = rotatx;
	}

	public float getRotaty() {
		return rotaty;
	}

	public void setRotaty(float rotaty) {
		this.rotaty = rotaty;
	}

	public float getRotatz() {
		return rotatz;
	}

	public void setRotatz(float rotatz) {
		this.rotatz = rotatz;
	}

	public static boolean isReloadModel() {
		return reloadModel;
	}

	public static void setReloadModel(boolean reloadModel) {
		SpatialStructure.reloadModel = reloadModel;
	}

	public static long getPeriod() {
		return period;
	}

	public static void setPeriod(long period) {
		SpatialStructure.period = period;
	}

	public static boolean isWalkthrough() {
		return walkthrough;
	}

	public static void setWalkthrough(boolean walkthrough) {
		boolean changed = SpatialStructure.walkthrough != walkthrough;
		SpatialStructure.walkthrough = walkthrough;
		if (changed) {
			Options3d.updateCameraModes();
		}
	}

	public GL2 getGl() {
		return gl;
	}

	public boolean isHeightMap() {
		return heightMap;
	}

	public void setHeightMap(boolean heightMap) {
		this.heightMap = heightMap;
	}

	public boolean isLoadModel() {
		return loadModel;
	}

	public void setLoadModel(boolean loadModel) {
		this.loadModel = loadModel;
	}

	public boolean isSplitView() {
		return splitView;
	}

	public void setSplitView(boolean splitView) {
		boolean changed = this.splitView != splitView;
		this.splitView = splitView;
		if (changed) {
			/* The check boxes of the camera tab show the state */
			Options3d.updateCameraModes();
		}
	}

	/**
	 * Returns true if the custom camera is used (see setCustomCamera).
	 * 
	 * @return true if the custom camera is enabled
	 */
	public boolean isCustomCamera() {
		return customCamera;
	}

	/**
	 * Enables the custom camera (the position and the look at of setCustomCamera).
	 * 
	 * @param enabled true to use the custom camera
	 */
	public void setCustomCameraEnabled(boolean enabled) {
		boolean changed = customCamera != enabled;
		customCamera = enabled;
		if (changed) {
			Options3d.updateCameraModes();
		}
	}

	public static HeightMaps getHeightMapInstance() {
		return hMap;
	}

	public boolean isFromOpenGl() {
		return fromOpenGl;
	}

	public void setFromOpenGl(boolean fromOpenGl) {
		boolean changed = this.fromOpenGl != fromOpenGl;
		this.fromOpenGl = fromOpenGl;
		if (changed) {
			/* The play/pause button shows the state (e.g., started by a script) */
			Options3d.updatePlayPause();
		}
	}

	public boolean canStep() {
		return step;
	}

	public void setLightPos1(float x, float y, float z) {

		this.lightPos1[0] = x;
		this.lightPos1[1] = y;
		this.lightPos1[2] = z;
	}

	public void setLightPos2(float x, float y, float z) {
		this.lightPos2[0] = x;
		this.lightPos2[1] = y;
		this.lightPos2[2] = z;

	}

	public float[] getLightPos1() {
		return lightPos1;
	}

	public float[] getLightPos2() {
		return lightPos2;
	}

	public float[] getLightPos3() {
		return lightPos3;
	}

	public void setLightPos3(float x, float y, float z) {
		this.lightPos3[0] = x;
		this.lightPos3[1] = y;
		this.lightPos3[2] = z;
	}

	public float[] getLightPos4() {
		return lightPos4;
	}

	public void setLightPos4(float x, float y, float z) {
		this.lightPos4[0] = x;
		this.lightPos4[1] = y;
		this.lightPos4[2] = z;
	}

	public com.eco.bio7.spatial.FPSAnimator getAnimator() {
		return animator;
	}

	public void setAnimator(com.eco.bio7.spatial.FPSAnimator animatore) {
		animator = animatore;
	}

	public float getGridSize() {
		return gridSize;
	}

	public void setGridSize(float gridSize) {
		this.gridSize = gridSize;
	}

	public static void setCustomCamera(double xpos, double ypos, double zpos, double xlook, double ylook,
			double zlook) {
		customCameraCoordinates[0] = xpos;
		customCameraCoordinates[1] = ypos;
		customCameraCoordinates[2] = zpos;
		customCameraCoordinates[3] = xlook;
		customCameraCoordinates[4] = ylook;
		customCameraCoordinates[5] = zlook;

	}

	public float[] getColorBackground() {
		return colorBackground;
	}

	public void setColorBackground(float[] colorBackground) {
		this.colorBackground = colorBackground;
	}

	public SpatialScene getScene() {
		return scene;
	}

	public static GLAutoDrawable getDrawable() {
		return drawable_;
	}

	public static void setRenderImageTo(int renderImageTo) {
		SpatialStructure.renderImageTo = renderImageTo;
	}

	public double getXCamPos() {
		return xCameraPosition;
	}

	public double getYCamPos() {
		return yCameraPosition;
	}

	public double getZCamPos() {
		return zCameraPosition;
	}

	public Matrix4f getLastRot() {
		return LastRot;
	}

	public void setLastRot(Matrix4f lastRot) {
		LastRot = lastRot;
	}

	public Matrix4f getThisRot() {
		return ThisRot;
	}

	public void setThisRot(Matrix4f thisRot) {
		ThisRot = thisRot;
	}

	public boolean isStopMovement() {
		return stopMovement;
	}

	public void setStopMovement(boolean stop) {
		stopMovement = stop;
	}

	/* Set the values for the split view camera! */
	public void setSplitCameraValues(int x, int y, int z, int xl, int yl, int zl) {
		xCamSplit = x;
		yCamSplit = y;
		zCamSplit = z;
		xSplitLookAt = xl;
		ySplitLookAt = yl;
		zSplitLookAt = zl;

	}

	public void setSplitScreenSize(int x, int y, int width, int height) {
		splitOriginX = x;
		splitOriginY = y;
		splitHeight = height;
		splitWidth = width;
	}

	/* Draws the quad and the height map in (positive) cartesian view! */
	/*
	 * The grids, the box and the axes (with the numbers) or the plot view (the
	 * grids behind the data and the axes at the front edges of the box)!
	 */
	private void drawGridAndAxes(GL2 gl, boolean cartesian) {
		boolean numbers = axisLabelsRenderer != null && axisLabelsRenderer.isEnabled();
		if (plotView) {
			scene.drawPlotView(gl, this, numbers);
			return;
		}
		if (cartesian) {
			scene.drawGrid(gl, this);
			this.renderer.drawQuad();
		} else {
			scene.drawGrid2(gl, this);
		}
		scene.showAxes(gl, this);
		if (axisLabelsRenderer != null) {
			axisLabelsRenderer.renderAxisLabels(gl, this);
		}
	}

	private void drawSpace(GL2 gl, GLAutoDrawable drawable) {
		/* The rotation of the scene for the axis orientation overlay */
		axisGizmo.capture(gl);
		if (textures.isRenderImage() == false) {

			if (hMap.cartesian == true) {

				if (textures.tex != null) {
					hMap.OriginX = -(int) (textures.tex.getWidth() / 2);
					hMap.OriginY = -(int) (textures.tex.getHeight() / 2);
				}
				if (textures.showTexture == true && textures.tex != null) {

					gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
					if (heightMap) {
						if (lightenedHeightMap == false) {
							gl.glDisable(GL_LIGHTING);
						}
						hMap.renderDynamicHeightMap(gl,drawable);
					} else {
						/* No Height Map, just a texture! */
						gl.glDisable(GL_LIGHTING);
						hMap.renderDynamicTexture(gl, drawable);
					}

				}

				gl.glDisable(GL_LIGHTING);

				drawGridAndAxes(gl, true);
				gl.glEnable(GL_LIGHTING);

			} else {

				hMap.OriginX = 0;
				hMap.OriginY = 0;

				if (textures.showTexture == true && textures.tex != null) {

					gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
					if (heightMap) {
						if (lightenedHeightMap == false) {
							gl.glDisable(GL_LIGHTING);
						}
						hMap.renderDynamicHeightMap(gl,drawable);
					} else {
						gl.glDisable(GL_LIGHTING);
						hMap.renderDynamicTexture(gl,drawable);
					}

				}

				gl.glDisable(GL_LIGHTING);
				drawGridAndAxes(gl, false);
				gl.glEnable(GL_LIGHTING);

			}

		} else {

			if (hMap.cartesian == true) {

				hMap.OriginX = -(int) (hMap.imageWidth / 2);
				hMap.OriginY = -(int) (hMap.imageHeight / 2);
				if (textures.showTexture == true && textures.tex != null) {

					gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
					if (heightMap) {
						if (lightenedHeightMap == false) {
							gl.glDisable(GL_LIGHTING);
						}
						hMap.RenderHeightMap(textures.tex, gl,drawable);
					} else {
						gl.glDisable(GL_LIGHTING);
						hMap.renderTexture(textures.tex, gl,drawable);
						gl.glEnable(GL_LIGHTING);
					}

				}

				gl.glDisable(GL_LIGHTING);

				drawGridAndAxes(gl, true);
				gl.glEnable(GL_LIGHTING);

			} else {

				hMap.OriginX = 0;
				hMap.OriginY = 0;
				if (textures.showTexture == true && textures.tex != null) {

					gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
					if (heightMap) {
						if (lightenedHeightMap == false) {
							gl.glDisable(GL_LIGHTING);
						}

						hMap.RenderHeightMap(textures.tex, gl,drawable);
					} else {
						/* It makes no sense to light a single textured quad! */
						gl.glDisable(GL_LIGHTING);
						hMap.renderTexture(textures.tex, gl,drawable);

					}

				}

				gl.glDisable(GL_LIGHTING);
				drawGridAndAxes(gl, false);
				gl.glEnable(GL_LIGHTING);

			}
		}
		/* The grids at the far sides of the box or the cage of a plot view! */
		gl.glDisable(GL_LIGHTING);
		scene.drawGridCage(gl, this);
		gl.glEnable(GL_LIGHTING);
	}

	public Preferences3d getPref3d() {
		return pref3d;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	/* This method comes from the preferences! */
	public void setModelValues(int x, int y, int z, int x2, int y2, int z2, int x3) {
		objModel1Position = new double[] { x, y, z };
		objModel1Rotation = new double[] { x2, y2, z2 };
		objModel1Scale = x3;

	}

	public boolean isSplitPanelDrawing() {
		return isSplitPanelDrawing;
	}

	public SpatialView getView() {
		return view;
	}

	public static void setMultiplyDragCameraSpeed(int multiplyDragCameraSpeed) {
		SpatialStructure.multiplyDragCameraSpeed = multiplyDragCameraSpeed;
	}

	/* Set view distance to object! */
	public void setDoz(int doz) {
		this.doz = doz;
	}

	/**
	 * Returns the default view distance: 2000 or, if a texture is displayed, a
	 * distance that shows the texture like the default 1000 units grid.
	 * 
	 * @return the default view distance
	 */
	public int getDefaultViewDistance() {
		float[] extent = getTextureExtent();
		if (extent == null) {
			/* The data of a model is scaled to the default size (see getDataScale) */
			return DEFAULT_VIEW_DISTANCE;
		}
		float size = Math.max(extent[0], extent[1]);
		float[] stack = getStackExtent();
		if (stack != null) {
			size = Math.max(size, stack[2]);
		}
		return Math.max(10, Math.round(DEFAULT_VIEW_DISTANCE * size / 1000.0f));
	}

	/*
	 * Zooms in or out if the size of the displayed texture changed (or the texture
	 * is hidden), so that the image is neither too close nor too far!
	 */
	private void fitViewToTexture() {
		float[] extent = getTextureExtent();
		float width = extent != null ? extent[0] : 0;
		float height = extent != null ? extent[1] : 0;
		float[] stack = getStackExtent();
		float depth = stack != null ? stack[2] : 0;
		if (width != fitTextureWidth || height != fitTextureHeight || depth != fitTextureDepth) {
			fitTextureWidth = width;
			fitTextureHeight = height;
			fitTextureDepth = depth;
			doz = getDefaultViewDistance();
		}
	}
	// Add getter method:
	public AxisLabelsRenderer getAxisLabelsRenderer() {
	    return axisLabelsRenderer;
	}

}
