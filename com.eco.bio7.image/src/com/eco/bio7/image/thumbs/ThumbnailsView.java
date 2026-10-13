/*******************************************************************************
 * Copyright (c) 2004-2026 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.image.thumbs;

import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.text.DecimalFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.prefs.Preferences;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.eco.bio7.image.ui.CanvasView;
import com.eco.bio7.image.ui.Util;
import ij.ImagePlus;
import ij.WindowManager;
import ij.io.Opener;
import ij.process.LUT;

/**
 * Displays the thumbnails of the images of a directory. The thumbnails are
 * painted by a {@link ThumbnailGrid} which needs no operating system resources
 * per thumbnail, so that thousands of images can be displayed. Thumbnails are
 * created in parallel and cached in memory and on disk
 * ({@link ThumbnailCache}), so that a reload is fast.
 */
public class ThumbnailsView extends ViewPart {

	private double zoomLevel[] = { 0.25, 0.5, 1.0, 1.5, 2.0, 3.0, 4.0 };

	private ThumbnailAction menuAction = new ThumbnailAction();

	private FiveInRowAction five = new FiveInRowAction();

	private TenInRowAction ten = new TenInRowAction();

	private FifteenInRowAction fifteen = new FifteenInRowAction();

	private SmallThumbAction small = new SmallThumbAction();

	private MediumThumbAction medium = new MediumThumbAction();

	private LargeThumbAction large = new LargeThumbAction();

	private EnablePreviewAction preview = new EnablePreviewAction();

	private RecursiveAction recursiveAction = new RecursiveAction();

	private String size;

	private Composite composite;

	private static ThumbnailsView thumb;

	private int inarow = 15;

	private int thumbsize = 50;

	public static final String ID = "com.eco.bio7.thumbnails";

	private volatile boolean canceled = false;

	private static boolean thumbSizeSelectable = true;

	public SashForm sashForm;

	private ThumbnailGrid grid;

	private Canvas overview;

	/* The files which are displayed (to reload them with another size)! */
	private File[] files;

	/* Incremented for each load, so that a new load cancels the old one! */
	private final AtomicInteger loadId = new AtomicInteger();

	public static boolean isThumbSizeSelectable() {
		return thumbSizeSelectable;
	}

	public static void setThumbSizeSelectable(boolean thumbSizeSelectable) {
		ThumbnailsView.thumbSizeSelectable = thumbSizeSelectable;
	}

	public ThumbnailsView() {

		thumb = this;
		size = "small";

		load_preferences();

	}

	private void load_preferences() {
		Preferences prefs = Preferences.userNodeForPackage(this.getClass());
		String prefer = prefs.get("SIZE", "");
		String prefer2 = prefs.get("INROW", "");

		if (prefer.equals("small")) {
			small.setChecked(true);
			medium.setChecked(false);
			large.setChecked(false);
			this.size = "small";
		} else if (prefer.equals("medium")) {
			small.setChecked(false);
			medium.setChecked(true);
			large.setChecked(false);
			this.size = "medium";
		} else if (prefer.equals("large")) {
			small.setChecked(false);
			medium.setChecked(false);
			large.setChecked(true);
			this.size = "large";
		}

		if (prefer2.equals("five")) {
			five.setChecked(true);
			this.inarow = 5;
		} else if (prefer2.equals("ten")) {
			ten.setChecked(true);
			this.inarow = 10;
		} else if (prefer2.equals("fifteen")) {
			fifteen.setChecked(true);
			this.inarow = 15;
		}
	}

	public void createPartControl(Composite parent) {
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "com.eco.bio7.thumbnails");

		initializeToolBar();

		composite = new Composite(parent, SWT.BORDER);
		composite.setLayout(new FillLayout());

		sashForm = new SashForm(composite, SWT.HORIZONTAL);
		sashForm.setBackground(composite.getBackground());

