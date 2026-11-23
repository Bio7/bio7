# Copyright (c) 2007-2026 M. Austenfeld
# SPDX-License-Identifier: EPL-2.0
"""The simulation API of Bio7 for Python (the module bio7sim).

A model is a class which extends one of the model classes and implements
init() (the start state) and step() (one time step). The settings are class
attributes with param(...), the buttons methods with @action. The last line
of the script starts the model with simulation(MyModel):

    from bio7sim import *

    class Life(GridModel):
        density = param(0.3, min=0, max=1, step=0.01, label="Start density", restart=True)

        def init(self):
            self.size(100, 100)
            self.palette("#ffffff", "#1f3a93")
            self.cells[:] = self.random_array() < self.density

        def step(self):
            n = self.count_neighbors(1)
            self.cells = ((n == 3) | ((self.cells == 1) & (n == 2))).astype(int)
            self.chart("Living cells", self.count(1))

    simulation(Life)

In Bio7 the model runs with the Play/Pause button: a grid in ImageJ, agents in
the 3D view, geospatial models on the WorldWind globe. "Export Simulation as
HTML..." writes a page which runs the same script in the web browser (Pyodide,
Python in WebAssembly) with the same results (the random numbers of numpy
with the same seed).

Models: GridModel (integer states), ValueGridModel (double values),
AgentModel (agents in a 3D box), GeoGridModel (a georeferenced grid with data
of GDAL), GeoAgentModel (agents on the Earth). The grids are numpy arrays
(cells[y, x], shape (height, width)).
"""
import json
import math

import numpy as np

__all__ = ["param", "action", "simulation", "Simulation", "GridModel", "ValueGridModel", "AgentModel", "Agent",
           "GeoGridModel", "GeoAgentModel", "GeoAgent", "clamp", "lerp", "map_range", "wrap_around", "sq",
           "distance", "distance3", "logistic", "logistic_growth", "mean", "variance", "sd", "rk4", "VIRIDIS",
           "noise", "fractal_noise", "heading_of", "linear_regression", "normal_pdf", "normal_cdf"]

# True in the web browser (set by the exported page before the model runs)
BROWSER = False
# The data of a geospatial model in the browser (set by the page)
GEO = None

DEFAULT_SEED = 42
VIRIDIS = ["#440154", "#3b528b", "#21908c", "#5dc963", "#fde725"]


# ------------------------------------------------------------------ settings


class param:
    """A setting of the model (a slider, a number field, a checkbox, a choice or a text field).

    value: the start value (its type is the kind: bool, int, float or str),
    min, max (and step): a slider, options: a choice of strings, label: the
    text, restart: init() again after a change, group: a heading above the
    settings of the group.
    """

    def __init__(self, value, min=None, max=None, step=None, label=None, options=None, restart=False, group=""):
        self.value = value
        self.min, self.max, self.step = min, max, step
        self.label = label
        self.options = list(options) if options else []
        self.restart = bool(restart)
        self.group = group or ""
        if isinstance(value, bool):
            self.kind = "boolean"
        elif isinstance(value, int):
            self.kind = "int"
        elif isinstance(value, float):
            self.kind = "double"
        else:
            self.kind = "string"
        self.name = None

    def __set_name__(self, owner, name):
        self.name = name

    def __get__(self, obj, owner=None):
        if obj is None:
            return self
        return obj.__dict__.get(self.name, self.value)

    def __set__(self, obj, value):
        obj.__dict__[self.name] = self.convert(value)

    def convert(self, value):
        if self.kind == "boolean":
            return value if isinstance(value, bool) else str(value).lower() in ("true", "1", "yes", "on")
        if self.kind == "int":
            return int(round(float(value)))
        if self.kind == "double":
            return float(value)
        return str(value)

    def spec(self):
        return {"name": self.name, "label": self.label or _words(self.name), "kind": self.kind, "value": self.value,
                "min": _num(self.min), "max": _num(self.max), "step": _num(self.step), "options": self.options,
                "restart": self.restart, "group": self.group}


def action(label=None, group=""):
    """A button of the model: @action or @action("Label", group="...") on a method without parameters."""
    if callable(label):
        label._bio7_action = {"label": _words(label.__name__), "group": group}
        return label

    def mark(f):
        f._bio7_action = {"label": label or _words(f.__name__), "group": group}
        return f
    return mark


def _num(v):
    return None if v is None else float(v)


def _words(name):
    out = []
    for i, c in enumerate(name.replace("_", " ")):
        if c.isupper() and i > 0 and name[i - 1].islower():
            out.append(" ")
        out.append(c)
    s = "".join(out).strip()
    return s[:1].upper() + s[1:]


# ------------------------------------------------------------------ functions


def clamp(value, lo, hi):
    """The value in the range lo to hi (also arrays)."""
    return np.clip(value, lo, hi) if isinstance(value, np.ndarray) else max(lo, min(hi, value))


def lerp(a, b, t):
    """The linear interpolation a + (b - a) * t."""
    return a + (b - a) * t


def map_range(value, in_min, in_max, out_min, out_max):
    """The value of the range in_min to in_max in the range out_min to out_max."""
    return out_min + (value - in_min) * (out_max - out_min) / (in_max - in_min)


