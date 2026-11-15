# Copyright (c) 2007-2026 M. Austenfeld
# SPDX-License-Identifier: EPL-2.0
#
# The bootstrap of the embedded Python of Bio7 (the module _bio7). Java calls
# the functions of this module with PyRun_SimpleString and exchanges the data
# through native memory (ctypes) and upcalls (functions of Java called by
# ctypes). Not for the use in scripts: see the module bio7.

import sys
import os
import ctypes
import json
import types
import traceback
import code as _code

try:
    import numpy as np
except Exception:  # numpy is optional
    np = None

import array as _array

_write_cb = _reply_cb = _call_cb = _plot_cb = _figs_cb = None
_console = None
_main = sys.modules["__main__"]
_ret = None
_view = [640, 480, 1.0]
_rendered = {}
_open_figures = []
_main_ident = 0
_NOVALUE = object()

# numpy dtypes <-> array typecodes (without numpy)
_TYPECODES = {"f8": "d", "f4": "f", "i8": "q", "i4": "i", "i2": "h", "i1": "b", "u1": "B", "u2": "H", "u4": "I", "u8": "Q", "b1": "B"}


class _Writer:
    """sys.stdout and sys.stderr: the text goes to the console of the application."""

    encoding = "utf-8"
    errors = "replace"

    def __init__(self, stream):
        self._stream = stream

    def write(self, s):
        if not isinstance(s, str):
            s = str(s)
        if s and _write_cb is not None:
            b = s.encode("utf-8", "replace")
            _write_cb(self._stream, b, len(b))
        return len(s)

    def writelines(self, lines):
        for line in lines:
            self.write(line)

    def flush(self):
        pass

    def isatty(self):
        return False

    def writable(self):
        return True

    def readable(self):
        return False

    def fileno(self):
        raise OSError("The console of Bio7 has no file descriptor")


class _Reader:
    """sys.stdin: no input (the console sends complete lines to the interpreter)."""

    encoding = "utf-8"

    def readline(self, *args):
        raise EOFError("input() is not available in the embedded Python of Bio7")

    def read(self, *args):
        return ""

    def isatty(self):
        return False

    def readable(self):
        return True

    def fileno(self):
        raise OSError("The console of Bio7 has no file descriptor")


def _init(write, reply, call, plot, figs, cfg_addr, cfg_len):
    global _write_cb, _reply_cb, _call_cb, _plot_cb, _figs_cb, _console, _main_ident
    import threading
    cfg = json.loads(ctypes.string_at(cfg_addr, cfg_len).decode("utf-8"))
    _write_cb = ctypes.CFUNCTYPE(None, ctypes.c_int, ctypes.c_char_p, ctypes.c_ssize_t)(write)
    _reply_cb = ctypes.CFUNCTYPE(None, ctypes.c_int, ctypes.c_char_p, ctypes.c_ssize_t, ctypes.c_void_p, ctypes.c_ssize_t)(reply)
    _call_cb = ctypes.CFUNCTYPE(ctypes.c_int, ctypes.c_char_p, ctypes.c_ssize_t, ctypes.c_char_p, ctypes.c_ssize_t, ctypes.c_void_p,
                                ctypes.c_ssize_t)(call)
    _plot_cb = ctypes.CFUNCTYPE(None, ctypes.c_int, ctypes.c_void_p, ctypes.c_int, ctypes.c_int, ctypes.c_double)(plot)
    _figs_cb = ctypes.CFUNCTYPE(None, ctypes.c_char_p, ctypes.c_ssize_t)(figs)
    sys.stdout = _Writer(0)
    sys.stderr = _Writer(1)
    sys.stdin = _Reader()
    sys.__stdout__, sys.__stderr__, sys.__stdin__ = sys.stdout, sys.stderr, sys.stdin
    # The modules of the selected Python (also of a venv or conda environment)
    for p in reversed(cfg.get("path", [])):
        if p not in sys.path:
            sys.path.insert(0, p)
    if cfg.get("executable"):
        sys.executable = cfg["executable"]
    sys.argv = [""]
    _view[:] = cfg.get("view", _view)
    # The plot device: matplotlib draws into the plot view of Bio7
    # (the backend module is created when matplotlib imports it: matplotlib may be installed later)
    os.environ["MPLBACKEND"] = "module://bio7_backend"
    sys.meta_path.insert(0, _BackendFinder())
    # Interactive charts (plotly, altair, folium, holoviews/hvplot): show() in the browser view of Bio7
    sys.meta_path.insert(0, _PatchFinder())
    _install_module("bio7", _BIO7_SOURCE)
    # Bio7 is the web browser of Python: plotly, bokeh, folium, webbrowser.open show in the browser view of Bio7
    _register_browser()
    _main.__dict__.setdefault("bio7", sys.modules["bio7"])
    _console = _Console(_main.__dict__, filename="<console>")
    _main_ident = threading.get_ident()


