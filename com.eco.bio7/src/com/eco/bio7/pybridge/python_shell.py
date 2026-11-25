# Copyright (c) 2007-2026 M. Austenfeld
# SPDX-License-Identifier: EPL-2.0
#
# The functions of the Python-Shell view of Bio7 (the module _bio7shell of the
# embedded Python): evaluation with the output as text, the objects of the
# workspace (__main__), descriptions, summaries, conversions and the data for
# the table of Bio7.

import sys
import io
import ast
import types
import traceback
import contextlib

_main = sys.modules["__main__"]
_HIDDEN_TYPES = (types.ModuleType,)


def _np():
    return sys.modules.get("numpy")


def _pd():
    return sys.modules.get("pandas")


def run(code):
    """Runs code like the console (the value of a last expression is printed); returns the output."""
    out = io.StringIO()
    with contextlib.redirect_stdout(out), contextlib.redirect_stderr(out):
        try:
            tree = ast.parse(code, "<shell>", "exec")
            last = None
            if tree.body and isinstance(tree.body[-1], ast.Expr):
                last = ast.Expression(tree.body.pop().value)
            exec(compile(tree, "<shell>", "exec"), _main.__dict__)
            if last is not None:
                value = eval(compile(last, "<shell>", "eval"), _main.__dict__)
                if value is not None:
                    _main.__dict__["_"] = value
                    print(repr(value))
        except SystemExit:
            print("exit() is not available in the embedded Python of Bio7")
        except BaseException as e:
            tb = e.__traceback__.tb_next if e.__traceback__ is not None else None
            traceback.print_exception(type(e), e, tb)
            print(sys.modules["_bio7"]._missing_hint(e), end="")
    try:
        sys.modules["_bio7"]._flush()
    except Exception:
        pass
    return out.getvalue()


def _kind(v):
    np, pd = _np(), _pd()
    if np is not None and isinstance(v, np.ndarray):
        return "array"
    if pd is not None and isinstance(v, pd.DataFrame):
        return "DataFrame"
    if pd is not None and isinstance(v, pd.Series):
        return "Series"
    if pd is not None and isinstance(v, pd.Categorical):
        return "Categorical"
    if isinstance(v, (list, tuple)):
        return "list"
    if isinstance(v, dict):
        return "dict"
    if callable(v):
        return "function"
    return "other"


def objects(kind="all"):
    """The objects of the workspace: [name, short description] (filtered by kind)."""
    result = []
    for name, v in sorted(_main.__dict__.items(), key=lambda kv: kv[0].lower()):
        if name.startswith("_") or isinstance(v, _HIDDEN_TYPES) or name in ("bio7",):
            continue
        k = _kind(v)
        if isinstance(v, type) and kind != "function":
            continue
        if kind != "all" and k != kind:
            continue
        result.append([name, _short(v)])
    return result


def _short(v):
    np, pd = _np(), _pd()
    t = type(v).__name__
    if np is not None and isinstance(v, np.ndarray):
        return "%s %s %s" % (t, v.dtype, "x".join(str(s) for s in v.shape))
    if pd is not None and isinstance(v, pd.DataFrame):
        return "DataFrame %dx%d" % v.shape
    if hasattr(v, "__len__") and not isinstance(v, str):
        try:
            return "%s [%d]" % (t, len(v))
        except Exception:
            pass
    if isinstance(v, (int, float, complex, bool, str)):
        r = repr(v)
        return "%s = %s" % (t, r if len(r) < 40 else r[:37] + "...")
    return t


def describe(name):
    """The description of an object (type, shape, the first values)."""
    v = _main.__dict__[name]
    np, pd = _np(), _pd()
    lines = ["Object description:", "", "name   = " + name, "type   = " + type(v).__module__ + "." + type(v).__qualname__]
    if np is not None and isinstance(v, np.ndarray):
        lines += ["dtype  = %s" % v.dtype, "shape  = %s" % (v.shape,), "size   = %d (%d bytes)" % (v.size, v.nbytes), "", np.array2string(v, threshold=200, edgeitems=4)]
    elif pd is not None and isinstance(v, pd.DataFrame):
        lines += ["shape  = %s" % (v.shape,), "", "dtypes:", v.dtypes.to_string(), "", v.head(10).to_string()]
    elif pd is not None and isinstance(v, pd.Series):
        lines += ["dtype  = %s" % v.dtype, "length = %d" % len(v), "", v.head(20).to_string()]
    elif hasattr(v, "__len__") and not isinstance(v, str):
        lines += ["length = %d" % len(v), "", _repr(v)]
    elif callable(v):
        import inspect
        try:
            lines += ["", name + str(inspect.signature(v))]
        except (TypeError, ValueError):
            pass
        doc = inspect.getdoc(v)
        if doc:
            lines += ["", doc]
    else:
        lines += ["", _repr(v)]
    return "\n".join(lines)


