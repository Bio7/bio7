/*
 * Starts an Rserve server for Python (pyRserve) with the R of the Bio7 console
 * (embedded R or native R). The server runs as a separate process on the port
 * 6312 (Bio7 itself uses 6311 for its Rserve connection).
 *
 * In Python:
 *   import pyRserve
 *   conn = pyRserve.connect(port=6312)
 *   conn.eval("summary(rnorm(100))")
 *   conn.eval("x <- c(1, 2, 3)")
 *   conn.r.x        # the vector x of R
 *
 * Python and pyRserve: Preferences Python ("Detect Python...") and
 * "pip install pyRserve". Stop the server with Stop_PyRserve.
 */
import com.eco.bio7.Bio7Plugin
import com.eco.bio7.console.Bio7Console
import com.eco.bio7.console.PythonFinder

int port = 6312

/* Is pyRserve available in the Python of the preferences (or a found one)? */
String python = PythonFinder.executable(Bio7Plugin.getDefault().getPreferenceStore().getString("python_pipe_path"))
try {
	Process p = [python, "-c", "import pyRserve"].execute()
	p.waitForOrKill(10000)
	if (p.exitValue() != 0) {
		println "pyRserve is not installed in " + python + ". Install it with:\n  " + python + " -m pip install pyRserve"
	}
} catch (Exception e) {
	println "Python was not found (" + python + "). Select it in the preferences (Preferences Python: 'Detect Python...')."
}

/* Starts Rserve as a separate process (doesn't block the R of the console) */
Bio7Console.setConsoleSelection("R")
Bio7Console.write("if (requireNamespace('Rserve', quietly = TRUE)) Rserve::Rserve(port = " + port + ", args = '--no-save') else message('Please install the package Rserve: install.packages(\"Rserve\")')", true, false)
println "Rserve for pyRserve: port " + port + "\nIn Python: import pyRserve; conn = pyRserve.connect(port=" + port + ")"
