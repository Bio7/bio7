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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.eclipse.swt.widgets.Link;

public class Bio7ReproducibleRProjectWizardPage extends WizardPage {

	private Text containerText;

	private ISelection selection;

	private Button btnCreateReadme;

	private Button btnCreateRFile;

	private Button btnCreateDataFolder;

	private Button btnCreateDocFolder;

	private Button btnCreateFigsFolder;

	private Button btnCreateOutputFolder;

	private Button btnCreateRFolder;

	private Button btnCreateReportsFolder;

	private Button btnCreateReferencesFolder;

	private Button btnCreateModelFolder;
	
	private Button btnCreateImgFolder;
	
	private Button btnCreatePlotFolder;

	public Bio7ReproducibleRProjectWizardPage(ISelection selection) {
		super("wizardPage");
		setTitle("Reproducible R Project");
		setDescription("This wizard creates a Reproducible R Project.");
		this.selection = selection;
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 3;
		layout.verticalSpacing = 9;
		Label label = new Label(container, SWT.NULL);
		label.setText("&Project Name:");

		containerText = new Text(container, SWT.BORDER | SWT.SINGLE);
		containerText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		containerText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});

		Button button = new Button(container, SWT.PUSH);
		button.setText("Browse...");
		button.setEnabled(false);

		initialize();
		dialogChanged();
		setControl(container);
		new Label(container, SWT.NONE);

		btnCreateReadme = new Button(container, SWT.CHECK);
		btnCreateReadme.setSelection(true);
		btnCreateReadme.setText("Create README file");
		new Label(container, SWT.NONE);
		new Label(container, SWT.NONE);

		btnCreateRFile = new Button(container, SWT.CHECK);
		btnCreateRFile.setSelection(true);
		btnCreateRFile.setText("Create R file template");
		new Label(container, SWT.NONE);
		new Label(container, SWT.NONE);

		Label label_1 = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
		label_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		new Label(container, SWT.NONE);
		new Label(container, SWT.NONE);

		btnCreateDataFolder = new Button(container, SWT.CHECK);
		btnCreateDataFolder.setSelection(true);
		btnCreateDataFolder.setText("Create data folder");
		new Label(container, SWT.NONE);
		new Label(container, SWT.NONE);

		btnCreateDocFolder = new Button(container, SWT.CHECK);
		btnCreateDocFolder.setSelection(true);
		btnCreateDocFolder.setText("Create doc folder");
		new Label(container, SWT.NONE);
		new Label(container, SWT.NONE);

		btnCreateFigsFolder = new Button(container, SWT.CHECK);
		btnCreateFigsFolder.setSelection(true);
		btnCreateFigsFolder.setText("Create figs folder");
		new Label(container, SWT.NONE);
		new Label(container, SWT.NONE);

		btnCreateOutputFolder = new Button(container, SWT.CHECK);
		btnCreateOutputFolder.setSelection(true);
		btnCreateOutputFolder.setText("Create output folder");
		new Label(container, SWT.NONE);
		new Label(container, SWT.NONE);

		btnCreateRFolder = new Button(container, SWT.CHECK);
		btnCreateRFolder.setSelection(true);
		btnCreateRFolder.setText("Create R folder");
		new Label(container, SWT.NONE);
		new Label(container, SWT.NONE);

		btnCreateReportsFolder = new Button(container, SWT.CHECK);
		btnCreateReportsFolder.setSelection(true);
		btnCreateReportsFolder.setText("Create reports folder");
		new Label(container, SWT.NONE);
		new Label(container, SWT.NONE);
		
		btnCreateImgFolder = new Button(container, SWT.CHECK);
		btnCreateImgFolder.setText("Create img folder");
		new Label(container, SWT.NONE);
		new Label(container, SWT.NONE);
		
		btnCreatePlotFolder = new Button(container, SWT.CHECK);
		btnCreatePlotFolder.setText("Create plot folder");
		new Label(container, SWT.NONE);
		new Label(container, SWT.NONE);

		btnCreateReferencesFolder = new Button(container, SWT.CHECK);
		btnCreateReferencesFolder.setText("Create references folder");
		new Label(container, SWT.NONE);
		new Label(container, SWT.NONE);

		btnCreateModelFolder = new Button(container, SWT.CHECK);
		btnCreateModelFolder.setText("Create model folder");
		new Label(container, SWT.NONE);
	}

	public Button getBtnCreateReadme() {
		return btnCreateReadme;
	}

	public Button getBtnCreateRFile() {
		return btnCreateRFile;
	}

	public Button getBtnCreateDataFolder() {
		return btnCreateDataFolder;
	}

	public Button getBtnCreateDocFolder() {
		return btnCreateDocFolder;
	}

	public Button getBtnCreateFigsFolder() {
		return btnCreateFigsFolder;
	}

	public Button getBtnCreateOutputFolder() {
		return btnCreateOutputFolder;
	}

	public Button getBtnCreateRFolder() {
		return btnCreateRFolder;
	}

	public Button getBtnCreateReportsFolder() {
		return btnCreateReportsFolder;
	}

	public Button getBtnCreateReferencesFolder() {
		return btnCreateReferencesFolder;
	}

	public Button getBtnCreateModelFolder() {
		return btnCreateModelFolder;
	}
	
	public Button getBtnCreateImgFolder() {
		return btnCreateImgFolder;
	}

	public Button getBtnCreatePlotFolder() {
		return btnCreatePlotFolder;
	}

	private void initialize() {
		if (selection != null && selection.isEmpty() == false && selection instanceof IStructuredSelection) {
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
		containerText.setText("My_Project");

	}

	private void handleBrowse() {
		ContainerSelectionDialog dialog = new ContainerSelectionDialog(getShell(), ResourcesPlugin.getWorkspace().getRoot(), false, "Select or create new Reproducible project");
		if (dialog.open() == ContainerSelectionDialog.OK) {
			Object[] result = dialog.getResult();
			if (result.length == 1) {
				containerText.setText(((Path) result[0]).toString());
			}
		}
	}

	private void dialogChanged() {
		IResource container = ResourcesPlugin.getWorkspace().getRoot().findMember(new Path(getContainerName()));
		if (getContainerName().length() == 0) {
			updateStatus("Project must be specified");
			return;
		}
		if (container != null && container.exists()) {
			updateStatus("Project with name already exists");
			return;
		}

		updateStatus(null);
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}

	public String getContainerName() {
		return containerText.getText();
	}

}