def _repr(v, limit=4000):
    r = repr(v)
    return r if len(r) <= limit else r[:limit] + " ..."


def show(name):
    """The object as text (as printed in the console)."""
    v = _main.__dict__[name]
    pd = _pd()
    if pd is not None and isinstance(v, (pd.DataFrame, pd.Series)):
        return v.to_string(max_rows=500)
    np = _np()
    if np is not None and isinstance(v, np.ndarray):
        return np.array2string(v, threshold=5000)
    return _repr(v, 20000)


def summary(name):
    """A summary: describe() of pandas, statistics of arrays, the lengths of lists."""
    v = _main.__dict__[name]
    np, pd = _np(), _pd()
    if pd is not None and isinstance(v, pd.DataFrame):
        return v.describe(include="all").to_string()
    if pd is not None and isinstance(v, pd.Series):
        return v.describe().to_string()
    if np is not None and isinstance(v, np.ndarray) and v.dtype.kind in "biuf" and v.size:
        a = v.astype("float64")
        return ("shape  %s\ncount  %d\nmean   %g\nstd    %g\nmin    %g\n25%%    %g\n50%%    %g\n75%%    %g\nmax    %g\nNaN    %d"
                % ((v.shape, a.size, np.nanmean(a), np.nanstd(a), np.nanmin(a)) + tuple(np.nanpercentile(a, [25, 50, 75])) + (np.nanmax(a), int(np.isnan(a).sum()))))
    if isinstance(v, (list, tuple)):
        kinds = {}
        for e in v:
            kinds[type(e).__name__] = kinds.get(type(e).__name__, 0) + 1
        return "length %d\ntypes  %s" % (len(v), ", ".join("%s: %d" % kv for kv in kinds.items()))
    if isinstance(v, dict):
        return "keys %d\n%s" % (len(v), "\n".join("%-20s %s" % (str(k), _short(e)) for k, e in list(v.items())[:200]))
    return _repr(v)


def doc(name):
    """The help of an object (pydoc)."""
    import pydoc
    try:
        obj = _main.__dict__[name] if name in _main.__dict__ else pydoc.locate(name)
        if obj is None:
            return "No documentation found for: " + name
        return pydoc.render_doc(obj, renderer=pydoc.plaintext)
    except Exception as e:
        return "No documentation for %s: %s" % (name, e)


def convert(name, to):
    """Converts an object in place: array, DataFrame, list, float, categorical, str."""
    v = _main.__dict__[name]
    np, pd = _np(), _pd()
    if to == "array":
        if np is None:
            raise RuntimeError("numpy is not installed")
        r = v.to_numpy() if pd is not None and isinstance(v, (pd.DataFrame, pd.Series)) else np.asarray(v)
    elif to == "DataFrame":
        if pd is None:
            import pandas as pd
        r = pd.DataFrame(v)
    elif to == "list":
        r = v.tolist() if hasattr(v, "tolist") else list(v)
    elif to == "float":
        r = np.asarray(v, dtype="float64") if np is not None else [float(e) for e in v]
    elif to == "categorical":
        if pd is None:
            import pandas as pd
        r = pd.Categorical(v.tolist() if hasattr(v, "tolist") else list(v))
    elif to == "str":
        r = v.astype(str) if hasattr(v, "astype") else [str(e) for e in v]
    else:
        raise ValueError(to)
    _main.__dict__[name] = r
    return _short(r)


