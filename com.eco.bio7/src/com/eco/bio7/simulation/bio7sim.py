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


def pairs_within(points, radius):
    """All pairs of points within the distance (fast for many points: a grid of cells, numpy).

    points: an array (n, 2) or (n, 3). Returns two arrays i, j of the indices (i < j, sorted by
    i, then j), e.g. the neighbours of all boids of a flock in one call."""
    p = np.asarray(points, dtype=float)
    if p.ndim == 1:
        p = p.reshape(-1, 1)
    n = len(p)
    empty = (np.zeros(0, dtype=np.int64), np.zeros(0, dtype=np.int64))
    if n < 2 or radius <= 0:
        return empty
    d = p.shape[1]
    cell = np.floor(p / radius).astype(np.int64)
    cell -= cell.min(axis=0)
    # one empty column after each row of cells: the cells -1 and +1 never mix rows
    dims = cell.max(axis=0) + 2
    mult = np.cumprod(np.concatenate([[1], dims[:-1]])).astype(np.int64)
    key = cell @ mult
    order = np.argsort(key, kind="stable")
    sk = key[order]
    # half of the neighbouring cells (each pair of cells once) and the cell itself
    offsets = [o for o in np.ndindex(*([3] * d))]
    offsets = [np.array(o) - 1 for o in offsets]
    offsets = [o for o in offsets if not o.any() or o[np.nonzero(o)[0][0]] > 0]
    ii_all, jj_all = [], []
    for o in offsets:
        tk = key + int(o @ mult)
        lo = np.searchsorted(sk, tk, "left")
        cnt = np.searchsorted(sk, tk, "right") - lo
        total = int(cnt.sum())
        if total == 0:
            continue
        ii = np.repeat(np.arange(n), cnt)
        within = np.arange(total) - np.repeat(np.cumsum(cnt) - cnt, cnt)
        jj = order[np.repeat(lo, cnt) + within]
        if not o.any():
            keep = ii < jj
            ii, jj = ii[keep], jj[keep]
        ii_all.append(ii)
        jj_all.append(jj)
    if not ii_all:
        return empty
    ii, jj = np.concatenate(ii_all), np.concatenate(jj_all)
    near = ((p[ii] - p[jj]) ** 2).sum(axis=1) <= radius * radius
    a, b = np.minimum(ii[near], jj[near]), np.maximum(ii[near], jj[near])
    k = np.lexsort((b, a))
    return a[k], b[k]


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
        self._seed_value = DEFAULT_SEED
        self._charts = []
        self._more_charts = []
        self._drawing = []
        self._records = {}
        self._stopped = False

    def init(self):
        """The start state (after the start, the Setup button and a restart setting)."""

    def step(self):
        """One time step (the Play/Pause button)."""

    # random numbers (a numpy Generator: the same numbers in Bio7 and in the browser)

    def seed(self, n):
        """Sets the seed of the random numbers (e.g., in init())."""
        self.rng = np.random.default_rng(int(n))
        self._seed_value = int(n)

    def random_at(self, index, k=0):
        """A random number in [0, 1) for an index (e.g., of a cell or an agent) and the number k of the draw:
        always the same for the seed, the step and the index (independent of the order of the calls; the
        same numbers as randomAt in Java and in the page). index can be an int or a numpy array."""
        # 32-bit arithmetic as in Java: the values below 2^32 in uint64 (a product of two of them doesn't overflow)
        M = np.uint64(0xFFFFFFFF)
        u = lambda v: np.asarray(np.asarray(v, dtype=np.int64) & 0xFFFFFFFF, dtype=np.uint64)
        h = ((u(self._seed_value) * np.uint64(0x9E3779B1)) & M) ^ ((u(self.t) * np.uint64(0x85EBCA77)) & M) \
            ^ ((u(index) * np.uint64(0xC2B2AE3D)) & M) ^ ((u(k) * np.uint64(0x27D4EB2F)) & M)
        h = h ^ (h >> np.uint64(16))
        h = (h * np.uint64(0x7FEB352D)) & M
        h = h ^ (h >> np.uint64(15))
        h = (h * np.uint64(0x846CA68B)) & M
        h = h ^ (h >> np.uint64(16))
        r = h / 4294967296.0
        return float(r) if np.ndim(r) == 0 else r

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

    def stop(self):
        """Ends the simulation after this step, e.g. `if infected == 0: self.stop()` in step():
        Bio7 pauses the simulation (Play continues it), the page stops, an experiment ends the run."""
        self._stopped = True

    def is_stopped(self):
        """True if stop() was called in this step (or in init())."""
        return bool(getattr(self, "_stopped", False))

    def record(self, name, value):
        """Records a value of the current step for an experiment (a column of its table,
        besides the values of the line charts)."""
        self._records[str(name)] = float(value)

    def chart_xy(self, chart, name, x, y):
        """A point (x, y) of a series of an XY chart (e.g., a phase plot)."""
        self._more_charts.append({"t": str(chart), "k": "xy", "n": str(name), "x": float(x), "y": float(y)})

    def chart_bars(self, chart, labels, values):
        """A bar chart (replaced in each call, e.g., a histogram)."""
        self._more_charts.append({"t": str(chart), "k": "bars", "l": [str(v) for v in labels], "v": [float(v) for v in values]})

    # drawing (in the coordinates of the model: cells, the box, longitude and latitude)

    def _shape(self, k, p, s=None, f=None, t=None, w=1.0, a=0.0, z=0.0):
        d = {"k": k, "p": [float(v) for v in p], "s": s or None, "f": f or None, "t": t, "w": float(w), "a": float(a)}
        if z:
            d["z"] = float(z)
        self._drawing.append(d)

    def draw_line(self, x1, y1, x2, y2, color="#000000", width=1, z=0):
        self._shape("line", (x1, y1, x2, y2), color, None, None, width, z=z)

    def draw_line3(self, x1, y1, z1, x2, y2, z2, color="#000000", width=1):
        self._shape("line3", (x1, y1, z1, x2, y2, z2), color, None, None, width)

    def draw_path(self, xs, ys, color="#000000", width=1, z=0):
        self._shape("path", [v for xy in zip(xs, ys) for v in xy], color, None, None, width, z=z)

    def draw_polygon(self, xs, ys, fill=None, stroke="#000000", z=0):
        self._shape("polygon", [v for xy in zip(xs, ys) for v in xy], stroke, fill, z=z)

    def draw_circle(self, x, y, radius, fill=None, stroke="#000000", z=0):
        self._shape("circle", (x, y, radius), stroke, fill, z=z)

    def draw_rect(self, x, y, w, h, fill=None, stroke="#000000", z=0):
        self._shape("rect", (x, y, w, h), stroke, fill, z=z)

    def draw_text(self, x, y, text, color="#000000", size=1, z=0):
        """A text; z: the height above the floor (an agent model, e.g. a label above an object)."""
        self._shape("text", (x, y), color, None, str(text), size, z=z)

    def draw_image(self, name, x, y, size=1, angle=0, z=0):
        """An image (load_image) centered at x, y: the size and the angle (degrees, clockwise);
        z: the height above the floor (an agent model)."""
        self._shape("image", (x, y), None, None, name, size, angle, z=z)

    # 3D shapes of an agent model (x, y, z of the box; lit solids)

    def draw_point3(self, x, y, z, size=1, color="#e53935"):
        """A point in 3D: a small sphere with the diameter size, e.g. a marker."""
        self._shape("point3", (x, y, z, size), None, color)

    def draw_path3(self, xs, ys, zs, color="#000000", width=1):
        """A line through the points in 3D (the width in pixels), e.g. a flight path."""
        self._shape("path3", [v for p in zip(xs, ys, zs) for v in p], color, None, None, width)

    def draw_box(self, x, y, z, w, h, d, fill="#b0b8c4", stroke=None):
        """A box in 3D: the corner x, y, z (the lowest values), the width (x), the height (y)
        and the depth (z, upwards) as in box(); fill: the sides, stroke: the edges (None: none)."""
        self._shape("box", (x, y, z, w, h, d), stroke, fill)

    def draw_sphere(self, x, y, z, r, fill="#d0d4dc"):
        """A sphere in 3D: the center and the radius, e.g. a balloon or a tree crown."""
        self._shape("sphere", (x, y, z, r), None, fill)

    def draw_polygon3(self, xs, ys, zs, fill="#b0b8c4", stroke=None):
        """A flat polygon in 3D (the points in a plane): the fill and the outline (None: none), e.g. a roof."""
        self._shape("polygon3", [v for p in zip(xs, ys, zs) for v in p], stroke, fill)

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
        """Loads a 3D model (glTF .glb or .gltf, also animated; OBJ with .mtl and textures; STL)
        for the agents (agent.model = name) and place_model."""
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