def wrap_around(value, size):
    """The value in 0 to size (a torus)."""
    return ((value % size) + size) % size


def sq(value):
    return value * value


def distance(x1, y1, x2, y2):
    return math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1))


def distance3(x1, y1, z1, x2, y2, z2):
    return math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1) + (z2 - z1) * (z2 - z1))


def logistic(x):
    return 1 / (1 + math.exp(-x))


def logistic_growth(n, r, k):
    """The logistic growth rate r * n * (1 - n / k)."""
    return r * n * (1 - n / k)


def mean(values):
    v = np.asarray(values, dtype=float)
    return float(v.mean()) if v.size else 0.0


def variance(values):
    """The sample variance (n - 1)."""
    v = np.asarray(values, dtype=float)
    return float(v.var(ddof=1)) if v.size > 1 else 0.0


def sd(values):
    return math.sqrt(variance(values))


def rk4(f, t, y, dt):
    """One step of the Runge-Kutta method: f(t, y) returns the derivatives of y."""
    y = np.asarray(y, dtype=float)
    k1 = np.asarray(f(t, y), dtype=float)
    k2 = np.asarray(f(t + dt / 2, y + dt / 2 * k1), dtype=float)
    k3 = np.asarray(f(t + dt / 2, y + dt / 2 * k2), dtype=float)
    k4 = np.asarray(f(t + dt, y + dt * k3), dtype=float)
    return y + dt / 6 * (k1 + 2 * k2 + 2 * k3 + k4)


def noise(x, y):
    """A smooth noise (0 - 1) at x, y (value noise, also for arrays; the same values as noise of Java)."""
    x, y = np.asarray(x, dtype=float), np.asarray(y, dtype=float)
    ix, iy = np.floor(x).astype(np.int64), np.floor(y).astype(np.int64)
    fx, fy = x - ix, y - iy
    sx, sy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)

    def corner(cx, cy):
        h = (cx * 374761393 + cy * 668265263) & 0xFFFFFFFF
        h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
        h = h ^ (h >> 16)
        return (h & 0xFFFFFF) / 16777216.0
    a, b, c, d = corner(ix, iy), corner(ix + 1, iy), corner(ix, iy + 1), corner(ix + 1, iy + 1)
    v = a + (b - a) * sx + (c - a) * sy + (a - b - c + d) * sx * sy
    return float(v) if v.ndim == 0 else v


def fractal_noise(x, y, octaves=4):
    """A fractal noise (0 - 1): the sum of octaves of noise (finer details with more octaves)."""
    total, amplitude, f, s = 0.0, 1.0, 1.0, 0.0
    for k in range(octaves):
        s = s + amplitude * noise(np.asarray(x) * f + k * 17.3, np.asarray(y) * f + k * 11.1)
        total += amplitude
        amplitude *= 0.5
        f *= 2
    return s / total


def heading_of(dx, dy):
    """The heading of a movement (dx, dy) in degrees: 0 the direction of y, clockwise (in a grid with y down: heading_of(dx, -dy))."""
    h = math.degrees(math.atan2(dx, dy))
    return h + 360 if h < 0 else h


def linear_regression(x, y):
    """The linear regression y = a + b x: (intercept a, slope b, r squared)."""
    x, y = np.asarray(x, dtype=float), np.asarray(y, dtype=float)
    mx, my = x.mean(), y.mean()
    sxy, sxx, syy = ((x - mx) * (y - my)).sum(), ((x - mx) ** 2).sum(), ((y - my) ** 2).sum()
    b = sxy / sxx if sxx else 0.0
    return float(my - b * mx), float(b), float(sxy * sxy / (sxx * syy)) if sxx and syy else 0.0


def normal_pdf(x, mean=0.0, sd=1.0):
    z = (np.asarray(x, dtype=float) - mean) / sd
    return np.exp(-0.5 * z * z) / (sd * math.sqrt(2 * math.pi))


def normal_cdf(x, mean=0.0, sd=1.0):
    from math import erf
    f = np.vectorize(lambda v: 0.5 * (1 + erf((v - mean) / (sd * math.sqrt(2)))))
    r = f(np.asarray(x, dtype=float))
    return float(r) if r.ndim == 0 else r


# ------------------------------------------------------------------ models


