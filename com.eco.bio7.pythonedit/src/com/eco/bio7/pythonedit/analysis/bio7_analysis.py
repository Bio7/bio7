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
    # Errors of the compiler which the parser accepts ('return' or 'yield' outside a function,
    # 'break' outside a loop, duplicate arguments, nonlocal/global declarations, ...)
    compile_line = None
    try:
        compile(tree, path or "<editor>", "exec", dont_inherit=True)
    except SyntaxError as e:
        compile_line = e.lineno or 1
        col = source.col(compile_line, max((e.offset or 1) - 1, 0))
        end_line = getattr(e, "end_lineno", None) or compile_line
        end_off = getattr(e, "end_offset", None)
        end_col = source.col(end_line, end_off - 1) if end_off else col + 1
        if end_line < compile_line or (end_line == compile_line and end_col <= col):
            end_line, end_col = compile_line, col + 1
        diags.append(diag("error", "syntax", e.msg or "invalid syntax", (compile_line, col, end_line, end_col)))
    except Exception:
        pass  # e.g., too deeply nested code: no compiler check
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
                if compile_line is not None and d["code"] in ("F701", "F702", "F704", "F706", "PLE0117", "PLE0118", "invalid-syntax", "syntax"):
                    continue  # the error of the compiler is shown (ruff reports it again)
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


def _local_help(tree, source_text, word, line):
    """The help of a name defined in the file (without jedi): functions, classes and
    methods (signature and docstring), else the assignment of a variable."""
    name = word.split(".")[-1]
    defs, assigns = [], []
    for node in ast.walk(tree):
        if isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef, ast.ClassDef)) and node.name == name:
            defs.append(node)
        elif isinstance(node, (ast.Assign, ast.AnnAssign)) and "." not in word:
            targets = node.targets if isinstance(node, ast.Assign) else [node.target]
            if any(isinstance(t, ast.Name) and t.id == name for t in targets):
                assigns.append(node)
    if defs:
        # the definition before the line (else the first)
        before = [d for d in defs if d.lineno <= line]
        d = max(before, key=lambda n: n.lineno) if before else defs[0]
        if isinstance(d, ast.ClassDef):
            init = next((n for n in d.body if isinstance(n, ast.FunctionDef) and n.name == "__init__"), None)
            head = "class " + d.name + ("(" + _ast_args(init.args, skip_self=True) + ")" if init else "")
        else:
            prefix = "async def " if isinstance(d, ast.AsyncFunctionDef) else "def "
            head = prefix + d.name + "(" + _ast_args(d.args) + ")" + (" -> " + ast.unparse(d.returns) if d.returns is not None else "")
        doc = ast.get_docstring(d)
        return head + ("\n\n" + doc[:3000] if doc else "")
    if assigns:
        before = [a for a in assigns if a.lineno <= line]
        a = max(before, key=lambda n: n.lineno) if before else assigns[0]
        seg = ast.get_source_segment(source_text, a) or ""
        return "Line " + str(a.lineno) + ": " + (seg if len(seg) < 400 else seg[:400] + " ...")
    return None


def _ast_args(args, skip_self=False):
    """The arguments of a definition as text (with defaults and annotations)."""
    try:
        text = ast.unparse(args)
    except Exception:
        return "..."
    if skip_self:
        parts = text.split(", ", 1)
        if parts and parts[0] in ("self", "cls"):
            text = parts[1] if len(parts) > 1 else ""
    return text


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
    if obj is None and tree is not None:
        local = _local_help(tree, source_text, word, line)
        if local:
            return {"text": local, "word": word}
    if obj is None:
        return {"text": "", "word": word}
    head = word + _signature(obj) if callable(obj) else word + ": " + type(obj).__name__
    return {"text": head + "\n\n" + _doc(obj, 3000), "word": word}


# ---------------------------------------------------------------- refactoring
# The commands return the new source ({"text": ..., "cursor": [line, col, length]})
# or {"error": message}. Lines are 1-based, columns characters (0-based).

_COMPREHENSIONS = (ast.ListComp, ast.SetComp, ast.DictComp, ast.GeneratorExp, ast.Lambda)
_SCOPES = (ast.FunctionDef, ast.AsyncFunctionDef, ast.ClassDef) + _COMPREHENSIONS


