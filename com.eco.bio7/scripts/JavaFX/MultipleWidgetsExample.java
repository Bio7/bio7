

import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.nebula.visualization.widgets.datadefinition.IManualValueChangeListener;
import org.eclipse.nebula.visualization.widgets.figures.GaugeFigure;
import org.eclipse.nebula.visualization.widgets.figures.KnobFigure;
import org.eclipse.nebula.visualization.widgets.figures.TankFigure;
import org.eclipse.nebula.visualization.widgets.figures.ThermometerFigure;
import org.eclipse.nebula.visualization.xygraph.util.XYGraphMediaFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.eco.bio7.collection.CustomView;

/**
 * A live updated Gauge Example.
 * 
 * @author Xihui Chen
 *
 */
public class MultipleWidgetsExample extends com.eco.bio7.compile.Model {
	public MultipleWidgetsExample() {

		CustomView view = new CustomView();
		Display dis = CustomView.getDisplay();

		dis.syncExec(new Runnable() {
			public void run() {
				org.eclipse.swt.widgets.Composite parent = view.getComposite("MultipleWidgetsExample");

				GridLayout layout = new GridLayout();
				layout.numColumns = 2;
				GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
				parent.setLayout(layout);
				// create canvases to hold the widgets.
				Canvas knobCanvas = new Canvas(parent, SWT.NONE);
				knobCanvas.setLayoutData(gd);
				Canvas gaugeCanvas = new Canvas(parent, SWT.NONE);
				gaugeCanvas.setLayoutData(gd);
				Canvas thermoCanvas = new Canvas(parent, SWT.NONE);
				thermoCanvas.setLayoutData(gd);
				Canvas tankCanvas = new Canvas(parent, SWT.NONE);
				tankCanvas.setLayoutData(gd);

				// use LightweightSystem to create the bridge between SWT and draw2D
				LightweightSystem lws = new LightweightSystem(knobCanvas);
				// Create widgets
				final KnobFigure knobFigure = new KnobFigure();
				lws.setContents(knobFigure);

				lws = new LightweightSystem(gaugeCanvas);
				final GaugeFigure gauge = new GaugeFigure();
				gauge.setBackgroundColor(XYGraphMediaFactory.getInstance().getColor(0, 0, 0));
				gauge.setForegroundColor(XYGraphMediaFactory.getInstance().getColor(255, 255, 255));
				lws.setContents(gauge);

				lws = new LightweightSystem(thermoCanvas);
				final ThermometerFigure thermo = new ThermometerFigure();
				lws.setContents(thermo);

				lws = new LightweightSystem(tankCanvas);
				final TankFigure tank = new TankFigure();
				lws.setContents(tank);

				// Add listener
				knobFigure.addManualValueChangeListener(new IManualValueChangeListener() {
					public void manualValueChanged(double newValue) {
						gauge.setValue(newValue);
						thermo.setValue(newValue);
						tank.setValue(newValue);
					}
				});

				/* Here we have to call the layout method! */
				parent.layout();

			}
		});
	}
}