class Simulation:
    """The methods of all models: random numbers, the chart and the time step t."""

    clamp = staticmethod(clamp)
    lerp = staticmethod(lerp)
    map_range = staticmethod(map_range)
    wrap_around = staticmethod(wrap_around)
    sq = staticmethod(sq)
    distance = staticmethod(distance)
    distance3 = staticmethod(distance3)
    logistic = staticmethod(logistic)
    logistic_growth = staticmethod(logistic_growth)
    mean = staticmethod(mean)
    variance = staticmethod(variance)
    sd = staticmethod(sd)
    rk4 = staticmethod(rk4)

    noise = staticmethod(noise)
    fractal_noise = staticmethod(fractal_noise)
    heading_of = staticmethod(heading_of)
    linear_regression = staticmethod(linear_regression)
    normal_pdf = staticmethod(normal_pdf)
    normal_cdf = staticmethod(normal_cdf)

    def __init__(self):
        self.t = 0
        self.rng = np.random.default_rng(DEFAULT_SEED)
        self._charts = []
        self._more_charts = []
        self._drawing = []

    def init(self):
        """The start state (after the start, the Setup button and a restart setting)."""

    def step(self):
        """One time step (the Play/Pause button)."""

    # random numbers (a numpy Generator: the same numbers in Bio7 and in the browser)

    def seed(self, n):
        """Sets the seed of the random numbers (e.g., in init())."""
        self.rng = np.random.default_rng(int(n))

    def random(self, min=0.0, max=1.0):
        """A random number from min to max (0 to 1)."""
        return min + (max - min) * float(self.rng.random())

    def random_array(self, shape=None, min=0.0, max=1.0):
        """An array of random numbers (the shape of the grid if shape is None)."""
        if shape is None:
            shape = (self.height, self.width)
        return min + (max - min) * self.rng.random(shape)

    def random_int(self, a, b=None):
        """A random integer from 0 to a - 1 (or from a to b - 1)."""
        return int(self.rng.integers(0, a)) if b is None else int(self.rng.integers(a, b))

    def chance(self, p):
        """True with the probability p."""
        return float(self.rng.random()) < p

    def random_gaussian(self, mean=0.0, sd=1.0):
        return float(self.rng.normal(mean, sd))

    def random_exponential(self, rate):
        return float(self.rng.exponential(1.0 / rate))

    def random_poisson(self, lam):
        return int(self.rng.poisson(lam))

    def random_element(self, items):
        return items[int(self.rng.integers(0, len(items)))]

    def shuffle(self, items):
        """Shuffles a list (or array) in place."""
        for i in range(len(items) - 1, 0, -1):
            j = int(self.rng.integers(0, i + 1))
            items[i], items[j] = items[j], items[i]

    def random_binomial(self, n, p):
        return int(self.rng.binomial(n, p))

    def random_weighted(self, weights):
        """A random index chosen with the weights (-1 if all are 0)."""
        w = np.maximum(np.asarray(weights, dtype=float), 0)
        total = w.sum()
        if total <= 0:
            return -1
        r = float(self.rng.random()) * total
        return int(min(len(w) - 1, np.searchsorted(np.cumsum(w), r, side="right")))

    # charts

    def chart(self, name, value, chart=None):
        """A value of a chart series (one value per step); chart: the title of another chart (a second chart)."""
        if chart is None:
            self._charts.append((str(name), float(value)))
        else:
            self._more_charts.append({"t": str(chart), "k": "line", "n": str(name), "x": self.t, "y": float(value)})

    def chart_xy(self, chart, name, x, y):
        """A point (x, y) of a series of an XY chart (e.g., a phase plot)."""
        self._more_charts.append({"t": str(chart), "k": "xy", "n": str(name), "x": float(x), "y": float(y)})

    def chart_bars(self, chart, labels, values):
        """A bar chart (replaced in each call, e.g., a histogram)."""
        self._more_charts.append({"t": str(chart), "k": "bars", "l": [str(v) for v in labels], "v": [float(v) for v in values]})

    # drawing (in the coordinates of the model: cells, the box, longitude and latitude)

    def _shape(self, k, p, s=None, f=None, t=None, w=1.0, a=0.0):
        self._drawing.append({"k": k, "p": [float(v) for v in p], "s": s or None, "f": f or None, "t": t, "w": float(w), "a": float(a)})

    def draw_line(self, x1, y1, x2, y2, color="#000000", width=1):
        self._shape("line", (x1, y1, x2, y2), color, None, None, width)

    def draw_line3(self, x1, y1, z1, x2, y2, z2, color="#000000", width=1):
        self._shape("line3", (x1, y1, z1, x2, y2, z2), color, None, None, width)

    def draw_path(self, xs, ys, color="#000000", width=1):
        self._shape("path", [v for xy in zip(xs, ys) for v in xy], color, None, None, width)

    def draw_polygon(self, xs, ys, fill=None, stroke="#000000"):
        self._shape("polygon", [v for xy in zip(xs, ys) for v in xy], stroke, fill)

    def draw_circle(self, x, y, radius, fill=None, stroke="#000000"):
        self._shape("circle", (x, y, radius), stroke, fill)

    def draw_rect(self, x, y, w, h, fill=None, stroke="#000000"):
        self._shape("rect", (x, y, w, h), stroke, fill)

    def draw_text(self, x, y, text, color="#000000", size=1):
        self._shape("text", (x, y), color, None, str(text), size)

    def draw_image(self, name, x, y, size=1, angle=0):
        """An image (load_image) centered at x, y: the size and the angle (degrees, clockwise)."""
        self._shape("image", (x, y), None, None, name, size, angle)

    def draw_pillar(self, x, y, radius, height=None, color="#9aa6b8"):
        """A pillar in 3D (an agent model): a vertical cylinder from the floor at x, y with the
        radius and the height (None: the height of the box), e.g. an obstacle."""
        if height is None:
            height = getattr(self, "box_depth", 1.0)
        self._shape("pillar", (x, y, radius, height), None, color)

    # images and 3D models

    def load_image(self, name, file):
        """Loads an image (PNG with transparency) for draw_image and the agents (agent.image = name)."""
        if not BROWSER:
            import bio7
            bio7.call("sim_asset", "image", name, file)

    def load_model(self, name, file):
        """Loads a 3D model (glTF .glb or .gltf, also animated) for the agents (agent.model = name)."""
        if not BROWSER:
            import bio7
            bio7.call("sim_asset", "model", name, file)

    # internal
    def _prepare(self):
        pass

    def _state(self):
        return {}


