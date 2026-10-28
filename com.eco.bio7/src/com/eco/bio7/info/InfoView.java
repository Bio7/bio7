package com.eco.bio7.info;

import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import com.eco.bio7.time.CalculationThread;
import com.eco.bio7.time.Time;

public class InfoView extends ViewPart {

	public InfoView() {

	}

	public static final String ID = "com.eco.bio7.control";

	private static SpeedSlider timeSwtScale;

	private static String timeSteps = "Time steps: ";

	private static Label lblTimesteps;

	private static Label lblSpeed;

	public static Label getLblTimesteps() {
		return lblTimesteps;
	}

	public static void setLblTimesteps(Label lblTimesteps) {
		InfoView.lblTimesteps = lblTimesteps;
	}

	public void createPartControl(Composite parent) {
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "com.eco.bio7.control");
		ScrolledComposite scroll = new ScrolledComposite(parent, SWT.V_SCROLL);
		Composite composite = new Composite(scroll, SWT.NONE);
		composite.setBackground(parent.getBackground());
		composite.setLayout(new GridLayout(1, true));
		scroll.setContent(composite);
		scroll.setExpandVertical(true);
		scroll.setExpandHorizontal(true);

		lblTimesteps = new Label(composite, SWT.NONE);
		GridData gd_lblTimesteps = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_lblTimesteps.heightHint = 35;
		lblTimesteps.setLayoutData(gd_lblTimesteps);
		lblTimesteps.setBackground(parent.getBackground());
		lblTimesteps.setText(timeSteps + Time.getTime());

		/* A logarithmic frame rate slider: each tick distance is the same speed factor! */
		timeSwtScale = new SpeedSlider(composite, SWT.NONE, Time.MIN_FPS, Time.MAX_FPS);
		timeSwtScale.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		timeSwtScale.setValue(Time.getFps());
		timeSwtScale.setToolTipText("Frames (calculation steps) per second: left slower, right faster.\n"
				+ "Keys: Left/Right one step (8%), Shift+Left/Right or PageDown/PageUp 10 steps, Home/End minimum/maximum.");
		timeSwtScale.addListener(SWT.Selection, e -> {
			Time.setFps(timeSwtScale.getValue());
			updateRate();
		});

		lblSpeed = new Label(composite, SWT.NONE);
		GridData gd_lblSpeed = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_lblSpeed.heightHint = 35;
		lblSpeed.setLayoutData(gd_lblSpeed);
		lblSpeed.setBackground(parent.getBackground());
		updateRate();

		scroll.setMinSize(composite.computeSize(200, SWT.DEFAULT));

	}

	/** Updates the speed label (in the UI thread). */
	public static void updateRate() {
		if (lblSpeed == null || lblSpeed.isDisposed()) {
			return;
		}
		String text = "Speed: " + SpeedSlider.format(Time.getFps()) + " fps (" + formatMillis(Time.getIntervalNanos() / 1e6) + " ms per frame)";
		double measured = CalculationThread.getMeasuredFps();
		if (measured > 0) {
			text += ", measured: " + SpeedSlider.format(measured) + " fps";
		}
		if (!text.equals(lblSpeed.getText())) {
			lblSpeed.setText(text);
		}
	}

	private static String formatMillis(double ms) {
		return ms >= 10 ? String.format(Locale.ROOT, "%.0f", ms) : String.format(Locale.ROOT, "%.2f", ms).replaceAll("0+$", "").replaceAll("\\.$", "");
	}

	/** Called when the interval is changed, e.g., by a script (any thread). */
	public static void intervalChanged() {
		if (!PlatformUI.isWorkbenchRunning()) {
			return;
		}
		Display display = PlatformUI.getWorkbench().getDisplay();
		if (display.isDisposed()) {
			return;
		}
		display.asyncExec(() -> {
			SpeedSlider slider = timeSwtScale;
			if (slider != null && !slider.isDisposed()) {
				slider.setValue(Time.getFps());
			}
			updateRate();
		});
	}

	/** The frame rate slider of the view (null if the view is not open). */
	public static SpeedSlider getTimeScaleSwt() {
		return timeSwtScale;
	}

	@Override
	public void setFocus() {
		if (timeSwtScale != null && !timeSwtScale.isDisposed()) {
			timeSwtScale.setFocus();
		}
	}

	public static String getTimeSteps() {
		return timeSteps;
	}

	public static void setTimeSteps(String timeSteps) {
		InfoView.timeSteps = timeSteps;
	}

}
