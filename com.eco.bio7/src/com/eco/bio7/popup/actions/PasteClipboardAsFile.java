/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.popup.actions;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.ExpressionInfo;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.ISources;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ISetSelectionTarget;

/**
 * Pastes the text of the clipboard (e.g., a script or a macro copied from a web
 * page) as a new file into the selected project or folder of the Project
 * Explorer (into the folder of a selected file). The type of the code is
 * detected (R, ImageJ macro, Java, Python, ...) and gives the extension of the
 * suggested file name, which can be changed in the dialog. The file is opened
 * in its editor.
 */
public class PasteClipboardAsFile extends AbstractHandler {

	/** A detected type of code: the extension, a name for the dialog and the default file name. */
	public record CodeType(String extension, String label, String baseName) {
	}

	private static final CodeType R = new CodeType("R", "R script", "script");
	private static final CodeType RMARKDOWN = new CodeType("Rmd", "R Markdown", "document");
	private static final CodeType MARKDOWN = new CodeType("md", "Markdown", "document");
	private static final CodeType IJMACRO = new CodeType("ijm", "ImageJ macro", "macro");
	private static final CodeType JAVA = new CodeType("java", "Java class", "Main");
	private static final CodeType PYTHON = new CodeType("py", "Python script", "script");
	private static final CodeType GROOVY = new CodeType("groovy", "Groovy script", "script");
	private static final CodeType BEANSHELL = new CodeType("bsh", "BeanShell script", "script");
	private static final CodeType JAVASCRIPT = new CodeType("js", "JavaScript", "script");
	private static final CodeType HTML = new CodeType("html", "HTML", "page");
	private static final CodeType LATEX = new CodeType("tex", "LaTeX", "document");
	private static final CodeType SWEAVE = new CodeType("Rnw", "Sweave (R and LaTeX)", "document");
	private static final CodeType TEXT = new CodeType("txt", "Text", "clipboard");

	/** The key context of the shortcut (Cmd+Shift+V, see plugin.xml). */
	public static final String CONTEXT_ID = "com.eco.bio7.projectExplorerContext";
	private static final String PROJECT_EXPLORER_ID = "org.eclipse.ui.navigator.ProjectExplorer";

	/**
	 * Activates the key context of the shortcut while the Project Explorer is the
	 * active part (there it overrides the binding of the window context, e.g.,
	 * "Open from Clipboard" of JDT, which stays in the editors and other views).
	 */
	public static void activateContext(IWorkbenchWindow window) {

		IContextService contexts = window.getService(IContextService.class);
		if (contexts == null) {
			return;
		}
		contexts.activateContext(CONTEXT_ID, new Expression() {

			@Override
			public EvaluationResult evaluate(IEvaluationContext context) {

				Object partId = context.getVariable(ISources.ACTIVE_PART_ID_NAME);
				return EvaluationResult.valueOf(PROJECT_EXPLORER_ID.equals(partId));
			}

			@Override
			public void collectExpressionInfo(ExpressionInfo info) {

				info.addVariableNameAccess(ISources.ACTIVE_PART_ID_NAME);
			}
		});
	}

	@Override
	public Object execute(ExecutionEvent event) {

		Shell shell = HandlerUtil.getActiveShell(event);
		IContainer container = targetContainer(HandlerUtil.getCurrentSelection(event));
		if (container == null) {
			MessageDialog.openInformation(shell, "Paste as File", "Please select a project or a folder in the Project Explorer.");
			return null;
		}
		String text = clipboardText(shell);
		if (text == null || text.isBlank()) {
			MessageDialog.openInformation(shell, "Paste as File", "The clipboard contains no text.");
			return null;
		}
		CodeType type = detect(text);
		String suggested = uniqueName(container, suggestedBaseName(text, type), type.extension());
		InputDialog dialog = new InputDialog(shell, "Paste as File",
				"Detected: " + type.label() + ". Name of the new file in '" + container.getFullPath().makeRelative() + "' (the extension can be changed):",
				suggested, name -> validate(container, name));
		if (dialog.open() != Window.OK) {
			return null;
		}
		IFile file = container.getFile(new Path(dialog.getValue().trim()));
		try {
			String charset = container.getDefaultCharset();
			file.create(new ByteArrayInputStream(text.getBytes(Charset.forName(charset))), true, null);
		} catch (CoreException | RuntimeException e) {
			MessageDialog.openError(shell, "Paste as File", "The file could not be created:\n" + e.getMessage());
			return null;
		}
		IWorkbenchPage page = HandlerUtil.getActiveWorkbenchWindow(event) != null ? HandlerUtil.getActiveWorkbenchWindow(event).getActivePage() : null;
		IWorkbenchPart part = HandlerUtil.getActivePart(event);
		if (part instanceof ISetSelectionTarget target) {
			target.selectReveal(new StructuredSelection(file));
		}
		if (page != null) {
			try {
				IDE.openEditor(page, file);
			} catch (PartInitException e) {
				/* The file is created, there is no editor for it */
			}
		}
		return null;
	}

