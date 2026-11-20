package com.eco.bio7.wizards.java;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;



public class JavaWindowBuilderBio7ClassWizardPage extends WizardPage {
	private Text containerText;

	private Text fileText;

	private ISelection selection;

	
	/* An option of the template (e.g., the app in its own project), null: none */
	private String option;
	private org.eclipse.swt.widgets.Button optionButton;
	private boolean optionSelected;

	/* The suggested names of the project and the file */
	private String defaultProject = "My_Project";
	private String defaultFile = "My_Model.java";

	public JavaWindowBuilderBio7ClassWizardPage(ISelection selection) {
		super("wizardPage");
		setTitle("WindowBuilder Files");
		setDescription("This wizard creates a Java Project and two new files with *.java extensions. The Gui file can be opened by the WindowBuilder.");
		this.selection = selection;
	}

	/** A page with its own title, description and the suggested names of the project and the file. */
	public JavaWindowBuilderBio7ClassWizardPage(ISelection selection, String title, String description, String defaultProject, String defaultFile) {
		this(selection);
		setTitle(title);
		setDescription(description);
		this.defaultProject = defaultProject;
		this.defaultFile = defaultFile;
	}

	
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 3;
		layout.verticalSpacing = 9;
		Label label = new Label(container, SWT.NULL);
		label.setText("&Container:");

		containerText = new Text(container, SWT.BORDER | SWT.SINGLE);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		containerText.setLayoutData(gd);
		containerText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});

		Button button = new Button(container, SWT.PUSH);
		button.setText("Browse...");
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				//handleBrowse();
			}
		});
		button.setEnabled(false);
		label = new Label(container, SWT.NULL);
		label.setText("&File name:");

		fileText = new Text(container, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fileText.setLayoutData(gd);
		fileText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});
		initialize();
		if (option != null) {
			new Label(container, SWT.NONE);
			optionButton = new org.eclipse.swt.widgets.Button(container, SWT.CHECK);
			optionButton.setText(option);
			optionButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
			optionButton.addListener(SWT.Selection, e -> optionSelected = optionButton.getSelection());
		}
		dialogChanged();
		setControl(container);
	}

	

	private void initialize() {
		if (selection != null && selection.isEmpty() == false
				&& selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) selection;
			if (ssel.size() > 1)
				return;
			Object obj = ssel.getFirstElement();
			if (obj instanceof IResource) {
				IContainer container;
				if (obj instanceof IContainer)
					container = (IContainer) obj;
				else
					container = ((IResource) obj).getParent();
				containerText.setText(container.getFullPath().toString());
			}
		}
		containerText.setText(defaultProject);
		fileText.setText(defaultFile);
	}

	

	private void handleBrowse() {
		ContainerSelectionDialog dialog = new ContainerSelectionDialog(
				getShell(), ResourcesPlugin.getWorkspace().getRoot(), false,
				"Select new file container");
		if (dialog.open() == ContainerSelectionDialog.OK) {
			Object[] result = dialog.getResult();
			if (result.length == 1) {
				containerText.setText(((Path) result[0]).toString());
			}
		}
	}

	

	private void dialogChanged() {
		IResource container = ResourcesPlugin.getWorkspace().getRoot()
				.findMember(new Path(getContainerName()));
		String fileName = getFileName();
		
		if (getContainerName().length() == 0) {
			updateStatus("Project must be specified");
			return;
		}
		
		
		if (container!=null&&container.exists()) {
			updateStatus("Project with name already exists");
			return;
		}

		/*if (getContainerName().length() == 0) {
			updateStatus("File container must be specified");
			return;
		}
		if (container == null
				|| (container.getType() & (IResource.PROJECT | IResource.FOLDER)) == 0) {
			updateStatus("File container must exist");
			return;
		}
		if (!container.isAccessible()) {
			updateStatus("Project must be writable");
			return;
		}*/
		if (fileName.length() == 0) {
			updateStatus("File name must be specified");
			return;
		}
		if (fileName.replace('\\', '/').indexOf('/', 1) > 0) {
			updateStatus("File name must be valid");
			return;
		}
		int dotLoc = fileName.lastIndexOf('.');
		if (dotLoc != -1) {
			String ext = fileName.substring(dotLoc + 1);
			if (ext.equalsIgnoreCase("java") == false) {
				updateStatus("File extension must be \"java\"");
				return;
			}
		}
		updateStatus(null);
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}

	/** An option of the template (a check box on the page), null: none. */
	public void setOption(String text) {
		option = text;
	}

	/** The option is selected. */
	public boolean isOptionSelected() {
		return optionSelected;
	}

	public String getContainerName() {
		return containerText.getText();
	}

	public String getFileName() {
		return fileText.getText();
	}
}