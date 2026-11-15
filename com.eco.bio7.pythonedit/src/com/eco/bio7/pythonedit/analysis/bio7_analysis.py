# Copyright (c) 2007-2026 M. Austenfeld
# SPDX-License-Identifier: EPL-2.0
#
# The code analysis of the Python editor of Bio7: a separate process of the
# selected Python, one JSON request per line on stdin, one JSON response per
# line on stdout. Works with the standard library alone; uses ruff (rules,
# fixes, formatting), jedi (completion, help) and black (formatting) if they
# are installed for this Python.
#
# Positions: lines from 1, columns from 0 (characters).

import ast
import builtins
import difflib
import importlib
import importlib.util
import inspect
import io
import json
import keyword
import os
import re
import subprocess
import sys
import tokenize

# ---------------------------------------------------------------- tools

_tools = None


def tools():
    global _tools
    if _tools is None:
        _tools = {}
        for name in ("ruff", "jedi", "black"):
            version = None
            if importlib.util.find_spec(name) is not None:
                if name == "ruff":
                    try:
                        out = subprocess.run([sys.executable, "-m", "ruff", "--version"], capture_output=True, text=True, timeout=20)
                        version = out.stdout.strip().split()[-1] if out.returncode == 0 else None
                    except Exception:
                        version = None
                else:
                    try:
                        version = getattr(importlib.import_module(name), "__version__", "?")
                    except Exception:
                        version = None
            _tools[name] = version
    return _tools


# ---------------------------------------------------------------- positions


class Source:
    def __init__(self, text):
        self.text = text
        self.lines = text.splitlines(True) or [""]

    def line(self, lineno):
        return self.lines[lineno - 1] if 1 <= lineno <= len(self.lines) else ""

    def col(self, lineno, byte_col):
        """ast columns are UTF-8 byte offsets: the character column."""
        if byte_col is None:
            return 0
        line = self.line(lineno)
        try:
            return len(line.encode("utf-8")[:byte_col].decode("utf-8", "replace"))
        except Exception:
            return byte_col

    def span(self, node):
        line = node.lineno
        end_line = getattr(node, "end_lineno", None) or line
        col = self.col(line, node.col_offset)
        end_col = self.col(end_line, getattr(node, "end_col_offset", None)) if getattr(node, "end_col_offset", None) is not None else col + 1
        return line, col, end_line, end_col


def edit(line, col, end_line, end_col, text):
    return {"line": line, "col": col, "endLine": end_line, "endCol": end_col, "text": text}


def diag(severity, code, message, span, fixes=None):
    line, col, end_line, end_col = span
    return {"line": line, "col": col, "endLine": end_line, "endCol": end_col, "severity": severity, "code": code, "message": message, "fixes": fixes or []}


# ---------------------------------------------------------------- scopes (the own checks)

_BUILTINS = set(dir(builtins)) | {"__file__", "__name__", "__doc__", "__builtins__", "__spec__", "__loader__", "__package__", "__path__",
                                   "__annotations__", "__dict__", "__module__", "__qualname__", "__class__", "bio7"}


class Scope:
    def __init__(self, kind, parent):
        self.kind = kind
        self.parent = parent
        self.defs = {}       # name -> list of (kind, node, stmt)
        self.used = set()
        self.globals = set()
        self.nonlocals = set()
        self.uses = []       # (name, node)
        self.children = []
        if parent is not None:
            parent.children.append(self)


