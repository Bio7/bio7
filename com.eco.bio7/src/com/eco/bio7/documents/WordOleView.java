package com.eco.bio7.documents;
import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.ole.win32.OLE;
import org.eclipse.swt.ole.win32.OleClientSite;
import org.eclipse.swt.ole.win32.OleFrame;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.collection.CustomView;

public class WordOleView extends com.eco.bio7.compile.Model {

	public static final String ID = "ExcelSheet";

	private static OleFrame frame;
	protected static OleClientSite clientSite;
	private static Composite parent;
	private CustomView view; 

	public WordOleView(String file) {
		try {
			view = new CustomView();
			parent = view.getComposite("Display");
			Display dis = parent.getDisplay();

			dis.asyncExec(new Runnable() {

				public void run() {

					try {
						frame = new OleFrame(parent, SWT.NONE);
						clientSite = new OleClientSite(frame, SWT.NONE, "Word.Document");
						clientSite.doVerb(OLE.OLEIVERB_INPLACEACTIVATE);
						fileOpen(file);
					} catch (SWTError e) {
						System.out.println("Unable to open activeX control");

						return;
					}

					clientSite.setFocus();

				}
			});

		} catch (SWTError e) {
			System.out.println("Unable to open activeX control");
			return;
		}

	}

	public void close() {
		Display dis = parent.getDisplay();

		dis.syncExec(new Runnable() {

			public void run() {
				String save = Bio7Dialog.saveFile();
				if (save != null) {
					if (clientSite.save(new File(save), true)) {
						System.out.println("Saved");
					} else {
						System.out.println("Not saved");
					}
				}
			}
		});

	}

	public static void fileOpen(String file) {
		if (file != null) {
			clientSite.dispose();
			clientSite = new OleClientSite(frame, SWT.NONE, "Word.Document", new File(file));
			clientSite.doVerb(OLE.OLEIVERB_INPLACEACTIVATE);
		}

	}

}