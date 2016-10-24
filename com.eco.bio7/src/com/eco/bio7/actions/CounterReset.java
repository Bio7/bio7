package com.eco.bio7.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import com.eco.bio7.discrete.Quad2d;
import com.eco.bio7.plot.LineChart;
import com.eco.bio7.plot.LineChartView;
import com.eco.bio7.rcp.ApplicationActionBarAdvisor;
import com.eco.bio7.time.Time;

public class CounterReset extends Action {

	private final IWorkbenchWindow window;

	public CounterReset(String text, IWorkbenchWindow window) {
		super(text);
		this.window = window;
		
		setId("com.eco.bio7.counter_reset");
		
		setImageDescriptor(com.eco.bio7.Bio7Plugin.getImageDescriptor("/icons/maintoolbar/counter_reset.png"));
	}

	public void run() {

		com.eco.bio7.time.Time.Timereset();
		LineChart.deleteDataset();
		LineChartView.linechart.createDataset();
		LineChart.renderer();

		Quad2d.getQuad2dInstance().createzaehler();// reset the counter object
													// !
		Time.setCounter(0);
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {

				ApplicationActionBarAdvisor.getUserItem().setText("Timesteps: " + Time.getCounter());

			}
		});

	}

}