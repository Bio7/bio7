/*
 * Stops the Rserve server for Python started with Start_PyRserve (port 6312).
 */
import com.eco.bio7.Bio7Plugin
import com.eco.bio7.console.PythonFinder

int port = 6312

String python = PythonFinder.executable(Bio7Plugin.getDefault().getPreferenceStore().getString("python_pipe_path"))
try {
	/* The arguments as a list: the code with spaces stays one argument */
	Process p = new ProcessBuilder(python, "-c", "import pyRserve; conn = pyRserve.connect(port=" + port + "); conn.shutdown()").redirectErrorStream(true).start()
	p.waitForOrKill(15000)
	String out = p.text.trim()
	if (p.exitValue() == 0) {
		println "Rserve for pyRserve (port " + port + ") was stopped."
	} else {
		println "Rserve for pyRserve could not be stopped (not running or pyRserve not installed):\n" + out
	}
} catch (Exception e) {
	println "Python was not found (" + python + "). Select it in the preferences (Preferences Python: 'Detect Python...')."
}