def _shift(a, dy, dx, wrap):
    """The array of the neighbors at (dy, dx): s[y, x] = a[y + dy, x + dx] (0 outside without wrap)."""
    if wrap:
        return np.roll(a, (-dy, -dx), axis=(0, 1))
    h, w = a.shape
    s = np.zeros_like(a)
    ys, yd = (slice(dy, h), slice(0, h - dy)) if dy >= 0 else (slice(0, h + dy), slice(-dy, h))
    xs, xd = (slice(dx, w), slice(0, w - dx)) if dx >= 0 else (slice(0, w + dx), slice(-dx, w))
    s[yd, xd] = a[ys, xs]
    return s


_N8 = [(-1, -1), (-1, 0), (-1, 1), (0, -1), (0, 1), (1, -1), (1, 0), (1, 1)]
_N4 = [(-1, 0), (1, 0), (0, -1), (0, 1)]


class GridModel(Simulation):
    """A 2D grid with integer states (shown with the colors of the palette).

    cells: a numpy array cells[y, x] (shape (height, width)), wrap: a torus.
    """

    def __init__(self):
        super().__init__()
        self.width = self.height = 0
        self.cells = np.zeros((0, 0), dtype=np.int32)
        self.wrap = True
        self._palette = ["#ffffff", "#000000"]

    def size(self, w, h):
        """A new grid (all states 0)."""
        self.width, self.height = int(w), int(h)
        self.cells = np.zeros((self.height, self.width), dtype=np.int32)

    def palette(self, *colors):
        """The colors of the states 0, 1, 2, ... (e.g., "#ffffff", "#000000")."""
        self._palette = list(colors)

    def get(self, x, y):
        if self.wrap:
            return int(self.cells[y % self.height, x % self.width])
        if x < 0 or y < 0 or x >= self.width or y >= self.height:
            return 0
        return int(self.cells[y, x])

    def set(self, x, y, state):
        self.cells[y, x] = state

    def count_neighbors(self, state, x=None, y=None):
        """The number of the 8 neighbors with the state: an array (or the number of the cell x, y)."""
        if x is not None:
            return sum(1 for dy, dx in _N8 if self.get(x + dx, y + dy) == state)
        m = (np.asarray(self.cells) == state).astype(np.int32)
        return sum(_shift(m, dy, dx, self.wrap) for dy, dx in _N8)

    def count4(self, state, x=None, y=None):
        """The number of the 4 neighbors (north, south, east, west) with the state."""
        if x is not None:
            return sum(1 for dy, dx in _N4 if self.get(x + dx, y + dy) == state)
        m = (np.asarray(self.cells) == state).astype(np.int32)
        return sum(_shift(m, dy, dx, self.wrap) for dy, dx in _N4)

    def count(self, state):
        """The number of the cells with the state."""
        return int(np.count_nonzero(np.asarray(self.cells) == state))

    def _state(self):
        c = np.ascontiguousarray(np.asarray(self.cells), dtype=np.int32)
        return {"width": self.width, "height": self.height, "cells": c.ravel(), "palette": list(self._palette)}


class ValueGridModel(Simulation):
    """A 2D grid with double values (shown with a color map from range(min, max)).

    values: a numpy array values[y, x] (shape (height, width)), wrap: a torus.
    """
    VIRIDIS = VIRIDIS

    def __init__(self):
        super().__init__()
        self.width = self.height = 0
        self.values = np.zeros((0, 0))
        self.wrap = True
        self._min, self._max = 0.0, 1.0
        self._colors = list(VIRIDIS)

    def size(self, w, h):
        self.width, self.height = int(w), int(h)
        self.values = np.zeros((self.height, self.width))

    def range(self, lo, hi):
        """The values of the first and the last color."""
        self._min, self._max = float(lo), float(hi)

    def color_map(self, *colors):
        if len(colors) >= 2:
            self._colors = list(colors)

    def get(self, x, y):
        if self.wrap:
            return float(self.values[y % self.height, x % self.width])
        if x < 0 or y < 0 or x >= self.width or y >= self.height:
            return 0.0
        return float(self.values[y, x])

    def set(self, x, y, value):
        self.values[y, x] = value

    def sum4(self):
        """The sums of the 4 neighbors (an array)."""
        v = np.asarray(self.values, dtype=float)
        return sum(_shift(v, dy, dx, self.wrap) for dy, dx in _N4)

    def sum8(self):
        v = np.asarray(self.values, dtype=float)
        return sum(_shift(v, dy, dx, self.wrap) for dy, dx in _N8)

    def laplacian(self):
        """sum4 - 4 * values (a diffusion: values + d * laplacian())."""
        return self.sum4() - 4 * np.asarray(self.values, dtype=float)

    def mean(self, values=None):
        """The mean of the values (or of an array)."""
        return mean(self.values if values is None else values)

    def _state(self):
        v = np.ascontiguousarray(np.asarray(self.values), dtype=np.float64)
        return {"width": self.width, "height": self.height, "values": v.ravel(), "min": self._min, "max": self._max,
                "colors": list(self._colors)}


