# Combines the selected objects (vectors, Series, DataFrames) to the DataFrame 'combined'
# (the columns side by side). The names of the selected objects are in 'bio7_shell_vars'.
import numpy as np
import pandas as pd

parts = []
for name in bio7_shell_vars:
    obj = globals()[name]
    if isinstance(obj, pd.DataFrame):
        parts.append(obj.reset_index(drop=True))
    else:
        arr = np.asarray(obj)
        if arr.ndim == 2:
            parts.append(pd.DataFrame(arr).add_prefix(name + "_"))
        else:
            parts.append(pd.Series(arr.ravel(), name=name))
combined = pd.concat(parts, axis=1) if parts else pd.DataFrame()
print("combined:", combined.shape)
