# com.eco.bio7.pyffm: embedded Python with the Foreign Function and Memory API

Loads an existing CPython installation (3.8 or later, built with a shared
library: python.org, Homebrew, conda, the Linux distributions, the Python of
the Xcode tools) into the Java process and calls it with the Foreign Function
and Memory API of Java 22+. No JNI, no compiled native code, no Python package
to install. The bundle depends only on the JDK (no Eclipse, no SWT).

## Requirements

* Java 22 or later, started with `--enable-native-access=ALL-UNNAMED`.
* A Python executable: its shared library (`libpython3.x.dylib/.so`,
  `python3x.dll`) and its module paths are used (also of a venv or conda
  environment). numpy, pandas and matplotlib are optional.

## Usage

```java
PySession py = PySession.start(PyStartOptions.of(Path.of("/opt/homebrew/bin/python3"))
        .withConsole((text, error) -> System.out.print(text)));
py.assign("x", 1.5, 2.5, 3.0);                       // numpy array (a list without numpy)
py.assign("m", new double[][] { { 1, 2 }, { 3, 4 } }); // 2-d numpy array
PyValue mean = py.eval("float(x.mean())");
py.exec("import pandas as pd\ndf = pd.DataFrame({'a': x})");
PyValue.Frame df = (PyValue.Frame) py.get("df");
py.console("print(x)");    // like typed in the Python console (blocks over several lines)
py.runFile(Path.of("script.py"));
py.interrupt();            // KeyboardInterrupt, like Ctrl+C
```

`PyValue` is the data model: `Array` (numeric and boolean arrays with dtype and
shape, scalars have the shape `{}`), `Str`, `PyList`, `PyDict`, `Frame` (pandas
DataFrame), `None`, `Other`. Arrays are copied once between Java and Python
(10 million doubles: about 20 ms).

## Plot device

matplotlib draws with the backend `module://bio7_backend`: after a console
line, a script or `plt.show()` the new and changed figures are drawn in the
size of the plot view and delivered as RGBA images to a `PyPlotListener`.
`resizePlot` draws a figure again in a new size, `saveFigure` saves it
(png, svg, pdf).

## Calls from Python

The module `bio7` calls functions of the application (`PyCallHandler`):
`bio7.call(name, *args, value=...)`.

## Implementation

Only a few functions of the C API are called (`Py_InitializeEx`,
`PyRun_SimpleStringFlags`, `PyGILState_Ensure/Release`, `PyEval_SaveThread`,
`PyThreadState_SetAsyncExc`); they exist in all versions of Python 3. The data
and the code are exchanged by the bootstrap module `_bio7`
(`internal/bio7_bootstrap.py`) through native memory (ctypes) and upcalls to
Java. On Linux and macOS the library is loaded with `RTLD_GLOBAL` (the extension
modules, e.g. numpy, find the symbols of Python).

## Limits of embedding

* One Python per process; it can't be stopped and started again.
* A crash of an extension module ends the process.
* Python code runs in one thread; other threads wait for it.
* `exit()` raises an error (it would end the process), `input()` is not
  available, Python installs no signal handlers.
