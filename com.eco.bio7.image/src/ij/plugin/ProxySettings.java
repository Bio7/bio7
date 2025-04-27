package ij.plugin;
import ij.*;
import ij.gui.*;
import ij.util.Tools;
import java.util.*;

/** This plugin implements the Plugins/Utilities/Proxy Settings command. It sets
* 	the JVM proxy properties to allow the Help/Update ImageJ command
*	and File/Open Samples menu to work on networks behind a proxy server. 
* 
*     @author	Dimiter Prodanov
*/
public class ProxySettings implements PlugIn {
	private Properties props = System.getProperties();
	private String proxyhost = Prefs.get("proxy.server", "");
	private int proxyport = (int)Prefs.get("proxy.port", 8080);
	
	public void run(String arg) {
		if (IJ.getApplet()!=null) return;
		String host = System.getProperty("http.proxyHost");
		if (host!=null) proxyhost = host;
		String port = System.getProperty("http.proxyPort");
		if (port!=null) {
			double portNumber = Tools.parseDouble(port);
			if (!Double.isNaN(portNumber))
				proxyport = (int)portNumber;
		}
		if (!showDialog()) return;
		if (!proxyhost.equals(""))
			props.put("proxySet", "true");
		else
			props.put("proxySet", "false");
		props.put("http.proxyHost", proxyhost);
		props.put("http.proxyPort", ""+proxyport);
		Prefs.set("proxy.server", proxyhost);
		Prefs.set("proxy.port", proxyport);
		String httpsHost = System.getProperty("https.proxyHost");
		if (httpsHost == null) {
			httpsHost = proxyhost;
		}
		int httpsPort = proxyport;
		String httpsSystemPort = System.getProperty("https.proxyPort");
		if (httpsSystemPort != null) {
			double portNumber = Tools.parseDouble(httpsSystemPort);
			if (!Double.isNaN(portNumber))
				httpsPort = (int)portNumber;
		}
		props.put("https.proxyHost", httpsHost);
		props.put("https.proxyPort", ""+httpsPort);
		try {
			System.setProperty("java.net.useSystemProxies", Prefs.useSystemProxies?"true":"false");
		} catch(Exception e) {}
		if (IJ.debugMode)
			logProperties();
	}
	
	public void logProperties() {
		IJ.log("proxy set: "+ System.getProperty("proxySet"));
		IJ.log("http proxy host: "+ System.getProperty("http.proxyHost"));
		IJ.log("http proxy port: "+System.getProperty("http.proxyPort"));
		IJ.log("https proxy host: "+ System.getProperty("https.proxyHost"));
		IJ.log("https proxy port: "+System.getProperty("https.proxyPort"));
		IJ.log("java.net.useSystemProxies: "+System.getProperty("java.net.useSystemProxies"));
	}

	boolean showDialog()   {
		GenericDialog gd=new GenericDialog("Proxy Settings");
		gd.addStringField("Proxy server:", proxyhost, 15);
		gd.addNumericField("Port:", proxyport , 0);
		gd.addCheckbox("Or, use system proxy settings", Prefs.useSystemProxies);
		gd.addHelp(IJ.URL2+"/docs/menus/edit.html#proxy");
		gd.showDialog();
		if (gd.wasCanceled())
			return false;
		proxyhost = gd.getNextString();
		proxyport = (int)gd.getNextNumber();
		Prefs.useSystemProxies = gd.getNextBoolean();
		return true;
	}

}