def _valid_name(name):
    if not name or not name.isidentifier() or keyword.iskeyword(name):
        return "'%s' is not a valid name" % name
    return None


def _index(lines, line, col):
    """The index in the text of a line (1-based) and a column (characters)."""
    return sum(len(x) for x in lines[:line - 1]) + col


def _apply(text, edits):
    """Applies edits (line, col, end_line, end_col, new text) from the end."""
    lines = text.splitlines(True) or [""]
    spans = sorted(((_index(lines, a, b), _index(lines, c, d), t) for a, b, c, d, t in edits), reverse=True)
    for start, end, t in spans:
        text = text[:start] + t + text[end:]
    return text


def _name_at(text, line, col):
    """The identifier at the position: (name, start column) or (None, None)."""
    lines = text.splitlines() or [""]
    if not 1 <= line <= len(lines):
        return None, None
    s = lines[line - 1]
    start = end = min(col, len(s))
    while start > 0 and (s[start - 1].isalnum() or s[start - 1] == "_"):
        start -= 1
    while end < len(s) and (s[end].isalnum() or s[end] == "_"):
        end += 1
    name = s[start:end]
    return (name, start) if name.isidentifier() else (None, None)


def _parse(text):
    try:
        return ast.parse(text), None
    except SyntaxError as e:
        return None, {"error": "The code has a syntax error (line %s): %s" % (e.lineno, e.msg)}


def _names_in_file(text):
    names = set()
    try:
        for tok in tokenize.generate_tokens(io.StringIO(text).readline):
            if tok.type == tokenize.NAME:
                names.add(tok.string)
    except (tokenize.TokenError, SyntaxError, IndentationError):
        pass
    return names


def _import_binding(tree, old):
    """The import statement and alias which bind the name (or None)."""
    for node in ast.walk(tree):
        if isinstance(node, (ast.Import, ast.ImportFrom)):
            for a in node.names:
                bound = a.asname or (a.name.split(".")[0] if isinstance(node, ast.Import) else a.name)
                if bound == old:
                    return node, a
    return None


def _own_references(text, tree, line, col, old):
    """The positions (line, column) of the name at the cursor and of all names
    which mean the same variable (the rules of the scopes of Python)."""
    source = Source(text)
    lines = text.splitlines()
    parent = {}
    for node in ast.walk(tree):
        for c in ast.iter_child_nodes(node):
            parent[c] = node
    scope_types = (ast.FunctionDef, ast.AsyncFunctionDef, ast.Lambda, ast.ClassDef) + _COMPREHENSIONS

    def enclosing(node):
        n = parent.get(node)
        while n is not None and not isinstance(n, scope_types):
            n = parent.get(n)
        return n or tree

    # the bindings and declarations of each scope
    bound, declared = {}, {}
    occurrences = []  # (position, scope of the occurrence, binds?)

    def bind(scope, name):
        bound.setdefault(scope, set()).add(name)

    for node in ast.walk(tree):
        if isinstance(node, ast.Name) and node.id == old:
            sc = enclosing(node)
            if not isinstance(node.ctx, ast.Load):
                bind(sc, old)
            ln, c, _, _ = source.span(node)
            occurrences.append(((ln, c), sc))
        elif isinstance(node, ast.arg) and node.arg == old:
            sc = enclosing(node)  # the function (or lambda) of the argument
            bind(sc, old)
            occurrences.append(((node.lineno, source.col(node.lineno, node.col_offset)), sc))
        elif isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef, ast.ClassDef)) and node.name == old:
            sc = enclosing(node)
            bind(sc, old)
            m = re.search(r"\b(?:def|class)\s+(" + re.escape(old) + r")\b", lines[node.lineno - 1])
            if m:
                occurrences.append(((node.lineno, m.start(1)), sc))
        elif isinstance(node, (ast.Import, ast.ImportFrom, ast.Global, ast.Nonlocal)):
            names = [a.asname or (a.name.split(".")[0] if isinstance(node, ast.Import) else a.name) for a in node.names] \
                if not isinstance(node, (ast.Global, ast.Nonlocal)) else node.names
            if old not in names:
                continue
            sc = enclosing(node)
            if isinstance(node, (ast.Global, ast.Nonlocal)):
                declared.setdefault(sc, {})[old] = "global" if isinstance(node, ast.Global) else "nonlocal"
            else:
                bind(sc, old)
            # the position of the name in the statement (tokens of its lines)
            seg = "\n".join(lines[node.lineno - 1:node.end_lineno])
            toks = [t for t in tokenize.generate_tokens(io.StringIO(seg + "\n").readline) if t.type == tokenize.NAME and t.string == old]
            if isinstance(node, ast.ImportFrom):
                # 'from m import old' or '... as old': the bound name (the last token of the name)
                toks = toks[-1:] if toks else toks
            for t in toks:
                occurrences.append(((node.lineno + t.start[0] - 1, t.start[1]), sc))

    def resolve(scope):
        d = declared.get(scope, {}).get(old)
        if d == "global":
            return tree
        first = True
        s = scope
        while True:
            if s is tree:
                return tree
            if d == "nonlocal" and s is scope:
                pass
            elif old in bound.get(s, ()) and (first or not isinstance(s, ast.ClassDef)):
                return s
            first = False
            s = enclosing(s)
            if s is not tree and isinstance(s, ast.ClassDef):
                # a class body is not visible to its methods
                s = enclosing(s)

    target = None
    for (ln, c), sc in occurrences:
        if ln == line and c == col:
            target = resolve(sc)
    if target is None:
        return "No variable at the cursor"
    return sorted({pos for pos, sc in occurrences if resolve(sc) is target})


