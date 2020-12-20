import com.eco.bio7.console.Bio7Console;
import com.eco.bio7.rcp.StartBio7Utils;
import  com.eco.bio7.Bio7Plugin;
import org.eclipse.jface.preference.IPreferenceStore
/*Sleep for a second before switch to R because Console runs in parallel and can't be controlled!*/
Thread.sleep(1000);
/*Activate the Bio7 console. PyDev opens a separate console!*/
StartBio7Utils.getConsoleInstance().cons.getConsole().activate();
/*Get the Python path from the preferences or if empty call Python interpreter without a path!*/
IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
String pathPython = store.getString("python_pipe_path");
/*Change the path sep. for all OS!*/
pathPython=pathPython.replace("\\", "/");
if(pathPython.isEmpty()==false) {
	pathPython=pathPython+"/python -c \"import pyRserve; conn = pyRserve.connect(); conn.shutdown()\""
}
else {
	pathPython=pathPython+"python -c \"import pyRserve; conn = pyRserve.connect(); conn.shutdown()\""
}
println pathPython.execute().text
/*Write a linebreak!*/
Bio7Console.write("\n", true, false);
println "Disconnected pyRserve from RServe!"