class _BackendFinder:
    """Creates the module bio7_backend (the plot device) when it is imported."""

    def find_spec(self, name, path=None, target=None):
        if name != "bio7_backend":
            return None
        import importlib.util
        return importlib.util.spec_from_loader(name, self)

    def create_module(self, spec):
        return None

    def exec_module(self, module):
        exec(compile(_BACKEND_SOURCE, "<bio7_backend>", "exec"), module.__dict__)


# ---------------------------------------------------------------- interactive charts

# A function(html, kind) which takes the HTML of the charts (a markdown chunk), else None: the browser view
_html_sink = None


def _html_of(obj):
    """The HTML page of an interactive chart (plotly, altair, bokeh, folium, holoviews/hvplot) or None."""
    if isinstance(obj, str):
        return obj if "<" in obj else None
    mod = type(obj).__module__.split(".")[0]
    if mod == "plotly" and hasattr(obj, "to_html"):
        return obj.to_html(include_plotlyjs="cdn", full_html=True)
    if mod == "altair" and hasattr(obj, "to_html"):
        return obj.to_html()
    if mod in ("folium", "branca") and hasattr(obj, "get_root"):
        return obj.get_root().render()
    if mod == "holoviews":
        import holoviews as hv
        obj = hv.render(obj, backend="bokeh")
        mod = "bokeh"
    if mod == "bokeh":
        from bokeh.embed import file_html
        from bokeh.resources import CDN
        return file_html(obj, CDN, "Bokeh")
    return None


def _is_chart(obj):
    """An object which _html_of shows (the last expression of a chunk)."""
    mod = type(obj).__module__.split(".")[0]
    if mod == "plotly":
        return hasattr(obj, "to_html") and hasattr(obj, "data")
    if mod == "altair":
        return hasattr(obj, "to_html") and hasattr(obj, "to_dict")
    if mod == "folium":
        return hasattr(obj, "get_root")
    if mod == "holoviews":
        return hasattr(obj, "opts")
    if mod == "bokeh":
        return hasattr(obj, "renderers") or type(obj).__name__ in ("Row", "Column", "GridPlot", "GridBox", "Tabs")
    return False


def _show_html(html, kind="chart"):
    """Shows an HTML page in the browser view of Bio7 (or gives it to a markdown chunk)."""
    if _html_sink is not None:
        _html_sink(html, kind)
        return
    import tempfile
    import pathlib
    fd, path = tempfile.mkstemp(suffix=".html", prefix="bio7_")
    with os.fdopen(fd, "w", encoding="utf-8") as f:
        f.write(html)
    _call("open_url", pathlib.Path(path).as_uri())


def _show_chart(obj, *args, **kwargs):
    html = _html_of(obj)
    if html is None:
        raise TypeError("Not an interactive chart: " + type(obj).__name__)
    _show_html(html, type(obj).__module__.split(".")[0])


def _patch_plotly(m):
    m.BaseFigure.show = lambda self, *a, **k: _show_chart(self)


def _patch_altair(m):
    for name in ("TopLevelMixin", "Chart"):
        cls = getattr(m, name, None)
        if cls is not None:
            cls.show = lambda self, *a, **k: _show_chart(self)


def _patch_folium(m):
    # A map has no show(): map.show() and map.show_in_browser() in Bio7
    for name in ("Map", "Figure"):
        cls = getattr(m, name, None)
        if cls is not None:
            cls.show = lambda self, *a, **k: _show_chart(self)
            cls.show_in_browser = lambda self, *a, **k: _show_chart(self)


def _patch_holoviews(m):
    try:
        dim = sys.modules.get("holoviews.core.dimension")
        if dim is not None and not hasattr(dim.Dimensioned, "show"):
            dim.Dimensioned.show = lambda self, *a, **k: _show_chart(self)
    except Exception:
        pass