class Checker(ast.NodeVisitor):
    def __init__(self):
        self.module = Scope("module", None)
        self.scope = self.module
        self.star = False
        self.stmt = None
        self.dunder_all = set()
        self.targets_in_tuple = set()

    # -- helpers
    def define(self, name, kind, node, scope=None):
        scope = scope or self.scope
        if name in scope.globals:
            scope = self.module
        scope.defs.setdefault(name, []).append((kind, node, self.stmt))

    def generic_visit(self, node):
        if isinstance(node, ast.stmt):
            old, self.stmt = self.stmt, node
            super().generic_visit(node)
            self.stmt = old
        else:
            super().generic_visit(node)

    def visit_stmt_body(self, body):
        for s in body:
            old, self.stmt = self.stmt, s
            self.visit(s)
            self.stmt = old

    # -- definitions
    def visit_Import(self, node):
        for a in node.names:
            name = a.asname or a.name.split(".")[0]
            self.define(name, "import", a)
            a._stmt = node

    def visit_ImportFrom(self, node):
        for a in node.names:
            if a.name == "*":
                self.star = True
                continue
            if node.module == "__future__":
                continue
            self.define(a.asname or a.name, "import", a)
            a._stmt = node

    def _function(self, node, name):
        for d in getattr(node, "decorator_list", []):
            self.visit(d)
        args = node.args
        for d in args.defaults + [d for d in args.kw_defaults if d is not None]:
            self.visit(d)
        for a in args.posonlyargs + args.args + args.kwonlyargs + ([args.vararg] if args.vararg else []) + ([args.kwarg] if args.kwarg else []):
            if a.annotation is not None:
                self.visit(a.annotation)
        if getattr(node, "returns", None) is not None:
            self.visit(node.returns)
        if name is not None:
            self.define(name, "def", node)
        parent = self.scope
        self.scope = Scope("lambda" if name is None else "function", parent)
        for a in args.posonlyargs + args.args + args.kwonlyargs + ([args.vararg] if args.vararg else []) + ([args.kwarg] if args.kwarg else []):
            self.define(a.arg, "param", a)
        if isinstance(node.body, list):
            self.visit_stmt_body(node.body)
        else:
            self.visit(node.body)
        self.scope = parent

    def visit_FunctionDef(self, node):
        self._function(node, node.name)

    visit_AsyncFunctionDef = visit_FunctionDef

    def visit_Lambda(self, node):
        self._function(node, None)

    def visit_ClassDef(self, node):
        for d in node.decorator_list + node.bases + [k.value for k in node.keywords]:
            self.visit(d)
        self.define(node.name, "class", node)
        parent = self.scope
        self.scope = Scope("class", parent)
        self.visit_stmt_body(node.body)
        self.scope = parent

    def _comprehension(self, node, elts):
        # the first iterable belongs to the enclosing scope
        self.visit(node.generators[0].iter)
        parent = self.scope
        self.scope = Scope("comprehension", parent)
        for i, g in enumerate(node.generators):
            self.visit(g.target)
            if i > 0:
                self.visit(g.iter)
            for c in g.ifs:
                self.visit(c)
        for e in elts:
            self.visit(e)
        self.scope = parent

    def visit_ListComp(self, node):
        self._comprehension(node, [node.elt])

    visit_SetComp = visit_GeneratorExp = visit_ListComp

    def visit_DictComp(self, node):
        self._comprehension(node, [node.key, node.value])

    def visit_Global(self, node):
        self.scope.globals.update(node.names)

    def visit_Nonlocal(self, node):
        self.scope.nonlocals.update(node.names)

    def visit_Name(self, node):
        if isinstance(node.ctx, (ast.Store, ast.Del)):
            kind = "tuple" if id(node) in self.targets_in_tuple else "assign"
            self.define(node.id, kind, node)
        else:
            self.scope.uses.append((node.id, node))

    def visit_Assign(self, node):
        for t in node.targets:
            self._mark_tuple(t)
            if isinstance(t, ast.Name) and t.id == "__all__" and self.scope is self.module and isinstance(node.value, (ast.List, ast.Tuple)):
                self.dunder_all.update(e.value for e in node.value.elts if isinstance(e, ast.Constant) and isinstance(e.value, str))
        self.generic_visit(node)

    def visit_For(self, node):
        self._mark_tuple(node.target, force=True)
        self.generic_visit(node)

    visit_AsyncFor = visit_For

    def _mark_tuple(self, target, force=False):
        if isinstance(target, (ast.Tuple, ast.List)) or force:
            for n in ast.walk(target):
                if isinstance(n, ast.Name):
                    self.targets_in_tuple.add(id(n))

    def visit_AugAssign(self, node):
        if isinstance(node.target, ast.Name):
            self.scope.uses.append((node.target.id, node.target))
        self.generic_visit(node)

    def visit_NamedExpr(self, node):
        scope = self.scope
        while scope.kind == "comprehension":
            scope = scope.parent
        self.define(node.target.id, "assign", node.target, scope)
        self.visit(node.value)

    def visit_ExceptHandler(self, node):
        if node.type is not None:
            self.visit(node.type)
        if node.name:
            self.define(node.name, "except", node)
        self.visit_stmt_body(node.body)

    def visit_MatchAs(self, node):
        if node.name:
            self.define(node.name, "assign", node)
        self.generic_visit(node)

    def visit_MatchStar(self, node):
        if node.name:
            self.define(node.name, "assign", node)

    def visit_MatchMapping(self, node):
        if node.rest:
            self.define(node.rest, "assign", node)
        self.generic_visit(node)

    # -- resolution
    def resolve(self, known):
        undefined = []

        def lookup(scope, name):
            s = scope
            first = True
            while s is not None:
                if name in s.nonlocals and s is scope:
                    s = s.parent
                    first = False
                    continue
                if (first or s.kind != "class") and name in s.defs:
                    return s
                first = False
                s = s.parent
            return None

        def walk(scope):
            for name, node in scope.uses:
                target = lookup(scope, name)
                if target is not None:
                    target.used.add(name)
                elif name in scope.globals and name in self.module.defs:
                    self.module.used.add(name)
                elif name not in _BUILTINS and name not in known:
                    undefined.append((name, node))
            for c in scope.children:
                walk(c)

        walk(self.module)
        return undefined

    def all_scopes(self):
        out = []

        def walk(s):
            out.append(s)
            for c in s.children:
                walk(c)

        walk(self.module)
        return out