def cells(name, mode):
    """The data for the table of Bio7 as rows of strings (mode: head, frame, matrix, rows, columns;
    auto: the column names of DataFrames and dicts, a vector as a column)."""
    v = _main.__dict__[name]
    auto = mode == "auto"
    if auto:
        mode = "head"
    np, pd = _np(), _pd()

    def text(e):
        if e is None:
            return ""
        if isinstance(e, float) and e != e:
            return "NA"
        return str(e)

    if pd is not None and isinstance(v, pd.Series):
        v = v.to_frame()
    if pd is not None and isinstance(v, pd.DataFrame):
        rows = [[text(e) for e in r] for r in v.itertuples(index=False, name=None)]
        if mode == "head":
            rows.insert(0, [str(c) for c in v.columns])
        return rows
    if isinstance(v, dict):
        keys = list(v)
        cols = [list(v[k]) if hasattr(v[k], "__len__") and not isinstance(v[k], str) else [v[k]] for k in keys]
        n = max((len(c) for c in cols), default=0)
        rows = [[text(c[i]) if i < len(c) else "" for c in cols] for i in range(n)]
        if mode == "head":
            rows.insert(0, [str(k) for k in keys])
        return rows
    seq = v.tolist() if hasattr(v, "tolist") else v
    if not isinstance(seq, (list, tuple)):
        seq = [seq]
    if seq and isinstance(seq[0], (list, tuple)):
        return [[text(e) for e in r] for r in seq]
    if mode == "rows" or auto:
        return [[text(e)] for e in seq]
    return [[text(e) for e in seq]]


def _cell_text(e):
    if e is None:
        return ""
    if isinstance(e, float) and e != e:
        return "NA"
    return str(e)


def _flat_strings(v):
    """The values of an object as strings (arrays, Series, DataFrames, nested lists flattened)."""
    np, pd = _np(), _pd()
    if v is None:
        return []
    if pd is not None and isinstance(v, (pd.Series, pd.Index)):
        v = v.tolist()
    elif pd is not None and isinstance(v, pd.DataFrame):
        v = v.to_numpy().ravel().tolist()
    elif np is not None and isinstance(v, np.ndarray):
        v = v.ravel().tolist()
    if isinstance(v, (str, bytes)) or not hasattr(v, "__iter__"):
        return [_cell_text(v)]
    if isinstance(v, dict):
        v = list(v.values())
    out = []
    for e in v:
        if isinstance(e, (list, tuple, dict)) or (np is not None and isinstance(e, np.ndarray)) or (pd is not None and isinstance(e, (pd.Series, pd.DataFrame))):
            out.extend(_flat_strings(e))
        else:
            out.append(_cell_text(e))
    return out


def _elements(v):
    """The elements of a list-like object with their names: a dict (the keys), a DataFrame
    (the columns), a 2-d array (the rows), a list or tuple (V1, V2, ...); None: no list."""
    np, pd = _np(), _pd()
    if isinstance(v, dict):
        return [(str(k), x) for k, x in v.items()]
    if pd is not None and isinstance(v, pd.DataFrame):
        return [(str(c), v[c]) for c in v.columns]
    if np is not None and isinstance(v, np.ndarray) and v.ndim >= 2:
        return [("V%d" % (i + 1), v[i]) for i in range(v.shape[0])]
    if isinstance(v, (list, tuple)):
        return [("V%d" % (i + 1), x) for i, x in enumerate(v)]
    return None


def cells_many(names, mode):
    """Several objects (columns: one column each, rows: one row each; names separated by
    line breaks) or the elements of a list (list_columns, list_rows) as rows of strings for
    the table, with the names as header (the first row or the first column)."""
    if mode.startswith("list"):
        v = _main.__dict__[names]
        items = _elements(v)
        if items is None:
            raise ValueError("%s is not a list, tuple, dict, DataFrame or 2-d array" % names)
    else:
        items = [(n, _main.__dict__[n]) for n in names.split("\n") if n]
    cols = [(n, _flat_strings(x)) for n, x in items]
    if mode.endswith("rows"):
        return [[n] + vals for n, vals in cols]
    length = max((len(vals) for _, vals in cols), default=0)
    rows = [[n for n, _ in cols]]
    rows += [[vals[i] if i < len(vals) else "" for _, vals in cols] for i in range(length)]
    return rows