	/* The selected project or folder, or the folder of a selected file */
	private static IContainer targetContainer(ISelection selection) {

		if (!(selection instanceof IStructuredSelection structured) || structured.isEmpty()) {
			return null;
		}
		IResource resource = Adapters.adapt(structured.getFirstElement(), IResource.class);
		if (resource instanceof IContainer container && resource.getType() != IResource.ROOT) {
			return container.isAccessible() ? container : null;
		}
		if (resource instanceof IFile file) {
			return file.getParent();
		}
		return null;
	}

	private static String clipboardText(Shell shell) {

		Clipboard clipboard = new Clipboard(shell.getDisplay());
		try {
			return (String) clipboard.getContents(TextTransfer.getInstance());
		} finally {
			clipboard.dispose();
		}
	}

	private static String validate(IContainer container, String name) {

		String trimmed = name == null ? "" : name.trim();
		if (trimmed.isEmpty()) {
			return "Please enter a file name.";
		}
		IStatus status = ResourcesPlugin.getWorkspace().validateName(trimmed, IResource.FILE);
		if (!status.isOK()) {
			return status.getMessage();
		}
		if (container.getFile(new Path(trimmed)).exists() || container.findMember(trimmed) != null) {
			return "A resource with this name already exists.";
		}
		return null;
	}

	/* The name, else with a number (script.R, script_2.R, ...) */
	private static String uniqueName(IContainer container, String base, String extension) {

		String name = base + "." + extension;
		for (int i = 2; container.findMember(name) != null; i++) {
			name = base + "_" + i + "." + extension;
		}
		return name;
	}

	/* The name of the Java class or of the ImageJ macro, else the default name of the type */
	private static String suggestedBaseName(String text, CodeType type) {

		if (type == JAVA) {
			Matcher m = Pattern.compile("public\\s+(?:final\\s+|abstract\\s+)*(?:class|interface|enum|record)\\s+([A-Za-z_]\\w*)").matcher(text);
			if (m.find()) {
				/* The name of a public class must be the file name */
				return m.group(1);
			}
		}
		if (type == IJMACRO) {
			Matcher m = Pattern.compile("macro\\s+\"([^\"]+)\"").matcher(text);
			if (m.find()) {
				String name = m.group(1).replaceAll("\\[.*?\\]", "").trim().replaceAll("[^\\w\\- ]", "").replace(' ', '_');
				if (!name.isEmpty()) {
					return name;
				}
			}
		}
		return type.baseName();
	}

