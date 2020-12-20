package com.eco.bio7.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.wizards.IWizardDescriptor;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.collection.Work;
import com.eco.bio7.util.Bio7Dialog;
import com.eco.bio7.util.Util;

public class ToolbarCreateProjectAndFiles extends Action implements IMenuCreator {

	private final IWorkbenchWindow window;

	private Menu fMenu;

	public static String selection = "r";// This variable is used in the called wizard to create the different filetypes!

	public ToolbarCreateProjectAndFiles(String text, IWorkbenchWindow window) {
		super(text, AS_DROP_DOWN_MENU);
		this.window = window;

		setId("com.eco.bio7.toolbar.files.create");
		// setActionDefinitionId("com.eco.bio7.toolbar.files.create");
		setMenuCreator(this);
		this.setToolTipText("Create new projects and files!");
		setImageDescriptor(com.eco.bio7.Bio7Plugin.getImageDescriptor("/icons/maintoolbar/create_project_file.png"));

	}

	public Menu getMenu(Control parent) {
		if (fMenu != null) {
			fMenu.dispose();
		}
		fMenu = new Menu(parent);

		MenuItem menuItemBio7JavaProject = new MenuItem(fMenu, SWT.PUSH);
		menuItemBio7JavaProject.setText("Bio7 Java Project");

		// Image image
		// =PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);

		menuItemBio7JavaProject.setImage(Bio7Plugin.getImageDescriptor("/icons/maintoolbar/create_project_file_java.png").createImage());
		menuItemBio7JavaProject.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				selection = "java";
				openWizard("com.eco.bio7.wizardJavaJdtBio7Model");
				selection = "r";
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}

		});

		MenuItem menuItemBio7JavaFXJavaProject = new MenuItem(fMenu, SWT.PUSH);
		menuItemBio7JavaFXJavaProject.setText("Bio7 JavaFX GUI Project");

		// Image image
		// =PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);

		menuItemBio7JavaFXJavaProject.setImage(Bio7Plugin.getImageDescriptor("/icons/maintoolbar/create_project_file_java.png").createImage());
		menuItemBio7JavaFXJavaProject.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				selection = "java";
				openWizard("com.eco.bio7.javafxclassjdt");
				selection = "r";
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}

		});

		MenuItem menuItemBio7WindowBuilderJavaProject = new MenuItem(fMenu, SWT.PUSH);
		menuItemBio7WindowBuilderJavaProject.setText("Bio7 Window Builder GUI Project");

		// Image image
		// =PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);

		menuItemBio7WindowBuilderJavaProject.setImage(Bio7Plugin.getImageDescriptor("/icons/maintoolbar/create_project_file_java.png").createImage());
		menuItemBio7WindowBuilderJavaProject.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				selection = "java";
				openWizard("com.eco.bio7.javafxclassjdtwindowbuilder");
				selection = "r";
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}

		});

		MenuItem menuItemBio7OpenGLJavaProject = new MenuItem(fMenu, SWT.PUSH);
		menuItemBio7OpenGLJavaProject.setText("Bio7 OpenGL Java Project");

		// Image image
		// =PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);

		menuItemBio7OpenGLJavaProject.setImage(Bio7Plugin.getImageDescriptor("/icons/maintoolbar/create_project_file_java.png").createImage());
		menuItemBio7OpenGLJavaProject.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				selection = "java";
				int dialogAction = openWizard("com.eco.bio7.wizardJavaJdtBio7ModelJogl");
				if (dialogAction == 0) {
					Work.openPerspectiveEditorInJob("com.eco.bio7.perspective_3d");

				}
				selection = "r";

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}

		});

		MenuItem menuItemBio7OpenGLWorldWindProject = new MenuItem(fMenu, SWT.PUSH);
		menuItemBio7OpenGLWorldWindProject.setText("Bio7 WorldWind Java Project");

		// Image image
		// =PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);

		menuItemBio7OpenGLWorldWindProject.setImage(Bio7Plugin.getImageDescriptor("/icons/maintoolbar/create_project_file_java.png").createImage());
		menuItemBio7OpenGLWorldWindProject.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				selection = "java";
				int dialogAction = openWizard("com.eco.bio7.wizardJavaJdtBio7ModelJogl");
				if (dialogAction == 0) {
					Work.openPerspectiveEditorInJob("com.eco.bio7.WorldWind.3dglobe");
				}
				selection = "r";
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}

		});
		
		
		
		MenuItem menuItemBio7ImageJPluginProject = new MenuItem(fMenu, SWT.PUSH);
		menuItemBio7ImageJPluginProject.setText("Bio7 ImageJ Plugin Project");

		// Image image
		// =PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);

		menuItemBio7ImageJPluginProject.setImage(Bio7Plugin.getImageDescriptor("/icons/maintoolbar/create_project_file_java.png").createImage());
		menuItemBio7ImageJPluginProject.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				selection = "java";
				int dialogAction = openWizard("com.eco.bio7.wizardJavaJdtBio7ImageJPlugin");
				if (dialogAction == 0) {
					//Work.openPerspectiveEditorInJob("com.eco.bio7.WorldWind.3dglobe");
				}
				selection = "r";
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}

		});

		new MenuItem(fMenu, SWT.SEPARATOR);
		MenuItem menuItemR = new MenuItem(fMenu, SWT.PUSH);
		menuItemR.setText("Project with R file");

		// Image image
		// =PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);

		menuItemR.setImage(Bio7Plugin.getImageDescriptor("/icons/maintoolbar/create_project_file.png").createImage());
		menuItemR.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				selection = "r";
				openWizard("com.eco.bio7.wizard.new.r.project.file");
				selection = "r";
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}

		});
		MenuItem menuItemRMarkdown = new MenuItem(fMenu, SWT.PUSH);
		menuItemRMarkdown.setText("Project with R Markdown file");

		// Image image
		// =PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);

		menuItemRMarkdown.setImage(Bio7Plugin.getImageDescriptor("/icons/maintoolbar/create_project_file.png").createImage());
		menuItemRMarkdown.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				selection = "rmarkdown";
				openWizard("com.eco.bio7.wizard.new.r.project.file");
				selection = "r";
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}

		});
		MenuItem menuItemRShiny = new MenuItem(fMenu, SWT.PUSH);
		menuItemRShiny.setText("Project with R Shiny");

		// Image image
		// =PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);

		menuItemRShiny.setImage(Bio7Plugin.getImageDescriptor("/icons/maintoolbar/create_project_file.png").createImage());
		menuItemRShiny.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				selection = "shiny";
				openWizard("com.eco.bio7.wizard.new.r.project.file");
				selection = "r";
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}

		});

		MenuItem menuItemRReprocible = new MenuItem(fMenu, SWT.PUSH);
		menuItemRReprocible.setText("Project with Reproducible R folders");

		// Image image
		// =PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);

		menuItemRReprocible.setImage(Bio7Plugin.getImageDescriptor("/icons/maintoolbar/create_project_file.png").createImage());
		menuItemRReprocible.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {

				openWizard("com.eco.bio7.wizard.reproducible");

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}

		});
		new MenuItem(fMenu, SWT.SEPARATOR);
		MenuItem menuItemPyhon = new MenuItem(fMenu, SWT.PUSH);
		menuItemPyhon.setText("Project with Python file");
		menuItemPyhon.setImage(Bio7Plugin.getImageDescriptor("/icons/maintoolbar/create_project_file.png").createImage());

		// Image image =
		// PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);

		// menuItem.setImage(Bio7Plugin.getImageDescriptor("/icons/views/raction.png").createImage());
		menuItemPyhon.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				selection = "python";
				openWizard("com.eco.bio7.wizard.new.r.project.file");
				selection = "r";
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}

		});
		//new MenuItem(fMenu, SWT.SEPARATOR);
		
		MenuItem menuItemJyhon = new MenuItem(fMenu, SWT.PUSH);
		menuItemJyhon.setText("Project with Jython file");
		menuItemJyhon.setImage(Bio7Plugin.getImageDescriptor("/icons/maintoolbar/create_project_file.png").createImage());

		// Image image =
		// PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);

		// menuItem.setImage(Bio7Plugin.getImageDescriptor("/icons/views/raction.png").createImage());
		menuItemJyhon.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				selection = "jython";
				openWizard("com.eco.bio7.wizard.new.r.project.file");
				selection = "r";
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}

		});
		new MenuItem(fMenu, SWT.SEPARATOR);
		MenuItem menuItemGroovy = new MenuItem(fMenu, SWT.PUSH);
		menuItemGroovy.setText("Project with Groovy file");
		menuItemGroovy.setImage(Bio7Plugin.getImageDescriptor("/icons/maintoolbar/create_project_file.png").createImage());

		// Image image =
		// PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);

		// menuItem.setImage(Bio7Plugin.getImageDescriptor("/icons/views/raction.png").createImage());
		menuItemGroovy.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				selection = "groovy";
				openWizard("com.eco.bio7.wizard.new.r.project.file");
				selection = "r";
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}

		});
		new MenuItem(fMenu, SWT.SEPARATOR);
		MenuItem menuItemJavaScript = new MenuItem(fMenu, SWT.PUSH);
		menuItemJavaScript.setText("Project with JavaScript file");
		menuItemJavaScript.setImage(Bio7Plugin.getImageDescriptor("/icons/maintoolbar/create_project_file.png").createImage());

		// Image image =
		// PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);

		// menuItem.setImage(Bio7Plugin.getImageDescriptor("/icons/views/raction.png").createImage());
		menuItemJavaScript.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				selection = "javascript";
				openWizard("com.eco.bio7.wizard.new.r.project.file");
				selection = "r";
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}

		});
		new MenuItem(fMenu, SWT.SEPARATOR);
		MenuItem menuItemImageJMacroFile = new MenuItem(fMenu, SWT.PUSH);
		menuItemImageJMacroFile.setText("Project with ImageJ Macro file");
		menuItemImageJMacroFile.setImage(Bio7Plugin.getImageDescriptor("/icons/maintoolbar/create_project_file.png").createImage());

		// Image image =
		// PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);

		// menuItem.setImage(Bio7Plugin.getImageDescriptor("/icons/views/raction.png").createImage());
		menuItemImageJMacroFile.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				selection = "ijmacro";
				openWizard("com.eco.bio7.wizard.new.r.project.file");
				selection = "r";
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}

		});
		new MenuItem(fMenu, SWT.SEPARATOR);
		
		MenuItem menuItemLatexFile = new MenuItem(fMenu, SWT.PUSH);
		menuItemLatexFile.setText("Project with Latex file");
		menuItemLatexFile.setImage(Bio7Plugin.getImageDescriptor("/icons/maintoolbar/create_project_file.png").createImage());

		// Image image =
		// PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);

		// menuItem.setImage(Bio7Plugin.getImageDescriptor("/icons/views/raction.png").createImage());
		menuItemLatexFile.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				selection = "latex";
				openWizard("net.sourceforge.texlipse.TexProjectWizard");
				selection = "r";
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}

		});
		new MenuItem(fMenu, SWT.SEPARATOR);
		MenuItem menuItemFile = new MenuItem(fMenu, SWT.PUSH);
		menuItemFile.setText("Other...");

		menuItemFile.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {

				Work.executeCommand("org.eclipse.ui.newWizard");

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}

		});
		return fMenu;
	}

	public Menu getMenu(Menu parent) {
		// TODO Auto-generated method stub
		return null;
	}

	public void run() {

	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	// Source from: https://resheim.net/2010/07/invoking-eclipse-wizard.html
	public int openWizard(String id) {
		int operation = 1;
		// First see if this is a "new wizard".
		IWizardDescriptor descriptor = PlatformUI.getWorkbench().getNewWizardRegistry().findWizard(id);
		// If not check if it is an "import wizard".
		if (descriptor == null) {
			descriptor = PlatformUI.getWorkbench().getImportWizardRegistry().findWizard(id);
		}
		// Or maybe an export wizard
		if (descriptor == null) {
			descriptor = PlatformUI.getWorkbench().getExportWizardRegistry().findWizard(id);
		}
		try {
			// Then if we have a wizard, open it.
			if (descriptor != null) {
				IWizard wizard = descriptor.createWizard();
				WizardDialog wd = new WizardDialog(Util.getShell(), wizard);
				wd.setTitle(wizard.getWindowTitle());
				operation = wd.open();
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return operation;
	}

}