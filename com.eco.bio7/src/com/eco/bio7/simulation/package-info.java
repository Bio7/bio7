/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************/
/**
 * The simulation API of Bio7: simulation models in Java which run live in Bio7
 * and can be exported as interactive HTML pages (the model is translated to
 * JavaScript and runs in the web browser with the same results).
 * <p>
 * A model extends one of the model classes and implements {@code init()} (the
 * start state) and {@code step()} (one time step):
 * <ul>
 * <li>{@link com.eco.bio7.simulation.GridModel}: a grid of cells with integer
 * states (ImageJ)</li>
 * <li>{@link com.eco.bio7.simulation.ValueGridModel}: a grid of double values
 * with a color map (ImageJ)</li>
 * <li>{@link com.eco.bio7.simulation.AgentModel}: agents in a 3D box (the 3D
 * view)</li>
 * <li>{@link com.eco.bio7.simulation.GeoGridModel}: a georeferenced grid with
 * data read by GDAL (ImageJ, the WorldWind globe, a map in the HTML page)</li>
 * <li>{@link com.eco.bio7.simulation.GeoAgentModel}: agents on the Earth (the
 * WorldWind globe, a map in the HTML page)</li>
 * </ul>
 * Fields with the annotation {@link com.eco.bio7.simulation.Param} are
 * settings (sliders, number fields, checkboxes, choices, text fields), methods
 * with {@link com.eco.bio7.simulation.Action} are buttons. The methods of all
 * models (random numbers, mathematics, statistics, differential equations,
 * charts, parallel random numbers) are inherited from
 * {@link com.eco.bio7.simulation.Simulation}.
 * <p>
 * All models can show images (loadImage, drawImage, Agent.image), animated 3D
 * models of glTF (loadModel, Agent.model, Agent.animation; {@link com.eco.bio7.simulation.Gltf};
 * also OBJ with its materials and STL, converted to glTF), shapes drawn in
 * each step ({@link com.eco.bio7.simulation.Shape}: drawLine, drawPath,
 * drawPolygon, drawCircle, drawRect, drawText; in agent models also at a
 * height z and the 3D shapes drawPoint3, drawPath3, drawBox, drawSphere,
 * drawPolygon3 and drawPillar) and several charts
 * ({@link com.eco.bio7.simulation.ChartData}: chart, chartXY, chartBars; the
 * Charts view of Bio7). The mathematical functions (vectors, matrices,
 * regression, quantiles, the normal distribution, noise) give the same results
 * in Bio7 and in the HTML page. The first setup arranges the views in the
 * Custom perspective: the settings on the left, the editor above the display
 * of the model and the charts beside the display.
 * <p>
 * The 3D scene of an agent model: the shape of the agents (spheres or
 * gliders), a floor, shadows, a camera which follows an agent and objects of
 * the environment (placeModel). Agents can be linked to networks (link,
 * linkRandom, linkSmallWorld, linkScaleFree, linkNearby, components,
 * shortestPath, clustering, layoutSpring; drawn as lines), and a
 * {@link com.eco.bio7.simulation.SpatialIndex} finds the neighbors of many
 * agents fast.
 * <p>
 * A model can react to the mouse and the keyboard of its view (the ImageJ
 * image of a grid, the 3D view of an agent model, the HTML export):
 * {@link com.eco.bio7.simulation.Simulation#click(double, double)},
 * {@link com.eco.bio7.simulation.Simulation#drag(double, double)} (in the 3D
 * view with Shift) and {@link com.eco.bio7.simulation.Simulation#key(String)},
 * called between the steps.
 * <p>
 * Grid models can carry agents ({@link com.eco.bio7.simulation.GridAgent}:
 * addAgent, forward, face, agentsAt, agentsNear), drawn on top of the grid.
 * A model ends a run with stop(); the field Seed of the settings (and of the
 * page) sets the seed of the setup.
 * <p>
 * An {@link com.eco.bio7.simulation.Experiment} runs a model several times
 * without a display (a sweep of the parameters, repetitions with other seeds,
 * parallel threads) and returns a table (CSV, a data frame in R) of the
 * values of the line charts and of record(name, value); in Bio7 also the
 * button "Experiment..." of the settings (Java, Python and R models).
 * <p>
 * The wizard "Bio7 Simulation Project" creates a model (in Java, Python or R),
 * "Export Simulation as HTML..." (the menu of the Play/Pause button, the
 * Settings view or the context menu of the Java and R editors) exports it
 * ({@link com.eco.bio7.simulation.SimulationExport}). The examples are in the
 * project Bio7_Java_Examples (packages simulation and simulation.api).
 * <p>
 * <b>Python and R:</b> the same models can be written as scripts with the
 * module bio7sim of the embedded Python ({@code from bio7sim import *}, numpy
 * arrays) or the functions of bio7sim of the embedded R ({@code grid_model()},
 * matrices and data frames). {@code simulation(model)} at the end of the script
 * starts a Java model of the same kind
 * ({@link com.eco.bio7.simulation.ScriptSimulation}): the same displays
 * (ImageJ, the 3D view, the WorldWind globe), the same settings, the same
 * geospatial data of GDAL. The HTML export
 * ({@link com.eco.bio7.simulation.ScriptExport}) runs the script in the web
 * browser with Pyodide or webR (loaded from the internet) with the same
 * results. The examples are in the projects Bio7_Python_Examples and
 * Bio7_R_Examples (folder Simulation).
 */
package com.eco.bio7.simulation;
