package com.eco.bio7.plot;

import java.awt.Color;
import java.util.ArrayList;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.DefaultPieDataset;
import com.eco.bio7.discrete.Field;
import com.eco.bio7.methods.CurrentStates;
import com.eco.bio7.time.ThreadPieChart;

public class Pie {

	private static PiePlot plot;

	private static DefaultPieDataset data = new DefaultPieDataset();

	private double zaehlp = 0;

	private static JFreeChart chart;

	private int RGB[] = new int[3];

	private static ArrayList<PieChartCounter> zaehlpie = new ArrayList<PieChartCounter>();// resizeable count list

	private static PieChartCounter zahl = null;

	public Pie() {

		for (int i = 0; i < CurrentStates.getR().size(); i++) {
			zaehlpie.add(new PieChartCounter());
		}
		ThreadPieChart att = new ThreadPieChart(this);

		att.start();
		chart = ChartFactory.createPieChart("States", data, true, true, false

		);
		chart.setBackgroundPaint(Color.white);

		plotter();

	}

	public void plotter() {

		plot = (PiePlot) chart.getPlot();

		for (int i = 0; i < CurrentStates.getR().size(); i++) {// for every
																// plant

			zaehlpie.add(new PieChartCounter());
			RGB = CurrentStates.getRGB(i);

			plot.setSectionPaint(i, new Color(RGB[0], RGB[1], RGB[2]));// draw

		}
	}

	public void werteneu() {

		data.removeAll();
		for (int i = 0; i < CurrentStates.getStateList().size(); i++) {
			zahl = (PieChartCounter) zaehlpie.get(i);
			int z = zahl.getZahl();

			data.setValue(CurrentStates.getStateName(i), z);

		}

	}

	public void reset() {
		for (int i = 0; i < CurrentStates.getStateList().size(); i++) {
			zahl = (PieChartCounter) zaehlpie.get(i);

			zahl.reset();
		}

	}

	public void dateninPie() {

		reset();// new count

		for (int i = 0; i < Field.getHeight(); i++) {
			for (int u = 0; u < Field.getWidth(); u++) {

				if (Field.getState(u, i) < CurrentStates.getStateList().size()
						&& CurrentStates.getStateList().size() > 0) {

					zahl = (PieChartCounter) zaehlpie.get(Field.getState(u, i));

					zahl.setZahl();

				}

			}

		}

		for (int i = 0; i < CurrentStates.getR().size(); i++) {

			zaehlp = zahl.getZahl();

		}

	}

	public void creation() {

	}

	public static void update() {
		PieChartView.getPiechart().plotter();
		PieChartView.getPiechart().werteneu();

	}

	public static JFreeChart getChart() {
		return chart;
	}
}
