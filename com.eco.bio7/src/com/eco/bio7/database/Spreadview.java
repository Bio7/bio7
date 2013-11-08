package com.eco.bio7.database;

import java.io.File;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.JRootPane;
import javax.swing.JScrollPane;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import com.eco.bio7.actions.SafeSaveDialog;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.discrete.Field;
import com.eco.bio7.javaeditors.JavaEditor;
import com.eco.bio7.jobs.LoadData;
import com.eco.bio7.jobs.LoadWorkspaceJob;
import com.eco.bio7.jobs.SaveWorkspaceJob;
import com.eco.bio7.methods.Compiled;
import com.eco.bio7.rbridge.RState;
import com.swtdesigner.ResourceManager;

public class Spreadview extends ViewPart {
	private java.awt.Frame frame;

	public static final String ID = "com.eco.bio7.spreadsheet";
	
	private Action addStateAction;

	private Action removeStateAction;

	private Action openStateSetup;

	private Action saveStateSetup;

	private ArrayList list = new ArrayList();

	private String value = null;

	public Text txt;

	protected SaveWorkspaceJob save;

	private String fileName;

	private String fileSource;

	public Spreadview() {
		super();

	}

	public void createPartControl(Composite parent) {
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "com.eco.bio7.spreadsheet");
		createActions();
		initializeToolBar();
		new StateTable(parent);

	}

	public void setFocus() {

	}

	private void initializeToolBar() {
		IToolBarManager toolbarManager = getViewSite().getActionBars().getToolBarManager();
		toolbarManager.add(new TextItem());
		toolbarManager.add(addStateAction);
		toolbarManager.add(removeStateAction);
		toolbarManager.add(saveStateSetup);
		toolbarManager.add(openStateSetup);

	}

	private void createActions() {

		saveStateSetup = new Action("Save") {
			public void run() {
				boolean saveSource;
				MessageBox message = new MessageBox(new Shell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
				message.setMessage("Save source from opened Java editor?");
				message.setText("Bio7");
				int response = message.open();
				if (response == SWT.YES) {
					saveSource = true;

				} else {
					saveSource = false;

				}

				Shell shell = new Shell(SWT.ON_TOP);
				SafeSaveDialog fd = new SafeSaveDialog(shell);
				fd.setText("Save Pattern");

				String[] filterExt = { "*.exml", "*." };
				fd.setFilterExtensions(filterExt);
				String selected = fd.open();
				boolean success = false;

				if (selected != null) {
					if (new File(selected).exists()) {
						success = (new File(selected)).delete();
						if (success) {
							System.out.println("File deleted");
							if (saveSource) {
								saveJavaSource();
								save = new SaveWorkspaceJob(selected, fileSource,fileName);
								save.setUser(true);
								save.schedule();
								fileSource=null;
							} else {
								save = new SaveWorkspaceJob(selected, null,null);
								save.setUser(true);
								save.schedule();
							}

						} else {

							Bio7Dialog.message("Couldn't delete file");
						}
					} else {
						saveJavaSource();
						save = new SaveWorkspaceJob(selected, fileSource,fileName);
						save.setUser(true);
						save.schedule();
						fileSource=null;
					}

				}

			}
		};
		saveStateSetup.setImageDescriptor(com.eco.bio7.Bio7Plugin.getImageDescriptor("/icons/save_edit.gif"));
		addStateAction = new Action("(+)") {
			public void run() {

				Bio7State.createState(txt.getText());
				Field.chance();
				Field.doPaint();
			}
		};
		addStateAction.setImageDescriptor(com.eco.bio7.Bio7Plugin.getImageDescriptor("/icons/add.gif"));
		openStateSetup = new Action("Open") {

			public void run() {
				Shell s = new Shell(SWT.ON_TOP);
				FileDialog fd = new FileDialog(s, SWT.OPEN);
				fd.setText("Load");

				String[] filterExt = { "*.exml", "*.*" };
				fd.setFilterExtensions(filterExt);
				String selected = fd.open();
				if (selected != null) {

					/* The same call as in Quadview for drag and drop! */
					LoadData.load(selected);
				}

			}
		};

		openStateSetup.setImageDescriptor(com.eco.bio7.Bio7Plugin.getImageDescriptor("/icons/file.gif"));

		removeStateAction = new Action("(-)") {

			public void run() {
				Grid grid = StateTable.grid;
				for (int i = 0; i < grid.getItemCount(); i++) {

					if (grid.getItem(i).getChecked(3) == true) {

						list.add(i);

						String state = grid.getItem(i).getText(0);

						StateTable.unsetCell(state);
					}
				}
				int[] index = new int[list.size()];
				for (int i = 0; i < list.size(); i++) {
					Integer ind = (Integer) list.get(i);
					index[i] = ind.intValue();

				}
				list.clear();

				grid.remove(index);

			}

		};
		removeStateAction.setImageDescriptor(com.eco.bio7.Bio7Plugin.getImageDescriptor("/icons/delete.gif"));
	}

	class TextItem extends ControlContribution {

		TextItem() {
			super("text");
		}

		protected Control createControl(Composite parent) {
			txt = new Text(parent, SWT.SINGLE | SWT.BORDER);
			txt.addListener(SWT.DefaultSelection, new Listener() {
				public void handleEvent(Event e) {

					Bio7State.createState(txt.getText());
					txt.setText("");
					Field.chance();
					Field.doPaint();
				}
			});
			DropTarget dt = new DropTarget(txt, DND.DROP_DEFAULT | DND.DROP_MOVE);
			dt.setTransfer(new Transfer[] { FileTransfer.getInstance() });
			dt.addDropListener(new DropTargetAdapter() {
				private String[] fileList;

				public void drop(DropTargetEvent event) {

					FileTransfer ft = FileTransfer.getInstance();
					if (ft.isSupportedType(event.currentDataType)) {
						fileList = (String[]) event.data;

						// browser.setUrl(fileList[0]);
						// txt.setText(fileList[0]);

					}
				}
			});
			txt.setText("Enter State!");
			txt.addMouseListener(new MouseListener() {

				@Override
				public void mouseDoubleClick(MouseEvent e) {

				}

				@Override
				public void mouseDown(MouseEvent e) {
					txt.setText("");

				}

				@Override
				public void mouseUp(MouseEvent e) {
					// TODO Auto-generated method stub

				}
			});

			return txt;
		}
	}

	public  void saveJavaSource() {
		
		IWorkbench bench = PlatformUI.getWorkbench();
		IWorkbenchWindow win = bench.getActiveWorkbenchWindow();
		IWorkbenchPage pa = win.getActivePage();

		IEditorPart currentEditor = (IEditorPart) pa.getActiveEditor();
		if (currentEditor != null) {

			IEditorInput input = currentEditor.getEditorInput();
			
			if (input instanceof IFileEditorInput) {
				
				String name = ((IFileEditorInput) input).getFile().getName();
				
				fileName = name.replaceFirst("[.][^.]+$", "");

			}
			if (currentEditor instanceof JavaEditor) {

				ITextEditor editor = (ITextEditor) currentEditor;
				
				IDocumentProvider dp = editor.getDocumentProvider();

				IDocument doc = dp.getDocument(editor.getEditorInput());

				fileSource = doc.get();

			}
		} 
		
	}

}
