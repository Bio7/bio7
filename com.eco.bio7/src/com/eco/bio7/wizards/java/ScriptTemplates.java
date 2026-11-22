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
package com.eco.bio7.wizards.java;

import com.eco.bio7.wizards.java.SimulationModelWizardPage.Kind;

/**
 * The templates of the simulation wizard for Python and R (the modules bio7sim):
 * a majority rule (a grid with states), a diffusion (a grid with values) and
 * walkers (agents in 3D), as the templates of Java.
 */
public final class ScriptTemplates {

	private ScriptTemplates() {
	}

	/** The name of the file of a model: MyModel -> my_model. */
	public static String fileName(String name) {
		return name.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(java.util.Locale.ROOT);
	}

	/* ===================== Python ===================== */

	/** The script of a model in Python. */
	public static String python(String name, Kind kind) {
		String header = """
				\"""%1$s: a simulation model of the simulation API of Bio7 (the module bio7sim).

				Run the script in Bio7 (Run in the Python editor): simulation(%1$s) at the end starts
				the model with the views of the model in the Custom perspective (the settings on the
				left, %2$s on the right). Start it with the Play/Pause button, Setup calls init()
				again. "Export Simulation as HTML..." (the menu of Play/Pause, the Settings view)
				writes a page which runs this script in the web browser (Pyodide: Python and numpy
				in WebAssembly, loaded from the internet) with the same results.

				The model:
				  init(self)          the start state (also after Setup and a restart setting)
				  step(self)          one time step
				  x = param(value, min=, max=, step=, label=, options=[...], restart=True, group=)
				                      a setting (a slider, a number, a checkbox, a choice, a text)
				  @action("Label")    a button (a method without parameters)
				  self.chart(name, value)   a value of a chart (one value per step)
				  self.t              the number of the step

				Random numbers (the same in Bio7 and in the browser): self.random(), self.random(min, max),
				self.random_int(n), self.chance(p), self.random_gaussian(mean, sd), self.random_array(shape),
				self.rng (a numpy Generator), self.seed(n). Functions: clamp, lerp, map_range, rk4, ...
				%3$s
				\"""
				""".formatted(name, kind == Kind.AGENTS ? "the 3D view" : "the grid in ImageJ", switch (kind) {
		case GRID -> """

				GridModel: self.size(w, h), self.cells[y, x] (a numpy array of integer states),
				self.palette(colors...), self.wrap (a torus), self.count_neighbors(state) (an array of
				the counts of the 8 neighbors), self.count4(state), self.count(state), self.get(x, y),
				self.set(x, y, state).""";
		case VALUES -> """

				ValueGridModel: self.size(w, h), self.values[y, x] (a numpy array of doubles),
				self.range(min, max), self.color_map(colors...), self.wrap, self.laplacian(),
				self.sum4(), self.sum8() (arrays), self.mean(), self.get(x, y), self.set(x, y, value).""";
		case AGENTS -> """

				AgentModel: self.agents (a list of Agent: x, y, z, color and own fields),
				self.box(w, h, d), self.agent_radius, self.agent_color, self.positions() (an array
				(n, 3)), self.neighbors(agent, r), self.nearest(agent, r), self.distance(a, b),
				self.keep_in_box(agent), self.wrap_in_box(agent).""";
		});
		String body = switch (kind) {
		case GRID -> """
				import numpy as np
				from bio7sim import GridModel, param, action, simulation


				class %1$s(GridModel):
				    \"""A majority rule: a cell takes the state of the majority of its neighborhood (with some noise). Black and white regions form and grow.\"""

				    grid_size = param(100, min=20, max=300, label="Grid size (cells)", restart=True, group="World")
				    share = param(0.5, min=0, max=1, step=0.01, label="Black cells at the start", restart=True, group="World")
				    noise = param(0.002, min=0, max=0.1, step=0.001, label="Noise (a random state)", group="Rule")

				    def init(self):
				        self.size(self.grid_size, self.grid_size)
				        self.palette("#ffffff", "#1d2433")
				        self.cells[:] = self.random_array() < self.share

				    def step(self):
				        # the black cells of the 3 x 3 neighborhood (the 8 neighbors and the cell) of all cells
				        black = self.count_neighbors(1) + self.cells
				        state = (black > 4).astype(np.int32)
				        noisy = self.random_array() < self.noise
				        state[noisy] = self.random_array()[noisy] < 0.5
				        self.cells = state
				        self.chart("Black cells", self.count(1))

				    @action("Add a black square")
				    def add_square(self):
				        cx, cy = self.random_int(self.width), self.random_int(self.height)
				        ys = np.arange(cy - 5, cy + 6) %% self.height
				        xs = np.arange(cx - 5, cx + 6) %% self.width
				        self.cells[np.ix_(ys, xs)] = 1


				simulation(%1$s)
				""";
		case VALUES -> """
				import numpy as np
				from bio7sim import ValueGridModel, param, action, simulation


				class %1$s(ValueGridModel):
				    \"""A diffusion: sources keep a value of 1, the value spreads to the neighbors and decays.\"""

				    grid_size = param(100, min=20, max=250, label="Grid size (cells)", restart=True, group="World")
				    diffusion = param(0.2, min=0, max=0.25, step=0.005, label="Diffusion", group="Process")
				    decay = param(0.002, min=0, max=0.02, step=0.0005, label="Decay", group="Process")

				    def init(self):
				        self.size(self.grid_size, self.grid_size)
				        self.wrap = False
				        self.range(0, 1)
				        self.source = np.zeros((self.height, self.width), dtype=bool)
				        self.add_source()

				    def step(self):
				        v = self.values
				        # the diffusion to the 4 neighbors and the decay (all cells at once)
				        v = v + self.diffusion * self.laplacian() - self.decay * v
				        v[self.source] = 1.0
				        self.values = v
				        self.chart("Mean value", self.mean())

				    @action("Add a source")
				    def add_source(self):
				        x, y = 2 + self.random_int(self.width - 4), 2 + self.random_int(self.height - 4)
				        self.source[y, x] = True
				        self.values[y, x] = 1.0


				simulation(%1$s)
				""";
		case AGENTS -> """
				import numpy as np
				from bio7sim import AgentModel, Agent, param, action, simulation


				class Walker(Agent):
				    \"""A walker: the position (Agent) and the velocity.\"""

				    def __init__(self, x, y, z, vx, vy, vz):
				        super().__init__(x, y, z)
				        self.vx, self.vy, self.vz = vx, vy, vz


				class %1$s(AgentModel):
				    \"""Walkers in a box: each walker changes its direction randomly, is attracted to the center of the box and turns back at the walls.\"""

				    walkers = param(100, min=10, max=500, label="Number of walkers", restart=True, group="Start")
				    speed = param(1.0, min=0.1, max=3, step=0.1, label="Speed", group="Movement")
				    attraction = param(0.005, min=0, max=0.05, step=0.001, label="Attraction to the center", group="Movement")

				    def init(self):
				        self.box(100, 100, 60)
				        self.agent_radius = 1.2
				        self.add_walkers(self.walkers)

				    def add_walkers(self, n):
				        for _ in range(n):
				            self.agents.append(Walker(self.random() * self.box_width, self.random() * self.box_height, self.random() * self.box_depth,
				                                      self.random() - 0.5, self.random() - 0.5, self.random() - 0.5))

				    def step(self):
				        size = (self.box_width, self.box_height, self.box_depth)
				        for w in self.agents:
				            # a random turn and the attraction to the center
				            v = [w.vx, w.vy, w.vz]
				            p = [w.x, w.y, w.z]
				            for k in range(3):
				                v[k] += (self.random() - 0.5) * 0.3 + self.attraction * (size[k] / 2 - p[k])
				            length = (v[0] ** 2 + v[1] ** 2 + v[2] ** 2) ** 0.5
				            if length > 0:
				                v = [c / length * self.speed for c in v]
				            # the walls: the walker turns back
				            for k in range(3):
				                p[k] += v[k]
				                if p[k] < 0 or p[k] > size[k]:
				                    v[k] = -v[k]
				                    p[k] = min(max(p[k], 0.0), size[k])
				            w.x, w.y, w.z = p
				            w.vx, w.vy, w.vz = v
				        self.chart("Mean height", self.positions()[:, 2].mean())

				    @action("Add 10 walkers")
				    def more_walkers(self):
				        self.add_walkers(10)


				simulation(%1$s)
				""";
		};
		return header + body.formatted(name);
	}

