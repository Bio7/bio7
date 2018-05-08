import com.eco.bio7.batch.Bio7Dialog;

import py4j.GatewayServer;

public class Start_Python_Server extends com.eco.bio7.compile.Model {

	private GatewayServer server=null;

	public Start_Python_Server() {
		

		if (server == null) {
			server = new GatewayServer(this);
			server.start();
			Bio7Dialog
					.message("Started Python Server (Py4J) connection!\n\nInvoke the 'Setup' action in the Bio7\ntoolbar to close or restart the server!");
		}
	}

	public void setup() {
		

		if (server == null) {
			server = new GatewayServer(this);
			server.start();
			Bio7Dialog.message("Started Python connection!");
		} else {
			server.shutdown();
			server = null;
			Bio7Dialog.message("Shutdown of Python connection!");
		}

	}
}