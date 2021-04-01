
/********************************************************************************
 * Copyright (c) 2020 Marcel Austenfeld
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 ********************************************************************************/

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.wizards.IWizardDescriptor;

import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.batch.FileRoot;
import com.eco.bio7.console.Bio7Console;
import com.eco.bio7.image.CanvasView;
import com.eco.bio7.image.RImageMethodsView;
import com.eco.bio7.image.Util;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.gui.RoiListener;
import org.eclipse.wb.swt.SWTResourceManager;

public class _ModelGui extends Composite implements RoiListener {
	protected boolean convolve;
	protected boolean gaussian;
	protected boolean median;
	protected boolean mean;
	protected boolean maximum;
	protected boolean minimum;
	protected boolean edges;
	protected String convolveOption = "text1=[\n-1 -1 -1 -1 -1\n-1 -1 -1 -1 -1\n-1 -1 24 -1 -1\n-1 -1 -1 -1 -1\n-1 -1 -1 -1 -1\n] normalize";
	protected String medianOption = "2";
	protected String channelOption = "";
	protected String gaussianOption = "2";
	protected String meanOption = "2";
	protected String maximumOption = "2";
	protected String minimumOption = "2";
	private Main model;
	protected Text channelSelectionText;
	protected Text optionGaussian;
	protected Text optionMedian;
	protected Text optionConvolve;
	protected Text optionsMean;
	protected Text optionsMaximum;
	protected Text optionsMinimum;
	protected Button checkGaussianFilter;
	protected Button checkMedian;
	protected Button checkConvolve;
	protected Button checkMaximum;
	protected Button checkMean;
	protected Button checkMinimum;
	protected Button checkEdges;
	private CTabFolder tabFolder;
	private CTabItem tabItemFeatures;
	private Composite composite;
	private CTabItem tbtmMore;
	private Composite composite_1;
	private Button btnLoadConfiguration;
	private Button btnNewButton_4;
	protected Text txtTrainingRScript;
	private Button btnNewButton_5;
	private Button btnRClassificationScript;
	protected Text txtClassificationRScript;
	protected String pathTrainingScript;
	protected String pathClassificationScript;
	protected Button checkConvertToHsb;
	protected boolean toHsb;
	private ScrolledComposite scrolledComposite;
	protected Button checkGradientHessian;
	protected Text optionGradientHessian;
	protected Button checkLaplacian;
	protected Text optionLaplacian;
	protected boolean gradientHessian;
	protected String gradientHessianOption;
	protected boolean laplacian;
	protected String laplacianOption;
	protected Button checkVariance;
	protected Text optionsVariance;
	protected boolean variance;
	protected String varianceOption;
	protected Text optionsEdges;
	protected Text optionDiffGaussian;
	protected Button checkDifferenceOfGaussian;
	protected boolean diffOfGaussian;
	protected String diffGaussianOption;
	protected Button checkLipschitz;
	protected Text optionLipschitz;
	protected Button checkGabor;
	protected Text optionGabor;
	protected boolean lipschitz;
	protected String lipschitzOption;
	protected boolean gabor;
	protected String gaborOption;
	protected Button checkUseImportMacro;
	protected Text textImageJMacro;
	protected boolean useImportMacro;
	protected String textOptionMacro;
	private Button buttonMacro;
	protected Button checkTopHat;
	protected Text optionsTopHat;
	protected boolean topHat;
	protected String topHatOption;
	protected Button checkKuwahara;
	protected Text optionsKuwahara;
	protected boolean kuwahara;
	protected String kuwaharaOption;
	protected String edgesOption;
	protected Button checkUseDirectory;
	protected boolean useDirectoryDialog;
	protected Button checkConvertToLab;
	protected boolean toLab;
	private Label transferTypeLabel;
	protected Combo transferTypeCombo;
	protected int transferType;
	protected boolean interruptBatch;
	private Button interruptButton;
	protected Button checkOpenStack;
	protected boolean openStack;
	protected Button checkUseGroups;
	protected boolean useGroups;
	protected Button checkGeneratePreview;
	protected boolean generatePreview;
	private Label scriptsLabel;
	private Label featureLabel;
	private Label previewLabel;
	private Label classificationOpenLabel;
	protected Button checkRetrainPreview;
	protected boolean retrainPreview;
	private Label lblNewLabel;
	private Label lblLutToApply;
	protected Text optionOpacity;
	protected Text optionLUT;
	protected String lutOption;
	protected String opacityOption;
	private ScrolledComposite scrolledCompositeSettings;
	private Button btnClassificationProject;
	private Label lblClassified;
	protected Button checkShowInImagej;
	protected boolean showClassifiedInImageJ;
	private Label lblFeatures;
	private Label lblColor;