		grid = new ThumbnailGrid(sashForm);
		grid.setColumns(inarow);
		grid.setOpenListener(item -> open(item.file));
		grid.setFolderListener(item -> {
			try {
				Program.launch(item.file.getParent());
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
		});

		/* The overview of all thumbnails (toggled with the "Mag." action)! */
		overview = new Canvas(sashForm, SWT.BORDER | SWT.DOUBLE_BUFFERED);
		overview.setBackground(parent.getBackground());
		overview.addListener(SWT.Paint, e -> grid.paintOverview(e.gc, overview.getClientArea()));
		overview.addListener(SWT.MouseDown, e -> grid.scrollFromOverview(overview.getClientArea(), e.x, e.y));
		overview.addListener(SWT.MouseMove, e -> {
			if ((e.stateMask & SWT.BUTTON1) != 0) {
				grid.scrollFromOverview(overview.getClientArea(), e.x, e.y);
			}
		});
		grid.setViewportListener(() -> {
			if (!overview.isDisposed() && overview.isVisible()) {
				overview.redraw();
			}
		});
		sashForm.setWeights(new int[] { 3, 1 });
		preview.setChecked(true);
	}

	/** Shows or hides the overview of the thumbnails. */
	void togglePreview() {
		if (overview.isVisible()) {
			overview.setVisible(false);
			sashForm.setWeights(new int[] { 1, 0 });
		} else {
			overview.setVisible(true);
			sashForm.setWeights(new int[] { 3, 1 });
			overview.redraw();
		}
	}