# ---------------------------------------------------------------- imports and suggestions

# The usual imports of names (undefined name -> import)
COMMON_IMPORTS = {
    "np": "import numpy as np", "pd": "import pandas as pd", "plt": "import matplotlib.pyplot as plt", "mpl": "import matplotlib as mpl",
    "sns": "import seaborn as sns", "sp": "import scipy as sp", "stats": "from scipy import stats", "tf": "import tensorflow as tf",
    "torch": "import torch", "nn": "from torch import nn", "cv2": "import cv2", "Image": "from PIL import Image", "Path": "from pathlib import Path",
    "defaultdict": "from collections import defaultdict", "Counter": "from collections import Counter", "OrderedDict": "from collections import OrderedDict",
    "namedtuple": "from collections import namedtuple", "deque": "from collections import deque", "dataclass": "from dataclasses import dataclass",
    "datetime": "import datetime", "partial": "from functools import partial", "reduce": "from functools import reduce", "glob": "import glob",
    "skimage": "import skimage", "io": "import io", "sleep": "from time import sleep",
}

# The pip names of modules (import name -> package)
PIP_NAMES = {
    "cv2": "opencv-python", "skimage": "scikit-image", "sklearn": "scikit-learn", "PIL": "pillow", "yaml": "pyyaml", "bs4": "beautifulsoup4",
    "Bio": "biopython", "dateutil": "python-dateutil", "attr": "attrs", "OpenGL": "PyOpenGL", "serial": "pyserial", "jwt": "PyJWT",
    "dotenv": "python-dotenv", "magic": "python-magic", "osgeo": "gdal", "wx": "wxPython", "tifffile": "tifffile", "docx": "python-docx",
}

_spec_cache = {}


def module_exists(name, folder):
    if folder and folder not in sys.path:
        sys.path.insert(0, folder)
    if name in _spec_cache:
        return _spec_cache[name]
    try:
        found = importlib.util.find_spec(name) is not None
    except Exception:
        found = True  # e.g., a module which can't be inspected: no warning
    _spec_cache[name] = found
    return found


def import_insert_position(tree, source):
    """The position of a new import: after the last import at the top (or the docstring)."""
    line = 1
    body = tree.body if tree is not None else []
    for i, s in enumerate(body):
        if i == 0 and isinstance(s, ast.Expr) and isinstance(getattr(s, "value", None), ast.Constant) and isinstance(s.value.value, str):
            line = s.end_lineno + 1
            continue
        if isinstance(s, (ast.Import, ast.ImportFrom)):
            line = s.end_lineno + 1
            continue
        break
    # after comments and the encoding line at the start
    if line == 1:
        while line <= len(source.lines) and source.line(line).lstrip().startswith("#"):
            line += 1
    return line