class Agent:
    """An agent in the 3D box: x, y, z and an own color (None: agent_color). Extend it with own fields."""

    def __init__(self, x=0.0, y=0.0, z=0.0, color=None, image=None, model=None, animation=None, heading=float("nan"), size=0.0):
        self.x, self.y, self.z = float(x), float(y), float(z)
        self.color = color
        self.image, self.model, self.animation = image, model, animation
        self.heading, self.size = heading, size


class AgentModel(Simulation):
    """Agents in a 3D box (the 3D view of Bio7, WebGL in the page): agents is a list of Agent."""

    def __init__(self):
        super().__init__()
        self.agents = []
        self.box_width = self.box_height = self.box_depth = 100.0
        self.agent_radius = 0.0
        self.agent_color = "#1f77b4"
        # "sphere" or "glider" (triangles: points in the direction of the movement, climbs, dives and banks)
        self.agent_shape = "sphere"
        # the floor: "none" (the edges of the box), "grid" or "plane" (a dark floor with a grid)
        self.floor = "none"
        # soft shadows of the agents on the floor
        self.shadows = False
        self._follow = -1
        self._scenery = []

    def place_model(self, model, x, y, z=0.0, heading=0.0, size=0.0, animation=None):
        """Places a 3D model (glTF, load_model) in the environment, e.g. a house, a tree or a rock:
        no agent, it stays until it is removed (not drawn again in each step; Setup removes all).
        z: the height of its base, heading: degrees (0 the direction of y, clockwise), size: its
        height (0: the diameter of an agent), animation: its name or number ("none": still).
        Returns its index (remove_model)."""
        self._scenery.append((str(model), float(x), float(y), float(z), float(heading), float(size), animation))
        return len(self._scenery) - 1

    def remove_model(self, index):
        """Removes the object of the environment with the index (place_model); the later ones move up by one."""
        if 0 <= index < len(self._scenery):
            del self._scenery[index]

    def clear_models(self):
        """Removes all objects of the environment."""
        self._scenery = []

    def camera_follow(self, agent=None):
        """The custom camera of the 3D view follows the agent (an Agent or its index) from behind,
        the global view beside it (split view); None: off. Only in Bio7 (the page shows the orbit view)."""
        if agent is None:
            self._follow = -1
        elif isinstance(agent, Agent):
            self._follow = next((i for i, a in enumerate(self.agents) if a is agent), -1)
        else:
            self._follow = int(agent)

    def box(self, w, h, d):
        """The size of the box: x, y and z (the height)."""
        self.box_width, self.box_height, self.box_depth = float(w), float(h), float(d)

    def distance(self, a, b):
        return math.sqrt((a.x - b.x) ** 2 + (a.y - b.y) ** 2 + (a.z - b.z) ** 2)

    def neighbors(self, agent, radius):
        """The other agents within the radius."""
        r2 = radius * radius
        return [o for o in self.agents if o is not agent and (o.x - agent.x) ** 2 + (o.y - agent.y) ** 2 + (o.z - agent.z) ** 2 <= r2]

    def nearest(self, agent, radius):
        """The nearest other agent within the radius (or None)."""
        best, r2 = None, radius * radius
        for o in self.agents:
            if o is not agent:
                d2 = (o.x - agent.x) ** 2 + (o.y - agent.y) ** 2 + (o.z - agent.z) ** 2
                if d2 <= r2:
                    r2, best = d2, o
        return best

    def keep_in_box(self, a):
        a.x = max(0.0, min(self.box_width, a.x))
        a.y = max(0.0, min(self.box_height, a.y))
        a.z = max(0.0, min(self.box_depth, a.z))

    def wrap_in_box(self, a):
        """x and y wrap around (a torus), z stays in the box."""
        a.x = ((a.x % self.box_width) + self.box_width) % self.box_width
        a.y = ((a.y % self.box_height) + self.box_height) % self.box_height
        a.z = max(0.0, min(self.box_depth, a.z))

    def positions(self):
        """The positions of the agents as an array (n, 3)."""
        return np.array([(a.x, a.y, a.z) for a in self.agents], dtype=float).reshape(-1, 3)

    def _prepare(self):
        self.agents = []
        self._scenery = []

    def _state(self):
        xyz = np.array([(a.x, a.y, a.z) for a in self.agents], dtype=np.float64).ravel()
        s = {"box": [self.box_width, self.box_height, self.box_depth], "radius": float(self.agent_radius),
             "color": self.agent_color, "xyz": xyz, "colors": [a.color or "" for a in self.agents],
             "shape": str(self.agent_shape), "floor": str(self.floor), "shadows": 1.0 if self.shadows else 0.0,
             "follow": float(self._follow if 0 <= self._follow < len(self.agents) else -1),
             # the objects of the environment (all in each state): x, y, z, heading and size for each model
             "scenery_models": [p[0] for p in self._scenery],
             "scenery": np.array([v for p in self._scenery for v in p[1:6]], dtype=np.float64),
             "scenery_animations": ["" if p[6] is None else str(p[6]) for p in self._scenery]}
        if any(getattr(a, "image", None) or getattr(a, "model", None) for a in self.agents):
            s.update({"images": [a.image or "" for a in self.agents], "models": [a.model or "" for a in self.agents],
                      "animations": [a.animation or "" for a in self.agents],
                      "headings": np.array([a.heading for a in self.agents], dtype=np.float64), "sizes": np.array([a.size for a in self.agents], dtype=np.float64)})
        return s