def refactor_rename(text, path, line, col, new_name):
    err = _valid_name(new_name)
    if err:
        return {"error": err}
    tree, err = _parse(text)
    if err:
        return err
    old, start = _name_at(text, line, col)
    if old is None:
        return {"error": "No name at the cursor"}
    if old == new_name:
        return {"text": text}
    if new_name in _names_in_file(text):
        return {"error": "'%s' is already used in this file" % new_name}
    binding = _import_binding(tree, old)
    positions = None
    if tools().get("jedi"):
        import jedi
        script = jedi.Script(text, path=path)
        refs = [r for r in script.get_references(line, start, scope="file", include_builtins=False) if r.name == old]
        if refs:
            local = any(r.is_definition() for r in refs)
            if not local:
                where = [d for d in script.goto(line, start, follow_imports=False)]
                mod = where[0].module_name if where else "another module"
                return {"error": "'%s' is defined outside this file (%s)" % (old, mod)}
            positions = sorted({(r.line, r.column) for r in refs})
    if positions is None:
        # Without jedi: the scopes of the syntax tree
        positions = _own_references(text, tree, line, start, old)
        if isinstance(positions, str):
            return {"error": positions}
    edits = []
    for ln, c in positions:
        new = new_name
        if binding is not None and binding[0].lineno <= ln <= binding[0].end_lineno:
            node, alias = binding
            if alias.asname is None:
                if "." in alias.name:
                    return {"error": "'%s' is imported as a package (import %s): please rename with 'import %s as name'" % (old, alias.name, alias.name)}
                new = old + " as " + new_name  # the imported name stays
        edits.append((ln, c, ln, c + len(old), new))
    if not edits:
        return {"error": "'%s' was not found" % old}
    new_text = _apply(text, edits)
    return {"text": new_text, "cursor": [line, start, len(new_name)], "count": len(edits), "jedi": bool(tools().get("jedi"))}


def _statements(tree):
    for node in ast.walk(tree):
        for field in ("body", "orelse", "finalbody"):
            block = getattr(node, field, None)
            if isinstance(block, list) and block and isinstance(block[0], ast.stmt):
                yield node, block
        if isinstance(node, ast.Try) or type(node).__name__ == "TryStar":
            for h in node.handlers:
                yield h, h.body
        if type(node).__name__ == "Match":
            for case in node.cases:
                yield case, case.body


def _first_line(stmt):
    decos = getattr(stmt, "decorator_list", None)
    return min([stmt.lineno] + [d.lineno for d in decos]) if decos else stmt.lineno


def _top_level(tree, line):
    for stmt in tree.body:
        if _first_line(stmt) <= line <= stmt.end_lineno:
            return stmt
    return None


def _indent(s):
    return s[:len(s) - len(s.lstrip())]