def import_fix(statement, tree, source):
    line = import_insert_position(tree, source)
    return {"title": "Add '" + statement + "'", "edits": [edit(line, 0, line, 0, statement + "\n")]}


def undefined_fixes(name, span, candidates, tree, source):
    fixes = []
    if name in COMMON_IMPORTS:
        fixes.append(import_fix(COMMON_IMPORTS[name], tree, source))
    elif name in getattr(sys, "stdlib_module_names", ()):
        fixes.append(import_fix("import " + name, tree, source))
    for m in difflib.get_close_matches(name, [c for c in candidates if c != name], n=3, cutoff=0.75):
        line, col, end_line, end_col = span
        fixes.append({"title": "Did you mean '" + m + "'?", "edits": [edit(line, col, end_line, end_col, m)]})
    return fixes


def remove_statement_edit(stmt, source):
    """Removes a statement on its own lines (with the line break)."""
    first, last = stmt.lineno, stmt.end_lineno
    text = "".join(source.lines[first - 1:last])
    seg = ast.get_source_segment(source.text, stmt) or ""
    if text.strip() != seg.strip():
        # another statement on the same line (;): only the statement
        line, col, end_line, end_col = source.span(stmt)
        return edit(line, col, end_line, end_col, "pass" if text.strip() == "" else "")
    return edit(first, 0, last + 1, 0, "") if last < len(source.lines) else edit(first, 0, last, len(source.line(last)), "")


def _comment_start(line):
    """The position of a comment (#) outside of strings in a line, else None."""
    try:
        for tok in tokenize.generate_tokens(io.StringIO(line + "\n").readline):
            if tok.type == tokenize.COMMENT:
                return tok.start[1]
    except (tokenize.TokenError, SyntaxError, IndentationError):
        pass
    return None


# ---------------------------------------------------------------- analysis


def analyze(source_text, path, known):
    source = Source(source_text)
    folder = os.path.dirname(os.path.abspath(path)) if path else None
    importlib.invalidate_caches()
    _spec_cache.clear()
    diags = []
    try:
        tree = ast.parse(source_text, filename=path or "<editor>")
    except SyntaxError as e:
        line = e.lineno or 1
        col = max((e.offset or 1) - 1, 0)
        end_line = getattr(e, "end_lineno", None) or line
        end_col = max((getattr(e, "end_offset", None) or col + 2) - 1, col + 1)
        if end_line < line or (end_line == line and end_col <= col):
            end_line, end_col = line, col + 1
        fixes = []
        msg = e.msg or "invalid syntax"
        text = source.line(line)
        m = re.match(r"(\s*)print\s+(.*?)\s*$", text)
        if "print" in msg and m:
            fixes.append({"title": "Change to print(...)", "edits": [edit(line, 0, line, len(text.rstrip("\r\n")), m.group(1) + "print(" + m.group(2) + ")")]})
        # brackets: an unclosed one is closed at the end of its line, an unmatched one removed
        closers = {"(": ")", "[": "]", "{": "}"}
        m = re.match(r"'([(\[{])' was never closed", msg)
        if m:
            end = len(text.rstrip("\r\n"))
            stripped = text.rstrip("\r\n")
            # before a comment at the end of the line
            hash_pos = _comment_start(stripped)
            if hash_pos is not None:
                end = len(stripped[:hash_pos].rstrip())
            fixes.append({"title": "Insert '" + closers[m.group(1)] + "' at the end of line " + str(line), "edits": [edit(line, end, line, end, closers[m.group(1)])]})
        m = re.match(r"unmatched '([)\]}])'", msg)
        if m and 0 <= col < len(text) and text[col] == m.group(1):
            fixes.append({"title": "Remove '" + m.group(1) + "'", "edits": [edit(line, col, line, col + 1, "")]})
        m = re.match(r"closing parenthesis '([)\]}])' does not match opening parenthesis '([(\[{])'", msg)
        if m and 0 <= col < len(text) and text[col] == m.group(1):
            fixes.append({"title": "Replace '" + m.group(1) + "' with '" + closers[m.group(2)] + "'", "edits": [edit(line, col, line, col + 1, closers[m.group(2)])]})
        diags.append(diag("error", "syntax", msg, (line, col, end_line, end_col), fixes))
        return {"diagnostics": diags, "tools": tools(), "outline": None}
    try:
        tree_outline = outline(tree, source)
    except Exception:
        tree_outline = None
    checker = Checker()
    checker.visit_stmt_body(tree.body)
    undefined = checker.resolve(set(known or ()))
    candidates = set(_BUILTINS) | set(known or ())
    for s in checker.all_scopes():
        candidates.update(s.defs)
    use_ruff = tools().get("ruff") is not None
    if use_ruff:
        ruff_diags = run_ruff(source_text, path)
        if ruff_diags is None:
            use_ruff = False
        else:
            for d in ruff_diags:
                if d["code"] == "F821":
                    m = re.search(r"`([^`]+)`", d["message"])
                    name = m.group(1) if m else None
                    if name and name in (known or ()):
                        continue  # a variable of the running Python
                    if name:
                        d["fixes"] = d["fixes"] + undefined_fixes(name, (d["line"], d["col"], d["endLine"], d["endCol"]), candidates, tree, source)
                diags.append(d)
    if not use_ruff:
        if not checker.star:
            for name, node in undefined:
                span = source.span(node)
                diags.append(diag("warning", "undefined-name", "Undefined name '" + name + "'", span, undefined_fixes(name, span, candidates, tree, source)))
        own_unused(checker, source, diags, path)
    # Modules which are not installed (always: neither ruff nor pyflakes check this)
    for node in ast.walk(tree):
        names = []
        if isinstance(node, ast.Import):
            names = [(a.name.split(".")[0], a) for a in node.names]
        elif isinstance(node, ast.ImportFrom) and node.level == 0 and node.module:
            names = [(node.module.split(".")[0], node)]
        for top, n in names:
            if top in ("__future__",) or module_exists(top, folder):
                continue
            pkg = PIP_NAMES.get(top, top)
            diags.append(diag("warning", "missing-module", "No module named '" + top + "' (not installed for this Python)", source.span(n),
                              [{"title": "Install '" + pkg + "' with Python Packages", "install": pkg}]))
    return {"diagnostics": diags, "tools": tools(), "outline": tree_outline}