def plot_variables():
    """The objects for the plot view: arrays, lists, Series, DataFrames and their columns (df['col'])."""
    out = []
    for n, v in sorted(_main.__dict__.items()):
        if n.startswith("_") or isinstance(v, _HIDDEN_TYPES) or (callable(v) and not hasattr(v, "shape")):
            continue
        if hasattr(v, "columns") and hasattr(v, "dtypes"):
            out.append(n)
            out.extend("%s[%r]" % (n, c) for c in v.columns)
        elif hasattr(v, "shape") or isinstance(v, (list, tuple)):
            out.append(n)
    return out


# ---------------- Code completion of the text field ----------------

_SEP = "\x1f"
_MAX = 400
_DOC_MAX = 120


def _doc_line(obj, name):
    """The signature and the first line of the documentation (short)."""
    import inspect
    text = ""
    try:
        if callable(obj):
            text = name + str(inspect.signature(obj))
    except Exception:
        pass
    try:
        d = inspect.getdoc(obj)
        if d and not isinstance(obj, (int, float, complex, str, bytes, bool)):
            first = d.strip().split("\n\n")[0].replace("\n", " ")
            text = (text + "\n\n" if text else "") + first[:400]
    except Exception:
        pass
    return text


def _kind_of(obj):
    if isinstance(obj, types.ModuleType):
        return "module"
    if isinstance(obj, type):
        return "class"
    if callable(obj):
        return "function"
    return "instance"


def _with_jedi(text, cursor):
    """The completions of jedi (if installed): (start, [(name, kind, doc)])."""
    try:
        import jedi
    except Exception:
        return None
    try:
        script = jedi.Interpreter(text[:cursor], [_main.__dict__])
        comps = script.complete()
    except Exception:
        return None
    if not comps:
        return cursor, []
    base = _object_before(text[:cursor])
    first = comps[0]
    start = cursor - (len(first.name) - len(first.complete))
    items = []
    for i, c in enumerate(comps[:_MAX]):
        doc = ""
        kind = c.type
        if kind in ("instance", "statement"):
            # A callable object (e.g., numpy.sin, a ufunc): with parentheses
            try:
                obj = getattr(base, c.name) if base is not None else _main.__dict__.get(c.name)
                if callable(obj) and not isinstance(obj, type):
                    kind = "function"
            except Exception:
                pass
        if i < _DOC_MAX:
            try:
                sigs = c.get_signatures()
                doc = sigs[0].to_string() if sigs else ""
                d = c.docstring(raw=True)
                if d:
                    doc = (doc + "\n\n" if doc else "") + d.strip().split("\n\n")[0].replace("\n", " ")[:400]
            except Exception:
                pass
        items.append((c.name, kind, doc))
    return start, items


def _object_before(line):
    """The object before ".name" at the end (only names, no calls), else None."""
    import re
    import builtins
    m = re.search(r"([A-Za-z_][\w]*(?:\.[A-Za-z_]\w*)*)\.\w*$", line)
    if not m:
        return None
    parts = m.group(1).split(".")
    try:
        ns = _main.__dict__
        obj = ns[parts[0]] if parts[0] in ns else getattr(builtins, parts[0])
        for p in parts[1:]:
            obj = getattr(obj, p)
        return obj
    except Exception:
        return None


def _modules(prefix):
    """Top-level modules or the submodules of a package (import statements)."""
    import pkgutil
    import importlib
    if "." in prefix:
        package, _, rest = prefix.rpartition(".")
        try:
            path = importlib.import_module(package).__path__
        except Exception:
            return []
        names = [m.name for m in pkgutil.iter_modules(path)]
    else:
        rest = prefix
        names = list(sys.builtin_module_names) + [m.name for m in pkgutil.iter_modules()]
    return sorted({n for n in names if n.startswith(rest) and not n.startswith("_")})


