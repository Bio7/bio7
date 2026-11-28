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

# numpy is optional and imported when needed (_numpy): the paths of the selected
# Python (e.g., the site-packages of a venv) are only added by _init, and an
# environment may be added later (or numpy installed while Bio7 runs)
np = None
_np_tried = None


def _numpy():
    """numpy or None: imported again after sys.path changed (until it is found)."""
    global np, _np_tried
    if np is not None:
        return np
    m = sys.modules.get("numpy")
    if m is None:
        key = tuple(sys.path)
        if key == _np_tried:
            return None
        _np_tried = key
        try:
            import numpy as m
        except Exception:
            m = None
    np = m
    return np

import array as _array

_write_cb = _reply_cb = _call_cb = _plot_cb = _figs_cb = None
_console = None
_main = sys.modules["__main__"]
_ret = None
# The results of the calls to Java per thread (_call, _put with target 1)
_rets = {}
from _thread import get_ident as _get_ident
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


# ---------------------------------------------------------------- progress (jobs of Bio7)

_progress_sent = {}


def _progress(key, done, total=None, text="", force=False):
    """The progress of a task as a job of Bio7 (at most about 10 updates per second)."""
    import time
    now = time.monotonic()
    last = _progress_sent.get(key)
    finished = total is not None and total > 0 and done >= total
    if not force and not finished and last is not None and now - last < 0.1:
        return
    _progress_sent[key] = now
    try:
        _call("progress", key, repr(float(done)), "" if not total else repr(float(total)), text or "")
    except Exception:
        pass
    if finished:
        _progress_sent.pop(key, None)


def _progress_end(key):
    _progress_sent.pop(key, None)
    try:
        _call("progress_end", key)
    except Exception:
        pass


def _patch_tqdm(m):
    """tqdm bars (also of libraries) report to a job of Bio7; the console output stays."""
    cls = m.tqdm
    display, close = cls.display, cls.close

    def bio7_display(self, *args, **kwargs):
        try:
            _progress("tqdm%d" % id(self), self.n, self.total, self.desc or "tqdm")
        except Exception:
            pass
        return display(self, *args, **kwargs)

    def bio7_close(self, *args, **kwargs):
        try:
            _progress_end("tqdm%d" % id(self))
        except Exception:
            pass
        return close(self, *args, **kwargs)

    cls.display = bio7_display
    cls.close = bio7_close


