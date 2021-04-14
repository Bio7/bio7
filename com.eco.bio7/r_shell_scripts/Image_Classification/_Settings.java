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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.image.RImageMethodsView;

public class _Settings {
	private _ModelGui modelGui;

	public _Settings(_ModelGui modelGui) {
		this.modelGui = modelGui;
	}

	public boolean read(BufferedReader reader) {
		boolean value = false;
		try {
			value = Boolean.parseBoolean(reader.readLine());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return value;
	}
	/*
	 * For each value follow the sequence of the GUI. Load and save actions must
	 * follow the same sequence (simple text file)!
	 */
	public void loadScript(String file) {

		if (file == null) {
			file = Bio7Dialog.openFile(new String[] { "*.txt", "*" });
		}
		if (file != null) {
			File fil = new File(file);

			try {

				FileReader fileReader = new FileReader(fil);
				BufferedReader reader = new BufferedReader(fileReader);

				modelGui.checkConvertToHsb.setSelection(read(reader));

				modelGui.checkConvertToLab.setSelection(read(reader));

				modelGui.channelSelectionText.setText(reader.readLine());

				modelGui.checkGaussianFilter.setSelection(read(reader));
				modelGui.optionGaussian.setText(reader.readLine());

				modelGui.checkDifferenceOfGaussian.setSelection(read(reader));
				modelGui.optionDiffGaussian.setText(reader.readLine());

				modelGui.checkMean.setSelection(read(reader));
				modelGui.optionsMean.setText(reader.readLine());

				modelGui.checkMedian.setSelection(read(reader));
				modelGui.optionMedian.setText(reader.readLine());

				modelGui.checkMinimum.setSelection(read(reader));
				modelGui.optionsMinimum.setText(reader.readLine());

				modelGui.checkVariance.setSelection(read(reader));
				modelGui.optionsVariance.setText(reader.readLine());

				modelGui.checkMaximum.setSelection(read(reader));
				modelGui.optionsMaximum.setText(reader.readLine());

				modelGui.checkGradientHessian.setSelection(read(reader));
				modelGui.optionGradientHessian.setText(reader.readLine());

				modelGui.checkLaplacian.setSelection(read(reader));
				modelGui.optionLaplacian.setText(reader.readLine());

				modelGui.checkEdges.setSelection(read(reader));
				modelGui.optionsEdges.setText(reader.readLine());

				modelGui.checkLipschitz.setSelection(read(reader));
				modelGui.optionLipschitz.setText(reader.readLine());

				modelGui.checkGabor.setSelection(read(reader));
				modelGui.optionGabor.setText(reader.readLine());

				modelGui.checkTopHat.setSelection(read(reader));
				modelGui.optionsTopHat.setText(reader.readLine());

				modelGui.checkKuwahara.setSelection(read(reader));
				modelGui.optionsKuwahara.setText(reader.readLine());

				modelGui.checkConvolve.setSelection(read(reader));
				modelGui.optionConvolve.setText(reader.readLine().replace("\\n", System.lineSeparator()));

				/* Second tab. Settings for the script paths! */

				modelGui.checkUseImportMacro.setSelection(read(reader));

				modelGui.checkUseDirectory.setSelection(read(reader));
				
				modelGui.checkOpenStack.setSelection(read(reader));
				
				modelGui.checkUseGroups.setSelection(read(reader));

				modelGui.textImageJMacro.setText(reader.readLine());

				modelGui.txtTrainingRScript.setText(reader.readLine());

				modelGui.txtClassificationRScript.setText(reader.readLine());

				int selection = Integer.parseInt(reader.readLine());
				modelGui.transferTypeCombo.select(selection);
				RImageMethodsView.getTransferTypeCombo().select(selection);
				
				modelGui.checkGeneratePreview.setSelection(read(reader));
				
				modelGui.checkRetrainPreview.setSelection(read(reader));
				
				modelGui.optionOpacity.setText(reader.readLine());
				
				modelGui.optionLUT.setText(reader.readLine());
				
				modelGui.checkShowInImagej.setSelection(read(reader));
				
				modelGui.checkApplyPostImagejMacro.setSelection(read(reader));
				
				modelGui.textPostMacro.setText(reader.readLine());

				reader.close();

			} catch (IOException ex) {

			}

		}
	}

	/*
	 * For each value follow the sequence of the GUI. Load and save actions must
	 * follow the same sequence (simple text file)!
	 */
	public void saveScript() {
		StringBuffer buffer = new StringBuffer();
		String sep = System.getProperty("line.separator");
		// buffer.append(sep);
		buffer.append(modelGui.checkConvertToHsb.getSelection());
		buffer.append(sep);

		buffer.append(modelGui.checkConvertToLab.getSelection());
		buffer.append(sep);

		buffer.append(modelGui.channelSelectionText.getText());
		buffer.append(sep);
		buffer.append(modelGui.checkGaussianFilter.getSelection());
		buffer.append(sep);
		buffer.append(modelGui.optionGaussian.getText());
		buffer.append(sep);

		buffer.append(modelGui.checkDifferenceOfGaussian.getSelection());
		buffer.append(sep);
		buffer.append(modelGui.optionDiffGaussian.getText());
		buffer.append(sep);

		buffer.append(modelGui.checkMean.getSelection());
		buffer.append(sep);
		buffer.append(modelGui.optionsMean.getText());
		buffer.append(sep);

		buffer.append(modelGui.checkMedian.getSelection());
		buffer.append(sep);
		buffer.append(modelGui.optionMedian.getText());
		buffer.append(sep);

		buffer.append(modelGui.checkMinimum.getSelection());
		buffer.append(sep);
		buffer.append(modelGui.optionsMinimum.getText());
		buffer.append(sep);

		buffer.append(modelGui.checkVariance.getSelection());
		buffer.append(sep);
		buffer.append(modelGui.optionsVariance.getText());
		buffer.append(sep);

		buffer.append(modelGui.checkMaximum.getSelection());
		buffer.append(sep);
		buffer.append(modelGui.optionsMaximum.getText());
		buffer.append(sep);

		buffer.append(modelGui.checkGradientHessian.getSelection());
		buffer.append(sep);
		buffer.append(modelGui.optionGradientHessian.getText());
		buffer.append(sep);

		buffer.append(modelGui.checkLaplacian.getSelection());
		buffer.append(sep);
		buffer.append(modelGui.optionLaplacian.getText());
		buffer.append(sep);

		buffer.append(modelGui.checkEdges.getSelection());
		buffer.append(sep);
		buffer.append(modelGui.optionsEdges.getText());
		buffer.append(sep);

		buffer.append(modelGui.checkLipschitz.getSelection());
		buffer.append(sep);
		buffer.append(modelGui.optionLipschitz.getText());
		buffer.append(sep);

		buffer.append(modelGui.checkGabor.getSelection());
		buffer.append(sep);
		buffer.append(modelGui.optionGabor.getText());
		buffer.append(sep);

		buffer.append(modelGui.checkTopHat.getSelection());
		buffer.append(sep);
		buffer.append(modelGui.optionsTopHat.getText());
		buffer.append(sep);

		buffer.append(modelGui.checkKuwahara.getSelection());
		buffer.append(sep);
		buffer.append(modelGui.optionsKuwahara.getText());
		buffer.append(sep);

		buffer.append(modelGui.checkConvolve.getSelection());
		buffer.append(sep);
		buffer.append(modelGui.optionConvolve.getText().replace(System.lineSeparator(), "\\n"));
		buffer.append(sep);

		/* Second tab. Settings for the script paths! */

		buffer.append(modelGui.checkUseImportMacro.getSelection());
		buffer.append(sep);

		buffer.append(modelGui.checkUseDirectory.getSelection());
		buffer.append(sep);
		
		buffer.append(modelGui.checkOpenStack.getSelection());
		buffer.append(sep);
		
		buffer.append(modelGui.checkUseGroups.getSelection());
		buffer.append(sep);

		buffer.append(modelGui.getMacroTextOption());
		buffer.append(sep);

		buffer.append(modelGui.getPathTrainingScript());
		buffer.append(sep);

		buffer.append(modelGui.getPathClassificationScript());
		buffer.append(sep);

		buffer.append(modelGui.transferTypeCombo.getSelectionIndex());
		buffer.append(sep);
		
		buffer.append(modelGui.checkGeneratePreview.getSelection());
		buffer.append(sep);
		
		buffer.append(modelGui.checkRetrainPreview.getSelection());
		buffer.append(sep);
		
		buffer.append(modelGui.optionOpacity.getText());
		buffer.append(sep);
		
		buffer.append(modelGui.optionLUT.getText());
		buffer.append(sep);
		
		buffer.append(modelGui.checkShowInImagej.getSelection());
		buffer.append(sep);
		
		buffer.append(modelGui.checkApplyPostImagejMacro.getSelection());
		buffer.append(sep);
		
		buffer.append(modelGui.getPathImageJMacroPostScript());
		
		String file = Bio7Dialog.saveFile("*.txt");
		if (file != null) {
			File fil = new File(file);
			FileWriter fileWriter = null;
			try {
				fileWriter = new FileWriter(fil);

				BufferedWriter buffWriter = new BufferedWriter(fileWriter);

				String write = buffer.toString();
				buffWriter.write(write, 0, write.length());
				buffWriter.close();
			} catch (IOException ex) {

			} finally {
				try {
					fileWriter.close();
				} catch (IOException ex) {

				}
			}
		}

	}

}