def _patch_hvplot(m):
    m.show = lambda obj, *a, **k: _show_chart(obj)


_PATCHES = {"plotly.basedatatypes": _patch_plotly, "altair": _patch_altair, "folium": _patch_folium,
            "holoviews": _patch_holoviews, "hvplot": _patch_hvplot}


class _PatchedLoader:
    """The loader of a module which is patched after it is executed (other attributes as the loader)."""

    def __init__(self, loader, patch):
        self._loader = loader
        self._patch = patch

    def create_module(self, spec):
        return self._loader.create_module(spec)

    def exec_module(self, module):
        self._loader.exec_module(module)
        try:
            self._patch(module)
        except Exception:
            pass

    def __getattr__(self, name):
        return getattr(self._loader, name)


class _PatchFinder:
    """Patches the show() of chart modules when they are imported."""

    def find_spec(self, name, path=None, target=None):
        patch = _PATCHES.get(name)
        if patch is None:
            return None
        for finder in sys.meta_path:
            if finder is self or not hasattr(finder, "find_spec"):
                continue
            spec = finder.find_spec(name, path, target)
            if spec is not None:
                if spec.loader is not None and hasattr(spec.loader, "exec_module"):
                    spec.loader = _PatchedLoader(spec.loader, patch)
                return spec
        return None


def _register_browser():
    import webbrowser

    class Bio7Browser(webbrowser.BaseBrowser):
        def open(self, url, new=0, autoraise=True):
            try:
                if url.startswith("file:") or (os.path.isabs(url) and url.lower().endswith((".html", ".htm"))):
                    # a page of a file (e.g., bokeh show()): in the browser view or a markdown chunk
                    import urllib.parse
                    import urllib.request
                    path = urllib.request.url2pathname(urllib.parse.urlparse(url).path) if url.startswith("file:") else url
                    with open(path, encoding="utf-8") as f:
                        html = f.read()
                    _show_html(html, "bokeh" if "Bokeh" in html[:4000] else "page")
                elif url.startswith(("http://127.0.0.1", "http://localhost")):
                    # a one-shot local server (e.g., plotly): the page is fetched here and shown as a file
                    # (Python doesn't wait for the browser, the page can be reloaded)
                    import threading
                    threading.Thread(target=_show_local_page, args=(url,), daemon=True).start()
                else:
                    _call("open_url", url)
                return True
            except Exception:
                return False

    try:
        webbrowser.register("bio7", None, Bio7Browser("bio7"), preferred=True)
    except Exception:
        pass


def _show_local_page(url):
    import time
    import tempfile
    import urllib.request
    import pathlib
    data = None
    for _ in range(100):
        try:
            data = urllib.request.urlopen(url, timeout=20).read()
            break
        except Exception:
            time.sleep(0.1)
    if data is None:
        sys.stderr.write("The page could not be shown: " + url + "\n")
        return
    fd, path = tempfile.mkstemp(suffix=".html", prefix="bio7_")
    with os.fdopen(fd, "wb") as f:
        f.write(data)
    try:
        _call("open_url", pathlib.Path(path).as_uri())
    except Exception as e:
        sys.stderr.write("The page could not be shown: %s\n" % e)


def _install_module(name, source):
    m = types.ModuleType(name)
    m.__file__ = "<" + name + ">"
    sys.modules[name] = m
    exec(compile(source, "<" + name + ">", "exec"), m.__dict__)


def _error_text(e, skip=1):
    tb = e.__traceback__
    for _ in range(skip):
        if tb is not None:
            tb = tb.tb_next
    return "".join(traceback.format_exception(type(e), e, tb)) + _missing_hint(e)


def _missing_hint(e):
    """A hint for a module which is not installed (the package can be installed in Bio7)."""
    if isinstance(e, ModuleNotFoundError) and e.name:
        return ("The module '%s' is not installed for the Python of Bio7 (%s).\n"
                "Install it with 'Python Packages...' (Python preferences or the menu of the console).\n"
                % (e.name.split(".")[0], sys.executable))
    return ""


class _Console(_code.InteractiveConsole):
    """The console: with the hint for modules which are not installed."""

    def showtraceback(self):
        super().showtraceback()
        self.write(_missing_hint(sys.exc_info()[1]))


def _reply_error(text):
    b = text.encode("utf-8", "replace")
    _reply_cb(1, b, len(b), None, 0)