def own_unused(checker, source, diags, path):
    if path and os.path.basename(path) == "__init__.py":
        return
    for scope in checker.all_scopes():
        if scope.kind == "class":
            continue
        for name, bindings in scope.defs.items():
            if name in scope.used or name in checker.dunder_all or name.startswith("_"):
                continue
            # a name used by a nested scope (closures) counts in lookup; globals of functions
            kind, node, stmt = bindings[-1]
            if kind == "import" and scope.kind == "module" or kind == "import" and scope.kind == "function":
                imp = getattr(node, "_stmt", stmt)
                span = source.span(imp) if imp is not None else source.span(node)
                fixes = []
                if imp is not None:
                    if len(imp.names) == 1:
                        fixes.append({"title": "Remove the unused import", "edits": [remove_statement_edit(imp, source)]})
                    else:
                        rest = [a for a in imp.names if (a.asname or a.name.split(".")[0] if isinstance(imp, ast.Import) else a.asname or a.name) != name]
                        new = ast.Import(names=rest) if isinstance(imp, ast.Import) else ast.ImportFrom(module=imp.module, names=rest, level=imp.level)
                        line, col, end_line, end_col = source.span(imp)
                        fixes.append({"title": "Remove '" + name + "' from the import", "edits": [edit(line, col, end_line, end_col, ast.unparse(new))]})
                diags.append(diag("warning", "unused-import", "'" + name + "' is imported but never used", span, fixes))
            elif kind == "assign" and scope.kind == "function" and all(k == "assign" for k, _, _ in bindings):
                if name in scope.globals or name in scope.nonlocals:
                    continue
                stmt = bindings[0][2]
                span = source.span(node)
                fixes = []
                if isinstance(stmt, ast.Assign) and len(stmt.targets) == 1 and isinstance(stmt.targets[0], ast.Name):
                    t = stmt.targets[0]
                    line, col, _, _ = source.span(t)
                    vline, vcol, _, _ = source.span(stmt.value)
                    fixes.append({"title": "Remove the assignment (keep the value)", "edits": [edit(line, col, vline, vcol, "")]})
                    fixes.append({"title": "Remove the statement", "edits": [remove_statement_edit(stmt, source)]})
                diags.append(diag("warning", "unused-variable", "Local variable '" + name + "' is assigned but never used", span, fixes))


