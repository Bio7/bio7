# Copyright (c) 2007-2026 M. Austenfeld
# SPDX-License-Identifier: EPL-2.0
#
# The debugger of the embedded Python of Bio7 (the module _bio7dbg): runs a
# script with bdb in __main__. At a stop the state (file, line, call stack) is
# sent to Bio7 (bio7.call("debug_pause")), which returns the next command
# (step, next, return, continue, line N, quit). While Bio7 waits for the user,
# it evaluates the variables, watch expressions and console lines in the frame
# (the functions below), the tracing is off then.

import bdb
import json
import linecache
import os
import sys
import traceback

_main = sys.modules["__main__"]
_debugger = None


def _short(v, limit=200):
    try:
        r = repr(v)
    except Exception as e:
        r = "<repr failed: %s>" % e
    r = r.replace("\n", " ")
    return r if len(r) <= limit else r[:limit] + " ..."


def _size(v):
    try:
        shape = getattr(v, "shape", None)
        if shape is not None and not callable(shape):
            return "x".join(str(s) for s in shape)
        if hasattr(v, "__len__") and not isinstance(v, (str, bytes)):
            return str(len(v))
    except Exception:
        pass
    return ""


def _type(v):
    t = type(v).__name__
    dtype = getattr(v, "dtype", None)
    if dtype is not None and not callable(dtype):
        t += " " + str(dtype)
    return t


def _library_dirs():
    """The folders of the standard library and the installed packages (not the whole
    prefix of Python: it is /usr on Linux or the home folder for a Python installed
    there, where the scripts of the user are)."""
    import sysconfig
    dirs = set()
    for key in ("stdlib", "platstdlib", "purelib", "platlib"):
        for vars_ in (None, {"base": sys.base_prefix, "platbase": sys.base_exec_prefix}):
            try:
                p = sysconfig.get_path(key, vars=vars_) if vars_ else sysconfig.get_path(key)
            except Exception:
                p = None
            if p:
                dirs.add(os.path.normcase(os.path.abspath(p)) + os.sep)
    return tuple(dirs)


_LIBRARY = _library_dirs()


def _is_library(filename):
    """Code of the standard library or of installed packages (not stopped in while stepping)."""
    f = os.path.normcase(os.path.abspath(filename))
    return "site-packages" in f or "dist-packages" in f or f.startswith(_LIBRARY)


class Debugger(bdb.Bdb):
    def __init__(self, path):
        super().__init__(skip=["_bio7*", "bdb", "bio7"])
        self.path = os.path.abspath(path)
        self.frames = []   # the stack at the stop: (frame, line)
        self.current = 0   # the selected frame (index into frames, the stop is the last)
        self.stopped = False
        self.run_to_breakpoint = False

    def user_line(self, frame):
        if frame.f_code.co_filename.startswith("<") or _is_library(frame.f_code.co_filename):
            # code of Python or of packages: stepping goes on to the own code
            return
        if self.run_to_breakpoint:
            # the first stop of bdb.run: on to the first breakpoint
            self.run_to_breakpoint = False
            self.set_continue()
            return
        self._hit_count_reached(frame)
        if self._log_point(frame):
            return
        self._pause(frame, "line", self._condition_error(frame))

    def _log_point(self, frame):
        """A log point (breakpoint with a log expression): prints the value, doesn't stop.
        While stepping the debugger stops as at every line."""
        if os.path.abspath(frame.f_code.co_filename) != self.path or self.stop_here(frame):
            return False
        logs = [getattr(bp, "_bio7_log", None) for bp in self.get_breaks(self.path, frame.f_lineno)]
        logs = [x for x in logs if x]
        if not logs:
            return False
        for expr in logs:
            try:
                value = _short(eval(expr, frame.f_globals, frame.f_locals), 2000)
            except Exception as e:
                value = "<%s: %s>" % (type(e).__name__, e)
            sys.stdout.write("\u25b6 line %d: %s = %s\n" % (frame.f_lineno, expr, value))
        return True

    def _hit_count_reached(self, frame):
        """A breakpoint with a hit count stops once (at the n-th hit): then it is off for this run."""
        if os.path.abspath(frame.f_code.co_filename) != self.path:
            return
        for bp in self.get_breaks(self.path, frame.f_lineno):
            if getattr(bp, "_bio7_hits", 0) > 1 and bp.enabled and bp.ignore == 0:
                bp.enabled = False

    def _condition_error(self, frame):
        """The error of the condition of a breakpoint at the line (bdb stops then), else None."""
        if os.path.abspath(frame.f_code.co_filename) != self.path:
            return None
        for bp in self.get_breaks(self.path, frame.f_lineno):
            if bp.cond:
                try:
                    eval(bp.cond, frame.f_globals, frame.f_locals)
                except Exception as e:
                    return "The condition of the breakpoint (%s) failed: %s: %s" % (bp.cond, type(e).__name__, e)
        return None

    def user_return(self, frame, value):
        pass

    def user_exception(self, frame, exc_info):
        if frame.f_code.co_filename.startswith("<") or _is_library(frame.f_code.co_filename):
            return
        if self.stop_here(frame):
            self._pause(frame, "exception", "".join(traceback.format_exception_only(exc_info[0], exc_info[1])).strip())

    def _pause(self, frame, reason, message=None):
        stack, _ = self.get_stack(frame, None)
        self.frames = [(f, line) for f, line in stack if not f.f_code.co_filename.startswith("<") and "bdb.py" not in f.f_code.co_filename]
        if not self.frames:
            self.frames = [(frame, frame.f_lineno)]
        self.current = len(self.frames) - 1
        self.stopped = True
        state = {"file": frame.f_code.co_filename, "line": frame.f_lineno, "function": frame.f_code.co_name, "reason": reason, "message": message,
                 "stack": self.stack_rows()}
        # no tracing of the evaluations of Bio7 while waiting
        trace = sys.gettrace()
        sys.settrace(None)
        try:
            command = sys.modules["_bio7"]._call("debug_pause", json.dumps(state))
        except BaseException:
            command = "quit"
        finally:
            sys.settrace(trace)
            self.stopped = False
        self._command(str(command or "continue"), frame)

    def _command(self, command, frame):
        if command == "step":
            self.set_step()
        elif command == "next":
            self.set_next(frame)
        elif command == "return":
            self.set_return(frame)
        elif command.startswith("line "):
            line = int(command.split()[1])
            self.set_break(self.path, line, temporary=True)
            self.set_continue()
        elif command == "quit":
            self.set_quit()
        else:
            self.set_continue()

    def stack_rows(self):
        rows = []
        for i, (f, line) in enumerate(self.frames):
            name = f.f_code.co_name
            rows.append([i, ("<module>" if name == "<module>" else name + "()"), os.path.basename(f.f_code.co_filename), line, f.f_code.co_filename])
        return rows

    def frame(self):
        return self.frames[self.current][0] if self.frames else None