def _run(addr, n, mode):
    """Runs code from Java (mode: exec, eval, file, console, consolefile)."""
    text = ctypes.string_at(addr, n).decode("utf-8")
    try:
        if mode == "exec":
            exec(compile(text, "<bio7>", "exec"), _main.__dict__)
        elif mode == "eval":
            _reply(eval(compile(text, "<bio7>", "eval"), _main.__dict__))
        elif mode == "file":
            _run_file(text)
        elif mode == "console":
            more = _console.push(text)
            if not more:
                _flush()
            _prompt(more)
    except SystemExit:
        if mode in ("exec", "eval"):
            _reply_error("SystemExit: exit() is not available in the embedded Python")
        else:
            sys.stderr.write("exit() is not available in the embedded Python of Bio7\n")
            if mode == "console":
                _console.resetbuffer()
                _prompt(False)
    except BaseException as e:
        if mode in ("exec", "eval"):
            _reply_error(_error_text(e))
        else:
            sys.stderr.write(_error_text(e, 2 if mode == "file" else 1))
            if mode == "console":
                _console.resetbuffer()
                _prompt(False)


def _run_file(path):
    folder = os.path.dirname(os.path.abspath(path))
    if folder not in sys.path:
        sys.path.insert(0, folder)
    with open(path, "rb") as f:
        source = f.read()
    ns = _main.__dict__
    ns["__file__"] = path
    try:
        exec(compile(source, path, "exec"), ns)
    finally:
        ns.pop("__file__", None)
        _flush()


def _prompt(more=False):
    sys.stdout.write("... " if more else ">>> ")


# ---------------------------------------------------------------- data


def _np_code(a):
    k, size = a.dtype.kind, a.dtype.itemsize
    if k == "b":
        return "b1"
    if k in "iu" and size in (1, 2, 4, 8):
        return k + str(size)
    if k == "f" and size in (4, 8):
        return "f" + str(size)
    return None


def _export(v):
    """The value as header (JSON) and buffers [(address, length)]; hold keeps the memory."""
    hold, bufs = [], []

    def add(obj, address, length):
        hold.append(obj)
        bufs.append((address, length))
        return len(bufs) - 1

    def nd(a):
        a = np.ascontiguousarray(a)
        c = _np_code(a)
        if c is None:
            if a.dtype.kind == "f":
                a, c = a.astype("f8"), "f8"
            elif a.dtype.kind in "USO" or a.dtype.kind in "mM":
                return {"t": "str", "v": [None if x is None else str(x) for x in a.ravel().tolist()], "shape": list(a.shape)}
            else:
                return {"t": "other", "type": "ndarray[" + str(a.dtype) + "]", "repr": repr(a)[:2000]}
        if not a.dtype.isnative:
            a = a.astype(a.dtype.newbyteorder("="))
        return {"t": "nd", "dtype": c, "shape": list(a.shape), "b": add(a, a.ctypes.data, a.nbytes)}

    def packed(typecode, dtype, values):
        a = _array.array(typecode, values)
        address, count = a.buffer_info()
        return {"t": "nd", "dtype": dtype, "shape": [count], "b": add(a, address, count * a.itemsize)}

    def enc(x):
        if x is None:
            return {"t": "none"}
        if np is not None:
            if isinstance(x, np.ndarray):
                return nd(x)
            if isinstance(x, np.generic):
                return nd(np.asarray(x))
        if isinstance(x, bool):
            return {"t": "nd", "dtype": "b1", "shape": [], "b": add(*_scalar("B", int(x)))}
        if isinstance(x, int):
            if -2 ** 63 <= x < 2 ** 63:
                return {"t": "nd", "dtype": "i8", "shape": [], "b": add(*_scalar("q", x))}
            return {"t": "nd", "dtype": "f8", "shape": [], "b": add(*_scalar("d", float(x)))}
        if isinstance(x, float):
            return {"t": "nd", "dtype": "f8", "shape": [], "b": add(*_scalar("d", x))}
        if isinstance(x, str):
            return {"t": "str", "v": [x], "scalar": True}
        if isinstance(x, (list, tuple)):
            if x and all(isinstance(e, str) or e is None for e in x):
                return {"t": "str", "v": list(x)}
            if x and all(isinstance(e, bool) for e in x):
                return packed("B", "b1", [int(e) for e in x])
            if x and all(isinstance(e, (int, float)) and not isinstance(e, bool) for e in x):
                if all(isinstance(e, int) for e in x) and all(-2 ** 63 <= e < 2 ** 63 for e in x):
                    return packed("q", "i8", x)
                return packed("d", "f8", [float(e) for e in x])
            return {"t": "list", "v": [enc(e) for e in x]}
        if isinstance(x, dict):
            return {"t": "dict", "v": {str(k): enc(e) for k, e in x.items()}}
        if isinstance(x, _array.array):
            dtype = {v: k for k, v in _TYPECODES.items() if k != "b1"}.get(x.typecode)
            if dtype is not None:
                address, count = x.buffer_info()
                return {"t": "nd", "dtype": dtype, "shape": [count], "b": add(x, address, count * x.itemsize)}
        pd = sys.modules.get("pandas")
        if pd is not None:
            if isinstance(x, pd.DataFrame):
                cols = []
                for name in x.columns:
                    col = x[name]
                    values = col.to_numpy()
                    if values.dtype.kind == "O" or str(col.dtype) in ("category", "string"):
                        values = [None if (v is None or (isinstance(v, float) and v != v)) else str(v) for v in col.tolist()]
                    cols.append([str(name), enc(values)])
                index = None if isinstance(x.index, pd.RangeIndex) and x.index.start == 0 and x.index.step == 1 else [str(i) for i in x.index]
                return {"t": "df", "cols": cols, "index": index}
            if isinstance(x, pd.Series):
                return enc(x.to_numpy())
        if np is not None and hasattr(x, "__array__"):
            try:
                a = np.asarray(x)
                if _np_code(a) is not None or a.dtype.kind == "f":
                    return nd(a)
            except Exception:
                pass
        return {"t": "other", "type": type(x).__name__, "repr": repr(x)[:2000]}

    return enc(v), bufs, hold