def run_ruff(source_text, path):
    try:
        out = subprocess.run([sys.executable, "-m", "ruff", "check", "--output-format=json", "--no-cache", "--stdin-filename", path or "script.py", "-"],
                             input=source_text, capture_output=True, text=True, timeout=30)
        data = json.loads(out.stdout or "[]")
    except Exception:
        return None
    result = []
    for r in data:
        loc, end = r.get("location") or {}, r.get("end_location") or {}
        code = r.get("code") or "syntax"
        if code in ("E999",) or code is None:
            continue
        fixes = []
        fix = r.get("fix")
        if fix and fix.get("edits"):
            edits = [edit(e["location"]["row"], e["location"]["column"] - 1, e["end_location"]["row"], e["end_location"]["column"] - 1, e.get("content") or "")
                     for e in fix["edits"]]
            fixes.append({"title": fix.get("message") or ("Fix " + code), "edits": edits})
        severity = "warning"
        result.append({"line": loc.get("row", 1), "col": loc.get("column", 1) - 1, "endLine": end.get("row", loc.get("row", 1)),
                       "endCol": end.get("column", loc.get("column", 1)) - 1, "severity": severity, "code": code,
                       "message": r.get("message", ""), "fixes": fixes, "url": r.get("url")})
    return result


# ---------------------------------------------------------------- outline


def outline(tree, source):
    """The structure of the code: classes, functions, variables, imports, main blocks."""

    def item(name, kind, node, children=None):
        line, col, end_line, end_col = source.span(node)
        return {"name": name, "kind": kind, "line": line, "col": col, "endLine": end_line, "endCol": end_col, "children": children or []}

    def args(f):
        a = f.args
        names = [x.arg for x in a.posonlyargs + a.args]
        if a.vararg:
            names.append("*" + a.vararg.arg)
        names += [x.arg for x in a.kwonlyargs]
        if a.kwarg:
            names.append("**" + a.kwarg.arg)
        return "(" + ", ".join(names) + ")"

    def body(stmts, in_class):
        out = []
        seen = set()
        for s in stmts:
            if isinstance(s, (ast.FunctionDef, ast.AsyncFunctionDef)):
                kind = "method" if in_class else "function"
                out.append(item(s.name + args(s), kind, s, body(s.body, False) if not in_class else body(s.body, False)))
                # only nested functions and classes inside functions
                out[-1]["children"] = [c for c in out[-1]["children"] if c["kind"] in ("function", "class")]
            elif isinstance(s, ast.ClassDef):
                bases = ", ".join(ast.get_source_segment(source.text, b) or "" for b in s.bases)
                out.append(item(s.name + ("(" + bases + ")" if bases else ""), "class", s, body(s.body, True)))
            elif isinstance(s, (ast.Assign, ast.AnnAssign)) :
                targets = s.targets if isinstance(s, ast.Assign) else [s.target]
                for t in targets:
                    for n in ast.walk(t):
                        if isinstance(n, ast.Name) and n.id not in seen:
                            seen.add(n.id)
                            out.append(item(n.id, "attribute" if in_class else "variable", n))
            elif isinstance(s, (ast.Import, ast.ImportFrom)) and not in_class:
                for a in s.names:
                    label = (a.asname + " = " if a.asname else "") + (a.name if isinstance(s, ast.Import) else (s.module or ".") + "." + a.name)
                    out.append(item(label, "import", s))
            elif isinstance(s, ast.If) and not in_class and "__name__" in (ast.get_source_segment(source.text, s.test) or ""):
                out.append(item("if __name__ == '__main__'", "main", s, body(s.body, False)))
        return out

    return body(tree.body, False)


# ---------------------------------------------------------------- formatting