	public _ModelGui(Composite parent, Main model, int style) {
		super(parent, SWT.NONE);
		this.model = model;
		setLayout(new FillLayout(SWT.HORIZONTAL));
		tabFolder = new CTabFolder(this, SWT.BORDER);
		/* Add drag and drop for the Configuration file! */
		DropTarget dt = new DropTarget(tabFolder, DND.DROP_DEFAULT | DND.DROP_MOVE);
		dt.setTransfer(new Transfer[] { FileTransfer.getInstance() });
		dt.addDropListener(new DropTargetAdapter() {
			public void drop(DropTargetEvent event) {

				FileTransfer ft = FileTransfer.getInstance();
				if (ft.isSupportedType(event.currentDataType)) {
					String[] fileList = (String[]) event.data;
					for (int i = 0; i < fileList.length; i++) {
						// System.out.println(fileList[i]);
						new _Settings(_ModelGui.this).loadScript(fileList[0]);
					}
				}
			}
		});
		// tabFolder.setSelectionBackground(
		// Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));

		tabItemFeatures = new CTabItem(tabFolder, SWT.NONE);
		tabItemFeatures.setText("Features");
		tabFolder.setSelection(tabItemFeatures);
		scrolledComposite = new ScrolledComposite(tabFolder, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		tabItemFeatures.setControl(scrolledComposite);
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setExpandVertical(false);
		composite = new Composite(scrolledComposite, SWT.NONE);
		composite.setSize(300, 1350);
		scrolledComposite.setContent(composite);
		composite.setLayout(new GridLayout(2, true));

		Button btnNewButton_1 = new Button(composite, SWT.NONE);
		btnNewButton_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		btnNewButton_1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				model.executeSelection(2);
			}
		});
		btnNewButton_1.setText("Select Features (1)");

		Button btnNewButton = new Button(composite, SWT.NONE);
		btnNewButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		btnNewButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				model.executeSelection(1);
			}
		});
		btnNewButton.setText("Create Features (2)");

		Button btnNewButton_2 = new Button(composite, SWT.NONE);
		btnNewButton_2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		btnNewButton_2.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				model.executeSelection(3);
			}
		});
		btnNewButton_2.setText("Train Script (3)");

		Button btnNewButton_3 = new Button(composite, SWT.NONE);
		btnNewButton_3.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		btnNewButton_3.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				model.executeSelection(4);
			}
		});
		btnNewButton_3.setText("Classify Script (4)");

		btnLoadConfiguration = new Button(composite, SWT.NONE);
		btnLoadConfiguration.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		btnLoadConfiguration.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				new _Settings(_ModelGui.this).loadScript(null);
			}
		});
		btnLoadConfiguration.setText("Load Configuration");

		btnNewButton_4 = new Button(composite, SWT.NONE);
		btnNewButton_4.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		btnNewButton_4.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				new _Settings(_ModelGui.this).saveScript();
			}
		});
		btnNewButton_4.setText("Save Configuration");

		interruptButton = new Button(composite, SWT.NONE);
		interruptButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				interruptBatch = true;
				boolean answer = Bio7Dialog.decision("Should the R process be interrupted (if possible)?");
				if (answer) {
					if (Util.getOS().equals("Win")) {
						Bio7Console.sendWinCtrlBreak();
					} else {
						Bio7Console.sendLinCtrlC();
					}
				}

			}
		});
		interruptButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		interruptButton.setText("Interrupt Classification");
		
		lblColor = new Label(composite, SWT.NONE);
		lblColor.setFont(SWTResourceManager.getFont("Arial", 9, SWT.BOLD));
		lblColor.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false, 2, 1));
		lblColor.setText("Color");

		checkConvertToHsb = new Button(composite, SWT.CHECK);
		checkConvertToHsb.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				checkConvertToLab.setSelection(false);
			}
		});
		checkConvertToHsb.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		checkConvertToHsb.setText("Convert to HSB");

		checkConvertToLab = new Button(composite, SWT.CHECK);
		checkConvertToLab.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				checkConvertToHsb.setSelection(false);
			}
		});
		checkConvertToLab.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		checkConvertToLab.setText("Convert to LAB");
		new Label(composite, SWT.NONE);
		new Label(composite, SWT.NONE);

		Label lblSelectChannels = new Label(composite, SWT.NONE);
		GridData gd_lblSelectChannels = new GridData(SWT.CENTER, SWT.CENTER, true, false, 2, 1);
		gd_lblSelectChannels.widthHint = 279;
		lblSelectChannels.setLayoutData(gd_lblSelectChannels);
		lblSelectChannels.setText("Select Channels (1,2,... - Leave blank for all!)\r\n");

		channelSelectionText = new Text(composite, SWT.BORDER);
		channelSelectionText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		lblFeatures = new Label(composite, SWT.NONE);
		lblFeatures.setFont(SWTResourceManager.getFont("Arial", 9, SWT.BOLD));
		lblFeatures.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false, 2, 1));
		lblFeatures.setText("Features");

		checkGaussianFilter = new Button(composite, SWT.CHECK);
		checkGaussianFilter.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		checkGaussianFilter.setText("Gaussian Blur");

		checkDifferenceOfGaussian = new Button(composite, SWT.CHECK);
		checkDifferenceOfGaussian.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		checkDifferenceOfGaussian.setText("Difference of Gaussian");

		optionGaussian = new Text(composite, SWT.BORDER);
		optionGaussian.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		optionGaussian.setText("2");

		optionDiffGaussian = new Text(composite, SWT.BORDER);
		optionDiffGaussian.setText("2,4");
		optionDiffGaussian.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		checkMean = new Button(composite, SWT.CHECK);
		checkMean.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		checkMean.setText("Mean");

		checkMedian = new Button(composite, SWT.CHECK);
		checkMedian.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		checkMedian.setText("Median");

		optionsMean = new Text(composite, SWT.BORDER);
		optionsMean.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		optionsMean.setText("2");

		optionMedian = new Text(composite, SWT.BORDER);
		optionMedian.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		optionMedian.setText("2");

		checkMinimum = new Button(composite, SWT.CHECK);
		checkMinimum.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		checkMinimum.setText("Minimum");

		checkVariance = new Button(composite, SWT.CHECK);
		checkVariance.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		checkVariance.setText("Variance");

		optionsMinimum = new Text(composite, SWT.BORDER);
		GridData gd_optionsMinimum = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_optionsMinimum.widthHint = 167;
		optionsMinimum.setLayoutData(gd_optionsMinimum);
		optionsMinimum.setText("2");

		optionsVariance = new Text(composite, SWT.BORDER);
		optionsVariance.setText("2");
		optionsVariance.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		checkMaximum = new Button(composite, SWT.CHECK);
		checkMaximum.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		checkMaximum.setText("Maximum");

		checkGradientHessian = new Button(composite, SWT.CHECK);
		checkGradientHessian.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		checkGradientHessian.setText("Gradient");

		optionsMaximum = new Text(composite, SWT.BORDER);
		optionsMaximum.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		optionsMaximum.setText("2");

		optionGradientHessian = new Text(composite, SWT.BORDER);
		optionGradientHessian.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		checkLaplacian = new Button(composite, SWT.CHECK);
		checkLaplacian.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		checkLaplacian.setText("Laplacian");

		checkEdges = new Button(composite, SWT.CHECK);
		checkEdges.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		checkEdges.setText("Sobel Edge");

		optionLaplacian = new Text(composite, SWT.BORDER);
		optionLaplacian.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		optionsEdges = new Text(composite, SWT.BORDER);
		optionsEdges.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		checkLipschitz = new Button(composite, SWT.CHECK);
		checkLipschitz.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		checkLipschitz.setText("Lipschitz");

		checkGabor = new Button(composite, SWT.CHECK);
		checkGabor.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		checkGabor.setText("Gabor");

		optionLipschitz = new Text(composite, SWT.BORDER);
		optionLipschitz.setText("true,true,10");
		optionLipschitz.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		optionGabor = new Text(composite, SWT.BORDER);
		optionGabor.setText("3,4.0,0.6,1.0,2.0,0.3");
		optionGabor.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {

			}
		});
		optionGabor.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		checkTopHat = new Button(composite, SWT.CHECK);
		checkTopHat.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		checkTopHat.setText("Top Hat");

		checkKuwahara = new Button(composite, SWT.CHECK);
		checkKuwahara.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		checkKuwahara.setText("Kuwahara");

		optionsTopHat = new Text(composite, SWT.BORDER);
		optionsTopHat.setText("2");
		optionsTopHat.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		optionsKuwahara = new Text(composite, SWT.BORDER);
		optionsKuwahara.setText("2");
		optionsKuwahara.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		new Label(composite, SWT.NONE);
		new Label(composite, SWT.NONE);

		checkConvolve = new Button(composite, SWT.CHECK);
		checkConvolve.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		checkConvolve.setText("Convolve");
		new Label(composite, SWT.NONE);

		optionConvolve = new Text(composite, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.MULTI);
		GridData gd_optionConvolve = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
		gd_optionConvolve.widthHint = 189;
		optionConvolve.setLayoutData(gd_optionConvolve);
		optionConvolve.setText(
				"text1=[\n-1 -1 -1 -1 -1\n-1 -1 -1 -1 -1\n-1 -1 24 -1 -1\n-1 -1 -1 -1 -1\n-1 -1 -1 -1 -1\n] normalize");

		tbtmMore = new CTabItem(tabFolder, SWT.NONE);
		tbtmMore.setText("Settings");
		scrolledCompositeSettings = new ScrolledComposite(tabFolder, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		tbtmMore.setControl(scrolledCompositeSettings);
		scrolledCompositeSettings.setExpandHorizontal(true);
		scrolledCompositeSettings.setExpandVertical(false);
		composite_1 = new Composite(scrolledCompositeSettings, SWT.NONE);
		composite_1.setSize(300, 600);
		scrolledCompositeSettings.setContent(composite_1);
		composite_1.setLayout(new GridLayout(2, true));

		classificationOpenLabel = new Label(composite_1, SWT.NONE);
		classificationOpenLabel.setFont(SWTResourceManager.getFont("Arial", 9, SWT.BOLD));
		classificationOpenLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 2, 1));
		classificationOpenLabel.setText("Classify Images");

		checkUseImportMacro = new Button(composite_1, SWT.CHECK);
		checkUseImportMacro.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		checkUseImportMacro.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

			}
		});
		checkUseImportMacro.setText("ImageJ Macro");

		checkUseDirectory = new Button(composite_1, SWT.CHECK);
		GridData gd_checkUseDirectory = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_checkUseDirectory.heightHint = 25;
		checkUseDirectory.setLayoutData(gd_checkUseDirectory);
		checkUseDirectory.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
			}
		});
		checkUseDirectory.setText("From Directory");

		scriptsLabel = new Label(composite_1, SWT.NONE);
		scriptsLabel.setFont(SWTResourceManager.getFont("Arial", 9, SWT.BOLD));
		scriptsLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false, 2, 1));
		scriptsLabel.setText("Scripts");

		textImageJMacro = new Text(composite_1, SWT.BORDER);
		textImageJMacro.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		textImageJMacro.setText(FileRoot.getCurrentCompileDir() + "/_Macro/IJMacro.ijm");

		buttonMacro = new Button(composite_1, SWT.NONE);
		buttonMacro.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String path = Bio7Dialog.openFile();
				path = path.replace("\\", "/");
				textImageJMacro.setText(path);
			}
		});
		buttonMacro.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		buttonMacro.setText("Macro");

		txtTrainingRScript = new Text(composite_1, SWT.BORDER);
		txtTrainingRScript.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		txtTrainingRScript.setText(FileRoot.getCurrentCompileDir() + "/_R/Train.R");

		btnNewButton_5 = new Button(composite_1, SWT.NONE);
		btnNewButton_5.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String path = Bio7Dialog.openFile();
				path = path.replace("\\", "/");
				txtTrainingRScript.setText(path);
			}
		});
		btnNewButton_5.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		btnNewButton_5.setText("Training Script");

		txtClassificationRScript = new Text(composite_1, SWT.BORDER);
		txtClassificationRScript.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		txtClassificationRScript.setText(FileRoot.getCurrentCompileDir() + "/_R/Classify.R");
		btnRClassificationScript = new Button(composite_1, SWT.NONE);
		btnRClassificationScript.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String path = Bio7Dialog.openFile();
				path = path.replace("\\", "/");
				txtClassificationRScript.setText(path);
			}
		});
		btnRClassificationScript.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		btnRClassificationScript.setText("Classification Script");

		btnClassificationProject = new Button(composite_1, SWT.NONE);
		btnClassificationProject.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IWizardDescriptor descriptor = PlatformUI.getWorkbench().getNewWizardRegistry()
						.findWizard("com.eco.bio7.wizard.reproducible");
				IWizard wizard = null;
				try {
					wizard = descriptor.createWizard();
				} catch (CoreException e1) {
					e1.printStackTrace();
				}
				WizardDialog wd = new WizardDialog(Util.getShell(), wizard);
				wd.setTitle(wizard.getWindowTitle());
				wd.open();
				boolean doOpen = Bio7Dialog.decision("Copy the train and classify scripts to the new project folder.\n"
						+ "Adjust the paths to the scripts and save a configuration file\n"
						+ "in the project folder!\n\n"
						+ "Would you like to copy the R scripts for customization (simply drag and drop to the project)?");
				if (doOpen) {
					org.eclipse.swt.program.Program.launch(new File(txtTrainingRScript.getText()).getParent());
				}
			}
		});
		btnClassificationProject.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		btnClassificationProject.setText("Create Classification Project");

		featureLabel = new Label(composite_1, SWT.NONE);
		featureLabel.setFont(SWTResourceManager.getFont("Arial", 9, SWT.BOLD));
		featureLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false, 2, 1));
		featureLabel.setText("Feature Options");

		checkOpenStack = new Button(composite_1, SWT.CHECK);
		checkOpenStack.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
			}
		});
		checkOpenStack.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		checkOpenStack.setText("Open Feature Stack");

		checkUseGroups = new Button(composite_1, SWT.CHECK);
		checkUseGroups.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
			}
		});
		checkUseGroups.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		checkUseGroups.setText("Use Group Signature");

		transferTypeLabel = new Label(composite_1, SWT.CENTER);
		transferTypeLabel.setFont(SWTResourceManager.getFont("Arial", 9, SWT.BOLD));
		GridData gd_transferTypeLabel = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		gd_transferTypeLabel.widthHint = 254;
		transferTypeLabel.setLayoutData(gd_transferTypeLabel);
		transferTypeLabel.setText("Select Transfer Type");

		transferTypeCombo = new Combo(composite_1, SWT.NONE);
		GridData gd_transferTypeCombo = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		gd_transferTypeCombo.widthHint = 269;
		transferTypeCombo.setLayoutData(gd_transferTypeCombo);
		transferTypeCombo.setItems(new String[] { "Double", "Integer", "Byte" });
		transferTypeCombo.select(0);
		transferTypeCombo.setText("Double");

		previewLabel = new Label(composite_1, SWT.NONE);
		previewLabel.setFont(SWTResourceManager.getFont("Arial", 9, SWT.BOLD));
		previewLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false, 2, 1));
		previewLabel.setText("Preview Overlay");

		checkGeneratePreview = new Button(composite_1, SWT.CHECK);
		checkGeneratePreview.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (checkGeneratePreview.getSelection() == true) {
					Roi.addRoiListener(_ModelGui.this);
				} else {
					Roi.removeRoiListener(_ModelGui.this);
					IJ.run("Remove Overlay", "");
				}
			}
		});
		GridData gd_checkGeneratePreview = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_checkGeneratePreview.widthHint = 247;
		checkGeneratePreview.setLayoutData(gd_checkGeneratePreview);
		checkGeneratePreview.setText("Selection Preview");

		checkRetrainPreview = new Button(composite_1, SWT.CHECK);
		checkRetrainPreview.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		checkRetrainPreview.setText("Retrain for Preview");

		lblNewLabel = new Label(composite_1, SWT.NONE);
		lblNewLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false, 1, 1));
		lblNewLabel.setText("Opacity");

		lblLutToApply = new Label(composite_1, SWT.NONE);
		lblLutToApply.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false, 1, 1));
		lblLutToApply.setText("LUT");

		optionOpacity = new Text(composite_1, SWT.BORDER);
		optionOpacity.setText("0.4");
		optionOpacity.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		optionLUT = new Text(composite_1, SWT.BORDER);

		optionLUT.setText("Spectrum");

		setLutCompletion(optionLUT);

		optionLUT.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		lblClassified = new Label(composite_1, SWT.NONE);
		lblClassified.setFont(SWTResourceManager.getFont("Arial", 9, SWT.BOLD));
		lblClassified.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false, 2, 1));
		lblClassified.setText("Classified Images");
		
		checkShowInImagej = new Button(composite_1, SWT.CHECK);
		checkShowInImagej.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
			}
		});
		checkShowInImagej.setSelection(true);
		checkShowInImagej.setText("Show in ImageJ");
		
		new Label(composite_1, SWT.NONE);
		transferTypeCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				int index = transferTypeCombo.getSelectionIndex();
				RImageMethodsView.getTransferTypeCombo().select(index);
			}
		});
	}

	/* Add code completion to the LUT textfield! */
	private void setLutCompletion(Text lutText) {
		String[] lutProposals = IJ.getLuts();
		final ControlDecoration controlDecor = new ControlDecoration(lutText, SWT.TOP | SWT.RIGHT);
		Image image = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION)
				.getImage();
		controlDecor.setDescriptionText("Use CTRL + SPACE for available LUTs!");
		controlDecor.setImage(image);
		// always show decoration
		controlDecor.setShowOnlyOnFocus(false);

		// hide the decoration if the text widget has content
		/*
		 * lutText.addModifyListener(e -> { Text source = (Text) e.getSource(); if
		 * (!source.getText().isEmpty()) { controlDecor.hide(); } else {
		 * controlDecor.show(); } });
		 */
		char[] activChars = new char[] { ' ' };
		KeyStroke keystr;
		try {
			keystr = KeyStroke.getInstance("Ctrl+Space");
			ContentProposalAdapter adapter = new ContentProposalAdapter(lutText, new TextContentAdapter(),
					new SimpleContentProposalProvider(lutProposals), keystr, activChars);
			adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
		} catch (ParseException e1) {
			e1.printStackTrace();
		}
	}

	/*
	 * A method to access all options wrapped in a syncExec for SWT widgets access!
	 */
	public void getFeatureOptions() {

		Display display = Util.getDisplay();

		display.syncExec(() -> {

			toHsb = checkConvertToHsb.getSelection();

			toLab = checkConvertToLab.getSelection();

			useImportMacro = checkUseImportMacro.getSelection();

			useDirectoryDialog = checkUseDirectory.getSelection();

			openStack = checkOpenStack.getSelection();

			useGroups = checkUseGroups.getSelection();

			transferType = transferTypeCombo.getSelectionIndex();

			generatePreview = checkGeneratePreview.getSelection();

			lutOption = optionLUT.getText();

			opacityOption = optionOpacity.getText();

			retrainPreview = checkRetrainPreview.getSelection();
			
			showClassifiedInImageJ=checkShowInImagej.getSelection();

			channelOption = channelSelectionText.getText();

			gaussian = checkGaussianFilter.getSelection();
			gaussianOption = optionGaussian.getText();

			diffOfGaussian = checkDifferenceOfGaussian.getSelection();
			diffGaussianOption = optionDiffGaussian.getText();

			median = checkMedian.getSelection();
			medianOption = optionMedian.getText();

			mean = checkMean.getSelection();
			meanOption = optionsMean.getText();

			variance = checkVariance.getSelection();
			varianceOption = optionsVariance.getText();

			maximum = checkMaximum.getSelection();
			maximumOption = optionsMaximum.getText();

			minimum = checkMinimum.getSelection();
			minimumOption = optionsMinimum.getText();

			edges = checkEdges.getSelection();
			edgesOption = optionsEdges.getText();

			convolve = checkConvolve.getSelection();
			convolveOption = optionConvolve.getText();

			gradientHessian = checkGradientHessian.getSelection();
			gradientHessianOption = optionGradientHessian.getText();

			laplacian = checkLaplacian.getSelection();
			laplacianOption = optionLaplacian.getText();

			lipschitz = checkLipschitz.getSelection();
			lipschitzOption = optionLipschitz.getText();

			gabor = checkGabor.getSelection();
			gaborOption = optionGabor.getText();

			topHat = checkTopHat.getSelection();
			topHatOption = optionsTopHat.getText();

			kuwahara = checkKuwahara.getSelection();
			kuwaharaOption = optionsKuwahara.getText();

		});

	}

	/* Return the path to the training script! */
	public String getPathTrainingScript() {
		Display display = Util.getDisplay();
		display.syncExec(() -> pathTrainingScript = txtTrainingRScript.getText());
		return pathTrainingScript;
	}

	/* Return the path to the classification script! */
	public String getPathClassificationScript() {
		Display display = Util.getDisplay();
		display.syncExec(() -> pathClassificationScript = txtClassificationRScript.getText());
		return pathClassificationScript;
	}

	public String getMacroTextOption() {
		Display display = Util.getDisplay();
		display.syncExec(() -> textOptionMacro = textImageJMacro.getText());
		return textOptionMacro;
	}

	/* Here we layout the ImageJ panel! */
	public void layout() {
		CanvasView canvasView = CanvasView.getCanvas_view();
		canvasView.updatePlotCanvas();
	}

	@Override
	/*
	 * If the ImageJ ROI has changed this method will be called for preview. A job
	 * avoids a recall if the job is busy to update the preview!
	 */
	public void roiModified(ImagePlus imp, int id) {
		/* Call update method in main class! */
		model.roiModified(imp, id);
	}
}