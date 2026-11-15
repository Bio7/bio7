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
    """The data for the table of Bio7 as rows of strings (mode: head, frame, matrix, rows, columns)."""
    v = _main.__dict__[name]
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
    if mode == "rows":
        return [[text(e)] for e in seq]
    return [[text(e) for e in seq]]