def _simple(text, cursor):
    """Completions without jedi: names, attributes of objects (no calls), keywords and modules."""
    import re
    import keyword
    import builtins
    line = text[:cursor]
    m = re.search(r"^\s*(?:import|from)\s+([\w.]*)$", line)
    if m:
        token = m.group(1)
        start = cursor - len(token.rpartition(".")[2])
        return start, [(n, "module", "") for n in _modules(token)]
    m = re.search(r"^\s*from\s+([\w.]+)\s+import\s+(?:.*,\s*)?(\w*)$", line)
    if m:
        prefix = m.group(2)
        try:
            import importlib
            mod = importlib.import_module(m.group(1))
            names = [n for n in dir(mod) if n.startswith(prefix) and not n.startswith("_")]
            return cursor - len(prefix), [(n, _kind_of(getattr(mod, n, None)), "") for n in sorted(names)]
        except Exception:
            return cursor - len(prefix), []
    m = re.search(r"([A-Za-z_][\w]*(?:\.[A-Za-z_]\w*)*)\.(\w*)$", line)
    if m:
        expr, prefix = m.group(1), m.group(2)
        parts = expr.split(".")
        try:
            ns = _main.__dict__
            obj = ns[parts[0]] if parts[0] in ns else getattr(builtins, parts[0])
            for p in parts[1:]:
                obj = getattr(obj, p)
        except Exception:
            return cursor - len(prefix), []
        names = sorted(set(dir(obj)), key=lambda n: (n.startswith("_"), n.lower()))
        if not prefix.startswith("_"):
            names = [n for n in names if not n.startswith("_")]
        items = []
        for n in names:
            if not n.startswith(prefix):
                continue
            try:
                a = getattr(obj, n)
            except Exception:
                a = None
            items.append((n, _kind_of(a), a))
        return cursor - len(prefix), items
    m = re.search(r"(\w*)$", line)
    prefix = m.group(1)
    found = {}
    for n in keyword.kwlist:
        if n.startswith(prefix):
            found[n] = ("keyword", None)
    for n, v in _main.__dict__.items():
        if n.startswith(prefix) and (prefix.startswith("_") or not n.startswith("_")):
            found[n] = (_kind_of(v), v)
    for n in dir(builtins):
        if n.startswith(prefix) and not n.startswith("_") and n not in found:
            found[n] = (_kind_of(getattr(builtins, n)), getattr(builtins, n))
    items = [(n, k, v) for n, (k, v) in sorted(found.items(), key=lambda kv: kv[0].lower())]
    return cursor - len(prefix), items


def complete(text, cursor):
    """The completions at the cursor: [start, "name<US>kind<US>doc", ...] (jedi if installed)."""
    cursor = int(cursor)
    result = _with_jedi(text, cursor)
    if result is None:
        start, raw = _simple(text, cursor)
        items = []
        for i, (n, k, obj) in enumerate(raw[:_MAX]):
            items.append((n, k, _doc_line(obj, n) if obj is not None and i < _DOC_MAX else ""))
    else:
        start, items = result
    items = _workspace_first(text, cursor, start, items)
    return [str(start)] + [n + _SEP + k + _SEP + d.replace(_SEP, " ") for n, k, d in items]


def _workspace_first(text, cursor, start, items):
    """A name (not an attribute, not an import): the objects of the workspace first
    (data as "variable" with a short description, e.g. "ndarray int64 5")."""
    import re
    before = text[:start]
    if before.rstrip().endswith(".") or re.search(r"^\s*(?:import|from)\b", before) or re.search(r"\bimport\s+(?:.*,\s*)?$", before):
        return items
    prefix = text[start:cursor]
    ws = []
    for n, v in sorted(_main.__dict__.items(), key=lambda kv: kv[0].lower()):
        if not n.startswith(prefix) or (n.startswith("_") and not prefix.startswith("_")) or n == "bio7":
            continue
        k = _kind_of(v)
        if k == "instance":
            ws.append((n, "variable", _short(v)))
        else:
            ws.append((n, k, _doc_line(v, n) if k != "module" else (n + " (module)")))
    names = {n for n, _, _ in ws}
    return ws + [it for it in items if it[0] not in names]


# ---------------------------------------------------------------- actions
# The actions of the view (load, save, plot, ...) run here in their own scope:
# only their result (e.g., a loaded variable) is written to the workspace, no
# helper names (the workspace of the user stays as it is).

def _action(fn, *args):
    """Runs an action with its output (and errors) as text, like run()."""
    out = io.StringIO()
    with contextlib.redirect_stdout(out), contextlib.redirect_stderr(out):
        try:
            fn(*args)
        except SystemExit:
            print("exit() is not available in the embedded Python of Bio7")
        except BaseException as e:
            tb = e.__traceback__.tb_next if e.__traceback__ is not None else None
            traceback.print_exception(type(e), e, tb)
            print(sys.modules["_bio7"]._missing_hint(e), end="")
    try:
        sys.modules["_bio7"]._flush()
    except Exception:
        pass
    return out.getvalue()