_MERCATOR = 20037508.342789244


class GeoGridModel(GridModel):
    """A grid on the Earth: the data (rasters and vectors of GDAL) is read in load() (only in Bio7);
    the page gets the loaded layers. The grid is shown in ImageJ (and on the WorldWind globe with
    show_on_globe()), in the page on an online map."""

    def __init__(self):
        super().__init__()
        self.origin_x = self.origin_y = 0.0
        self.cell_width, self.cell_height = 1.0, -1.0
        self.cell_size_meters = 1.0
        self.south = self.west = self.north = self.east = 0.0
        self.crs = ""
        self._mode = "other"
        self._layers = {}
        self._work = {}
        self._shown = None

    def load(self):
        """Reads the data (in Bio7 with GDAL, once before the first init; not in the page)."""

    # load()
    def grid(self, file, columns):
        """The grid of a raster file: the extent, the coordinate system and the number of the columns."""
        g = _java("grid", file, int(columns))
        self._geometry({k: (v.item() if hasattr(v, "item") else v) for k, v in g.items()})

    def raster(self, file, band=1, resampling="bilinear"):
        """The values of a raster band resampled to the cells (an array (height, width))."""
        return self._grid_array(_java("raster", file, int(band), resampling))

    def vector(self, file, value=1.0, attribute=None):
        """A vector file rasterized: the value in the features (or the value of the attribute), 0 outside."""
        return self._grid_array(_java("vector", file, "" if attribute is None else attribute, float(value)))

    def add_layer(self, name, values):
        """Adds a layer for init() and step() (layer(name))."""
        v = np.asarray(values, dtype=np.float64).reshape(self.height, self.width)
        self._layers[name] = v
        if not BROWSER:
            _java("add_layer", name, value=np.ascontiguousarray(v).ravel())

    def attribution(self, text):
        """The sources and licenses of the data (shown on the map of the page)."""
        if not BROWSER:
            _java("attribution", text)

    def show_on_globe(self):
        """Shows the grid also on the WorldWind globe of Bio7 (call it in load())."""
        if not BROWSER:
            _java("globe")

    # init() and step()
    def layer(self, name):
        """A copy of a loaded layer for this setup (an array (height, width)), None if unknown."""
        return self._work.get(name)

    def show_values(self, values, lo, hi, *colors):
        """Shows values (e.g., a layer) with a color map instead of the states."""
        self._shown = (np.asarray(values, dtype=float), float(lo), float(hi), list(colors) if len(colors) >= 2 else list(VIRIDIS))

    def show_states(self):
        self._shown = None

    def col_at(self, lon, lat):
        """The column of the cell at the longitude and latitude (WGS84), -1 outside."""
        if self._mode == "mercator":
            x = math.floor((lon * _MERCATOR / 180 - self.origin_x) / self.cell_width)
        elif self._mode == "geographic":
            x = math.floor((lon - self.origin_x) / self.cell_width)
        else:
            x = math.floor((lon - self.west) / (self.east - self.west) * self.width)
        return x if 0 <= x < self.width else -1

    def row_at(self, lon, lat):
        if self._mode == "mercator":
            my = math.log(math.tan((90 + lat) * math.pi / 360)) / (math.pi / 180) * _MERCATOR / 180
            y = math.floor((my - self.origin_y) / self.cell_height)
        elif self._mode == "geographic":
            y = math.floor((lat - self.origin_y) / self.cell_height)
        else:
            y = math.floor((self.north - lat) / (self.north - self.south) * self.height)
        return y if 0 <= y < self.height else -1

    def lon(self, x):
        """The longitude of the center of the column x."""
        if self._mode == "mercator":
            return (self.origin_x + (x + 0.5) * self.cell_width) / _MERCATOR * 180
        if self._mode == "geographic":
            return self.origin_x + (x + 0.5) * self.cell_width
        return self.west + (x + 0.5) / self.width * (self.east - self.west)

    def lat(self, y):
        if self._mode == "mercator":
            my = (self.origin_y + (y + 0.5) * self.cell_height) / _MERCATOR * 180
            return 180 / math.pi * (2 * math.atan(math.exp(my * math.pi / 180)) - math.pi / 2)
        if self._mode == "geographic":
            return self.origin_y + (y + 0.5) * self.cell_height
        return self.north - (y + 0.5) / self.height * (self.north - self.south)

    def value(self, layer, x, y):
        """The value of the layer at the cell (the nearest cell at the border)."""
        return float(layer[max(0, min(self.height - 1, y)), max(0, min(self.width - 1, x))])

    def gradient(self, layer):
        """The gradients (rise per meter) of a layer (e.g., the elevation): an array."""
        p = np.pad(np.asarray(layer, dtype=float), 1, mode="edge")
        dzdx = (p[1:-1, 2:] - p[1:-1, :-2]) / (2 * self.cell_size_meters)
        dzdy = (p[2:, 1:-1] - p[:-2, 1:-1]) / (2 * self.cell_size_meters)
        return np.sqrt(dzdx * dzdx + dzdy * dzdy)

    def slope_degrees(self, layer):
        return np.degrees(np.arctan(self.gradient(layer)))

    # internal
    def _geometry(self, g):
        self.width, self.height = int(g["width"]), int(g["height"])
        self.origin_x, self.origin_y = float(g["originX"]), float(g["originY"])
        self.cell_width, self.cell_height = float(g["cellWidth"]), float(g["cellHeight"])
        self.cell_size_meters = float(g["cellSizeMeters"])
        self.south, self.west, self.north, self.east = float(g["south"]), float(g["west"]), float(g["north"]), float(g["east"])
        self._mode = str(g.get("mode", "other"))
        self.crs = str(g.get("crs", ""))
        self.cells = np.zeros((self.height, self.width), dtype=np.int32)

    def _grid_array(self, v):
        return np.asarray(v, dtype=np.float64).reshape(self.height, self.width)

    def _load(self):
        self._layers = {}
        if BROWSER:
            self._geometry(GEO)
            for k, v in GEO["layers"].items():
                self._layers[k] = np.asarray(v, dtype=np.float64).reshape(self.height, self.width)
        else:
            self.load()

    def _prepare(self):
        self.cells = np.zeros((self.height, self.width), dtype=np.int32)
        self._work = {k: v.copy() for k, v in self._layers.items()}
        self._shown = None

    def _state(self):
        s = super()._state()
        if self._shown is not None:
            v, lo, hi, colors = self._shown
            s.update({"shown": np.ascontiguousarray(v, dtype=np.float64).ravel(), "min": lo, "max": hi, "colors": colors})
        return s