def refactor_extract_variable(text, path, line, col, end_line, end_col, name):
    err = _valid_name(name)
    if err:
        return {"error": err}
    if name in _names_in_file(text):
        return {"error": "'%s' is already used in this file" % name}
    tree, err = _parse(text)
    if err:
        return err
    lines = text.splitlines(True)
    start, end = _index(lines, line, col), _index(lines, end_line, end_col)
    selected = text[start:end]
    expr = selected.strip()
    if not expr:
        return {"error": "Please select an expression"}
    try:
        ast.parse("(" + expr + "\n)", mode="eval")
    except SyntaxError:
        return {"error": "The selection is not an expression"}
    # the innermost statement which contains the selection
    best = None
    for node in ast.walk(tree):
        if isinstance(node, ast.stmt) and node.lineno <= line and node.end_lineno >= end_line:
            if best is None or (node.lineno, -node.end_lineno) > (best.lineno, -best.end_lineno):
                best = node
    if best is None:
        return {"error": "The selection is not in a statement"}
    header = lines[best.lineno - 1]
    if header.lstrip().startswith("elif"):
        return {"error": "Not possible in the condition of 'elif'"}
    if isinstance(best, (ast.While,)) and line <= best.test.end_lineno:
        return {"error": "Not possible in the condition of 'while' (the value would be computed only once)"}
    if isinstance(best, (ast.FunctionDef, ast.AsyncFunctionDef, ast.ClassDef)) and line < best.body[0].lineno:
        return {"error": "Not possible in a definition of a function or class"}
    indent = _indent(header)
    lead = selected[:len(selected) - len(selected.lstrip())]
    trail = selected[len(selected.rstrip()):]
    new_text = text[:start] + lead + name + trail + text[end:]
    insert = _index(lines, best.lineno, 0)
    new_text = new_text[:insert] + indent + name + " = " + expr + "\n" + new_text[insert:]
    lead_lines = lead.count("\n")
    cur_col = len(lead.rsplit("\n", 1)[-1]) if lead_lines else col + len(lead)
    return {"text": new_text, "cursor": [line + 1 + lead_lines, cur_col, len(name)]}


class _Flow(ast.NodeVisitor):
    """The names of statements in the order of the evaluation: reads before the
    first write (the parameters), the writes; nested scopes only read."""

    def __init__(self):
        self.reads, self.writes, self.order = [], set(), []
        self.bad = None
        self.loop = 0

    def read(self, name):
        if name not in self.writes and name not in self.reads:
            self.reads.append(name)

    def write(self, name):
        self.writes.add(name)

    def visit_Name(self, node):
        if isinstance(node.ctx, ast.Load):
            self.read(node.id)
        else:
            self.write(node.id)

    def visit_Assign(self, node):
        self.visit(node.value)
        for t in node.targets:
            self.visit(t)

    def visit_AnnAssign(self, node):
        if node.value is not None:
            self.visit(node.value)
        self.visit(node.target)

    def visit_AugAssign(self, node):
        self.visit(node.value)
        if isinstance(node.target, ast.Name):
            self.read(node.target.id)
            self.write(node.target.id)
        else:
            self.visit(node.target)

    def visit_NamedExpr(self, node):
        self.visit(node.value)
        self.visit(node.target)

    def _loop(self, node, iterable, target):
        if iterable is not None:
            self.visit(iterable)
        if target is not None:
            self.visit(target)
        self.loop += 1
        for s in node.body:
            self.visit(s)
        self.loop -= 1
        for s in node.orelse:
            self.visit(s)

    def visit_For(self, node):
        self._loop(node, node.iter, node.target)

    visit_AsyncFor = visit_For

    def visit_While(self, node):
        self.visit(node.test)
        self._loop(node, None, None)

    def visit_With(self, node):
        for item in node.items:
            self.visit(item.context_expr)
            if item.optional_vars is not None:
                self.visit(item.optional_vars)
        for s in node.body:
            self.visit(s)

    def _nested(self, node):
        # a nested scope: its free names are read
        inner = set()
        for n in ast.walk(node):
            if isinstance(n, ast.Name) and not isinstance(n.ctx, ast.Load):
                inner.add(n.id)
            elif isinstance(n, ast.arg):
                inner.add(n.arg)
        for n in ast.walk(node):
            if isinstance(n, ast.Name) and isinstance(n.ctx, ast.Load) and n.id not in inner:
                self.read(n.id)

    def visit_FunctionDef(self, node):
        for d in node.decorator_list:
            self.visit(d)
        self._nested(node)
        self.write(node.name)

    visit_AsyncFunctionDef = visit_FunctionDef

    def visit_ClassDef(self, node):
        for d in node.decorator_list + node.bases:
            self.visit(d)
        self._nested(node)
        self.write(node.name)

    def visit_Lambda(self, node):
        self._nested(node)

    visit_ListComp = visit_SetComp = visit_DictComp = visit_GeneratorExp = visit_Lambda

    def visit_Import(self, node):
        for a in node.names:
            self.write(a.asname or a.name.split(".")[0])

    def visit_ImportFrom(self, node):
        for a in node.names:
            self.write(a.asname or a.name)

    def visit_Return(self, node):
        self.bad = "'return' in the selection"

    def visit_Yield(self, node):
        self.bad = "'yield' in the selection"

    visit_YieldFrom = visit_Yield

    def visit_Await(self, node):
        self.bad = "'await' in the selection"

    def visit_Global(self, node):
        self.bad = "'global' or 'nonlocal' in the selection"

    visit_Nonlocal = visit_Global

    def visit_Break(self, node):
        if self.loop == 0:
            self.bad = "'break' or 'continue' without its loop in the selection"

    visit_Continue = visit_Break


