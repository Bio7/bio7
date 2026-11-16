# Descriptive statistics of the selected objects (numbers: count, mean, std, quantiles)
import numpy as np
import pandas as pd

for name in bio7_shell_vars:
    obj = globals()[name]
    print("\n" + name)
    if isinstance(obj, (pd.DataFrame, pd.Series)):
        print(obj.describe(include="all"))
    else:
        try:
            print(pd.Series(np.asarray(obj).ravel()).describe())
        except Exception as e:
            print("  not supported:", type(obj).__name__, e)
