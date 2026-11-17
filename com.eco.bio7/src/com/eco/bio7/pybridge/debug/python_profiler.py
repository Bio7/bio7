# Copyright (c) 2007-2026 M. Austenfeld
# SPDX-License-Identifier: EPL-2.0
#
# The profiler of the Python editor of Bio7 (the module _bio7prof): runs a script
# (or a selection of it) in __main__ with two profilers of the standard library:
#  - a sampling profiler (the stack of the main thread every millisecond): the time
#    of each line (with the functions it calls) and the stacks of a flame graph,
#  - cProfile: the calls and the times of the functions.
# The result: an HTML report in the browser view of Bio7 and the times of the lines
# as marks in the editor (bio7.call "profile_result").

import cProfile
import html as _html
import json
import linecache
import os
import pathlib
import pstats
import sys
import tempfile
import threading
import time
import traceback

_main = sys.modules["__main__"]


class _Sampler(threading.Thread):
    """Samples the stack of a thread: the time between two samples goes to the stack
    of the second (the main thread may hold the GIL longer than the interval)."""

    def __init__(self, ident, interval):
        super().__init__(daemon=True, name="Bio7 profiler")
        self.target = ident
        self.interval = interval
        self.stacks = {}
        self.samples = 0
        self.running = True

    def run(self):
        last = time.perf_counter()
        while self.running:
            time.sleep(self.interval)
            now = time.perf_counter()
            weight = now - last
            last = now
            frame = sys._current_frames().get(self.target)
            stack = []
            while frame is not None:
                code = frame.f_code
                if not code.co_filename.startswith("<_bio7"):
                    stack.append((code.co_filename, frame.f_lineno, code.co_name))
                frame = frame.f_back
            if stack:
                key = tuple(reversed(stack))
                self.stacks[key] = self.stacks.get(key, 0.0) + weight
                self.samples += 1


def run(path, code=None, first_line=1, interval=0.001):
    """Profiles a script file (code: a selection which starts at first_line)."""
    path = os.path.abspath(path)
    if code is None:
        with open(path, "rb") as f:
            source = f.read()
    else:
        # the selection with the line numbers of the file
        source = "\n" * (max(first_line, 1) - 1) + code
    compiled = compile(source, path, "exec")
    linecache.checkcache(path)
    folder = os.path.dirname(path)
    if folder not in sys.path:
        sys.path.insert(0, folder)
    ns = _main.__dict__
    old_file = ns.get("__file__", None)
    ns["__file__"] = path
    sampler = _Sampler(threading.get_ident(), interval)
    prof = cProfile.Profile()
    message = None
    start = time.perf_counter()
    sampler.start()
    try:
        prof.enable()
        try:
            exec(compiled, ns)
        finally:
            prof.disable()
    except KeyboardInterrupt:
        message = "Stopped: the result up to the stop."
    except BaseException:
        text = traceback.format_exc()
        sys.stderr.write(text)
        message = "The script ended with an error (the result up to the error): " + text.strip().splitlines()[-1]
    finally:
        elapsed = time.perf_counter() - start
        sampler.running = False
        sampler.join(1.0)
        if old_file is None:
            ns.pop("__file__", None)
        else:
            ns["__file__"] = old_file
    result = analyze(path, sampler, prof, elapsed, code is not None, message)
    report = html_report(result)
    fd, out = tempfile.mkstemp(suffix=".html", prefix="bio7_profile_")
    with os.fdopen(fd, "w", encoding="utf-8") as f:
        f.write(report)
    bio7 = sys.modules.get("_bio7")
    if bio7 is not None:
        try:
            bio7._call("open_url", pathlib.Path(out).as_uri())
            bio7._call("profile_result", path, json.dumps({"total": result["sampled"], "lines": result["marks"]}))
        except Exception as e:
            sys.stderr.write("Profile: the result could not be shown: %s\n" % e)
    sys.stdout.write("Profile of %s: %.3f s (%d samples)%s\n" % (os.path.basename(path), elapsed, sampler.samples,
                                                                  "" if message is None else " - " + message.splitlines()[0]))
    return out