	/** Detects the type of the code (a simple score of typical patterns of each language). */
	public static CodeType detect(String text) {

		String t = text.strip();
		/* Documents with a clear start or structure */
		if (Pattern.compile("(?m)^```\\{r").matcher(t).find() || (t.startsWith("---") && Pattern.compile("(?m)^output\\s*:").matcher(t).find())) {
			return RMARKDOWN;
		}
		if (Pattern.compile("(?m)^<<[^>]*>>=").matcher(t).find()) {
			return SWEAVE;
		}
		if (t.contains("\\documentclass") || t.contains("\\begin{document}")) {
			return LATEX;
		}
		if (Pattern.compile("^(<!DOCTYPE html|<html)", Pattern.CASE_INSENSITIVE).matcher(t).find()) {
			return HTML;
		}
		int r = score(t, 3, "<-", "library\\(", "require\\(", "function\\s*\\(", "data\\.frame\\(", "ggplot\\(", "\\$\\w+", "%>%", "\\|>", "\\bNULL\\b", "\\bTRUE\\b|\\bFALSE\\b")
				+ score(t, 1, "\\bc\\(", "<-\\s*function")
				/* Common R functions and the formula (also code with = instead of <-) */
				+ score(t, 2, "\\b(plot|hist|boxplot|summary|str|head|print|cat|paste0?|seq|rep|matrix|apply|sapply|lapply|vapply)\\(",
						"\\b(rnorm|runif|rbinom|rpois|lm|glm|aov|t\\.test|cor|mean|sd|median|var|quantile)\\(", "\\b(read\\.csv|read\\.table|write\\.csv|setwd|getwd|install\\.packages)\\(",
						"\\w\\s*~\\s*[\\w.]");
		int ijm = score(t, 4, "\\brun\\(\"", "\\bmacro\\s+\"", "setBatchMode\\(", "roiManager\\(", "getTitle\\(\\)", "selectWindow\\(", "selectImage\\(", "newImage\\(", "getPixel\\(",
				"nResults", "setResult\\(", "makeRectangle\\(|makeOval\\(|makePolygon\\(", "\\bIJ\\.\\w+\\(\"")
				+ score(t, 1, "\\bvar\\s+\\w+\\s*=");
		int java = score(t, 4, "\\bpublic\\s+(?:final\\s+)?class\\b", "^\\s*package\\s+[\\w.]+;", "(?m)^import\\s+[\\w.]+(\\.\\*)?;", "\\bpublic\\s+static\\s+void\\s+main", "System\\.out\\.print",
				"\\b(private|protected|public)\\s+\\w+(<[^>]*>)?\\s+\\w+\\s*\\(", "\\bnew\\s+\\w+(<[^>]*>)?\\(");
		int python = score(t, 4, "(?m)^\\s*def\\s+\\w+\\s*\\(.*\\)\\s*:", "(?m)^\\s*(from\\s+[\\w.]+\\s+)?import\\s+[\\w., ]+$", "(?m)^\\s*(if|for|while|elif|else|try|except|with)\\b.*:\\s*$",
				"(?m)^\\s*class\\s+\\w+(\\(.*\\))?\\s*:", "\\bself\\.", "\\bNone\\b|\\bTrue\\b|\\bFalse\\b", "__name__");
		int groovy = score(t, 4, "\\bdef\\s+\\w+\\s*=", "\\bprintln\\b", "->\\s*\\w", "\\.each\\s*\\{");
		int js = score(t, 4, "\\b(const|let)\\s+\\w+\\s*=", "=>", "console\\.log\\(", "\\bfunction\\s+\\w+\\s*\\(.*\\)\\s*\\{", "document\\.", "===");
		int markdown = score(t, 3, "(?m)^#{1,6}\\s+\\S", "(?m)^\\s*[-*]\\s+\\S", "\\[[^\\]]+\\]\\([^)]+\\)", "(?m)^```");
		/* Semicolons at the line ends: not R, not Python, not Markdown */
		int lines = Math.max(1, t.split("\\R").length);
		long semicolons = t.lines().filter(l -> l.strip().endsWith(";")).count();
		if (semicolons * 3 >= lines) {
			r -= 4;
			python -= 6;
			markdown -= 6;
		}
		/* ImageJ macros use print() and var like JavaScript, but run("...") etc. is unique */
		if (ijm >= 4) {
			js -= 4;
		}
		/* Java statements are also BeanShell: only with the Java structure as Java */
		if (java >= 4 && !Pattern.compile("\\b(class|interface|enum|record)\\s+\\w+").matcher(t).find()) {
			int bsh = java;
			java = 0;
			if (bsh > Math.max(Math.max(r, ijm), Math.max(python, js))) {
				return BEANSHELL;
			}
		}
		CodeType best = TEXT;
		int bestScore = 3;
		int[] scores = { ijm, r, java, python, js, groovy, markdown };
		CodeType[] types = { IJMACRO, R, JAVA, PYTHON, JAVASCRIPT, GROOVY, MARKDOWN };
		for (int i = 0; i < scores.length; i++) {
			if (scores[i] > bestScore) {
				bestScore = scores[i];
				best = types[i];
			}
		}
		return best;
	}

	/* The weight for each pattern which is found */
	private static int score(String text, int weight, String... patterns) {

		int score = 0;
		for (String pattern : patterns) {
			if (Pattern.compile(pattern).matcher(text).find()) {
				score += weight;
			}
		}
		return score;
	}
}