def _scope_locals(scope):
    """The local variables of a function or the module (assigned, the arguments; not imports, functions, classes)."""
    names = set()
    if isinstance(scope, (ast.FunctionDef, ast.AsyncFunctionDef)):
        a = scope.args
        for arg in a.posonlyargs + a.args + a.kwonlyargs + [x for x in (a.vararg, a.kwarg) if x]:
            names.add(arg.arg)
    stack = list(scope.body)
    while stack:
        n = stack.pop()
        if isinstance(n, _SCOPES):
            continue
        if isinstance(n, ast.Name) and not isinstance(n.ctx, ast.Load):
            names.add(n.id)
        stack.extend(ast.iter_child_nodes(n))
    return names


def refactor_extract_function(text, path, line, end_line, name):
    err = _valid_name(name)
    if err:
        return {"error": err}
    if name in _names_in_file(text):
        return {"error": "'%s' is already used in this file" % name}
    tree, err = _parse(text)
    if err:
        return err
    lines = text.splitlines(True)
    # the selected statements: complete statements of one block
    chosen = None
    for parent, block in _statements(tree):
        inside = [s for s in block if _first_line(s) >= line and s.end_lineno <= end_line]
        partial = [s for s in block if s not in inside and _first_line(s) <= end_line and s.end_lineno >= line]
        if inside and not partial:
            chosen = (parent, inside)
            break
    if chosen is None:
        return {"error": "Please select complete statements (of one block)"}
    parent, stmts = chosen
    if isinstance(parent, ast.ClassDef):
        return {"error": "Not possible in the body of a class"}
    first, last = _first_line(stmts[0]), stmts[-1].end_lineno
    # the scope (function or module) of the statements
    scope = tree
    for node in ast.walk(tree):
        if isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef)) and node.body[0].lineno <= first and last <= node.end_lineno:
            if scope is tree or node.lineno > scope.lineno:
                scope = node
    flow = _Flow()
    for s in stmts:
        flow.visit(s)
    if flow.bad:
        return {"error": "Not possible: " + flow.bad}
    local = _scope_locals(scope)
    params = [n for n in flow.reads if n in local]
    # the results: written names which are read after the selection (or elsewhere at module level)
    after = set()
    for node in ast.walk(scope if scope is not tree else tree):
        if isinstance(node, ast.Name) and isinstance(node.ctx, ast.Load):
            if node.lineno > last or (scope is tree and node.lineno < first):
                after.add(node.id)
    results = sorted(n for n in flow.writes if n in after and n in (local | {s.name for s in stmts if hasattr(s, "name")}))
    # the body: the selected lines (dedented), the return
    body_lines = lines[first - 1:last]
    base = min((len(_indent(x)) for x in body_lines if x.strip()), default=0)
    body = "".join(("    " + x[base:]) if x.strip() else "\n" for x in body_lines)
    if not body.endswith("\n"):
        body += "\n"
    if results:
        body += "    return " + ", ".join(results) + "\n"
    definition = "def " + name + "(" + ", ".join(params) + "):\n" + body + "\n\n"
    call = _indent(lines[first - 1]) + (", ".join(results) + " = " if results else "") + name + "(" + ", ".join(params) + ")\n"
    # the new function before the statement of the module which contains the selection
    top = _top_level(tree, first)
    top_line = _first_line(top)
    # comments directly above the statement stay with it
    while top_line > 1 and lines[top_line - 2].lstrip().startswith("#"):
        top_line -= 1
    end_index = _index(lines, last, 0) + len(lines[last - 1])
    new_text = text[:_index(lines, first, 0)] + call + text[end_index:]
    insert = _index(lines, top_line, 0)
    # two blank lines before the new function (PEP 8)
    blank = 0
    while top_line - 1 - blank >= 1 and not lines[top_line - 2 - blank].strip():
        blank += 1
    if top_line > 1 and blank < 2:
        definition = "\n" * (2 - blank) + definition
    new_text = new_text[:insert] + definition + new_text[insert:]
    def_lines = definition.count("\n")
    top_line += (2 - blank) if top_line > 1 and blank < 2 else 0
    return {"text": new_text, "cursor": [top_line, 4, len(name)] if top_line < first else [first + def_lines, len(_indent(lines[first - 1])), len(name)],
            "params": params, "results": results}