_PATCHES = {"matplotlib": lambda m: _apply_mpl_style(m), "tqdm.std": _patch_tqdm, "plotly.basedatatypes": _patch_plotly, "altair": _patch_altair, "folium": _patch_folium,
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


# ---------------------------------------------------------------- Java objects (bio7.java)

_java_released = []
_java_members = {}


def _jcall(function, *args, value=None):
    """A call of the Java API; the handles of released proxies are sent first."""
    if _java_released:
        ids = list(_java_released)
        del _java_released[:]
        try:
            _call("java_release", value=ids)
        except Exception:
            pass
    return _jwrap(_call(function, *args, value=value))


def _jwrap(v):
    """Java handles (dicts with __java__) as proxies, also in lists and dicts."""
    if isinstance(v, dict):
        if "__java__" in v:
            return _JavaObject(int(v["__java__"]), v.get("class", "java.lang.Object"))
        return {k: _jwrap(e) for k, e in v.items()}
    if isinstance(v, list):
        return [_jwrap(e) for e in v]
    return v


def _jargs(args):
    # By position: a list of Python numbers would be sent as one array
    return {"args": {str(i): a for i, a in enumerate(args)}}


def _jmembers(cls):
    m = _java_members.get(cls)
    if m is None:
        d = _call("java_members", cls)
        m = {k: set(d[k]) if isinstance(d[k], list) else ({d[k]} if isinstance(d[k], str) else d[k]) for k in d}
        _java_members[cls] = m
    return m


class _JavaMethod:
    """A method of a Java class or object (overloads are chosen by the arguments)."""
    __slots__ = ("_target", "_cls", "_name", "_ui")

    def __init__(self, target, cls, name, ui=False):
        self._target, self._cls, self._name, self._ui = target, cls, name, ui

    def __call__(self, *args):
        value = _jargs(args)
        value["target"] = self._target
        return _jcall("java_call", self._cls, self._name, "ui" if self._ui else "", value=value)

    def __repr__(self):
        return "<Java method %s.%s>" % (self._cls, self._name)


class _JavaClass:
    """A Java class: its static methods and fields, the constructor (call it)."""
    __slots__ = ("_name", "__weakref__")

    def __init__(self, name):
        object.__setattr__(self, "_name", name)

    def __call__(self, *args):
        return _jcall("java_new", self._name, value=_jargs(args))

    def __getattr__(self, name):
        if name.startswith("__"):
            raise AttributeError(name)
        if name == "class_":
            return _jcall("java_call", "java.lang.Class", "forName", value=dict(_jargs([self._name]), target=None))
        m = _jmembers(self._name)
        if name in m["static_methods"]:
            return _JavaMethod(None, self._name, name)
        if name in m["static_fields"]:
            return _jcall("java_get", self._name, name, value=None)
        if name in m["classes"]:
            return _JavaClass(self._name + "$" + name)
        raise AttributeError("The Java class %s has no static method, field or class '%s'" % (self._name, name))

    def __setattr__(self, name, value):
        _jcall("java_set", self._name, name, value={"target": None, "value": value})

    def __dir__(self):
        m = _jmembers(self._name)
        return sorted(m["static_methods"] | m["static_fields"] | m["classes"])

    def __repr__(self):
        return "<Java class %s>" % self._name


class _JavaObject:
    """A Java object: its methods and fields as attributes (released when the proxy is deleted)."""
    __slots__ = ("_id", "_cls", "__weakref__")

    def __init__(self, id_, cls):
        object.__setattr__(self, "_id", id_)
        object.__setattr__(self, "_cls", cls)

    def __del__(self):
        try:
            _java_released.append(self._id)
        except Exception:
            pass

    def __getattr__(self, name):
        if name.startswith("__"):
            raise AttributeError(name)
        m = _jmembers(self._cls)
        if name in m["methods"]:
            return _JavaMethod(self, self._cls, name)
        if name in m["fields"]:
            return _jcall("java_get", self._cls, name, value=self)
        raise AttributeError("The Java object %s has no method or field '%s'" % (self._cls, name))

    def __setattr__(self, name, value):
        _jcall("java_set", self._cls, name, value={"target": self, "value": value})

    def __dir__(self):
        m = _jmembers(self._cls)
        return sorted(m["methods"] | m["fields"])

    def __str__(self):
        return _jcall("java_call", "java.lang.Object", "toString", value=dict(_jargs([]), target=self))

    def __repr__(self):
        text = str(self)
        return "<Java %s: %s>" % (self._cls, text if len(text) < 200 else text[:200] + "...")

    def __eq__(self, other):
        if not isinstance(other, _JavaObject):
            return NotImplemented
        return other._id == self._id or bool(
            _jcall("java_call", "java.lang.Object", "equals", value=dict(_jargs([other]), target=self)))

    def __hash__(self):
        return _jcall("java_call", "java.lang.Object", "hashCode", value=dict(_jargs([]), target=self))

    def __iter__(self):
        """An Iterable (List, Set, ...), a Map (its keys), an Iterator or an Enumeration."""
        return iter(_jcall("java_iter", value=self))

    def __bool__(self):
        # A Java object is true (not its size, see __len__)
        return True

    def __len__(self):
        m = _jmembers(self._cls)
        for name in ("size", "getSize", "length", "getCount"):
            if name in m["methods"]:
                return int(getattr(self, name)())
        raise TypeError("The Java object %s has no size" % self._cls)

    def __getitem__(self, key):
        m = _jmembers(self._cls)
        if "get" in m["methods"]:
            return self.get(key)
        raise TypeError("The Java object %s has no get()" % self._cls)


class _JavaUi:
    """The methods of a Java object (or class) called in the UI thread (SWT widgets)."""
    __slots__ = ("_obj",)

    def __init__(self, obj):
        self._obj = obj

    def __getattr__(self, name):
        if isinstance(self._obj, _JavaClass):
            return _JavaMethod(None, self._obj._name, name, ui=True)
        return _JavaMethod(self._obj, self._obj._cls, name, ui=True)

    def __call__(self, *args):
        """A new object of the class in the UI thread (e.g., a window or an SWT widget)."""
        if not isinstance(self._obj, _JavaClass):
            raise TypeError("java.ui(...)() needs a Java class")
        return _jcall("java_new", self._obj._name, "", "ui", value=_jargs(args))


class _JavaPackage:
    """A Java package: java.ij.IJ is the class ij.IJ, java.ij.gui a package."""
    __slots__ = ("_name",)

    def __init__(self, name):
        self._name = name

    def __getattr__(self, name):
        if name.startswith("__"):
            raise AttributeError(name)
        full = self._name + "." + name if self._name else name
        try:
            return _JavaClass(_call("java_type", full))
        except Exception:
            return _JavaPackage(full)

    def __repr__(self):
        return "<Java package %s>" % self._name


class _Java(_JavaPackage):
    """The Java API of Bio7 (ImageJ, Bio7, Java):

        from bio7 import java
        IJ = java.type("ij.IJ")             # a class (also java.ij.IJ)
        imp = IJ.getImage()                 # static methods, a proxy of the object
        imp.getProcessor().smooth()         # methods of objects
        roi = java.new("ij.gui.OvalRoi", 10, 10, 50, 50)   # constructors
        pixels = imp.getProcessor().getPixels()             # arrays as numpy
        java.ui(shell).setText("Title")     # in the UI thread (SWT widgets)
        java.listen(button, "Selection", lambda event: print(event["text"]))   # events of widgets
    """
    __slots__ = ()

    def __init__(self):
        _JavaPackage.__init__(self, "")

    def type(self, name):
        """The Java class (a proxy: static methods and fields, call it for a new object)."""
        return _JavaClass(_call("java_type", name))

    def new(self, name, *args):
        """A new Java object of a class (the constructor is chosen by the arguments)."""
        return self.type(name)(*args)

    def ui(self, obj):
        """The methods of a Java object or class called in the UI thread."""
        return _JavaUi(obj)

    def is_java(self, obj):
        """True for a Java object (proxy)."""
        return isinstance(obj, _JavaObject)

    def listen(self, widget, event, function):
        """Calls function(event) for an event of an SWT widget (e.g., "Selection" of a
        button, "Modify" of a text, "MouseDown", "KeyDown"). The event is a dict: type,
        widget, x, y, button, count, character, keyCode, stateMask, detail and the
        state of the widget when the event happened: text, selection (a check box, a
        slider, a spinner), index (a list, a combo). The function runs in a thread of
        Python (not in the UI thread: use java.ui(...) for the widgets); the events
        run one after the other. Returns the id of the listener (see unlisten)."""
        if not callable(function):
            raise TypeError("java.listen needs a function")
        lid = next(_java_listener_ids)
        _java_listeners[lid] = function
        try:
            _jcall("java_listen", "py", str(lid), event, value={"target": widget})
        except BaseException:
            _java_listeners.pop(lid, None)
            raise
        return lid

    def unlisten(self, listener):
        """Removes a listener (the id of listen)."""
        _java_listeners.pop(listener, None)
        _jcall("java_unlisten", "py", str(listener))

    def __repr__(self):
        return "<Java API of Bio7>"


java = _Java()

# The functions of the listeners of widgets (java.listen) by their ids
_java_listeners = {}
_java_listener_ids = __import__("itertools").count(1)


def _java_event(listener, event):
    """Calls the function of a listener with the event (called by Java in a thread of Python)."""
    function = _java_listeners.get(listener)
    data = _jcall("java_event", str(event))
    if function is None or data is None:
        return None
    try:
        function(data)
    except BaseException:
        __import__("traceback").print_exc()
    return None


def _file_url_path(url):
    """The path of a file URL, also of "file://" + a Windows path (e.g., bokeh show():
    file://C:\\Users\\...\\x.html, where urlparse takes the path as host)."""
    import re
    import urllib.parse
    import urllib.request
    rest = url[len("file:"):]
    m = re.match(r"^/*([A-Za-z]:[\\/].*)$", rest)
    if m and "\\" in m.group(1):
        # A Windows path, not URL-encoded
        return m.group(1)
    p = urllib.parse.urlparse(url)
    if p.netloc and re.match(r"^[A-Za-z]:$", p.netloc):
        # file://C:/Users/... (the drive as host)
        return urllib.request.url2pathname(p.netloc + p.path)
    return urllib.request.url2pathname(p.path)


def _register_browser():
    import webbrowser

    class Bio7Browser(webbrowser.BaseBrowser):
        def open(self, url, new=0, autoraise=True):
            try:
                if url.startswith("file:") or (os.path.isabs(url) and url.lower().endswith((".html", ".htm"))):
                    # a page of a file (e.g., bokeh show()): in the browser view or a markdown chunk
                    path = _file_url_path(url) if url.startswith("file:") else url
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


# ---------------------------------------------------------------- script runs
# A script runs in the workspace (__main__), like in Spyder:
# - the modules of the user (files in the folder of the script or of the
#   previous script, not the standard library, site-packages or compiled
#   extensions) are removed from sys.modules before the run: an edited module
#   is imported again (User Module Reloader; _reload_user_modules, preference)
# - only the folder of the current script is added to sys.path (the folder of
#   the previous script is removed: no shadowing between projects)
# - __file__ is the path of the script during the run, then the previous value
# - the working directory is the folder of the script during the run, then the
#   previous one (_script_chdir, preference; R shares it: the same process)
# - optionally all variables are removed before the run (_clear_before_run,
#   preference; off: flows use the variables of earlier steps)

_reload_user_modules = True
_script_chdir = True
_clear_before_run = False
_script_dir = None        # the folder of the last script (added to sys.path by Bio7)
_script_dir_added = False
_NO_VALUE = object()


def _is_user_module(module, folders):
    f = getattr(module, "__file__", None)
    if not f or not isinstance(f, str) or not f.endswith((".py", ".pyc")):
        return False   # built-in, namespace package or compiled extension
    f = os.path.normcase(os.path.abspath(f))
    if any(part in ("site-packages", "dist-packages") for part in f.split(os.sep)):
        return False
    for prefix in {sys.prefix, sys.base_prefix, getattr(sys, "exec_prefix", sys.prefix)}:
        lib = os.path.normcase(os.path.abspath(os.path.join(prefix, "lib")))
        if f.startswith(lib + os.sep):
            return False
    return any(f.startswith(d + os.sep) for d in folders)


def _script_begin(path):
    """Prepares a script run (see above); returns the state for _script_end."""
    global _script_dir, _script_dir_added
    folder = os.path.dirname(os.path.abspath(path))
    key = os.path.normcase(folder)
    # The modules of the user: removed, imported again by the script
    if _reload_user_modules:
        folders = {key}
        if _script_dir:
            folders.add(os.path.normcase(_script_dir))
        names = [n for n, m in list(sys.modules.items())
                 if n not in ("__main__", "bio7", "_bio7") and m is not None and _is_user_module(m, folders)]
        for n in names:
            sys.modules.pop(n, None)
        if names:
            sys.stdout.write("Reloaded modules: " + ", ".join(sorted(names)) + "\n")
    # New or changed files of modules are found (the caches of the import finders)
    import importlib
    importlib.invalidate_caches()
    # sys.path: only the folder of the current script
    if _script_dir and os.path.normcase(_script_dir) != key and _script_dir_added:
        try:
            sys.path.remove(_script_dir)
        except ValueError:
            pass
        _script_dir_added = False
    if folder not in sys.path:
        sys.path.insert(0, folder)
        _script_dir_added = True
    elif os.path.normcase(_script_dir or "") != key:
        _script_dir_added = False   # already in sys.path (e.g., the preferences): not removed later
    _script_dir = folder
    ns = _main.__dict__
    # A clean run: the variables of the user are removed (not the names __x__ and bio7)
    if _clear_before_run:
        for k in [k for k in ns if not (k.startswith("__") and k.endswith("__")) and k != "bio7"]:
            del ns[k]
        sys.stdout.write("Removed all variables (clean run)\n")
    old_file = ns.get("__file__", _NO_VALUE)
    ns["__file__"] = path
    old_cwd = None
    if _script_chdir:
        try:
            old_cwd = os.getcwd()
        except OSError:
            old_cwd = None
        try:
            os.chdir(folder)
        except OSError:
            old_cwd = None
    return (old_file, old_cwd)


def _script_end(state):
    """Restores __file__ and the working directory after a script run."""
    old_file, old_cwd = state
    ns = _main.__dict__
    if old_file is _NO_VALUE:
        ns.pop("__file__", None)
    else:
        ns["__file__"] = old_file
    if old_cwd is not None:
        try:
            os.chdir(old_cwd)
        except OSError:
            pass


def _run_file(path):
    with open(path, "rb") as f:
        source = f.read()
    state = _script_begin(path)
    try:
        exec(compile(source, path, "exec"), _main.__dict__)
    finally:
        _script_end(state)
        _flush()


def _prompt(more=False):
    sys.stdout.write("... " if more else ">>> ")


def _open_brackets(source):
    """True if the source ends in an open bracket or string (not in a block)."""
    import io
    import tokenize
    try:
        for _ in tokenize.generate_tokens(io.StringIO(source + "\n").readline):
            pass
    except (tokenize.TokenError, SyntaxError):
        return True
    return False


# The indentation removed from the lines of the block which is entered (line by line)
_console_dedent = ""


def _console_lines(text):
    """Evaluates lines in the console as if they were typed (the line or the
    selection of the editor): each line after the prompt, the results as in
    the console. The indentation of the code in the editor is removed (e.g., a
    line of the body of a loop); while a block is entered line by line, its
    lines keep their indentation relative to the first line of the block. A
    selection of several lines which ends in a block is closed with an empty
    line, else the next statement which is not indented (or an empty line)
    closes the block."""
    global _console_dedent
    import textwrap
    lines = text.splitlines() or [""]
    more = bool(_console.buffer)
    if more:
        # Continued: the same indentation as the start of the block is removed
        lines = [ln[len(_console_dedent):] if ln.startswith(_console_dedent) else ln.lstrip() for ln in lines]
    else:
        # The common indentation of the lines (kept for the next lines of a block)
        code = [ln for ln in lines if ln.strip()]
        first = code[0] if code else ""
        dedented = textwrap.dedent("\n".join(code)).split("\n")[0] if code else ""
        _console_dedent = first[:len(first) - len(dedented)]
        lines = [ln[len(_console_dedent):] if ln.startswith(_console_dedent) else ln.lstrip() for ln in lines]
    if len(lines) > 1 and lines[-1][:1] in (" ", "\t"):
        lines.append("")
    for i, line in enumerate(lines):
        # A statement after a block (not indented): the block is closed first (an empty line, as typed)
        if more and line and not line[0].isspace() and not line.lstrip().startswith(("else", "elif", "except", "finally", ")", "]", "}")) \
                and not _open_brackets("\n".join(_console.buffer)):
            sys.stdout.write("\n")
            more = _console.push("")
            _prompt(more)
        sys.stdout.write(line + "\n")
        more = _console.push(line)
        if i < len(lines) - 1:
            _prompt(more)
    if not more:
        _flush()
    _prompt(more)


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
    _numpy()
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
        if isinstance(x, _JavaObject):
            return {"t": "dict", "v": {"__java__": enc(x._id)}}
        if isinstance(x, _JavaClass):
            return {"t": "dict", "v": {"__javaclass__": enc(x._name)}}
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
        if _numpy() is not None:
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
            _rets[_get_ident()] = value
        else:
            _main.__dict__[name] = value
    except BaseException as e:
        if target == 1:
            _rets[_get_ident()] = e
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
    # The result of this thread: Java delivers it in the calling thread (_put); the
    # GIL is released during the call, other threads may call at the same time
    result = _rets.pop(_get_ident(), None)
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


# The preferences of the plot view (Preferences Python Plot): fit (the figures in
# the size of the view, else their figsize), hidpi (the resolution of the screen,
# else 1x), style (a matplotlib style, "" the default), font_size (0 the default)
_plot_options = {"fit": True, "hidpi": True, "style": "", "font_size": 0}
_style_applied = [False]


def _set_plot_options(options):
    """New preferences: the style for new figures, the open figures drawn again."""
    _plot_options.update(options)
    if "matplotlib" in sys.modules:
        _apply_mpl_style()
    if "matplotlib.pyplot" in sys.modules:
        _rendered.clear()
        _flush()


def _apply_mpl_style(module=None):
    """The style and the font size of the preferences (for the figures created after)."""
    try:
        import matplotlib
        style = _plot_options.get("style") or ""
        if style:
            matplotlib.style.use(style)
            _style_applied[0] = True
        elif _style_applied[0]:
            # Back to the defaults of matplotlib (the backend is kept)
            matplotlib.style.use("default")
            _style_applied[0] = False
        size = float(_plot_options.get("font_size") or 0)
        if size > 0:
            matplotlib.rcParams["font.size"] = size
    except Exception as e:
        sys.stderr.write("The matplotlib style could not be set: %s\n" % e)


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
    if not _plot_options.get("hidpi", True):
        scale = 1.0
    # The size of the figure (figsize) before it is fitted to the view
    if not hasattr(fig, "_bio7_figsize"):
        fig._bio7_figsize = tuple(fig.get_size_inches())
    if _plot_options.get("fit", True):
        fig.set_size_inches(max(w, 50) / 96.0, max(h, 50) / 96.0, forward=False)
    else:
        fig.set_size_inches(*fig._bio7_figsize, forward=False)
    fig.set_dpi(96.0 * scale)
    m.canvas.draw()
    rgba = memoryview(m.canvas.buffer_rgba())
    if _numpy() is not None:
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
    bio7.update_image(frame, "Video")  # the pixels of an open image replaced (video, live view)
    bio7.show_html(fig)                # a plotly/altair/bokeh/folium chart in the browser view
    bio7.ij_macro('run("Blobs (25K)");')   # an ImageJ macro (also bio7.ij_run, bio7.ij_macro_file)
    imp = bio7.java.ij.IJ.getImage()   # the Java API (ImageJ, Bio7): proxies of Java objects
    bio7.progress(i, n, "text")        # the progress in a job of Bio7 (also bio7.task, tqdm)
    df = bio7.results_table()          # the ImageJ results table
    x, y = bio7.roi()                  # the coordinates of the current selection
    bio7.to_r("x", [1, 2, 3])          # assign in R
    m = bio7.r("summary(cars)")        # evaluate in R
    bio7.plot3d(bio7.scatter3d(x, y, z), title="3D")   # 3D plots in the 3D view (also surface3d,
                                       # line3d, wireframe3d, bars3d, contour3d, pie3d, plot3d_clear)
                                       # charts: histogram3d, linegraph3d, area3d, boxplot3d,
                                       # violin3d, radar3d, rose3d, treemap3d
    p = bio7.plot3d(bio7.histogram3d(v), zlim=(0, 80))    # dynamic: in each step
    bio7.plot3d(bio7.histogram3d(v2), plot=p)             # updated (camera kept)
    rec = bio7.plot3d_recorder(speed=20)                  # recorded as animation
    bio7.plot3d(bio7.histogram3d(v), record=rec, show=False)
    bio7.plot3d_export(rec, "animation.html")
"""
import _bio7


def image(title=None):
    """The current (or the named) ImageJ image: 2-d (height, width), a stack
    (slices, height, width), RGB (height, width, 3). The data type of the
    image: uint8 (8-bit, RGB), uint16 (16-bit), float32 (32-bit), float64
    (64-bit, the exact values)."""
    return _bio7._call("image", title or "")


def show_image(array, title="Python", display_range=None):
    """Shows a 2-d array (or a stack, RGB uint8 (h, w, 3)) as ImageJ image.

    The data type gives the image type: uint8 8-bit, uint16 16-bit, float64
    64-bit (the exact values), other types 32-bit (e.g., float32; for a
    32-bit image of float64 values: array.astype("float32")).

    display_range: the contrast, (min, max) or "auto" (the range of the values,
    e.g., for labels 1..k in uint8 which look black with the range 0..255);
    None: the default of ImageJ.
    """
    if display_range is None:
        dr = ()
    elif isinstance(display_range, str):
        dr = (display_range, "")
    else:
        dr = (repr(float(display_range[0])), repr(float(display_range[1])))
    nbytes = getattr(array, "nbytes", 0)
    shape = getattr(array, "shape", ())
    rgb_image = len(shape) == 3 and shape[-1] == 3 and str(getattr(array, "dtype", "")) == "uint8"
    if nbytes > _PART_BYTES and len(shape) >= 3 and not rgb_image:
        # A large stack in parts (a Java array holds at most 2 GB; the stack can be larger)
        frame = max(1, nbytes // shape[0])
        n = max(1, _PART_BYTES // frame)
        key = _bio7._call("stack_begin")
        try:
            for i in range(0, shape[0], n):
                _bio7._call("stack_add", key, value=array[i:i + n].copy())
        except BaseException:
            _bio7._call("stack_cancel", key)
            raise
        return _bio7._call("stack_show", key, title, *dr)
    if nbytes >= 2**31 - 1:
        raise ValueError("The image is too large for one ImageJ image (%.1f GB, at most 2 GB per image or slice)" % (nbytes / 2**30))
    return _bio7._call("show_image", title, *dr, value=array)


# A stack larger than this is transferred in parts (each part a Java array)
_PART_BYTES = 256 * 2**20


def update_image(array, title="Python", display_range=None):
    """Replaces the pixels of the open ImageJ image with the title (the same
    window, zoom and contrast): a video or a live view, frame by frame. Creates
    the image if it is not open (or the size or type changed).

        for frame in frames:
            bio7.update_image(frame, "Video")

    display_range: "auto" adapts the contrast to each frame.
    """
    if display_range == "auto":
        return _bio7._call("update_image", title, "auto", value=array)
    return _bio7._call("update_image", title, value=array)


java = _bio7.java
"""The Java API (ImageJ, Bio7, Java): from bio7 import java; IJ = java.ij.IJ; imp = IJ.getImage()"""


def progress(done, total=None, text=""):
    """Shows the progress of the running code in Bio7 (a job in the status bar and
    the Progress view, its stop button interrupts the code):

        for i, f in enumerate(files):
            bio7.progress(i + 1, len(files), "Processing " + f)

    The job ends when done reaches total (or when the code ends).
    """
    _bio7._progress("progress", done, total, text)


class task:
    """A task with progress in Bio7 (a job of its own), also for an iterable:

        with bio7.task("Segmentation", total=len(images)) as t:
            for img in images:
                ...
                t.update()             # one step (t.update(n, text) for more)

        for img in bio7.task("Segmentation", images):   # wraps an iterable
            ...
    """

    _count = 0

    def __init__(self, text, iterable=None, total=None):
        task._count += 1
        self.key = "task%d" % task._count
        self.text = text
        self.iterable = iterable
        if total is None and iterable is not None:
            try:
                total = len(iterable)
            except TypeError:
                total = None
        self.total = total
        self.n = 0
        _bio7._progress(self.key, 0, self.total, self.text, force=True)

    def update(self, n=1, text=None):
        self.n += n
        if text:
            self.text = text
        _bio7._progress(self.key, self.n, self.total, self.text)

    def close(self):
        _bio7._progress_end(self.key)

    def __enter__(self):
        return self

    def __exit__(self, *exc):
        self.close()
        return False

    def __iter__(self):
        try:
            for item in self.iterable:
                yield item
                self.update()
        finally:
            self.close()


def ij_macro(code, arg=""):
    """Runs an ImageJ macro (the macro language of ImageJ) and returns the value
    of its return statement (a string) or None. getArgument() in the macro is arg.
    An error of the macro raises a RuntimeError with the message of ImageJ.

        n = bio7.ij_macro('run("Blobs (25K)"); return "" + nImages;')
    """
    return _bio7._call("ij_macro", code, arg)


def ij_macro_file(path, arg=""):
    """Runs an ImageJ macro file (a path, or relative to the macros folder of
    ImageJ) and returns the value of its return statement or None."""
    return _bio7._call("ij_macro_file", str(path), arg)


def ij_run(command, options=""):
    """Runs an ImageJ command on the current image, as run() in a macro:

        bio7.ij_run("Gaussian Blur...", "sigma=2")
    """
    _bio7._call("ij_run", command, options)


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


# ---------------------------------------------------------------- 3D plots
# The 3D plots of the 3D view (OpenGL, com.eco.bio7.spatial.Plot3D): the layers
# with scatter3d(), surface3d(), ... shown with plot3d().

def _plot3d_class():
    return java.type("com.eco.bio7.spatial.Plot3D")


_NAMED_COLORS = {"black": 0x000000, "white": 0xFFFFFF, "red": 0xD62728, "green": 0x2CA02C, "blue": 0x1F77B4,
                 "orange": 0xFF7F0E, "purple": 0x9467BD, "brown": 0x8C564B, "pink": 0xE377C2, "gray": 0x7F7F7F,
                 "grey": 0x7F7F7F, "olive": 0xBCBD22, "cyan": 0x17BECF, "yellow": 0xFFD700}


def _rgb_int(color):
    """A color (0xRRGGBB, "#RRGGBB", a name, an (r, g, b) tuple of 0..1 or 0..255) as int."""
    import numbers
    if isinstance(color, numbers.Integral) and not isinstance(color, bool):
        return int(color)
    if isinstance(color, str):
        c = color.strip()
        if c.startswith("#") and len(c) == 7:
            return int(c[1:], 16)
        if c.lower() in _NAMED_COLORS:
            return _NAMED_COLORS[c.lower()]
        try:
            from matplotlib.colors import to_rgb
            color = to_rgb(c)
        except Exception:
            raise ValueError("unknown color: %r" % color) from None
    r, g, b = list(color)[:3]
    if max(r, g, b) <= 1:
        r, g, b = r * 255, g * 255, b * 255
    return (int(round(r)) << 16) | (int(round(g)) << 8) | int(round(b))


def _color_map(cmap):
    """"viridis", "rainbow", "diverging", "terrain" (of Bio7), a matplotlib colormap name or a list of colors."""
    if not isinstance(cmap, str):
        return [_rgb_int(c) for c in cmap]
    legend = java.type("com.eco.bio7.spatial.PlotLegend")
    key = cmap.upper()
    if key in ("VIRIDIS", "RAINBOW", "DIVERGING", "TERRAIN"):
        return [int(v) for v in getattr(legend, key)]
    try:
        import matplotlib
        m = matplotlib.colormaps[cmap]
        return [_rgb_int(m(i / 6)[:3]) for i in range(7)]
    except Exception:
        raise ValueError("unknown colormap: %r" % cmap) from None


def _style(layer, label, color, cmap, alpha):
    if label is not None:
        layer = layer.label(str(label))
    if color is not None:
        layer = layer.color(_rgb_int(color))
    if cmap is not None:
        layer = layer.colorMap(_color_map(cmap))
    if alpha is not None:
        layer = layer.alpha(float(alpha))
    return layer


def _vector(v):
    import numpy as np
    return np.asarray(v, dtype=float).ravel()


def _grid(x, y, z):
    """The grid z[i, j] at x[i], y[j]: an array (also (len(y), len(x)) from np.meshgrid, transposed)
    or a function f(X, Y) of the grid."""
    import numpy as np
    x, y = _vector(x), _vector(y)
    if callable(z):
        X, Y = np.meshgrid(x, y, indexing="ij")
        z = z(X, Y)
    z = np.asarray(z, dtype=float)
    if z.shape != (len(x), len(y)):
        if z.shape == (len(y), len(x)):
            z = z.T
        else:
            raise ValueError("z must have the shape (len(x), len(y)): z[i, j] at x[i], y[j]")
    return np.ascontiguousarray(z)


def _points(x, y, z):
    """x, y, z of points: three sequences or one array/DataFrame with 3 columns in x."""
    import numpy as np
    if y is None and z is None:
        a = np.asarray(x, dtype=float)
        return a[:, 0].copy(), a[:, 1].copy(), a[:, 2].copy()
    return _vector(x), _vector(y), _vector(z)


def scatter3d(x, y=None, z=None, label=None, color=None, size=None, values=None, cmap=None, alpha=None,
              drop_lines=False):
    """A scatter layer (spheres) of the points (x[i], y[i], z[i]) for plot3d(); x can be an
    array or DataFrame with 3 columns. size: the radius; values: colors the points with the
    colormap (and a color bar); drop_lines: lines down to the floor."""
    px, py, pz = _points(x, y, z)
    layer = _plot3d_class().scatter(px, py, pz)
    if size is not None:
        layer = layer.size(float(size))
    if values is not None:
        layer = layer.values(_vector(values))
    if drop_lines:
        layer = layer.dropLines(True)
    return _style(layer, label, color, cmap if cmap is not None or values is None else "viridis", alpha)


def line3d(x, y=None, z=None, label=None, color=None, alpha=None, filled=False):
    """A line layer through the points (x[i], y[i], z[i]) for plot3d(); filled: filled down
    to the floor (e.g. several series at different y)."""
    px, py, pz = _points(x, y, z)
    p = _plot3d_class()
    layer = p.filledLine(px, py, pz) if filled else p.line(px, py, pz)
    return _style(layer, label, color, None, alpha)


def surface3d(x, y, z, label=None, cmap="viridis", color=None, alpha=None, mesh=None):
    """A surface layer of the grid z[i, j] at x[i], y[j] for plot3d(): z an array or a
    function f(X, Y) of the grid. mesh: the grid lines on the surface."""
    layer = _plot3d_class().surface(_vector(x), _vector(y), _grid(x, y, z))
    if mesh is not None:
        layer = layer.mesh(bool(mesh))
    return _style(layer, label, color, cmap if color is None else None, alpha)


def wireframe3d(x, y, z, label=None, color=None, cmap=None, alpha=None, step=None):
    """A wireframe layer of the grid z[i, j] at x[i], y[j] for plot3d() (z an array or a
    function f(X, Y)); step: every step-th grid line."""
    layer = _plot3d_class().wireframe(_vector(x), _vector(y), _grid(x, y, z))
    if step is not None:
        layer = layer.step(int(step))
    return _style(layer, label, color, cmap, alpha)


def bars3d(x, y, z, label=None, color=None, cmap=None, alpha=None, width=None):
    """A bar layer for plot3d(): z a sequence (bars at the points (x[i], y[i])) or a grid
    z[i, j] at x[i], y[j] (e.g. a 2D histogram); width: the width of the bars (0..1)."""
    import numpy as np
    p = _plot3d_class()
    if not callable(z) and np.ndim(z) == 1 and len(z) == len(x):
        layer = p.bars(_vector(x), _vector(y), _vector(z))
    else:
        layer = p.bars(_vector(x), _vector(y), _grid(x, y, z))
    if width is not None:
        layer = layer.width(float(width))
    return _style(layer, label, color, cmap, alpha)


def contour3d(x, y, z, levels=None, on_floor=False, label=None, color=None, cmap="viridis", alpha=None):
    """The contour lines of the grid z[i, j] at x[i], y[j] for plot3d(); levels: the number
    of levels or the levels; on_floor: drawn on the floor of the box."""
    import numpy as np
    layer = _plot3d_class().contour(_vector(x), _vector(y), _grid(x, y, z))
    if levels is not None:
        layer = layer.levels(int(levels)) if np.ndim(levels) == 0 else layer.levels(_vector(levels))
    if on_floor:
        layer = layer.onFloor(True)
    return _style(layer, label, color, cmap if color is None else None, alpha)


def pie3d(values, labels=None, colors=None, explode=None, height=None):
    """A 3D pie chart layer for plot3d() (shown alone)."""
    v = _vector(values)
    if labels is None:
        labels = ["Value %d" % (i + 1) for i in range(len(v))]
    layer = _plot3d_class().pie(v, [str(s) for s in labels])
    if colors is not None:
        layer = layer.colors([_rgb_int(c) for c in colors])
    if explode is not None:
        layer = layer.explode(float(explode))
    if height is not None:
        layer = layer.height(float(height))
    return layer


def plot3d(*layers, title=None, xlabel=None, ylabel=None, zlabel=None, log="", xlim=None, ylim=None,
           zlim=None, aspect=None, legend=True, colorbar=None, font=None, html=None, show=True, plot=None,
           record=None):
    """Shows the layers (scatter3d(), surface3d(), histogram3d(), ...) as 3D plot in the 3D view.

        x = np.linspace(-3, 3, 50)
        bio7.plot3d(bio7.surface3d(x, x, lambda X, Y: np.sin(X) * np.cos(Y)), colorbar="z")

    log: the logarithmic axes (e.g. "z" or "xy"); xlim, ylim, zlim: the ranges (fixed
    ranges for a plot which is updated); aspect: the size of the box (x, y, z); colorbar:
    the title of the color bar; html: also exported as HTML page (WebGL). Returns the plot
    (Java).

    Dynamic plots: plot=the returned plot updates it with the new layers (in each step;
    the camera and the zoom are kept, the settings stay); record=a recorder of
    plot3d_recorder() adds the plot as frame (show=False: only recorded)."""
    import os
    if len(layers) == 1 and isinstance(layers[0], (list, tuple)):
        layers = layers[0]
    p = _plot3d_class()() if plot is None else plot.clearLayers()
    if title is not None:
        p = p.title(str(title))
    if xlabel is not None or ylabel is not None or zlabel is not None:
        p = p.axisTitles(xlabel or "X", ylabel or "Y", zlabel or "Z")
    if log:
        p = p.logAxes("x" in log, "y" in log, "z" in log)
    if xlim is not None:
        p = p.xRange(float(xlim[0]), float(xlim[1]))
    if ylim is not None:
        p = p.yRange(float(ylim[0]), float(ylim[1]))
    if zlim is not None:
        p = p.zRange(float(zlim[0]), float(zlim[1]))
    if aspect is not None:
        p = p.aspect(float(aspect[0]), float(aspect[1]), float(aspect[2]))
    if not legend:
        p = p.legend(False)
    if colorbar is not None:
        p = p.colorBarTitle(str(colorbar))
    if font is not None:
        p = p.font(str(font))
    for layer in layers:
        p = p.add(layer)
    if html is not None:
        p.exportHtml(os.path.abspath(html))
    if record is not None:
        record.frame(p)
    if show:
        if plot is None:
            p.show()
        else:
            p.update()
    return p


# ---- The charts (layers for plot3d(), also updated in each step)

def _groups(x, names=None):
    """Groups of values: one sequence, a list of sequences, a 2-d array (rows), a dict or
    a DataFrame (columns): (list of float arrays, names or None)."""
    import numpy as np
    if hasattr(x, "columns") and hasattr(x, "__getitem__"):
        cols = list(x.columns)
        return [np.asarray(x[c], dtype=float).ravel() for c in cols], names or [str(c) for c in cols]
    if isinstance(x, dict):
        return [np.asarray(v, dtype=float).ravel() for v in x.values()], names or [str(k) for k in x]
    if isinstance(x, np.ndarray) and x.ndim == 2:
        return [np.ascontiguousarray(r, dtype=float) for r in x], names
    x = list(x) if not isinstance(x, np.ndarray) else x
    if len(x) == 0 or np.ndim(x[0]) == 0:
        # One group (the groups can have different sizes: no np.ndim of the whole list)
        return [np.asarray(x, dtype=float).ravel()], names
    return [np.asarray(v, dtype=float).ravel() for v in x], names


def _chart_colors(layer, colors):
    return layer if colors is None else layer.colors([_rgb_int(c) for c in colors])


def _default_names(names, n, prefix):
    return [str(s) for s in names] if names is not None else ["%s %d" % (prefix, i + 1) for i in range(n)]


def histogram3d(x, names=None, bins=None, bin_width=None, density=False, colors=None, label=None, alpha=None):
    """A histogram layer for plot3d(): x the values or groups (a list, dict or DataFrame;
    the groups in the depth with the names on the y axis). bins: the number of bins or
    bin_width; density: densities instead of counts."""
    g, n = _groups(x, names)
    p = _plot3d_class()
    layer = p.histogram(g[0]) if len(g) == 1 else p.histogram(g, _default_names(n, len(g), "Group"))
    if bins is not None:
        layer = layer.bins(int(bins))
    if bin_width is not None:
        layer = layer.binWidth(float(bin_width))
    if density:
        layer = layer.density(True)
    return _style(_chart_colors(layer, colors), label, None, None, alpha)


def linegraph3d(x, series, names=None, colors=None, markers=True, marker_size=0.12, fill=False, label=None,
                alpha=None):
    """A line graph layer for plot3d(): the series over x (one sequence, a list, a 2-d
    array with a series in each row, a dict or a DataFrame), each series at its own
    depth. markers: points at the values; fill: filled down to the floor."""
    s, n = _groups(series, names)
    layer = _plot3d_class().lineGraph(_vector(x), s)
    if n is not None and len(n) == len(s):
        layer = layer.names([str(v) for v in n])
    layer = layer.markers(bool(markers), float(marker_size))
    if fill:
        layer = layer.fill(True)
    return _style(_chart_colors(layer, colors), label, None, None, alpha)


def area3d(x, series, names=None, colors=None, stacked=True, label=None, alpha=None):
    """An area chart layer for plot3d(): the series over x (as linegraph3d()), stacked (or
    each in its own depth: stacked=False)."""
    s, n = _groups(series, names)
    layer = _plot3d_class().area(_vector(x), s)
    if n is not None and len(n) == len(s):
        layer = layer.names([str(v) for v in n])
    if not stacked:
        layer = layer.stacked(False)
    return _style(_chart_colors(layer, colors), label, None, None, alpha)


def boxplot3d(groups, names=None, colors=None, width=None, outliers=True, label=None, alpha=None):
    """A box plot layer for plot3d(): the groups (a list, dict or DataFrame; the names on
    the x axis)."""
    g, n = _groups(groups, names)
    layer = _plot3d_class().boxplot(g, _default_names(n, len(g), "Group"))
    if width is not None:
        layer = layer.width(float(width))
    if not outliers:
        layer = layer.outliers(False)
    return _style(_chart_colors(layer, colors), label, None, None, alpha)


def violin3d(groups, names=None, colors=None, width=None, bandwidth=None, quartiles=True, label=None, alpha=None):
    """A violin plot layer for plot3d(): the kernel densities of the groups (as
    boxplot3d()); quartiles: the median and the quartiles."""
    g, n = _groups(groups, names)
    layer = _plot3d_class().violin(g, _default_names(n, len(g), "Group"))
    if width is not None:
        layer = layer.width(float(width))
    if bandwidth is not None:
        layer = layer.bandwidth(float(bandwidth))
    if not quartiles:
        layer = layer.quartiles(False)
    return _style(_chart_colors(layer, colors), label, None, None, alpha)


def radar3d(series, axes=None, names=None, colors=None, max=None, rings=None, label=None, alpha=None):
    """A radar chart layer for plot3d(): series[k][axis] (a 2-d array or list with a series
    in each row; a DataFrame: the rows are the series, the columns the axes)."""
    import numpy as np
    if hasattr(series, "columns"):
        axes = axes if axes is not None else [str(c) for c in series.columns]
        names = names if names is not None else [str(i) for i in series.index]
        series = series.to_numpy()
    m = np.ascontiguousarray(np.asarray(series, dtype=float))
    layer = _plot3d_class().radar(m, _default_names(axes, m.shape[1], "Axis"))
    if names is not None and len(names) == m.shape[0]:
        layer = layer.names([str(v) for v in names])
    if max is not None:
        layer = layer.max(float(max))
    if rings is not None:
        layer = layer.rings(int(rings))
    return _style(_chart_colors(layer, colors), label, None, None, alpha)


def rose3d(values, sectors=None, classes=None, colors=None, area_proportional=False, height=None, gap=None,
           label=None, alpha=None):
    """A rose chart layer for plot3d() (e.g. a wind rose): a value for each sector, or a
    2-d array values[sector][class] stacked from the center."""
    import numpy as np
    v = np.asarray(values, dtype=float)
    p = _plot3d_class()
    if v.ndim == 2:
        layer = p.rose(np.ascontiguousarray(v), _default_names(sectors, v.shape[0], "Sector"))
        if classes is not None:
            layer = layer.classes([str(c) for c in classes])
    else:
        layer = p.rose(v.ravel(), _default_names(sectors, v.size, "Sector"))
    if area_proportional:
        layer = layer.areaProportional(True)
    if height is not None or gap is not None:
        layer = layer.shape(1.0 if height is None else float(height), 0.0 if gap is None else float(gap))
    return _style(_chart_colors(layer, colors), label, None, None, alpha)


def treemap3d(values, labels=None, colors=None, heights=False, show_values=True, label=None, alpha=None):
    """A treemap layer for plot3d(): rectangles with the areas of the values (a sequence
    or a dict: the keys are the labels); heights: the values also as heights."""
    if isinstance(values, dict):
        labels = labels if labels is not None else [str(k) for k in values]
        values = list(values.values())
    v = _vector(values)
    layer = _plot3d_class().treemap(v, _default_names(labels, len(v), "Value"))
    if heights:
        layer = layer.heights(True)
    if not show_values:
        layer = layer.showValues(False)
    return _style(_chart_colors(layer, colors), label, None, None, alpha)


# ---- Recording (an animation of a plot, e.g. of each step of a simulation)

def plot3d_recorder(speed=10, loop=True, autoplay=True, max_frames=None):
    """A recorder of plots: plot3d(..., record=rec) adds a frame (also without the 3D view:
    show=False), plot3d_export(rec, file) writes the animation as HTML page (WebGL with
    Play/Pause, a timeline, the speed). speed: frames per second."""
    r = java.type("com.eco.bio7.spatial.PlotRecorder")().speed(float(speed)).loop(bool(loop)).autoplay(bool(autoplay))
    if max_frames is not None:
        r = r.maxFrames(int(max_frames))
    return r


def plot3d_export(recorder, file, blender=False, open=True):
    """Exports the frames of the recorder as HTML page (WebGL animation) and opens it in
    the browser view (open=True). blender: also as animation for Blender (a .glb file and
    a Python script beside the page). Returns the path."""
    import os
    import re
    import webbrowser
    path = os.path.abspath(file)
    recorder.export(path)
    if blender:
        recorder.exportBlender(re.sub(r"\.html?$", "", path, flags=re.IGNORECASE) + ".glb")
    if open:
        webbrowser.open(path)
    return path


def plot3d_clear():
    """Removes the 3D plot from the 3D view."""
    _plot3d_class().clear()
'''