class GridAgent:
    """An agent on a grid (a turtle): x, y in cells (the centre of the cell (i, j) is i + 0.5,
    j + 0.5), the heading (degrees, 0 up, clockwise), the color, an image (load_image), the size
    (cells) and the shape ("circle", "arrow" or "square"). Extend it with own fields."""

    def __init__(self, x=0.5, y=0.5, heading=0.0, color="#e53935", image=None, size=0.8, shape="circle"):
        self.x, self.y, self.heading = float(x), float(y), float(heading)
        self.color, self.image, self.size, self.shape = color, image, float(size), shape

    def cell_x(self):
        """The column of the cell of the agent."""
        return int(math.floor(self.x))

    def cell_y(self):
        """The row of the cell of the agent."""
        return int(math.floor(self.y))


def _agent_shapes(agents):
    """The shapes of the agents of a grid (drawn on top of the grid in Bio7 and in the page)."""
    out = []
    for a in agents:
        size = float(getattr(a, "size", 0.8) or 0.8)
        if getattr(a, "image", None):
            out.append({"k": "image", "p": [a.x, a.y], "s": None, "f": None, "t": a.image, "w": size, "a": a.heading})
            continue
        color = a.color or "#e53935"
        shape = getattr(a, "shape", "circle") or "circle"
        if shape == "arrow":
            r, h, p = size / 2, a.heading, []
            for d, f in ((0, 1.0), (140, 1.0), (180, 0.35), (220, 1.0)):
                p += [a.x + r * f * sin_deg(h + d), a.y - r * f * cos_deg(h + d)]
            out.append({"k": "polygon", "p": p, "s": "#00000066", "f": color, "t": None, "w": 1.0, "a": 0.0})
        elif shape == "square":
            out.append({"k": "rect", "p": [a.x - size / 2, a.y - size / 2, size, size], "s": "#00000066", "f": color, "t": None, "w": 1.0, "a": 0.0})
        else:
            out.append({"k": "circle", "p": [a.x, a.y, size / 2], "s": "#00000066", "f": color, "t": None, "w": 1.0, "a": 0.0})
    return out