	/** Loads the thumbnails of the files (called in a job). */
	public void setImages(final File files[], IProgressMonitor monitor) {
		if (files == null) {
			return;
		}
		final int id = loadId.incrementAndGet();
		this.files = files;
		ThumbnailCache.prune();
		setThumbSize();
		final int[] state = new int[2]; // generation, pixel size
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(() -> {
			if (grid == null || grid.isDisposed()) {
				return;
			}
			/* ImageJ must be initialized in the UI thread before it is used by the loader threads! */
			new Opener();
			/* Thumbnails have device pixels, so that they are sharp on a Retina display! */
			double scale = Math.max(1, grid.getMonitor().getZoom() / 100.0);
			state[1] = (int) Math.round(thumbsize * scale);
			state[0] = grid.setFiles(files, thumbsize, state[1]);
		});
		if (state[1] == 0) {
			return;
		}
		String lutSample = null;
		try {
			lutSample = Util.getImageJPath().replace("\\", "/") + "/src/images/lut200.tif";
		} catch (RuntimeException e) {
		}
		final ThumbnailLoader loader = new ThumbnailLoader(state[1], lutSample);
		/* Released thumbnails are loaded again from the cache! */
		display.asyncExec(() -> {
			if (grid != null && !grid.isDisposed() && loadId.get() == id) {
				grid.setReloader(loader::load);
			}
		});
		int threads = Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() - 1));
		ExecutorService pool = Executors.newFixedThreadPool(threads, r -> {
			Thread t = new Thread(r, "Thumbnail loader");
			t.setDaemon(true);
			t.setPriority(Thread.NORM_PRIORITY - 1);
			return t;
		});
		final AtomicInteger done = new AtomicInteger();
		for (int i = 0; i < files.length; i++) {
			final int index = i;
			pool.execute(() -> {
				if (canceled || loadId.get() != id) {
					return;
				}
				ThumbData data = loader.load(files[index]);
				if (loadId.get() == id) {
					grid.setData(state[0], index, data);
				}
				done.incrementAndGet();
			});
		}
		pool.shutdown();
		int reported = 0;
		try {
			while (!pool.awaitTermination(100, TimeUnit.MILLISECONDS)) {
				if (monitor.isCanceled() || canceled || loadId.get() != id) {
					canceled = true;
					pool.shutdownNow();
				}
				int n = done.get();
				monitor.worked(n - reported);
				reported = n;
				monitor.setTaskName("opened: " + n + " from: " + files.length);
			}
		} catch (InterruptedException e) {
			pool.shutdownNow();
			Thread.currentThread().interrupt();
		}
		monitor.worked(done.get() - reported);
	}

	/* Reloads the displayed files (e.g., with another thumbnail size)! */
	private void reload() {
		if (files == null || files.length == 0) {
			return;
		}
		canceled = false;
		LoadThumbnailsJob job = new LoadThumbnailsJob(files);
		job.setUser(true);
		job.schedule();
	}

	/*
	 * Opens the file in ImageJ. A LUT is applied to the current image if possible
	 * with this image type!
	 */
	private void open(File f) {
		if (com.eco.bio7.image.ui.CanvasView.getCanvas_view() == null) {
			MessageDialog.openWarning(new Shell(), "ImageJ", "You probably closed all ImageJ-Views");
			return;
		}
		Job job = new Job("Open...") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Opening...", IProgressMonitor.UNKNOWN);
				String file = f.toString();
				if (file.endsWith(".lut")) {
					ImagePlus currentImage = WindowManager.getCurrentImage();
					if (currentImage != null) {
						ColorModel cm = currentImage.getProcessor().getColorModel();
						if (cm instanceof IndexColorModel) {
							LUT lut = Opener.openLut(file);
							currentImage.setLut(lut);
						}
					}
				} else {
					Opener o = new Opener();
					o.open(file);
					Display dis = Util.getDisplay();
					dis.syncExec(new Runnable() {

						public void run() {
							/* Call parent layout before the plot layout! */
							CanvasView.parent2.layout();

						}
					});
				}
				monitor.done();
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	protected void zoomChange(int level) {
		if (grid != null && !grid.isDisposed()) {
			grid.setZoom(zoomLevel[level]);
		}
	}

	@Override
	public void setFocus() {
		if (grid != null && !grid.isDisposed()) {
			grid.setFocus();
		}
	}

	@Override
	public void dispose() {
		/* Stops a running load! */
		loadId.incrementAndGet();
		canceled = true;
		super.dispose();
	}

	private void initializeToolBar() {
		IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

		ZoomCombo zo = new ZoomCombo();
		tbm.add(five);
		tbm.add(ten);
		tbm.add(fifteen);
		tbm.add(small);
		tbm.add(medium);
		tbm.add(large);
		tbm.add(menuAction);
		tbm.add(recursiveAction);
		tbm.add(preview);
		tbm.add(zo);
	}

	class ZoomCombo extends ControlContribution {
		ZoomCombo() {
			super("zoomCombo");

		}

		protected Control createControl(Composite parent) {

			Combo combo = new Combo(parent, SWT.READ_ONLY);

			for (int i = 0; i < zoomLevel.length; i++) {
				DecimalFormat decimal = new DecimalFormat("###%");
				combo.add(decimal.format(zoomLevel[i]));
			}

			combo.addSelectionListener(new SelectionListener() {

				public void widgetSelected(SelectionEvent e) {
					zoomChange(((Combo) e.getSource()).getSelectionIndex());
				}

				public void widgetDefaultSelected(SelectionEvent e) {
				}
			});
			/* 100% (the thumbnails are displayed in their size)! */
			combo.select(2);
			return combo;
		}
	}

	private void setThumbSize() {
		if (size.equals("small")) {
			thumbsize = 50;
		} else if (size.equals("medium")) {
			thumbsize = 100;
		} else {
			thumbsize = 200;
		}
	}

	public static ThumbnailsView getThumb() {
		return thumb;
	}

	public SmallThumbAction getSmall() {
		return small;
	}

	public MediumThumbAction getMedium() {
		return medium;
	}

	public LargeThumbAction getLarge() {
		return large;
	}

	public String getSize() {
		return size;
	}

	public void setSmall(SmallThumbAction small) {
		this.small = small;
	}

	public void setMedium(MediumThumbAction medium) {
		this.medium = medium;
	}

	public void setLarge(LargeThumbAction large) {
		this.large = large;
	}

	/** Sets the thumbnail size ("small", "medium" or "large") and reloads the displayed files. */
	public void setSize(String size) {
		boolean changed = !size.equals(this.size);
		this.size = size;
		if (changed) {
			reload();
		}
	}

	public int getInarow() {
		return inarow;
	}

	/** Sets the number of thumbnails in a row. */
	public void setInarow(int inarow) {
		this.inarow = inarow;
		if (grid != null && !grid.isDisposed()) {
			grid.setColumns(inarow);
		}
	}

	public void setCanceled(boolean canceled) {
		this.canceled = canceled;
	}

	public FiveInRowAction getFive() {
		return five;
	}

	public void setFive(FiveInRowAction five) {
		this.five = five;
	}

	public TenInRowAction getTen() {
		return ten;
	}

	public void setTen(TenInRowAction ten) {
		this.ten = ten;
	}

	public FifteenInRowAction getFifteen() {
		return fifteen;
	}

	public void setFifteen(FifteenInRowAction fifteen) {
		this.fifteen = fifteen;
	}
}