def _scalar(typecode, value):
    a = _array.array(typecode, [value])
    address, _ = a.buffer_info()
    return a, address, a.itemsize


def _pointers(bufs):
    arr = (ctypes.c_int64 * (2 * len(bufs)))()
    for i, (address, length) in enumerate(bufs):
        arr[2 * i] = address
        arr[2 * i + 1] = length
    return arr


def _reply(value):
    header, bufs, hold = _export(value)
    h = json.dumps(header).encode("utf-8")
    ptrs = _pointers(bufs)
    _reply_cb(0, h, len(h), ctypes.addressof(ptrs), len(bufs))
    del hold


def _build(h, ptrs):
    t = h["t"]
    if t == "nd":
        address, length = ptrs[h["b"]]
        shape = tuple(h["shape"])
        dtype = h["dtype"]
        if np is not None:
            a = np.empty(shape, dtype=np.dtype("?" if dtype == "b1" else dtype))
            if length:
                ctypes.memmove(a.ctypes.data, address, length)
            return a.item() if shape == () else a
        a = _array.array(_TYPECODES[dtype], ctypes.string_at(address, length))
        values = [bool(e) for e in a] if dtype == "b1" else a
        if shape == ():
            return values[0]
        if len(shape) > 1:
            return _nest(list(values), shape)
        return values if dtype != "b1" else list(values)
    if t == "str":
        v = h["v"]
        return v[0] if h.get("scalar") or len(v) == 1 else list(v)
    if t == "list":
        return [_build(e, ptrs) for e in h["v"]]
    if t == "dict":
        return {k: _build(e, ptrs) for k, e in h["v"].items()}
    if t == "df":
        # a column of one string is a str: a list for the DataFrame
        cols = {name: (lambda v: [v] if isinstance(v, str) else v)(_build(col, ptrs)) for name, col in h["cols"]}
        try:
            import pandas as pd
            return pd.DataFrame(cols, index=h.get("index"))
        except ImportError:
            return cols
    return None


def _nest(values, shape):
    if len(shape) == 1:
        return values
    step = len(values) // shape[0]
    return [_nest(values[i * step:(i + 1) * step], shape[1:]) for i in range(shape[0])]


def _put(name, hdr_addr, hdr_len, bufs_addr, nbuf, target):
    """Assigns a value from Java (target: 0 __main__, 1 the result of a call)."""
    global _ret
    try:
        h = json.loads(ctypes.string_at(hdr_addr, hdr_len).decode("utf-8"))
        raw = (ctypes.c_int64 * (2 * nbuf)).from_address(bufs_addr) if nbuf else []
        ptrs = [(raw[2 * i], raw[2 * i + 1]) for i in range(nbuf)]
        value = _build(h, ptrs)
        if target == 1:
            _ret = value
        else:
            _main.__dict__[name] = value
    except BaseException as e:
        if target == 1:
            _ret = e
        else:
            _reply_error(_error_text(e))