class _GridAgents:
    """The agents of a grid model (GridModel, ValueGridModel, GeoGridModel): agents is a list of
    GridAgent, drawn on the grid in each step (Setup removes them)."""

    def add_agent(self, agent):
        """Adds an agent (a GridAgent) and returns it."""
        self.agents.append(agent)
        return agent

    def remove_agent(self, agent):
        if agent in self.agents:
            self.agents.remove(agent)

    def _keep_on_grid(self, a):
        if self.wrap:
            a.x, a.y = a.x % self.width, a.y % self.height
        else:
            a.x = min(max(a.x, 0.0), self.width - 1e-9)
            a.y = min(max(a.y, 0.0), self.height - 1e-9)

    def forward(self, agent, distance):
        """Moves the agent in its heading (0 up, clockwise; a torus or stops at the edge: wrap)."""
        agent.x += distance * sin_deg(agent.heading)
        agent.y -= distance * cos_deg(agent.heading)
        self._keep_on_grid(agent)

    def move_to(self, agent, x, y):
        agent.x, agent.y = float(x), float(y)
        self._keep_on_grid(agent)

    def face(self, agent, x, y):
        """Turns the agent to the point (the shortest way on a torus)."""
        dx, dy = x - agent.x, y - agent.y
        if self.wrap:
            dx -= self.width * round(dx / self.width)
            dy -= self.height * round(dy / self.height)
        if dx or dy:
            agent.heading = heading_of(dx, -dy)

    def agent_distance(self, a, b):
        """The distance of two agents (cells; the shortest way on a torus)."""
        dx, dy = b.x - a.x, b.y - a.y
        if self.wrap:
            dx -= self.width * round(dx / self.width)
            dy -= self.height * round(dy / self.height)
        return math.sqrt(dx * dx + dy * dy)

    def agents_at(self, x, y):
        """The agents in the cell (x, y)."""
        return [a for a in self.agents if a.cell_x() == x and a.cell_y() == y]

    def count_agents_at(self, x, y):
        return sum(1 for a in self.agents if a.cell_x() == x and a.cell_y() == y)

    def agents_near(self, agent, radius):
        """The other agents within the radius (cells)."""
        return [o for o in self.agents if o is not agent and self.agent_distance(agent, o) <= radius]

    def _prepare(self):
        self.agents = []
        super()._prepare()