_SIMPLE = (ast.Name, ast.Constant, ast.Attribute, ast.Subscript, ast.Call, ast.List, ast.Dict, ast.Set, ast.ListComp, ast.SetComp,
           ast.DictComp, ast.GeneratorExp, ast.JoinedStr)


def refactor_inline(text, path, line, col):
    tree, err = _parse(text)
    if err:
        return err
    name, start = _name_at(text, line, col)
    if name is None:
        return {"error": "No variable at the cursor"}
    scope = tree
    for node in ast.walk(tree):
        if isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef)) and node.body[0].lineno <= line <= node.end_lineno:
            if scope is tree or node.lineno > scope.lineno:
                scope = node
    stores, loads = [], []
    stack = [(c, False) for c in scope.body]
    while stack:
        n, nested = stack.pop()
        if isinstance(n, ast.Name) and n.id == name:
            (loads if isinstance(n.ctx, ast.Load) else stores).append((n, nested))
        elif isinstance(n, (ast.Global, ast.Nonlocal)) and name in n.names:
            return {"error": "'%s' is declared global or nonlocal" % name}
        elif isinstance(n, ast.arg) and n.arg == name and not nested:
            return {"error": "'%s' is a parameter" % name}
        nest = nested or isinstance(n, _SCOPES)
        stack.extend((c, nest) for c in ast.iter_child_nodes(n))
    own = [n for n, nested in stores if not nested]
    if len(own) != 1:
        return {"error": "'%s' must be assigned exactly once (%d assignments)" % (name, len(own))}
    target = own[0]
    if any(nested for _, nested in stores):
        return {"error": "'%s' is assigned in a nested function too" % name}
    assign = None
    for node in ast.walk(scope):
        if isinstance(node, ast.Assign) and len(node.targets) == 1 and node.targets[0] is target:
            assign = node
    if assign is None:
        return {"error": "'%s' is not assigned with 'name = value'" % name}
    uses = [n for n, _ in loads]
    if not uses:
        return {"error": "'%s' is not used" % name}
    if any((n.lineno, n.col_offset) < (assign.end_lineno, assign.end_col_offset) for n in uses):
        return {"error": "'%s' is used before the assignment" % name}
    value = ast.get_source_segment(text, assign.value)
    v = assign.value
    simple = isinstance(v, _SIMPLE) or isinstance(v, ast.Tuple) and value.startswith("(")
    always = isinstance(v, (ast.Tuple, ast.NamedExpr)) and not simple
    parent = {}
    for node in ast.walk(scope):
        for c in ast.iter_child_nodes(node):
            parent[c] = node

    def wrapped(use):
        """Parentheses only where the value is part of an operation (e.g., not as an argument)."""
        if simple:
            return value
        p = parent.get(use)
        safe = (isinstance(p, ast.Call) and use in p.args or isinstance(p, ast.keyword) or isinstance(p, (ast.List, ast.Set))
                or isinstance(p, (ast.Assign, ast.AnnAssign, ast.Return, ast.Expr)) and getattr(p, "value", None) is use
                or isinstance(p, ast.Subscript) and p.slice is use or isinstance(p, ast.FormattedValue))
        return value if safe and not always else "(" + value + ")"

    source = Source(text)
    edits = []
    for n in uses:
        ln, c, eln, ec = source.span(n)
        edits.append((ln, c, eln, ec, wrapped(n)))
    rm = remove_statement_edit(assign, source)
    edits.append((rm["line"], rm["col"], rm["endLine"], rm["endCol"], rm["text"]))
    new_text = _apply(text, edits)
    return {"text": new_text, "count": len(uses), "cursor": [assign.lineno, 0, 0]}


