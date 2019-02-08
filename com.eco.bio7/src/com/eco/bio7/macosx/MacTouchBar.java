package com.eco.bio7.macosx;

import org.eclipse.swt.widgets.Scale;

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
		JTouchBar jTouchBar = new JTouchBar();
		jTouchBar.setCustomizationIdentifier("Bio7 Touchbar");

		// flexible space
		jTouchBar.getItems().add(new TouchBarItem(TouchBarItem.NSTouchBarItemIdentifierFlexibleSpace));

		// button
		TouchBarItem touchBarButtonItem = new TouchBarItem("T1");
		touchBarButtonItem.setCustomizationAllowed(true);

		TouchBarButton touchBarButtonStartStop = new TouchBarButton();
		touchBarButtonStartStop.setAction(new TouchBarViewAction() {

			public void onCall(TouchBarView view) {
				Bio7Action.startCalculation();
			}
		});
		touchBarButtonStartStop.setTitle("Play/Pause");
		//touchBarButtonStartStop.setImage(new Image(ImageName.NSImageNameTouchBarAlarmTemplate, false));
		jTouchBar.addItem(new TouchBarItem("Play/Pause", touchBarButtonStartStop, true));

		TouchBarButton touchBarButtonSetup = new TouchBarButton();
		touchBarButtonSetup.setAction(new TouchBarViewAction() {

			public void onCall(TouchBarView view) {
				Bio7Action.reset();
			}
		});
		touchBarButtonSetup.setTitle("Reset");
		jTouchBar.addItem(new TouchBarItem("Reset", touchBarButtonSetup, true));

		TouchBarButton touchBarButtonStartRserve = new TouchBarButton();
		touchBarButtonStartRserve.setAction(new TouchBarViewAction() {
			public void onCall(TouchBarView view) {
				Bio7Action.callRserve();
			}
		});
		touchBarButtonStartRserve.setTitle("Start Rserve");
		jTouchBar.addItem(new TouchBarItem("Start Rserve", touchBarButtonStartRserve, true));

		// jTouchBar.getItems().add( new TouchBarItem(
		// TouchBarItem.NSTouchBarItemIdentifierFlexibleSpace ) );
		TouchBarButton touchBarButtonOpenImageJImage = new TouchBarButton();
		touchBarButtonOpenImageJImage.setAction(new TouchBarViewAction() {

			public void onCall(TouchBarView view) {
				IJ.open();
			}
		});
		touchBarButtonOpenImageJImage.setTitle("Image");
		jTouchBar.addItem(new TouchBarItem("Image", touchBarButtonOpenImageJImage, true));

		TouchBarSlider slider = new TouchBarSlider();
		slider.setMinValue(1.0);
		slider.setMaxValue(1000.0);

		slider.setActionListener(new SliderActionListener() {
			@Override
			public void sliderValueChanged(TouchBarSlider slider, double value) {
				System.out.println("Selected Scrubber Index: " + value);

				Scale scale = InfoView.getTimeScaleSwt();
				scale.setSelection((int) (value));
				Time.setInterval((int) (1000.0 - value));
			}
		});

		jTouchBar.addItem(new TouchBarItem("Speed", slider, true));

		return jTouchBar;
	}

}