class GridModel(_GridAgents, Simulation):
    """A 2D grid with integer states (shown with the colors of the palette).

    cells: a numpy array cells[y, x] (shape (height, width)), wrap: a torus.
    """

    def __init__(self):
        super().__init__()
        self.width = self.height = 0
        self.cells = np.zeros((0, 0), dtype=np.int32)
        self.wrap = True
        self._palette = ["#ffffff", "#000000"]
        self.agents = []

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


class ValueGridModel(_GridAgents, Simulation):
    """A 2D grid with double values (shown with a color map from range(min, max)).

    values: a numpy array values[y, x] (shape (height, width)), wrap: a torus.
    """
    VIRIDIS = VIRIDIS

    def __init__(self):
        super().__init__()
        self.width = self.height = 0
        self.values = np.zeros((0, 0))
        self.wrap = True
        self.agents = []
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
        # the network: the links (an agent -> its linked agents and the color of the link), drawn as lines
        self._links = {}
        self.link_color = "#8a94a6"
        self.link_width = 1.0

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
        """The distance of two agents (3D); the same as agent_distance(a, b)."""
        return math.sqrt((a.x - b.x) ** 2 + (a.y - b.y) ** 2 + (a.z - b.z) ** 2)

    def agent_distance(self, a, b):
        """The distance of two agents (3D): the same name as for the agents of a grid."""
        return self.distance(a, b)

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

    def neighbor_pairs(self, radius):
        """All pairs of agents within the distance as two arrays of their indices i < j (fast for
        many agents: pairs_within of the positions)."""
        return pairs_within(self.positions(), radius)

    # networks: links between the agents (drawn as lines in the 3D view and in the page)

    def link(self, a, b, color=None):
        """Links two agents (undirected, once; color: of this link, None: link_color)."""
        if a is None or b is None or a is b:
            return
        self._links.setdefault(a, {})[b] = color
        self._links.setdefault(b, {})[a] = color

    def unlink(self, a, b):
        self._links.get(a, {}).pop(b, None)
        self._links.get(b, {}).pop(a, None)

    def remove_links(self, a):
        """Removes all links of the agent (e.g. before it is removed from agents)."""
        for o in self._links.pop(a, {}):
            self._links.get(o, {}).pop(a, None)

    def clear_links(self):
        self._links = {}

    def linked(self, a, b):
        return b in self._links.get(a, {})

    def link_neighbors(self, a):
        """The agents linked with the agent (in the order of the links)."""
        return list(self._links.get(a, {}))

    def degree(self, a):
        return len(self._links.get(a, {}))

    def set_link_color(self, a, b, color):
        if self.linked(a, b):
            self.link(a, b, color)

    def link_count(self):
        """The number of links (between agents of the list)."""
        return len(self.links())

    def links(self):
        """The links as a list of pairs of the indices of the agents (i < j)."""
        idx = {}
        for i, a in enumerate(self.agents):
            idx.setdefault(id(a), i)
        out = []
        for i, a in enumerate(self.agents):
            for o in self._links.get(a, {}):
                j = idx.get(id(o))
                if j is not None and j > i:
                    out.append((i, j))
        return out

    def link_random(self, p):
        """A random network: each pair of agents is linked with the probability p (Erdos-Renyi)."""
        n = len(self.agents)
        i, j = np.triu_indices(n, 1)
        hit = self.rng.random(len(i)) < p
        for a, b in zip(i[hit], j[hit]):
            self.link(self.agents[a], self.agents[b])

    def link_small_world(self, k, p):
        """A small-world network (Watts-Strogatz): a ring of the agents (their order), each linked
        with its k nearest (k / 2 on each side), then each link is moved with the probability p."""
        ag, n, half = self.agents, len(self.agents), max(1, int(k) // 2)
        if n < 2:
            return
        for i in range(n):
            for d in range(1, half + 1):
                self.link(ag[i], ag[(i + d) % n])
        for i in range(n):
            for d in range(1, half + 1):
                a, b = ag[i], ag[(i + d) % n]
                if self.random() < p and self.linked(a, b):
                    for _ in range(n):
                        c = ag[self.random_int(n)]
                        if c is not a and not self.linked(a, c):
                            self.unlink(a, b)
                            self.link(a, c)
                            break

    def link_scale_free(self, m):
        """A scale-free network (Barabasi-Albert): the first m + 1 agents are linked with each
        other, each further agent links with m agents before it (chosen by their number of links)."""
        ag, n, m = self.agents, len(self.agents), max(1, int(m))
        ends, start = [], min(n, m + 1)
        for i in range(start):
            for j in range(i + 1, start):
                self.link(ag[i], ag[j])
                ends += [i, j]
        for i in range(start, n):
            chosen = []
            while len(chosen) < min(m, i):
                j = ends[self.random_int(len(ends))] if ends else self.random_int(i)
                if j not in chosen:
                    chosen.append(j)
            for j in chosen:
                self.link(ag[i], ag[j])
                ends += [i, j]

    def link_nearby(self, radius):
        """Links all agents within the distance (a network of the neighbourhood)."""
        for i, j in zip(*self.neighbor_pairs(radius)):
            self.link(self.agents[i], self.agents[j])

    def components(self):
        """The groups of agents connected by links (lists in the order of agents)."""
        idx = {id(a): i for i, a in enumerate(self.agents)}
        seen, out = set(), []
        for a in self.agents:
            if id(a) in seen:
                continue
            group, queue = [], [a]
            seen.add(id(a))
            while queue:
                c = queue.pop(0)
                group.append(idx[id(c)])
                for o in self._links.get(c, {}):
                    if id(o) in idx and id(o) not in seen:
                        seen.add(id(o))
                        queue.append(o)
            out.append([self.agents[i] for i in sorted(group)])
        return out

    def shortest_path(self, a, b):
        """The number of links on the shortest path between two agents (-1: not connected)."""
        if a is b:
            return 0
        dist, queue = {id(a): 0}, [a]
        while queue:
            c = queue.pop(0)
            for o in self._links.get(c, {}):
                if id(o) not in dist:
                    dist[id(o)] = dist[id(c)] + 1
                    if o is b:
                        return dist[id(o)]
                    queue.append(o)
        return -1

    def clustering(self, a):
        """The share of the pairs of the linked agents of a which are linked too (0 with less than 2)."""
        nb = self.link_neighbors(a)
        k = len(nb)
        if k < 2:
            return 0.0
        closed = sum(1 for i in range(k) for j in range(i + 1, k) if self.linked(nb[i], nb[j]))
        return 2.0 * closed / (k * (k - 1))

    def layout_circle(self):
        """Places the agents on a circle in the middle of the box (their order; z: the middle)."""
        n = len(self.agents)
        r = min(self.box_width, self.box_height) * 0.42
        for i, a in enumerate(self.agents):
            t = 2 * math.pi * i / max(1, n)
            a.x, a.y, a.z = self.box_width / 2 + r * math.cos(t), self.box_height / 2 + r * math.sin(t), self.box_depth / 2
        return self

    def layout_spring(self, iterations=50):
        """Arranges the network in x and y (a spring layout: the links pull, all agents push each
        other away; in the box, z kept). Once (e.g. 100 iterations in init) or a few in each step."""
        n = len(self.agents)
        if n == 0:
            return
        w, h = self.box_width, self.box_height
        k = math.sqrt(w * h / n)
        pairs = np.array(self.links(), dtype=np.int64).reshape(-1, 2)
        p = np.array([(a.x, a.y) for a in self.agents], dtype=float)
        iu, ju = np.triu_indices(n, 1)
        for it in range(int(iterations)):
            temp = max(w, h) * 0.1 * (1 - it / iterations)
            disp = np.zeros((n, 2))
            e = p[iu] - p[ju]
            d = np.sqrt((e ** 2).sum(axis=1))
            same = d < 1e-9
            if same.any():
                s = (iu[same] + ju[same]).astype(float)
                e[same] = 0.01 * k * np.column_stack([np.cos(s), np.sin(s)])
                d[same] = 0.01 * k
            f = (e / d[:, None]) * (k * k / d)[:, None]
            np.add.at(disp, iu, f)
            np.add.at(disp, ju, -f)
            if len(pairs):
                e = p[pairs[:, 0]] - p[pairs[:, 1]]
                d = np.maximum(1e-9, np.sqrt((e ** 2).sum(axis=1)))
                f = (e / d[:, None]) * (d * d / k)[:, None]
                np.add.at(disp, pairs[:, 0], -f)
                np.add.at(disp, pairs[:, 1], f)
            dl = np.sqrt((disp ** 2).sum(axis=1))
            move = dl > 0
            sc = np.where(move, np.minimum(dl, temp) / np.where(move, dl, 1), 0)
            p = p + disp * sc[:, None]
            p[:, 0] = np.clip(p[:, 0], 0, w)
            p[:, 1] = np.clip(p[:, 1], 0, h)
        for a, (x, y) in zip(self.agents, p):
            a.x, a.y = float(x), float(y)

    def positions(self):
        """The positions of the agents as an array (n, 3)."""
        return np.array([(a.x, a.y, a.z) for a in self.agents], dtype=float).reshape(-1, 3)

    def _prepare(self):
        self.agents = []
        self._scenery = []
        self._links = {}

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
        pairs = self.links()
        if pairs:
            s["links"] = np.array(pairs, dtype=np.float64).ravel()
            s["link_colors"] = [self._links[self.agents[i]][self.agents[j]] or "" for i, j in pairs]
        s["link_color"] = str(self.link_color or "")
        s["link_width"] = float(self.link_width)
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
        self.agents = []
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


# for all models (as in Java): the same numbers in Bio7 and in the page
Simulation.cos_deg = staticmethod(cos_deg)
Simulation.sin_deg = staticmethod(sin_deg)


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


def _param_names(cls):
    names = []
    for c in reversed(cls.__mro__):
        for name, v in c.__dict__.items():
            if isinstance(v, param) and name not in names:
                names.append(name)
    return names


def _measures(m):
    """The values of the step: the line charts (the first chart by the name of the series,
    the others "chart: series") and the recorded values."""
    r = {}
    for name, value in m._charts:
        r[name] = value
    for c in m._more_charts:
        if c.get("k") == "line":
            r[c["t"] + ": " + c["n"]] = c["y"]
    r.update(m._records)
    return r


def _design_rows(design):
    """The parameter sets of a design: a pandas DataFrame, a dict of columns or a list of dicts."""
    if design is None:
        return None
    if hasattr(design, "to_dict") and hasattr(design, "columns"):
        return design.to_dict("records")
    if isinstance(design, dict):
        cols = {k: list(v) if isinstance(v, (list, tuple, range, np.ndarray)) else [v] for k, v in design.items()}
        n = max((len(v) for v in cols.values()), default=0)
        return [{k: v[i] for k, v in cols.items()} for i in range(n)]
    return [dict(r) for r in design]


def _as_kind(kind, v):
    """A value of a design or a sweep as the kind of the parameter (an int parameter: rounded)."""
    if isinstance(v, np.generic):
        v = v.item()
    if kind is bool:
        return bool(v)
    if kind is int and isinstance(v, (int, float)):
        return int(round(v))
    if kind is float and isinstance(v, (int, float)):
        return float(v)
    return v


def experiment(model, params=None, steps=100, repetitions=1, seed=DEFAULT_SEED, every=1, as_frame=True, progress=None, design=None):
    """Runs the model several times without a display (batch runs) and returns a table.

    model: the class of the model (the parameters: its defaults) or a model (its parameter
    values are the start values), params: a dict of the swept parameters and their values
    (a list or a single value; all combinations are run), steps: the steps of each run,
    repetitions: the runs of each combination (the seeds seed, seed + 1, ...: the same for all
    parameter values), every: a row every n steps (step 0 and the last step always).
    The rows: run, seed, the swept parameters, step, the values of the line charts in the step
    (chart(...): the name of the series, a second chart "chart: series") and of record(name, value).
    Returns a pandas DataFrame (if pandas is installed and as_frame) or a list of dicts.
    progress(done, total): called after each run.
    design: given parameter sets instead of (or combined with) the sweep, e.g. a Latin hypercube or
    the samples of a sensitivity analysis (SALib): a pandas DataFrame, a dict of columns or a list
    of dicts (the column names are parameters). Each row is run with the repetitions; the column
    "point" of the table is the row of the design (0, 1, ...). Values of int parameters are rounded."""
    import itertools
    cls = model if isinstance(model, type) else type(model)
    base = None if isinstance(model, type) else model
    kinds = {n: type(getattr(cls, n).value) if isinstance(getattr(cls, n, None), param) else None for n in _param_names(cls)}
    params = dict(params or {})
    points = _design_rows(design)
    lists = [list(v) if isinstance(v, (list, tuple, range, np.ndarray)) else [v] for v in params.values()]
    grid = [dict(zip(params, c)) for c in itertools.product(*lists)]
    # the parameter sets: each row of the design with each combination of the sweep
    combos = [(None, g) for g in grid] if points is None else [(i, {**pt, **g}) for i, pt in enumerate(points) for g in grid]
    names = list(dict.fromkeys([n for _, c in combos for n in c]))
    unknown = [n for n in names if n not in kinds]
    if unknown:
        raise ValueError("Not a parameter of the model: " + ", ".join(unknown))
    total, rows, run = len(combos) * max(1, int(repetitions)), [], 0
    if progress is None and not BROWSER:
        # in Bio7: a job in the Progress view (its stop button interrupts the experiment)
        try:
            import bio7

            def progress(done, all_runs):
                bio7.progress(done, all_runs, "Experiment: run %d of %d" % (done, all_runs))
        except ImportError:
            pass
    if progress is not None:
        progress(0, total)
    for point, combo in combos:
        for k in range(max(1, int(repetitions))):
            run += 1
            m = cls()
            if base is not None:
                for n in _param_names(cls):
                    setattr(m, n, getattr(base, n))
            for n, v in combo.items():
                setattr(m, n, _as_kind(kinds[n], v))
            m.t = 0
            m.seed(int(seed) + k)
            m._charts, m._more_charts, m._drawing, m._records = [], [], [], {}
            m._stopped = False
            m._prepare()
            m.init()

            def row():
                r = {"run": run} if point is None else {"run": run, "point": point}
                r["seed"] = int(seed) + k
                r.update({n: getattr(m, n) for n in names})
                r["step"] = m.t
                r.update(_measures(m))
                return r

            rows.append(row())
            for s in range(1, int(steps) + 1):
                if m._stopped:
                    break
                m._charts, m._more_charts, m._drawing = [], [], []
                m.step()
                m.t += 1
                # the last step of the run: also a step with stop()
                if s % max(1, int(every)) == 0 or s == int(steps) or m._stopped:
                    rows.append(row())
            if progress is not None:
                progress(run, total)
    if as_frame:
        try:
            import pandas as pd
            return pd.DataFrame(rows)
        except ImportError:
            pass
    return rows


def _csv(rows, file):
    """Writes the rows (dicts) as CSV (NA for missing values)."""
    cols = []
    for r in rows:
        for c in r:
            if c not in cols:
                cols.append(c)

    def cell(v):
        if v is None or (isinstance(v, float) and math.isnan(v)):
            return "NA"
        if isinstance(v, bool):
            return "TRUE" if v else "FALSE"
        if isinstance(v, (int, float, np.integer, np.floating)):
            f = float(v)
            return str(int(f)) if f == int(f) and abs(f) < 1e15 else repr(f)
        return '"' + str(v).replace('"', '""') + '"'

    with open(file, "w", encoding="utf-8") as fh:
        fh.write(",".join('"' + c.replace('"', '""') + '"' for c in cols) + "\n")
        for r in rows:
            fh.write(",".join(cell(r.get(c)) for c in cols) + "\n")


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
        m._stopped = False
        # the seed of the settings (the field Seed of Bio7 and of the page)
        m.seed(int(float(arg)) if str(arg).strip() else DEFAULT_SEED)
        m._charts = []
        m._more_charts = []
        m._drawing = []
        m._prepare()
        m.init()
    elif op == "step":
        m._drawing = []
        m._stopped = False
        m.step()
        m.t += 1
    elif op == "action":
        getattr(m, arg)()
    elif op == "input":
        # a click, a drag (x, y: cells of a grid, the floor of an agent model) or a key of the view
        ev = arg.split("\x1f")
        if ev[0] == "key":
            m.key(ev[1] if len(ev) > 1 else "")
        else:
            getattr(m, ev[0])(float(ev[1]), float(ev[2]))
    elif op == "set":
        for k, v in json.loads(arg).items():
            setattr(m, k, v)
        return {}
    elif op == "experiment":
        # an experiment of Bio7 (the dialog): the settings of the model are the start values, the table as CSV
        spec = json.loads(arg)
        rows = experiment(m, spec.get("params", {}), spec.get("steps", 100), spec.get("repetitions", 1),
                          spec.get("seed", DEFAULT_SEED), spec.get("every", 1), as_frame=False)
        _csv(rows, spec["file"])
        # the table also as a variable of the console (a pandas DataFrame, a list of dicts without pandas)
        frame = spec.get("frame")
        if frame:
            import __main__
            value = rows
            try:
                import pandas as pd
                value = pd.DataFrame(rows)
            except ImportError:
                pass
            setattr(__main__, frame, value)
        return {"rows": float(len(rows))}
    s = m._state()
    s["chart_names"] = [c[0] for c in m._charts]
    s["chart_values"] = np.array([c[1] for c in m._charts], dtype=np.float64)
    s["more_charts"] = json.dumps(m._more_charts)
    # the agents of a grid are drawn on top of the shapes of the step
    s["draw"] = json.dumps(m._drawing + (_agent_shapes(m.agents) if isinstance(m, _GridAgents) else []))
    s["stopped"] = 1.0 if getattr(m, "_stopped", False) else 0.0
    # the input methods of the model (Bio7 and the page give it the clicks, drags and keys of the view)
    s["inputs"] = [n for n in ("click", "drag", "key") if callable(getattr(m, n, None))]
    m._charts = []
    m._more_charts = []
    return s