class GeoAgent:
    """An agent on the Earth: lon, lat (degrees), color, size (pixels), label, altitude (m), heading (degrees)."""

    def __init__(self, lon=0.0, lat=0.0, color=None, size=0.0, label=None, altitude=0.0, heading=float("nan"), image=None, model=None, animation=None):
        self.lon, self.lat = float(lon), float(lat)
        self.color, self.size, self.label = color, size, label
        self.altitude, self.heading = altitude, heading
        self.image, self.model, self.animation = image, model, animation


_COS = [1.0, -0.5, 0.041666666666666664, -0.001388888888888889, 2.48015873015873e-05, -2.755731922398589e-07,
        2.08767569878681e-09, -1.1470745597729725e-11, 4.779477332387385e-14, -1.5619206968586225e-16,
        4.110317623312165e-19, -8.896791392450574e-22]


def cos_lat(lat_deg):
    """The cosine of a latitude in degrees (the same digits in Bio7 and in the browser)."""
    x = lat_deg * (math.pi / 180)
    x2 = x * x
    c = _COS[11]
    for k in range(10, -1, -1):
        c = c * x2 + _COS[k]
    return c


def cos_deg(deg):
    d = deg % 360
    if d > 180:
        d -= 360
    d = abs(d)
    return cos_lat(d) if d <= 90 else -cos_lat(180 - d)


def sin_deg(deg):
    return cos_deg(90 - deg)