	/* ===================== R ===================== */

	/** The script of a model in R. */
	public static String r(String name, Kind kind) {
		String header = """
				# %1$s: a simulation model of the simulation API of Bio7 (the functions of bio7sim).
				#
				# Run the script in Bio7 (source it in the R editor): simulation(m) at the end starts the model
				# with the views of the model in the Custom perspective (the settings on the left, %2$s
				# on the right). Start it with the Play/Pause button, Setup calls m$init again. "Export
				# Simulation as HTML..." (the menu of Play/Pause, the Settings view) writes a page which runs
				# this script in the web browser (webR: R in WebAssembly, loaded from the internet) with the
				# same results.
				#
				# The model (an environment):
				#   m$init <- function(m) ...   the start state (also after Setup and a restart setting)
				#   m$step <- function(m) ...   one time step
				#   m$param(name, value, min =, max =, step =, label =, options =, restart = TRUE, group =, type =)
				#                               a setting (a slider, a number, a checkbox, a choice, a text)
				#   m$action(name, function(m) ..., label =, group =)   a button
				#   m$chart(name, value)        a value of a chart (one value per step)
				#   m$t                         the number of the step
				#
				# Random numbers (the generator of R: the same in Bio7 and in the browser): m$random(n, min, max),
				# m$random_int(min, max, n), m$chance(p, n), m$random_gaussian(n, mean, sd), m$seed(n) (and runif,
				# rnorm, sample of R). Functions: clamp, lerp, map_range, rk4, ...
				#%3$s

				""".formatted(name, kind == Kind.AGENTS ? "the 3D view" : "the grid in ImageJ", switch (kind) {
		case GRID -> """

				# grid_model: m$size(w, h), m$cells[y, x] (an integer matrix: rows y from the top, columns x),
				# m$palette(colors...), m$wrap (a torus), m$count_neighbors(state) (a matrix of the counts of
				# the 8 neighbors), m$count4(state), m$count(state), m$get(x, y), m$set(x, y, state) (1-based).""";
		case VALUES -> """

				# value_grid_model: m$size(w, h), m$values[y, x] (a matrix of doubles), m$range(min, max),
				# m$color_map(colors...), m$wrap, m$laplacian(), m$sum4(), m$sum8() (matrices), m$mean().""";
		case AGENTS -> """

				# agent_model: m$agents (a data frame: x, y, z, color and own columns), m$box(w, h, d),
				# m$agent_radius, m$agent_color, m$neighbors(i, r), m$nearest(i, r), m$distance(i, j),
				# m$keep_in_box(), m$wrap_in_box() (all agents).""";
		});
		String body = switch (kind) {
		case GRID -> """
				m <- grid_model("%1$s", "A majority rule: a cell takes the state of the majority of its neighborhood (with some noise). Black and white regions form and grow.")

				m$param("grid_size", 100, min = 20, max = 300, label = "Grid size (cells)", restart = TRUE, group = "World")
				m$param("share", 0.5, min = 0, max = 1, step = 0.01, label = "Black cells at the start", restart = TRUE, group = "World")
				m$param("noise", 0.002, min = 0, max = 0.1, step = 0.001, label = "Noise (a random state)", group = "Rule")

				m$init <- function(m) {
				  m$size(m$grid_size, m$grid_size)
				  m$palette("#ffffff", "#1d2433")
				  m$cells[] <- as.integer(m$random(length(m$cells)) < m$share)
				}

				m$step <- function(m) {
				  # the black cells of the 3 x 3 neighborhood (the 8 neighbors and the cell) of all cells
				  black <- m$count_neighbors(1) + m$cells
				  state <- (black > 4) * 1L
				  noisy <- m$random(length(state)) < m$noise
				  state[noisy] <- as.integer(m$random(sum(noisy)) < 0.5)
				  m$cells <- state
				  m$chart("Black cells", m$count(1))
				}

				m$action("add_square", function(m) {
				  cx <- m$random_int(1, m$width); cy <- m$random_int(1, m$height)
				  ys <- ((cy - 6):(cy + 4)) %%%% m$height + 1
				  xs <- ((cx - 6):(cx + 4)) %%%% m$width + 1
				  m$cells[ys, xs] <- 1L
				}, label = "Add a black square")

				simulation(m)
				""";
		case VALUES -> """
				m <- value_grid_model("%1$s", "A diffusion: sources keep a value of 1, the value spreads to the neighbors and decays.")

				m$param("grid_size", 100, min = 20, max = 250, label = "Grid size (cells)", restart = TRUE, group = "World")
				m$param("diffusion", 0.2, min = 0, max = 0.25, step = 0.005, label = "Diffusion", group = "Process")
				m$param("decay", 0.002, min = 0, max = 0.02, step = 0.0005, label = "Decay", group = "Process")

				add_source <- function(m) {
				  x <- m$random_int(3, m$width - 2); y <- m$random_int(3, m$height - 2)
				  m$source[y, x] <- TRUE
				  m$values[y, x] <- 1
				}

				m$init <- function(m) {
				  m$size(m$grid_size, m$grid_size)
				  m$wrap <- FALSE
				  m$range(0, 1)
				  m$source <- matrix(FALSE, m$height, m$width)
				  add_source(m)
				}

				m$step <- function(m) {
				  v <- m$values
				  # the diffusion to the 4 neighbors and the decay (all cells at once)
				  v <- v + m$diffusion * m$laplacian() - m$decay * v
				  v[m$source] <- 1
				  m$values <- v
				  m$chart("Mean value", m$mean())
				}

				m$action("add_source", add_source, label = "Add a source")

				simulation(m)
				""";
		case AGENTS -> """
				m <- agent_model("%1$s", "Walkers in a box: each walker changes its direction randomly, is attracted to the center of the box and turns back at the walls.")

				m$param("walkers", 100, min = 10, max = 500, label = "Number of walkers", restart = TRUE, group = "Start")
				m$param("speed", 1, min = 0.1, max = 3, step = 0.1, label = "Speed", group = "Movement")
				m$param("attraction", 0.005, min = 0, max = 0.05, step = 0.001, label = "Attraction to the center", group = "Movement")

				# n new walkers: the position and the velocity (the columns of the data frame m$agents)
				new_walkers <- function(m, n) {
				  data.frame(x = m$random(n) * m$box_width, y = m$random(n) * m$box_height, z = m$random(n) * m$box_depth,
				             vx = m$random(n) - 0.5, vy = m$random(n) - 0.5, vz = m$random(n) - 0.5)
				}

				m$init <- function(m) {
				  m$box(100, 100, 60)
				  m$agent_radius <- 1.2
				  m$agents <- new_walkers(m, m$walkers)
				}

				m$step <- function(m) {
				  a <- m$agents
				  n <- nrow(a)
				  size <- c(x = m$box_width, y = m$box_height, z = m$box_depth)
				  # a random turn and the attraction to the center (all walkers at once)
				  for (k in c("x", "y", "z")) {
				    v <- paste0("v", k)
				    a[[v]] <- a[[v]] + (m$random(n) - 0.5) * 0.3 + m$attraction * (size[[k]] / 2 - a[[k]])
				  }
				  len <- sqrt(a$vx^2 + a$vy^2 + a$vz^2)
				  f <- ifelse(len > 0, m$speed / len, 0)
				  # the walls: the walkers turn back
				  for (k in c("x", "y", "z")) {
				    v <- paste0("v", k)
				    a[[v]] <- a[[v]] * f
				    a[[k]] <- a[[k]] + a[[v]]
				    out <- a[[k]] < 0 | a[[k]] > size[[k]]
				    a[[v]][out] <- -a[[v]][out]
				    a[[k]] <- pmin(pmax(a[[k]], 0), size[[k]])
				  }
				  m$agents <- a
				  m$chart("Mean height", mean(a$z))
				}

				m$action("more_walkers", function(m) m$agents <- rbind(m$agents, new_walkers(m, 10)), label = "Add 10 walkers")

				simulation(m)
				""";
		};
		return header + body.formatted(name);
	}
}