def refactor_surround(text, path, line, end_line):
    lines = text.splitlines(True)
    if not lines:
        return {"error": "No code"}
    end_line = min(end_line, len(lines))
    block = lines[line - 1:end_line]
    if not any(x.strip() for x in block):
        return {"error": "Please select one or more lines"}
    indent = min((_indent(x) for x in block if x.strip()), key=len)
    body = "".join(("    " + x) if x.strip() else x for x in block)
    if not body.endswith("\n"):
        body += "\n"
    new_block = indent + "try:\n" + body + indent + "except Exception as e:\n" + indent + "    print(e)\n"
    new_text = "".join(lines[:line - 1]) + new_block + "".join(lines[end_line:])
    ok_before = _parse(text)[0] is not None
    if ok_before and _parse(new_text)[0] is None:
        return {"error": "Please select complete statements"}
    print_line = line + len(block) + 2
    return {"text": new_text, "cursor": [print_line, len(indent) + 4, 8]}


def refactor_docstring(text, path, line):
    tree, err = _parse(text)
    if err:
        return err
    target, parent_of = None, {}
    for node in ast.walk(tree):
        for c in ast.iter_child_nodes(node):
            parent_of[c] = node
    for node in ast.walk(tree):
        if isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef, ast.ClassDef)) and _first_line(node) <= line <= node.end_lineno:
            if target is None or node.lineno > target.lineno:
                target = node
    if target is None:
        return {"error": "The cursor is not in a function or class"}
    if ast.get_docstring(target) is not None:
        return {"error": "'%s' has a docstring" % target.name}
    first = target.body[0]
    if first.lineno == target.lineno:
        return {"error": "The body is on the line of the definition"}
    lines = text.splitlines(True)
    first_line = _first_line(first)
    indent = _indent(lines[first_line - 1])
    out = [indent + '"""Summary.\n']
    if isinstance(target, (ast.FunctionDef, ast.AsyncFunctionDef)):
        a = target.args
        args = a.posonlyargs + a.args
        if args and isinstance(parent_of.get(target), ast.ClassDef) and args[0].arg in ("self", "cls"):
            args = args[1:]
        params = [(x.arg, x.annotation) for x in args]
        if a.vararg:
            params.append(("*" + a.vararg.arg, a.vararg.annotation))
        params += [(x.arg, x.annotation) for x in a.kwonlyargs]
        if a.kwarg:
            params.append(("**" + a.kwarg.arg, a.kwarg.annotation))
        if params:
            out += ["\n", indent + "Parameters\n", indent + "----------\n"]
            for p, ann in params:
                out.append(indent + p + (" : " + ast.unparse(ann) if ann is not None else "") + "\n")
                out.append(indent + "    Description.\n")
        returns = False
        stack = list(target.body)
        while stack:
            n = stack.pop()
            if isinstance(n, _SCOPES):
                continue
            if isinstance(n, ast.Return) and n.value is not None:
                returns = True
            stack.extend(ast.iter_child_nodes(n))
        if returns or target.returns is not None:
            out += ["\n", indent + "Returns\n", indent + "-------\n"]
            out.append(indent + (ast.unparse(target.returns) if target.returns is not None else "value") + "\n")
            out.append(indent + "    Description.\n")
    out.append(indent + '"""\n')
    insert = _index(lines, first_line, 0)
    new_text = text[:insert] + "".join(out) + text[insert:]
    return {"text": new_text, "cursor": [first_line, len(indent) + 3, len("Summary.")]}


