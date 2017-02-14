package com.eco.bio7.plot;

import java.awt.Color;
import java.util.ArrayList;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardLegend;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.XYLineAndShapeRenderer;
import org.jfree.data.XYDataset;
import org.jfree.data.XYSeries;
import org.jfree.data.XYSeriesCollection;
import org.jfree.ui.Spacer;

import com.eco.bio7.time.*;

import com.eco.bio7.discrete.Field;
import com.eco.bio7.methods.*;

public class LineChart  {
	private static ChartPanel chartPanel;

	private static int RGB[] = new int[3];// Colour Array

	private final static XYSeriesCollection dataset = new XYSeriesCollection();

	private static XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();

	private static ArrayList<XYSeries> Series = new ArrayList<XYSeries>();

	final static JFreeChart chart = ChartFactory.createXYLineChart("States", "Iteration", "Amount", dataset, PlotOrientation.VERTICAL, true, // include
			// legend
			true, // tooltips
			false // urls
			);

	final static XYPlot plot = chart.getXYPlot();

	int zaehler = 1;

	public static ArrayList<LineChartCounter> zaehl = new ArrayList<LineChartCounter>();// resizeable count list

	public LineChart() {
		deleteDataset();

		for (int i = 0; i < CurrentStates.getR().size(); i++) {
			zaehl.add(new LineChartCounter());

			Series.add(new XYSeries(CurrentStates.getStateName(i)));
		}

		ThreadLineChart a = new ThreadLineChart(this);

		a.start();
		final XYDataset dataset = createDataset();
		final JFreeChart chart = createChart(dataset);
		chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));

		

		renderer();

	}

	public static void delete() {
		for (int i = 0; i < CurrentStates.getR().size(); i++) {

			Series.clear();
		}
	}

	/*private void exitForm(java.awt.event.WindowEvent evt) {
		for (int i = 0; i < CurrentStates.getR().size(); i++) {

			Series.clear();
		}
		//dispose();
	}*/

	public void reset() {
		for (int i = 0; i < zaehl.size(); i++) {

			LineChartCounter zahl = (LineChartCounter) zaehl.get(i);

			zahl.reset();

		}
	}

	public void datenin() {

		reset();// start new count
		if (Time.isPause() == false) {

			for (int i = 0; i < Field.getHeight(); i++) {
				for (int u = 0; u < Field.getWidth(); u++) {

					if (Field.getState(u, i) < CurrentStates.getStateList().size() && CurrentStates.getStateList().size() > 0) {

						LineChartCounter zahl = (LineChartCounter) zaehl.get(Field.getState(u, i));

						zahl.setZahl();

					}

				}

			}

			for (int i = 0; i < CurrentStates.getR().size(); i++) {
				XYSeries xyseries = (XYSeries) Series.get(i);

				LineChartCounter zahl = (LineChartCounter) zaehl.get(i);

				xyseries.add(Time.getCounter(), zahl.getZahl());
			}

		}

	}

	public static void deleteDataset() {
		dataset.removeAllSeries();
		Series.clear();
	}

	public XYDataset createDataset() {

		for (int i = 0; i < CurrentStates.getR().size(); i++) {

			Series.add(new XYSeries(CurrentStates.getStateName(i)));
			XYSeries xyseries = (XYSeries) Series.get(i);
			xyseries.setMaximumItemCount(50);
			zaehl.add(new LineChartCounter());
		}

		for (int i = 0; i < CurrentStates.getR().size(); i++) {
			XYSeries xyseries = (XYSeries) Series.get(i);

			dataset.addSeries(xyseries);
		}

		return dataset;

	}

	JFreeChart createChart(final XYDataset dataset) {

		chart.setBackgroundPaint(Color.white);

		final StandardLegend legend = (StandardLegend) chart.getLegend();
		legend.setDisplaySeriesShapes(false);// 

		plot.setBackgroundPaint(Color.lightGray);
		plot.setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5.0, 5.0, 5.0, 5.0));
		plot.setDomainGridlinePaint(Color.white);
		plot.setRangeGridlinePaint(Color.white);

		for (int i = 0; i < CurrentStates.getR().size(); i++) {
			RGB = CurrentStates.getRGB(i);
			renderer.setSeriesLinesVisible(i, true);
			renderer.setSeriesShapesVisible(i, false);

		}
		plot.setRenderer(renderer);

		final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

		return chart;

	}

	public static void renderer() {
		for (int i = 0; i < CurrentStates.getR().size(); i++) {
			RGB = CurrentStates.getRGB(i);

			renderer.setSeriesPaint(i, new Color(RGB[0], RGB[1], RGB[2]));
			renderer.setSeriesShapesVisible(i, false);

		}
		plot.setRenderer(renderer);

	}

	public void run() {

	}

	public static void update() {
		deleteDataset();
		LineChartView.linechart.createDataset();
		renderer();
	}

	public static ChartPanel getChartPanel() {
		return chartPanel;
	}
	public static JFreeChart getChart() {
		return chart;
	}

}