def format_code(source_text, path):
    t = tools()
    if t.get("ruff"):
        out = subprocess.run([sys.executable, "-m", "ruff", "format", "--stdin-filename", path or "script.py", "-"], input=source_text, capture_output=True,
                             text=True, timeout=60)
        if out.returncode == 0:
            return {"text": out.stdout, "tool": "ruff"}
        return {"error": (out.stderr or out.stdout).strip()}
    if t.get("black"):
        out = subprocess.run([sys.executable, "-m", "black", "-q", "-"], input=source_text, capture_output=True, text=True, timeout=60)
        if out.returncode == 0:
            return {"text": out.stdout, "tool": "black"}
        return {"error": (out.stderr or out.stdout).strip()}
    return {"error": "No formatter: please install ruff or black (Python Packages: the bundle 'Editor tools')."}


# ---------------------------------------------------------------- completion and help

_IDENT = re.compile(r"[A-Za-z_][A-Za-z0-9_]*(?:\.[A-Za-z_][A-Za-z0-9_]*)*\.?$|[A-Za-z_][A-Za-z0-9_]*$|\.$")


def _expression_before(source, line, col):
    text = source.line(line)[:col]
    m = re.search(r"([A-Za-z_][A-Za-z0-9_]*(?:\.[A-Za-z_][A-Za-z0-9_]*)*\.?)$", text)
    return m.group(1) if m else ""


def _aliases(tree):
    """The imported names of the file: alias -> module (or module.attribute)."""
    out = {}
    if tree is None:
        return out
    for node in ast.walk(tree):
        if isinstance(node, ast.Import):
            for a in node.names:
                if a.asname:
                    out[a.asname] = a.name
                else:
                    out[a.name.split(".")[0]] = a.name.split(".")[0]
        elif isinstance(node, ast.ImportFrom) and node.level == 0 and node.module:
            for a in node.names:
                if a.name != "*":
                    out[a.asname or a.name] = node.module + "." + a.name
    return out


def _resolve(path_expr, aliases, folder):
    """An object for a dotted name of the file (an imported module or its attributes)."""
    parts = path_expr.split(".")
    head = parts[0]
    if head in aliases:
        target = aliases[head]
    elif head in getattr(builtins, "__dict__", {}):
        obj = getattr(builtins, head)
        for p in parts[1:]:
            obj = getattr(obj, p)
        return obj
    else:
        return None
    # the module part of the target
    tparts = target.split(".")
    obj = None
    for i in range(len(tparts), 0, -1):
        mod_name = ".".join(tparts[:i])
        spec = None
        try:
            spec = importlib.util.find_spec(mod_name)
        except Exception:
            spec = None
        if spec is None:
            continue
        origin = getattr(spec, "origin", None) or ""
        if folder and origin.startswith(folder):
            return None  # an own module of the script folder: not imported (its code would run)
        obj = importlib.import_module(mod_name)
        for p in tparts[i:]:
            obj = getattr(obj, p)
        break
    if obj is None:
        return None
    for p in parts[1:]:
        obj = getattr(obj, p)
    return obj


def _kind(obj):
    if inspect.ismodule(obj):
        return "module"
    if inspect.isclass(obj):
        return "class"
    if callable(obj):
        return "function"
    return "instance"


def _signature(obj):
    try:
        return str(inspect.signature(obj))
    except (TypeError, ValueError):
        return ""


def _doc(obj, limit=1200):
    try:
        d = inspect.getdoc(obj) or ""
    except Exception:
        d = ""
    return d[:limit]


