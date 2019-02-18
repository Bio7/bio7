package com.eco.bio7.macosx;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.osgi.framework.Bundle;

import com.eco.bio7.actions.Bio7Action;
import com.eco.bio7.info.InfoView;
import com.eco.bio7.time.Time;
import com.eco.bio7.util.Util;
import com.thizzer.jtouchbar.JTouchBar;
import com.thizzer.jtouchbar.common.Image;
import com.thizzer.jtouchbar.item.TouchBarItem;
import com.thizzer.jtouchbar.item.view.TouchBarButton;
import com.thizzer.jtouchbar.item.view.TouchBarSlider;
import com.thizzer.jtouchbar.item.view.TouchBarView;
import com.thizzer.jtouchbar.item.view.action.TouchBarViewAction;
import com.thizzer.jtouchbar.slider.SliderActionListener;
import com.thizzer.jtouchbar.swt.JTouchBarSWT;

import ij.IJ;

public class MacTouchBar {

	public MacTouchBar() {
		JTouchBar jTouchBar = constructTouchBar();
		JTouchBarSWT.show(jTouchBar, Util.getShell());
	}

	public JTouchBar constructTouchBar() {

		URL url = getPath();

		JTouchBar jTouchBar = new JTouchBar();
		jTouchBar.setCustomizationIdentifier("Bio7 Touchbar");

		// flexible space
		jTouchBar.getItems().add(new TouchBarItem(TouchBarItem.NSTouchBarItemIdentifierFlexibleSpace));

		TouchBarButton touchBarButtonStartStop = new TouchBarButton();
		touchBarButtonStartStop.setAction(new TouchBarViewAction() {

			public void onCall(TouchBarView view) {
				Bio7Action.startCalculation();
			}
		});
		// touchBarButtonStartStop.setTitle("Play/Pause");
		// touchBarButtonStartStop.setType(TouchBarButton.ButtonType.TOGGLE);
		// touchBarButtonStartStop.setImage(new
		// Image(ImageName.NSImageNameTouchBarAlarmTemplate, false));
		jTouchBar.addItem(new TouchBarItem("Play/Pause", touchBarButtonStartStop, true));

		// org.eclipse.swt.graphics.Image
		// imStart=com.eco.bio7.Bio7Plugin.getImageDescriptor("/icons/maintoolbar/play_pause.png").createImage().;
		touchBarButtonStartStop.setImage(new com.thizzer.jtouchbar.common.Image(
				new File(url.getFile() + "/maintoolbar/play_pause@2x.png").getAbsolutePath(), true));

		TouchBarButton touchBarButtonSetup = new TouchBarButton();
		touchBarButtonSetup.setAction(new TouchBarViewAction() {

			public void onCall(TouchBarView view) {
				Bio7Action.reset();
			}
		});
		//touchBarButtonSetup.setTitle("Reset");
		touchBarButtonSetup.setImage(new com.thizzer.jtouchbar.common.Image(
				new File(url.getFile() + "/maintoolbar/counter_reset@2x.png").getAbsolutePath(), true));
		jTouchBar.addItem(new TouchBarItem("Reset", touchBarButtonSetup, true));

		TouchBarSlider slider = new TouchBarSlider();
		slider.setMinValue(1.0);
		slider.setMaxValue(1000.0);

		slider.setActionListener(new SliderActionListener() {
			@Override
			public void sliderValueChanged(TouchBarSlider slider, double value) {
				// System.out.println("Selected Scrubber Index: " + value);

				Scale scale = InfoView.getTimeScaleSwt();
				scale.setSelection((int) (value));
				Time.setInterval((int) (1000.0 - value));
			}
		});

		jTouchBar.addItem(new TouchBarItem("Speed", slider, true));

		TouchBarButton touchBarButtonStartRserve = new TouchBarButton();
		touchBarButtonStartRserve.setAction(new TouchBarViewAction() {
			public void onCall(TouchBarView view) {
				Bio7Action.callRserve();
			}
		});
		//touchBarButtonStartRserve.setTitle("Start R");
		touchBarButtonStartRserve.setImage(new com.thizzer.jtouchbar.common.Image(
				new File(url.getFile() + "/maintoolbar/r@2x.png").getAbsolutePath(), true));
		jTouchBar.addItem(new TouchBarItem("Start Rserve", touchBarButtonStartRserve, true));

		TouchBarButton touchBarButtonOpenRPlotPref = new TouchBarButton();
		touchBarButtonOpenRPlotPref.setAction(new TouchBarViewAction() {
			public void onCall(TouchBarView view) {
				PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(null, "com.eco.bio7.RServePlot",
						null, null);
				dialog.open();
			}
		});
		//touchBarButtonOpenRPlotPref.setTitle("R Plot");
		touchBarButtonOpenRPlotPref.setImage(new com.thizzer.jtouchbar.common.Image(
				new File(url.getFile() + "/views/piechartview@2x.png").getAbsolutePath(), true));
		jTouchBar.addItem(new TouchBarItem("R Plot", touchBarButtonOpenRPlotPref, true));

		// jTouchBar.getItems().add( new TouchBarItem(
		// TouchBarItem.NSTouchBarItemIdentifierFlexibleSpace ) );
		TouchBarButton touchBarButtonOpenImageJImage = new TouchBarButton();
		touchBarButtonOpenImageJImage.setAction(new TouchBarViewAction() {

			public void onCall(TouchBarView view) {
				IJ.open();
			}
		});
		//touchBarButtonOpenImageJImage.setTitle("");
		touchBarButtonOpenImageJImage.setImage(new com.thizzer.jtouchbar.common.Image(
				new File(url.getFile() + "/views/imagejview@2x.png").getAbsolutePath(), true));
		jTouchBar.addItem(new TouchBarItem("Open Image", touchBarButtonOpenImageJImage, true));

		return jTouchBar;
	}

	private URL getPath() {
		Bundle bundle = Platform.getBundle("com.eco.bio7");

		URL locationUrl = FileLocator.find(bundle, new Path("icons"), null);
		URL url = null;
		try {
			url = FileLocator.toFileURL(locationUrl);
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		return url;
	}

}
