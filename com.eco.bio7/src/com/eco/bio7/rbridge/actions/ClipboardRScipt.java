package com.eco.bio7.rbridge.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.console.ConsolePageParticipant;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.RServeUtil;
import com.eco.bio7.util.Util;

public class ClipboardRScipt extends Action {

	protected String data;

	public ClipboardRScipt(String text, IWorkbenchWindow window) {
		super(text);

		setId("com.eco.bio7.rserve_clipboardR");
		setActionDefinitionId("com.eco.bio7.execute_r_clipboard");
	}

	public void run() {
		// IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		// boolean rPipe = store.getBoolean("r_pipe");

		if (RServe.getConnection() == null) {
			String selectionConsole = ConsolePageParticipant.getInterpreterSelection();
			if (selectionConsole.equals("R")) {
				ConsolePageParticipant.pipeInputToConsole("fileClipboardTemp<-file(\"clipboard\", open=\"r\");source(fileClipboardTemp,echo=T);close(fileClipboardTemp);remove(fileClipboardTemp)", true, true);
				System.out.println();
			} else {
				Bio7Dialog.message("Please start the \"Native R\" shell in the Bio7 console!");
			}

		}

		else {

			Display dis = Util.getDisplay();
			dis.syncExec(new Runnable() {

				public void run() {

					Clipboard cb = new Clipboard(Util.getDisplay());

					TextTransfer transfer = TextTransfer.getInstance();
					data = (String) cb.getContents(transfer);
				}
			});
			data = data.replace("\r", "");
			
			if (data != null) {
				RServeUtil.evalR(data, null);
				RServeUtil.listRObjects();
			}

		}

	}

}