class GeoAgentModel(Simulation):
    """Agents on the Earth: points or 3D shapes on the WorldWind globe of Bio7, points on a map in the page."""
    EARTH_RADIUS = 6371.0088
    cos_lat = staticmethod(cos_lat)
    cos_deg = staticmethod(cos_deg)
    sin_deg = staticmethod(sin_deg)

    def __init__(self):
        super().__init__()
        self.agents = []
        self.south, self.west, self.north, self.east = -60.0, -180.0, 75.0, 180.0
        self.agent_size = 4.0
        self.agent_color = "#d62728"
        self.agent_shape = "point"
        self.agent_size_meters = 300.0
        self.view_pitch = 0.0

    def area(self, south, west, north, east):
        """The area of the model (the globe flies to it, the map shows it)."""
        self.south, self.west, self.north, self.east = float(south), float(west), float(north), float(east)

    def distance_km(self, a, b, lon2=None, lat2=None):
        """The great circle distance of two agents (or of lon1, lat1, lon2, lat2) in km."""
        if lon2 is None:
            lon1, lat1, lon2, lat2 = a.lon, a.lat, b.lon, b.lat
        else:
            lon1, lat1 = a, b
        p1, p2 = lat1 * (math.pi / 180), lat2 * (math.pi / 180)
        dp, dl = p2 - p1, (lon2 - lon1) * (math.pi / 180)
        h = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
        return 2 * self.EARTH_RADIUS * math.asin(min(1.0, math.sqrt(h)))

    def move(self, a, east_km, north_km):
        """Moves the agent (km to the east and to the north)."""
        a.lat += north_km / 110.574
        a.lon += east_km / (111.320 * cos_lat(a.lat))
        if a.lon > 180:
            a.lon -= 360
        elif a.lon < -180:
            a.lon += 360
        a.lat = max(-89.9, min(89.9, a.lat))

    def move_towards(self, a, lon, lat, km):
        """Moves the agent km towards the point (True if it arrived)."""
        east = (lon - a.lon) * 111.320 * cos_lat((lat + a.lat) / 2)
        north = (lat - a.lat) * 110.574
        d = math.sqrt(east * east + north * north)
        if d <= km:
            a.lon, a.lat = lon, lat
            return True
        self.move(a, east / d * km, north / d * km)
        return False

    def neighbors_km(self, agent, km):
        return [o for o in self.agents if o is not agent and self.distance_km(agent, o) <= km]

    def _prepare(self):
        self.agents = []

    def _state(self):
        a = self.agents
        return {"area": [self.south, self.west, self.north, self.east], "size": float(self.agent_size), "color": self.agent_color,
                "shape": self.agent_shape, "meters": float(self.agent_size_meters), "pitch": float(self.view_pitch),
                "lon": np.array([g.lon for g in a], dtype=np.float64), "lat": np.array([g.lat for g in a], dtype=np.float64),
                "altitude": np.array([g.altitude for g in a], dtype=np.float64),
                "heading": np.array([g.heading for g in a], dtype=np.float64),
                "sizes": np.array([g.size for g in a], dtype=np.float64),
                "colors": [g.color or "" for g in a], "labels": [g.label or "" for g in a],
                "images": [getattr(g, "image", None) or "" for g in a], "models": [getattr(g, "model", None) or "" for g in a],
                "animations": [getattr(g, "animation", None) or "" for g in a]}


# ------------------------------------------------------------------ the running model

_KINDS = [(GeoGridModel, "GeoGridModel"), (GeoAgentModel, "GeoAgentModel"), (ValueGridModel, "ValueGridModel"),
          (GridModel, "GridModel"), (AgentModel, "AgentModel")]
_model = None


def _java(op, *args, value=None):
    """Calls the functions of the simulation in Bio7 (not in the browser)."""
    if BROWSER:
        raise RuntimeError("load() and its functions (grid, raster, vector) run only in Bio7: the page gets the loaded layers")
    import bio7
    return bio7.call("sim_geo", op, *args, value=value)


def _spec(cls):
    kind = next((k for c, k in _KINDS if issubclass(cls, c)), None)
    if kind is None:
        raise TypeError("The model must extend GridModel, ValueGridModel, AgentModel, GeoGridModel or GeoAgentModel")
    params, actions, seen = [], [], set()
    for c in reversed(cls.__mro__):
        for name, v in c.__dict__.items():
            if isinstance(v, param) and name not in seen:
                seen.add(name)
                params.append(v.spec())
            elif callable(v) and hasattr(v, "_bio7_action") and name not in seen:
                seen.add(name)
                actions.append({"name": name, "label": v._bio7_action["label"], "group": v._bio7_action["group"]})
    doc = (cls.__doc__ or "").strip()
    return {"name": cls.__name__, "kind": kind, "language": "python", "description": " ".join(doc.split()), "params": params, "actions": actions}


def simulation(cls, source=None):
    """Starts the model (the class): in Bio7 with the views of the model, in the page with its controls."""
    global _model
    spec = _spec(cls)
    _model = cls()
    if BROWSER:
        return _model
    if source is None:
        import sys
        source = sys._getframe(1).f_globals.get("__file__")
    if source:
        spec["source"] = str(source)
    import bio7
    bio7.call("sim_start", json.dumps(spec))
    return _model


def _do(op, arg=""):
    """The protocol of Bio7 and of the page: setup, step, action, set, load; returns the state."""
    m = _model
    if m is None:
        raise RuntimeError("No model: call simulation(MyModel) at the end of the script")
    if op == "load":
        if isinstance(m, GeoGridModel):
            m._load()
        return {}
    if op == "setup":
        m.t = 0
        m.seed(DEFAULT_SEED)
        m._charts = []
        m._more_charts = []
        m._drawing = []
        m._prepare()
        m.init()
    elif op == "step":
        m._drawing = []
        m.step()
        m.t += 1
    elif op == "action":
        getattr(m, arg)()
    elif op == "set":
        for k, v in json.loads(arg).items():
            setattr(m, k, v)
        return {}
    s = m._state()
    s["chart_names"] = [c[0] for c in m._charts]
    s["chart_values"] = np.array([c[1] for c in m._charts], dtype=np.float64)
    s["more_charts"] = json.dumps(m._more_charts)
    s["draw"] = json.dumps(m._drawing)
    m._charts = []
    m._more_charts = []
    return s