def _call(function, *args, value=_NOVALUE):
    """Calls a function of the application (see the module bio7)."""
    global _ret
    request = "\x01".join([function] + [("" if a is None else str(a)) for a in args]).encode("utf-8")
    if value is _NOVALUE:
        status = _call_cb(request, len(request), None, 0, None, 0)
    else:
        header, bufs, hold = _export(value)
        h = json.dumps(header).encode("utf-8")
        ptrs = _pointers(bufs)
        status = _call_cb(request, len(request), h, len(h), ctypes.addressof(ptrs), len(bufs))
        del hold
    result, _ret = _ret, None
    if isinstance(result, BaseException):
        raise result
    if status != 0:
        raise RuntimeError(result if isinstance(result, str) else "The call of bio7." + function + " failed")
    return result


# ---------------------------------------------------------------- memory


def _memory():
    """The memory of the variables of __main__ in MB (numpy arrays with their data,
    pandas objects with their content, lists and dicts with their elements)."""
    np, pd = sys.modules.get("numpy"), sys.modules.get("pandas")
    seen = set()
    total = 0
    for name, v in list(_main.__dict__.items()):
        if name.startswith("__") or isinstance(v, types.ModuleType) or callable(v) or id(v) in seen:
            continue
        seen.add(id(v))
        total += _sizeof(v, np, pd, seen, 0)
    return total / 1048576.0


def _sizeof(v, np, pd, seen, depth):
    try:
        if np is not None and isinstance(v, np.ndarray):
            # getsizeof of an array which owns its data contains the data; a view counts
            # its data only if its base is not a variable itself
            if v.base is None:
                return sys.getsizeof(v)
            return sys.getsizeof(v) + (0 if id(v.base) in seen else v.nbytes)
        if pd is not None and isinstance(v, pd.DataFrame):
            return int(v.memory_usage(deep=True).sum())
        if pd is not None and isinstance(v, pd.Series):
            return int(v.memory_usage(deep=True))
        size = sys.getsizeof(v)
        if depth < 3 and isinstance(v, (list, tuple, set, frozenset, dict)):
            items = v.values() if isinstance(v, dict) else v
            for i, e in enumerate(items):
                if i >= 100000:
                    break
                if id(e) not in seen:
                    seen.add(id(e))
                    size += _sizeof(e, np, pd, seen, depth + 1)
        return size
    except Exception:
        return 0


# ---------------------------------------------------------------- plots


def _set_view(w, h, scale):
    _view[:] = [w, h, scale]


def _figure_numbers():
    if "matplotlib.pyplot" not in sys.modules:
        return []
    from matplotlib._pylab_helpers import Gcf
    return [m.num for m in Gcf.get_all_fig_managers()]


def _notify_figures():
    global _open_figures
    nums = _figure_numbers()
    if nums != _open_figures:
        _open_figures = nums
        for n in list(_rendered):
            if n not in nums:
                del _rendered[n]
        b = json.dumps(nums).encode("utf-8")
        _figs_cb(b, len(b))


def _render(num):
    """Draws a figure in the size of the plot view and sends the image to Java."""
    from matplotlib._pylab_helpers import Gcf
    m = Gcf.get_fig_manager(num)
    if m is None:
        return False
    fig = m.canvas.figure
    w, h, scale = _view
    fig.set_size_inches(max(w, 50) / 96.0, max(h, 50) / 96.0, forward=False)
    fig.set_dpi(96.0 * scale)
    m.canvas.draw()
    rgba = memoryview(m.canvas.buffer_rgba())
    if np is not None:
        a = np.ascontiguousarray(np.asarray(rgba))
        height, width = a.shape[0], a.shape[1]
        address = a.ctypes.data
    else:
        width, height = m.canvas.get_width_height(physical=True)
        a = (ctypes.c_char * len(rgba.tobytes())).from_buffer_copy(rgba.tobytes())
        address = ctypes.addressof(a)
    _plot_cb(int(num), address, int(width), int(height), float(scale))
    fig.stale = False
    _rendered[num] = True
    return True