def _short(filename):
    if filename == "~":
        return "built-in"
    parts = filename.replace("\\", "/").split("/")
    for marker in ("site-packages", "dist-packages", "lib"):
        if marker in parts:
            i = len(parts) - 1 - parts[::-1].index(marker)
            if i + 1 < len(parts):
                return "/".join(parts[i + 1:])
    return parts[-1]


def analyze(path, sampler, prof, elapsed, selection, message):
    # the stacks from the first frame of the script (not the code which started it)
    stacks = {}
    for stack, w in sampler.stacks.items():
        start = next((i for i, fr in enumerate(stack) if fr[0] == path), None)
        if start is not None:
            key = stack[start:]
            stacks[key] = stacks.get(key, 0.0) + w
    sampled = sum(stacks.values())
    # the lines of the script: inclusive (the line and what it calls) and self
    incl, own = {}, {}
    for stack, w in stacks.items():
        seen = set()
        for fn, line, _ in stack:
            if fn == path and line not in seen:
                seen.add(line)
                incl[line] = incl.get(line, 0.0) + w
        fn, line, _ = stack[-1]
        if fn == path:
            own[line] = own.get(line, 0.0) + w
    # the flame graph: a tree of the calls (name, file, line of the call)
    root = {"n": "all", "f": "", "l": 0, "v": 0.0, "c": {}}
    for stack, w in stacks.items():
        node = root
        node["v"] += w
        for fn, line, name in stack:
            key = (name, fn, line)
            child = node["c"].get(key)
            if child is None:
                child = node["c"][key] = {"n": name, "f": fn, "l": line, "v": 0.0, "c": {}}
            child["v"] += w
            node = child

    def compact(node, depth=0):
        children = sorted(node["c"].values(), key=lambda c: -c["v"])
        # very small calls are not shown (the report stays small)
        children = [c for c in children if c["v"] >= sampled * 0.0005] if depth > 0 else children
        return [node["n"], round(node["v"] * 1000, 3), _short(node["f"]) if node["f"] else "", node["l"],
                1 if node["f"] == path else 0, [compact(c, depth + 1) for c in children]]

    # the functions (cProfile)
    functions = []
    try:
        st = pstats.Stats(prof).stats
        for (fn, line, name), (cc, nc, tt, ct, _) in st.items():
            if fn.startswith("<_bio7") or name in ("<module>",) and fn.startswith("<"):
                continue
            functions.append([name, _short(fn), line, nc, cc, round(tt * 1000, 3), round(ct * 1000, 3), 1 if fn == path else 0])
    except Exception:
        pass
    functions.sort(key=lambda r: -r[6])
    functions = functions[:400]
    # the source lines
    lines = linecache.getlines(path)
    source = [[i + 1, round(incl.get(i + 1, 0.0) * 1000, 3), round(own.get(i + 1, 0.0) * 1000, 3), text.rstrip("\r\n")]
              for i, text in enumerate(lines)]
    # the marks in the editor: the lines with at least 1 % of the time
    marks = sorted(([line, round(t * 1000, 3), round(own.get(line, 0.0) * 1000, 3)] for line, t in incl.items()
                    if sampled > 0 and t >= sampled * 0.01), key=lambda m: -m[1])[:60]
    return {"path": path, "name": os.path.basename(path), "selection": selection, "elapsed": elapsed, "sampled": sampled,
            "samples": sampler.samples, "interval": sampler.interval, "message": message, "source": source,
            "flame": compact(root), "functions": functions, "marks": marks}


def html_report(r):
    if _TEMPLATE is None:
        raise RuntimeError("The template of the report is not set")
    data = json.dumps({k: r[k] for k in ("name", "selection", "elapsed", "sampled", "samples", "interval", "message", "source",
                                         "flame", "functions")})
    data = data.replace("</", "<\\/")
    return _TEMPLATE.replace("__TITLE__", _html.escape("Profile: " + r["name"])).replace("__DATA__", data)


# The HTML template of the report (profile_report.html, set by Bio7 when the module is loaded)
_TEMPLATE = None