def collect():
    """The garbage collection (the action Gc)."""
    def do():
        import gc
        print("Collected objects:", gc.collect())
    return _action(do)


def load_file(path, name):
    """Loads a file as the variable name (.npy, .npz, .xlsx, .pkl, else a CSV/TSV/text table)."""
    def do():
        low = path.lower()
        if low.endswith(".npy"):
            import numpy as np
            v = np.load(path)
            info = repr(v.shape)
        elif low.endswith(".npz"):
            import numpy as np
            v = dict(np.load(path))
            info = repr(list(v))
        elif low.endswith(".xlsx"):
            import pandas as pd
            v = pd.read_excel(path)
            info = repr(v.shape)
        elif low.endswith(".pkl"):
            import pickle
            with open(path, "rb") as f:
                v = pickle.load(f)
            info = repr(type(v).__name__)
        else:
            import pandas as pd
            v = pd.read_csv(path, sep=None, engine="python")
            info = repr(v.shape)
        _main.__dict__[name] = v
        print(info)
    return _action(do)


def save_object(name, path):
    """Saves a variable (.csv, .xlsx as table, .pkl, else .npy)."""
    def do():
        v = _main.__dict__[name]
        low = path.lower()
        if low.endswith((".csv", ".xlsx")):
            import pandas as pd
            df = v if isinstance(v, pd.DataFrame) else pd.DataFrame(v)
            if low.endswith(".csv"):
                df.to_csv(path, index=False)
            else:
                df.to_excel(path, index=False)
        elif low.endswith(".pkl"):
            import pickle
            with open(path, "wb") as f:
                pickle.dump(v, f)
        else:
            import numpy as np
            np.save(path, np.asarray(v))
        print("Saved:", path)
    return _action(do)


def save_objects(path):
    """Save All Objects: the objects of the workspace which can be pickled (not
    modules, functions, classes and names beginning with _) as a dict
    {name: object}; the others are listed."""
    def do():
        import pickle
        objects, skipped = {}, []
        for k, v in list(_main.__dict__.items()):
            if k.startswith("_") or isinstance(v, (types.ModuleType, types.FunctionType, types.BuiltinFunctionType, type)):
                continue
            try:
                pickle.dumps(v, protocol=pickle.HIGHEST_PROTOCOL)
                objects[k] = v
            except Exception:
                skipped.append(k)
        with open(path, "wb") as f:
            pickle.dump(objects, f, protocol=pickle.HIGHEST_PROTOCOL)
        print("Saved %d objects:" % len(objects), path)
        if skipped:
            print("Not saved (cannot be pickled):", ", ".join(skipped))
    return _action(do)


def load_objects(path):
    """Load Objects: adds the objects of Save All Objects to the workspace
    (existing names are replaced)."""
    def do():
        import pickle
        with open(path, "rb") as f:
            objects = pickle.load(f)
        if not isinstance(objects, dict) or not all(isinstance(k, str) for k in objects):
            raise TypeError("The file contains no objects of Save All Objects (a dict {name: object})")
        _main.__dict__.update(objects)
        print("Loaded %d objects: %s" % (len(objects), ", ".join(sorted(objects))))
    return _action(do)


def quick_plot(name, kind):
    """A quick plot of a variable: hist, image or line (or the plot() of the object)."""
    def do():
        import matplotlib.pyplot as plt
        v = _main.__dict__[name]
        if kind == "hist":
            import numpy as np
            a = np.asarray(v, dtype=float).ravel()
            plt.figure()
            plt.hist(a[~np.isnan(a)], bins=30)
        elif kind == "image":
            plt.figure()
            plt.imshow(v, cmap="gray")
            plt.colorbar()
        elif hasattr(v, "plot"):
            v.plot()
        else:
            plt.figure()
            plt.plot(v)
        plt.title(name)
        plt.show()
    return _action(do)


def run_private(code):
    """Runs code with the variables of the workspace, but its own names (e.g.,
    the helper function of Plot with Options) stay out of the workspace."""
    def do():
        ns = dict(_main.__dict__)
        exec(compile(code, "<shell>", "exec"), ns)
    return _action(do)