def complete(source_text, path, line, col):
    source = Source(source_text)
    folder = os.path.dirname(os.path.abspath(path)) if path else None
    if folder and folder not in sys.path:
        sys.path.insert(0, folder)
    expr = _expression_before(source, line, col)
    if tools().get("jedi"):
        try:
            import jedi
            script = jedi.Script(code=source_text, path=path or None)
            items = []
            for i, c in enumerate(script.complete(line, col)):
                if i >= 400:
                    break
                entry = {"name": c.name, "kind": c.type, "complete": c.complete}
                if i < 40:
                    try:
                        sigs = c.get_signatures()
                        if sigs:
                            entry["signature"] = sigs[0].to_string()
                        entry["doc"] = c.docstring(raw=True)[:1200]
                    except Exception:
                        pass
                items.append(entry)
            return {"items": items, "expression": expr, "tool": "jedi"}
        except Exception:
            pass
    # the standard library: the names of the file, imported modules, builtins, keywords
    try:
        tree = ast.parse(source_text)
    except SyntaxError:
        tree = None
    aliases = _aliases(tree)
    items = []
    if "." in expr:
        base, prefix = expr.rsplit(".", 1)
        try:
            obj = _resolve(base, aliases, folder)
        except Exception:
            obj = None
        if obj is not None:
            for name in sorted(dir(obj)):
                if name.startswith("_") and not prefix.startswith("_"):
                    continue
                if not name.startswith(prefix):
                    continue
                try:
                    member = getattr(obj, name)
                except Exception:
                    continue
                entry = {"name": name, "kind": _kind(member), "complete": name[len(prefix):]}
                if callable(member):
                    entry["signature"] = name + _signature(member)
                entry["doc"] = _doc(member, 600)
                items.append(entry)
        return {"items": items, "expression": expr, "tool": "standard library"}
    prefix = expr
    names = set(keyword.kwlist) | set(dir(builtins))
    if tree is not None:
        checker = Checker()
        checker.visit_stmt_body(tree.body)
        for s in checker.all_scopes():
            names.update(s.defs)
    for name in sorted(names):
        if name.startswith(prefix) and not (name.startswith("_") and not prefix.startswith("_")):
            kind = "keyword" if keyword.iskeyword(name) else ("module" if aliases.get(name) and "." not in aliases[name] else "name")
            entry = {"name": name, "kind": kind, "complete": name[len(prefix):]}
            if hasattr(builtins, name) and callable(getattr(builtins, name)):
                entry["signature"] = name + _signature(getattr(builtins, name))
                entry["doc"] = _doc(getattr(builtins, name), 600)
                entry["kind"] = "function"
            items.append(entry)
    return {"items": items, "expression": expr, "tool": "standard library"}


def word_at(source, line, col):
    text = source.line(line)
    start = col
    while start > 0 and (text[start - 1].isalnum() or text[start - 1] in "_."):
        start -= 1
    end = col
    while end < len(text) and (text[end].isalnum() or text[end] == "_"):
        end += 1
    return text[start:end].strip(".")


def help_at(source_text, path, line, col):
    source = Source(source_text)
    folder = os.path.dirname(os.path.abspath(path)) if path else None
    word = word_at(source, line, col)
    if not word:
        return {"text": ""}
    if tools().get("jedi"):
        try:
            import jedi
            script = jedi.Script(code=source_text, path=path or None)
            names = script.help(line, col) or script.infer(line, col)
            for n in names:
                doc = n.docstring()
                if doc:
                    return {"text": doc[:3000], "word": word}
        except Exception:
            pass
    try:
        tree = ast.parse(source_text)
    except SyntaxError:
        tree = None
    try:
        obj = _resolve(word, _aliases(tree), folder)
    except Exception:
        obj = None
    if obj is None and keyword.iskeyword(word):
        return {"text": "Python keyword: " + word, "word": word}
    if obj is None:
        return {"text": "", "word": word}
    head = word + _signature(obj) if callable(obj) else word + ": " + type(obj).__name__
    return {"text": head + "\n\n" + _doc(obj, 3000), "word": word}


# ---------------------------------------------------------------- the loop


def main():
    out = sys.stdout
    sys.stdout = sys.stderr  # prints of imported modules don't disturb the protocol
    for raw in sys.stdin:
        raw = raw.strip()
        if not raw:
            continue
        try:
            req = json.loads(raw)
        except Exception:
            continue
        rid = req.get("id")
        try:
            cmd = req.get("cmd")
            if cmd == "analyze":
                result = analyze(req["source"], req.get("path"), req.get("known"))
            elif cmd == "complete":
                result = complete(req["source"], req.get("path"), req["line"], req["col"])
            elif cmd == "help":
                result = help_at(req["source"], req.get("path"), req["line"], req["col"])
            elif cmd == "format":
                result = format_code(req["source"], req.get("path"))
            elif cmd == "tools":
                global _tools
                _tools = None
                importlib.invalidate_caches()
                result = tools()
            else:
                result = {"error": "unknown command " + str(cmd)}
            response = {"id": rid, "result": result}
        except Exception as e:
            response = {"id": rid, "error": type(e).__name__ + ": " + str(e)}
        out.write(json.dumps(response) + "\n")
        out.flush()


if __name__ == "__main__":
    main()
