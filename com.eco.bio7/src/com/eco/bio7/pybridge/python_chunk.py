# Copyright (c) 2007-2026 M. Austenfeld
# SPDX-License-Identifier: EPL-2.0
#
# The Python chunks of the markdown editor of Bio7 (```{python}): the code runs
# in __main__ (as in the console), the output is collected and the matplotlib
# figures of the chunk are saved as PNG files (shown below the chunk).

import sys
import io
import os
import ast
import traceback
import contextlib

_main = sys.modules["__main__"]


def _is_artist(v):
    """A matplotlib object (e.g., the lines of plt.plot()): not printed."""
    if isinstance(v, (list, tuple)) and v:
        return all(_is_artist(x) for x in v)
    return type(v).__module__.startswith("matplotlib")


def _figures():
    if "matplotlib.pyplot" not in sys.modules:
        return {}
    from matplotlib._pylab_helpers import Gcf
    return {m.num: m.canvas.figure for m in Gcf.get_all_fig_managers()}


def run(code, folder, width, height, dpi):
    """Runs the code of a chunk; returns 'error' or 'ok'.

    The output is written to folder/out.txt, the figures to folder/plotNNN.png
    (width and height in points of the editor, dpi for HiDPI).
    """
    bio7 = sys.modules["_bio7"]
    before = _figures()
    out = io.StringIO()
    error = False
    # plt.show() in a chunk: the figure is shown below the chunk, not in the plot view
    show_all, render = bio7._show_all, bio7._render
    bio7._show_all = lambda *a, **k: None
    bio7._render = lambda *a, **k: False
    try:
        with contextlib.redirect_stdout(out), contextlib.redirect_stderr(out):
            try:
                tree = ast.parse(code, "<chunk>", "exec")
                last = None
                if tree.body and isinstance(tree.body[-1], ast.Expr):
                    last = ast.Expression(tree.body.pop().value)
                exec(compile(tree, "<chunk>", "exec"), _main.__dict__)
                if last is not None:
                    value = eval(compile(last, "<chunk>", "eval"), _main.__dict__)
                    if value is not None and not _is_artist(value):
                        _main.__dict__["_"] = value
                        print(repr(value))
            except SystemExit:
                print("exit() is not available in the embedded Python of Bio7")
                error = True
            except SyntaxError as e:
                # Only the line of the chunk (not the frames of the parser)
                print("".join(traceback.format_exception_only(type(e), e)), end="")
                error = True
            except BaseException as e:
                tb = e.__traceback__.tb_next if e.__traceback__ is not None else None
                traceback.print_exception(type(e), e, tb)
                print(bio7._missing_hint(e), end="")
                error = True
    finally:
        bio7._show_all, bio7._render = show_all, render
    # The new figures and the changed figures of the plot view
    after = _figures()
    plots = [n for n, f in after.items() if n not in before or before[n] is not f or f.stale]
    if plots:
        import matplotlib.pyplot as plt
        default = tuple(plt.rcParams["figure.figsize"])
        for i, n in enumerate(sorted(plots)):
            fig = after[n]
            try:
                new = n not in before
                if new and tuple(fig.get_size_inches()) == default:
                    # Without an explicit figsize: the size of the plots of the editor
                    fig.set_size_inches(width / 96.0, height / 96.0)
                fig.savefig(os.path.join(folder, "plot%03d.png" % (i + 1)), dpi=dpi, facecolor="white")
                if new:
                    # As in a notebook: the figure belongs to the chunk
                    plt.close(fig)
            except Exception:
                out.write(traceback.format_exc())
        try:
            # The changed figures of the plot view are drawn again
            bio7._flush()
        except Exception:
            pass
    with open(os.path.join(folder, "out.txt"), "w", encoding="utf-8") as f:
        f.write(out.getvalue())
    return "error" if error else "ok"
