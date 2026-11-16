# The correlation matrix (Pearson) of the selected numeric objects or the columns of a DataFrame
import numpy as np
import pandas as pd

cols = {}
for name in bio7_shell_vars:
    obj = globals()[name]
    if isinstance(obj, pd.DataFrame):
        for c in obj.select_dtypes("number"):
            cols[name + "." + str(c)] = obj[c].to_numpy()
    else:
        cols[name] = np.asarray(obj, dtype=float).ravel()
correlation = pd.DataFrame({k: pd.Series(v) for k, v in cols.items()}).corr()
print(correlation.round(3))
