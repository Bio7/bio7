/*******************************************************************************
 * Copyright (c) 2007-2012 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.compile.utils;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import com.eco.bio7.javaeditor.Bio7EditorPlugin;
import com.eco.bio7.javaeditor.ast.CompilerAst;


public class ModelClassAst {

	private boolean classBody = true;
	private String source = null;

	public void parseAST(IEditorPart JEditor) {
		IPreferenceStore store = Bio7EditorPlugin.getDefault().getPreferenceStore();
		//classBody = store.getBoolean("classbody");
		ITextEditor editor = (ITextEditor) JEditor;
		IDocumentProvider dp = editor.getDocumentProvider();
		IDocument doc = dp.getDocument(editor.getEditorInput());

		/*if (classBody) {
			Get the source of a classbody with the help of the Janino API!
			ClassBodyEvaluator cbe = null;
			try {
				cbe = (ClassBodyEvaluator) CompilerFactoryFactory.getDefaultCompilerFactory().newClassBodyEvaluator();

				cbe.setClassName("Model");

			} catch (Exception e2) {

				e2.printStackTrace();
			}
			cbe.setExtendedClass(Model.class);

			try {
				source = cbe.cookSource("Model", new StringReader(doc.get()));
			} catch (CompileException e) {

				e.printStackTrace();

			} catch (IOException e) {

				e.printStackTrace();
			}
		} else {*/
			/* Get the source of a regular class! */
			source = doc.get();
		//}
		CompilerAst compilerAst = new CompilerAst(source);
		
		

		//ClassModel cm = new ClassModel();

		//compilerAst.parseAst(cm);

		//JavaEditor je = (JavaEditor) JEditor;
		/*Set a reference of the CompilationUnit in the Java editor!*/
       // je.setCompUnit(compilerAst.getCu());
        
		//je.outlineInputChanged(je.currentClassModel, cm);
		

	}

}