def run(path, breakpoints, stop_first):
    """Runs a script in __main__ with the debugger (breakpoints: a list of lines)."""
    global _debugger
    path = os.path.abspath(path)
    linecache.checkcache(path)
    with open(path, "rb") as f:
        code = compile(f.read(), path, "exec")
    folder = os.path.dirname(path)
    if folder not in sys.path:
        sys.path.insert(0, folder)
    d = Debugger(path)
    _debugger = d
    # the breakpoints of bdb are shared by all debuggers: only the ones of this run
    d.clear_all_breaks()
    _apply_breaks(d, breakpoints)
    ns = _main.__dict__
    ns["__file__"] = path
    try:
        # stops at the first line (without breakpoints or if asked), else at the first breakpoint
        d.run_to_breakpoint = bool(breakpoints) and not stop_first
        d.run(code, ns)
    except bdb.BdbQuit:
        sys.stderr.write("Debugging stopped\n")
    except BaseException:
        sys.stderr.write(traceback.format_exc())
    finally:
        ns.pop("__file__", None)
        d.clear_all_breaks()
        _debugger = None
        try:
            sys.modules["_bio7"]._call("debug_end")
        except BaseException:
            pass
        try:
            sys.modules["_bio7"]._flush()
        except Exception:
            pass


# ------------------------------------------------ the functions for Bio7 while stopped


def set_breaks(lines):
    """The breakpoints of the script (e.g., changed in the editor while debugging)."""
    if _debugger is None:
        return
    _apply_breaks(_debugger, lines)


def _spec(b):
    """A breakpoint: a line or [line, condition or None, hit count, log expression or None]."""
    if isinstance(b, (list, tuple)):
        line = int(b[0])
        cond = b[1] if len(b) > 1 and b[1] else None
        hits = int(b[2]) if len(b) > 2 and b[2] else 0
        log = b[3] if len(b) > 3 and b[3] else None
        return line, cond, hits, log
    return int(b), None, 0, None


def _apply_breaks(d, specs):
    """Sets the breakpoints of the script: unchanged breakpoints keep their hits
    (a hit count counts on), changed ones are new."""
    wanted = {}
    for b in specs:
        line, cond, hits, log = _spec(b)
        wanted[line] = (cond, hits, log)
    for bp in list(bdb.Breakpoint.bpbynumber):
        if bp is None or os.path.abspath(bp.file) != d.path:
            continue
        if wanted.get(bp.line) != (bp.cond, getattr(bp, "_bio7_hits", 0), getattr(bp, "_bio7_log", None)):
            d.clear_break(bp.file, bp.line)
    for line, (cond, hits, log) in wanted.items():
        if any(bp.cond == cond and getattr(bp, "_bio7_hits", 0) == hits and getattr(bp, "_bio7_log", None) == log
               for bp in d.get_breaks(d.path, line)):
            continue
        d.set_break(d.path, line, cond=cond)
        bp = d.get_breaks(d.path, line)[-1]
        bp._bio7_hits = hits
        bp._bio7_log = log
        # stops at the n-th hit: the first n - 1 are ignored
        bp.ignore = max(0, hits - 1)


