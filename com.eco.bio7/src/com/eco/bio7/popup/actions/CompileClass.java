/*package com.eco.bio7.popup.actions;

import ij.plugin.PlugIn;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.swing.SwingUtilities;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.jdk.SimpleCompiler;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.compile.Model;
import com.eco.bio7.methods.Compiled;
import com.eco.bio7.rcp.StartBio7Utils;
import com.eco.bio7.worldwind.DynamicLayer;

public class CompileClass implements IObjectActionDelegate {
	private IEditorPart editor;
	private IDocument doc;
	private IResource resource;
	private String docText;
	private String title;

	public CompileClass() {
		super();
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	public void run(IAction action) {
		StartBio7Utils.getConsole_instance().cons.clear();
		editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (editor != null) {
			doc = ((ITextEditor) editor).getDocumentProvider().getDocument(editor.getEditorInput());
			resource = (IResource) editor.getEditorInput().getAdapter(IResource.class);
			SimpleCompiler.resource = resource;
			docText = doc.get();
			title = null;

			Job job = new Job("Compile And Run") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					monitor.beginTask("Compile And Run...", IProgressMonitor.UNKNOWN);

					SimpleCompiler simple = new SimpleCompiler();
					
					 * Important to set the main plugin Classloader for the
					 * runtime!
					 
					simple.setParentClassLoader(Bio7Plugin.class.getClassLoader());

					title = editor.getTitle().replaceFirst("[.][^.]+$", "");
					simple.directCall = true;
					boolean success = true;
					try {
						simple.cook(title, new StringReader(docText));

					} catch (CompileException | IOException e3) {
						// TODO Auto-generated catch block
						// e3.printStackTrace();
						success = false;
						simple.directCall = false;
					}
					if (success) {

						
						 * A boolean variable for the error line correction in
						 * the Java editor!
						 
						simple.directCall = false;

						Class<?> cl = null;
						try {
							cl = simple.getClassLoader().loadClass(title);
						} catch (ClassNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						try {
							Object inst = cl.newInstance();
							if (inst != null) {
								if (inst instanceof PlugIn) {
									callPlugin(cl);

								} else if (inst instanceof PlugInFilter) {
									callPluginFilter(cl);

								}

								else if (inst instanceof Model) {
									Model model = (Model) inst;
									Compiled.setEcoclass(model);
									 For Java WorldWind! 
									DynamicLayer.setEcoclass(model);
								}

								else {
									callMainMethod(cl);
								}
							} else {
									System.out.println("Class file not created by compilation!");
							}
						} catch (InstantiationException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IllegalAccessException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

					monitor.done();
					return Status.OK_STATUS;
				}

			};
			job.addJobChangeListener(new JobChangeAdapter() {
				public void done(IJobChangeEvent event) {
					if (event.getResult().isOK()) {

					} else {

					}
				}
			});

			job.schedule();

		} else {
			Bio7Dialog.message("Please open the Java file for compilation!");
		}

	}

	private void compileAndRun() {

	}

	private void callPlugin(final Class cl) {
		SwingUtilities.invokeLater(new Runnable() {
			// !!
			public void run() {
				try {

					((PlugIn) cl.newInstance()).run("");
				} catch (InstantiationException | IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		
		 * SwingUtilities.invokeLater(new Runnable() { // !! public void run() {
		 * 
		 * try { Method method = cl.getMethod("run", new Class[] { String.class
		 * }); Object retVal; try { retVal = method.invoke(cl.newInstance(),
		 * " "); } catch (InstantiationException e) { // TODO Auto-generated
		 * catch block e.printStackTrace(); }
		 * 
		 * } catch (SecurityException e) { e.printStackTrace(); } catch
		 * (NoSuchMethodException e) { e.printStackTrace(); } catch
		 * (IllegalArgumentException e) { e.printStackTrace(); } catch
		 * (IllegalAccessException e) { e.printStackTrace(); } catch
		 * (InvocationTargetException e) { e.printStackTrace(); } } });
		 
	}

	private void callPluginFilter(final Class cl) {
		SwingUtilities.invokeLater(new Runnable() {
			// !!
			public void run() {
				// System.out.println("run plugin");
				try {
					new PlugInFilterRunner(cl.newInstance(), "plugin", "");
				} catch (InstantiationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				
				 * try { Method method = cl.getMethod("run", new Class[] {
				 * ImageProcessor.class }); Object retVal; try { retVal =
				 * method.invoke(cl.newInstance(), " "); } catch
				 * (InstantiationException e) { // TODO Auto-generated catch
				 * block e.printStackTrace(); }
				 * 
				 * } catch (SecurityException e) { e.printStackTrace(); } catch
				 * (NoSuchMethodException e) { e.printStackTrace(); } catch
				 * (IllegalArgumentException e) { e.printStackTrace(); } catch
				 * (IllegalAccessException e) { e.printStackTrace(); } catch
				 * (InvocationTargetException e) { e.printStackTrace(); }
				 
			}
		});
	}

	private void callMainMethod(final Class cl) {

		Method method = null;
		try {
			method = cl.getMethod("main", String[].class);

		} catch (NoSuchMethodException | SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

		try {
			String[] params = null; 
			method.invoke(null, (Object) params);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

}
*/