def _module_group(name, level, folder):
    if level > 0:
        return 3
    top = name.split(".")[0]
    if top == "__future__":
        return 0
    std = getattr(sys, "stdlib_module_names", None)
    if std is not None:
        if top in std:
            return 1
    else:
        try:
            spec = importlib.util.find_spec(top)
            origin = (spec.origin or "") if spec else ""
            if spec and (origin in ("built-in", "frozen") or ("site-packages" not in origin and "dist-packages" not in origin and origin.startswith(sys.base_prefix))):
                return 1
        except Exception:
            pass
    if folder and (os.path.exists(os.path.join(folder, top + ".py")) or os.path.isdir(os.path.join(folder, top))):
        return 3
    return 2


def refactor_imports(text, path):
    if tools().get("ruff"):
        out = subprocess.run([sys.executable, "-m", "ruff", "check", "--select", "I,F401", "--fix-only", "--no-cache", "--exit-zero",
                              "--stdin-filename", path or "script.py", "-"], input=text, capture_output=True, text=True, timeout=60)
        if out.returncode == 0 and out.stdout:
            return {"text": out.stdout, "tool": "ruff"}
    tree, err = _parse(text)
    if err:
        return err
    # without ruff: the imports at the start of the module (one line each) sorted in groups
    body = tree.body
    i = 1 if body and isinstance(body[0], ast.Expr) and isinstance(getattr(body[0], "value", None), ast.Constant) and isinstance(body[0].value.value, str) else 0
    block = []
    while i < len(body) and isinstance(body[i], (ast.Import, ast.ImportFrom)) and body[i].lineno == body[i].end_lineno:
        block.append(body[i])
        i += 1
    if not block:
        return {"error": "No imports at the start of the module"}
    lines = text.splitlines(True)
    folder = os.path.dirname(os.path.abspath(path)) if path else None
    for s in block:
        rest = lines[s.lineno - 1][s.end_col_offset:].strip()
        if rest and not rest.startswith("#") or s.col_offset:
            return {"error": "The imports are not on lines of their own"}
    entries = []
    for s in block:
        code = lines[s.lineno - 1].strip()
        if isinstance(s, ast.Import):
            entries.append((_module_group(s.names[0].name, 0, folder), 0, s.names[0].name.lower(), code))
        else:
            entries.append((_module_group(s.module or "", s.level, folder), 1, ("." * s.level + (s.module or "")).lower(), code))
    groups = {}
    for g, kind, key, code in sorted(set(entries)):
        groups.setdefault(g, []).append(code)
    new_block = "\n\n".join("\n".join(v) for _, v in sorted(groups.items())) + "\n"
    start, end = block[0].lineno, block[-1].end_lineno
    new_text = "".join(lines[:start - 1]) + new_block + "".join(lines[end:])
    return {"text": new_text, "tool": "own", "message": "Sorted and grouped (install ruff to remove unused imports too)"}


def refactor(req):
    kind, text, path = req["kind"], req["source"], req.get("path")
    if kind == "rename":
        return refactor_rename(text, path, req["line"], req["col"], req["name"])
    if kind == "extract_variable":
        return refactor_extract_variable(text, path, req["line"], req["col"], req["endLine"], req["endCol"], req["name"])
    if kind == "extract_function":
        return refactor_extract_function(text, path, req["line"], req["endLine"], req["name"])
    if kind == "inline":
        return refactor_inline(text, path, req["line"], req["col"])
    if kind == "surround":
        return refactor_surround(text, path, req["line"], req["endLine"])
    if kind == "docstring":
        return refactor_docstring(text, path, req["line"])
    if kind == "imports":
        return refactor_imports(text, path)
    if kind == "name_at":
        name, _ = _name_at(text, req["line"], req["col"])
        return {"name": name}
    return {"error": "unknown refactoring " + str(kind)}


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
            elif cmd == "refactor":
                result = refactor(req)
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