def _frame():
    if _debugger is None or not _debugger.stopped:
        raise RuntimeError("The debugger is not stopped")
    return _debugger.frame()


def select(index):
    """Selects a frame of the call stack (its variables, the evaluations)."""
    if _debugger is not None and 0 <= index < len(_debugger.frames):
        _debugger.current = index
    f = _frame()
    return [f.f_code.co_filename, f.f_lineno]


def variables():
    """The variables of the selected frame: name, type, size, value."""
    f = _frame()
    names = f.f_locals
    rows = []
    for name, v in sorted(names.items(), key=lambda kv: kv[0].lower()):
        if name.startswith("__") or name.startswith("_bio7") or type(v).__name__ in ("module", "function", "builtin_function_or_method", "type"):
            continue
        rows.append([name, _type(v), _size(v), _short(v)])
    return rows


def evaluate(expression):
    """The value of an expression in the selected frame (repr) or the error."""
    f = _frame()
    try:
        return _short(eval(expression, f.f_globals, f.f_locals), 2000)
    except Exception as e:
        return "<" + type(e).__name__ + ": " + str(e) + ">"


def console(line):
    """A line of the console in the selected frame: the value of an expression or the statement."""
    f = _frame()
    try:
        try:
            code = compile(line, "<debug console>", "eval")
        except SyntaxError:
            exec(compile(line, "<debug console>", "exec"), f.f_globals, f.f_locals)
            return ""
        value = eval(code, f.f_globals, f.f_locals)
        return "" if value is None else repr(value)
    except Exception:
        return traceback.format_exc(limit=0).strip()


def to_main(name):
    """Copies a variable of the frame to __main__ (e.g., to open it in the table or ImageJ)."""
    f = _frame()
    _main.__dict__[name] = eval(name, f.f_globals, f.f_locals)
    return name


def to_main_as(target, expression):
    """Copies the value of an expression of the frame to __main__ under the name target."""
    f = _frame()
    _main.__dict__[target] = eval(expression, f.f_globals, f.f_locals)
    return target


def describe(expression, max_lines=30, limit=3000):
    """The hover of the editor: the type, the size and the value (several lines) of an
    expression in the selected frame; a NameError: an empty string (no hover)."""
    f = _frame()
    if f is None:
        return ""
    try:
        v = eval(expression, f.f_globals, f.f_locals)
    except NameError:
        return ""
    except Exception as e:
        return "<" + type(e).__name__ + ": " + str(e) + ">"
    if _is_code(v):
        return ""  # functions, classes, modules: the help of the editor (signature, documentation)
    head = _type(v)
    parts = head.split(" ")
    if len(parts) == 2 and parts[0] == parts[1]:
        head = parts[0]  # a numpy scalar (e.g., float64)
    size = _size(v)
    if size:
        head += "  [" + size + "]"
    text = _pretty(v, max_lines)
    lines = text.splitlines()
    if len(lines) > max_lines:
        lines = lines[:max_lines] + ["..."]
    text = "\n".join(lines)
    if len(text) > limit:
        text = text[:limit] + " ..."
    return head + "\n" + text


def _is_code(v):
    import types
    if isinstance(v, (types.FunctionType, types.BuiltinFunctionType, types.MethodType, types.BuiltinMethodType, types.ModuleType, type)):
        return True
    # other callables (e.g., numpy functions, partial): not data with a shape
    return callable(v) and not hasattr(v, "shape")


def _pretty(v, max_lines):
    try:
        module = type(v).__module__ or ""
        if module.startswith("pandas"):
            import pandas as pd
            with pd.option_context("display.max_rows", max_lines - 2, "display.min_rows", 10, "display.max_columns", 12, "display.width", 110):
                return repr(v)
        if module.startswith("numpy") and hasattr(v, "shape"):
            import numpy as np
            return np.array2string(np.asarray(v), threshold=200, edgeitems=4, max_line_width=110)
        if isinstance(v, (list, tuple, dict, set, frozenset)) and len(v) <= 200:
            import pprint
            return pprint.pformat(v, width=100, compact=True)
        if isinstance(v, (list, tuple, dict, set, frozenset)):
            import reprlib
            r = reprlib.Repr()
            r.maxlist = r.maxtuple = r.maxset = r.maxfrozenset = r.maxdict = 60
            r.maxstring = r.maxother = 200
            return r.repr(v)
        return repr(v)
    except Exception as e:
        return "<repr failed: %s>" % e