def _show_all():
    """plt.show(): the new and changed figures, the active one last (shown in the view)."""
    _flush(always_active=True)


def _flush(always_active=False):
    """Draws the new and changed figures (after a console line or a script)."""
    if "matplotlib.pyplot" not in sys.modules:
        return
    try:
        from matplotlib._pylab_helpers import Gcf
        active = Gcf.get_active()
        changed = [m for m in Gcf.get_all_fig_managers()
                   if m.num not in _rendered or m.canvas.figure.stale or (always_active and m is active)]
        changed.sort(key=lambda m: m is active)
        for m in changed:
            _render(m.num)
        _notify_figures()
    except Exception:
        sys.stderr.write(traceback.format_exc())


def _resize(num, w, h, scale):
    _set_view(w, h, scale)
    if num in _figure_numbers():
        _render(num)


def _save(num, path, fmt, dpi):
    from matplotlib._pylab_helpers import Gcf
    m = Gcf.get_fig_manager(num)
    if m is None:
        raise ValueError("The figure %d is not open" % num)
    m.canvas.figure.savefig(path, format=fmt, dpi=dpi if dpi > 0 else "figure")


def _close(num):
    if "matplotlib.pyplot" in sys.modules:
        import matplotlib.pyplot as plt
        plt.close(num if num >= 0 else "all")
        _notify_figures()


_BACKEND_SOURCE = r'''
"""The plot device of Bio7: matplotlib draws into the plot view of Bio7."""
from matplotlib.backend_bases import _Backend, FigureManagerBase
from matplotlib.backends.backend_agg import FigureCanvasAgg
import _bio7


class FigureManagerBio7(FigureManagerBase):
    def show(self):
        _bio7._render(self.num)
        _bio7._notify_figures()


class FigureCanvasBio7(FigureCanvasAgg):
    manager_class = FigureManagerBio7


@_Backend.export
class _BackendBio7(_Backend):
    FigureCanvas = FigureCanvasBio7
    FigureManager = FigureManagerBio7

    @classmethod
    def show(cls, *args, **kwargs):
        _bio7._show_all()
'''

_BIO7_SOURCE = r'''
"""Bio7 for the embedded Python: the data of ImageJ, R and the plot view.

    import bio7
    img = bio7.image()                 # the current ImageJ image as numpy array
    bio7.show_image(img * 2, "Twice")  # a new ImageJ image
    bio7.show_html(fig)                # a plotly/altair/bokeh/folium chart in the browser view
    df = bio7.results_table()          # the ImageJ results table
    x, y = bio7.roi()                  # the coordinates of the current selection
    bio7.to_r("x", [1, 2, 3])          # assign in R
    m = bio7.r("summary(cars)")        # evaluate in R
"""
import _bio7


def image(title=None):
    """The current (or the named) ImageJ image: 2-d (height, width), a stack
    (slices, height, width), RGB (height, width, 3)."""
    return _bio7._call("image", title or "")


def show_image(array, title="Python"):
    """Shows a 2-d array (or a stack, RGB uint8 (h, w, 3)) as ImageJ image."""
    return _bio7._call("show_image", title, value=array)


def show_html(obj):
    """Shows an interactive chart (plotly, altair, bokeh, folium, holoviews/hvplot)
    or an HTML string in the browser view of Bio7 (in a markdown chunk: below the
    chunk, in a rendered document: embedded)."""
    html = _bio7._html_of(obj)
    if html is None:
        raise TypeError("Not an interactive chart or HTML: " + type(obj).__name__)
    _bio7._show_html(html, type(obj).__module__.split(".")[0])


def results_table():
    """The ImageJ results table (a pandas DataFrame, a dict without pandas)."""
    return _bio7._call("results_table")


def roi():
    """The coordinates (x, y) of the selection of the current ImageJ image."""
    d = _bio7._call("roi")
    return d["x"], d["y"]


def r(code):
    """Evaluates R code (embedded R) and returns the value."""
    return _bio7._call("r", code)


def from_r(name):
    """The value of an R object."""
    return _bio7._call("r", name)


def to_r(name, value):
    """Assigns a value (numbers, strings, arrays, DataFrame, dict) in R."""
    _bio7._call("to_r", name, value=value)


def call(function, *args, value=None):
    """Calls a function of Bio7 registered for Python."""
    return _bio7._call(function, *args, value=value) if value is not None else _bio7._call(function, *args)
'''
