# A histogram of each selected object (or numeric column of a DataFrame) in one figure
import math
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt

series = {}
for name in bio7_shell_vars:
    obj = globals()[name]
    if isinstance(obj, pd.DataFrame):
        for c in obj.select_dtypes("number"):
            series[name + "['" + str(c) + "']"] = obj[c].dropna().to_numpy()
    else:
        series[name] = np.asarray(obj, dtype=float).ravel()

if series:
    n = len(series)
    cols = min(n, 3)
    rows = math.ceil(n / cols)
    fig, axes = plt.subplots(rows, cols, figsize=(4 * cols, 3 * rows), squeeze=False)
    for ax, (label, values) in zip(axes.flat, series.items()):
        ax.hist(values[np.isfinite(values)], bins=30, color="#2B5C8F", edgecolor="white")
        ax.set_title(label, loc="left", fontweight="bold")
        ax.spines[["top", "right"]].set_visible(False)
    for ax in list(axes.flat)[n:]:
        ax.set_visible(False)
    fig.tight_layout()
    plt.show()
else:
    print("Please select one or more objects!")
