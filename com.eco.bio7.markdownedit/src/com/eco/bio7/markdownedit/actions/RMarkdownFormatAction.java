package com.eco.bio7.markdownedit.actions;

import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import com.eco.bio7.markdownedit.editors.MarkdownEditor;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor;
import com.vladsch.flexmark.formatter.internal.Formatter;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.profiles.pegdown.Extensions;
import com.vladsch.flexmark.profiles.pegdown.PegdownOptionsAdapter;
import com.vladsch.flexmark.util.options.DataHolder;
import com.vladsch.flexmark.util.options.MutableDataSet;

public class RMarkdownFormatAction extends AbstractHandler {

	static final DataHolder OPTIONS = PegdownOptionsAdapter.flexmarkOptions(Extensions.ALL);

	static final MutableDataSet FORMAT_OPTIONS = new MutableDataSet();
	static {
		// copy extensions from Pegdown compatible to Formatting
		FORMAT_OPTIONS.set(Parser.EXTENSIONS, OPTIONS.get(Parser.EXTENSIONS));
	}

	static final Parser PARSER = Parser.builder(OPTIONS).build();
	static final Formatter RENDERER = Formatter.builder(FORMAT_OPTIONS).build();

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart editore = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getActiveEditor();
		MarkdownEditor markdownEditor = (MarkdownEditor) editore;

		IDocumentProvider dp = markdownEditor.getDocumentProvider();
		IDocument doc = dp.getDocument(markdownEditor.getEditorInput());
		// String source = doc.get();

		ISelectionProvider sp = markdownEditor.getSelectionProvider();

		ISelection selectionsel = sp.getSelection();

		ITextSelection selection = (ITextSelection) selectionsel;

		int offset = selection.getOffset();
		int length = selection.getLength();
		String inhalt = null;

		try {

			inhalt = doc.get(offset, length);
		} catch (BadLocationException e) {

			e.printStackTrace();
		}

		/* The parser doesn't like Windows linebreak! */
		//inhalt = inhalt.replaceAll("\r", "");
		Node document = PARSER.parse(inhalt);
		String commonmark = RENDERER.render(document);

		try {
			doc.replace(offset, length, commonmark);
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

}