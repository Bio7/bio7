/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************/
package com.eco.bio7.actions;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import com.eco.bio7.compile.Model;
import com.eco.bio7.compile.utils.ScanClassPath;
import com.eco.bio7.methods.Compiled;
import com.eco.bio7.simulation.ScriptExport;
import com.eco.bio7.simulation.SimulationExport;
import com.eco.bio7.spatial.HtmlSimulation;

/**
 * Exports a simulation model of the simulation API (com.eco.bio7.simulation) as
 * an interactive HTML page: the model of the active Java editor (if it extends
 * GridModel, ValueGridModel or AgentModel), else the source of the compiled
 * model. Errors of the translation are marked in the Java file. The page
 * runs the same model in the web browser.
 */
public final class ExportSimulation {

	private static final Pattern SIMULATION = Pattern.compile("extends\\s+(com\\.eco\\.bio7\\.simulation\\.)?(GridModel|ValueGridModel|GeoGridModel|AgentModel|GeoAgentModel)\\b");

	private ExportSimulation() {
	}

	/** The type of the problem markers of the export (errors of the translation in the Java editor). */
	public static final String MARKER_ATTRIBUTE = "com.eco.bio7.simulationExport";

	/** Asks for the options and exports the model (in the thread of the user interface). */
	public static void run(Shell shell) {
		/* A model of Python or R (the script in the active editor or the running model of a script) */
		ScriptSource script = findScript();
		if (script != null) {
			runScript(shell, script);
			return;
		}
		Source source = findSource();
		if (source == null) {
			MessageDialog.openInformation(shell, "Export Simulation as HTML",
					"No simulation model found.\n\nOpen the Java class of the model in the editor (a class which extends GridModel, ValueGridModel, GeoGridModel, AgentModel or GeoAgentModel of com.eco.bio7.simulation) or compile it first.");
			return;
		}
		String[] classpath = bio7Classpath();
		/* A geospatial model: the layers which the running model loaded with GDAL */
		SimulationExport.Options first = new SimulationExport.Options();
		first.geo = geoData(source.name);
		assets(source, first);
		SimulationExport.Result result = SimulationExport.create(source.text, source.name, classpath, first);
		markers(source.file, result.errors);
		if (!result.ok()) {
			List<String> errors = result.errors;
			StringBuilder text = new StringBuilder("The model " + source.name + " cannot be exported to JavaScript:\n\n");
			for (int i = 0; i < Math.min(15, errors.size()); i++) {
				text.append(errors.get(i)).append('\n');
			}
			if (errors.size() > 15) {
				text.append("... (").append(errors.size() - 15).append(" more, see the console)\n");
			}
			if (source.file != null) {
				text.append("\nThe lines are marked in the Java editor.");
			}
			for (String e : errors) {
				System.err.println(source.name + ": " + e);
			}
			MessageDialog.openError(shell, "Export Simulation as HTML", text.toString());
			return;
		}
		String folder = source.folder != null ? source.folder : System.getProperty("user.home");
		ExportSimulationDialog dialog = new ExportSimulationDialog(shell, result.modelName, new File(folder, result.modelName + ".html").getPath(), result.title, result.description,
				"AgentModel".equals(result.kind), "GeoGridModel".equals(result.kind) || "GeoAgentModel".equals(result.kind));
		if (dialog.open() != Window.OK) {
			return;
		}
		SimulationExport.Options options = new SimulationExport.Options();
		options.geo = first.geo;
		options.images = first.images;
		options.models = first.models;
		options.basemap = dialog.getBasemap();
		options.title = dialog.getPageTitle();
		options.description = dialog.getDescription();
		options.speed = dialog.getSpeed();
		/* The seed of the running model (the field Seed of the settings) */
		options.seed = startSeed(source.name);
		options.autoplay = dialog.isAutoplay();
		options.theme = dialog.getTheme();
		options.layout = dialog.getPageLayout();
		options.accent = dialog.getAccentColor();
		dialog.applyScene(options);
		result = SimulationExport.create(source.text, source.name, classpath, options);
		String file = dialog.getFile();
		try {
			result.page.export(file);
		} catch (IOException e) {
			MessageDialog.openError(shell, "Export Simulation as HTML", "The file could not be written:\n" + e.getMessage());
			return;
		}
		for (String w : result.warnings) {
			System.out.println(source.name + ": " + w);
		}
		System.out.println("Exported the simulation " + result.modelName + ": " + file);
		refresh(file);
		if (dialog.isOpen()) {
			HtmlSimulation.open(file);
		}
	}

	/* ===================== Models of Python and R ===================== */

	/* A script and its running model (null: the script was not run) */
	private static final class ScriptSource {
		String text, name, folder;
		com.eco.bio7.simulation.Simulation model;
	}

	private static boolean isScript(String name) {
		String n = name.toLowerCase(java.util.Locale.ROOT);
		return n.endsWith(".py") || n.endsWith(".r");
	}

	/* The seed of the running model of the class (the field Seed of the settings; else 42) */
	private static int startSeed(String javaFile) {
		Model model = Compiled.getModel();
		if (model instanceof com.eco.bio7.simulation.Simulation sim && javaFile != null && javaFile.equals(sim.getClass().getSimpleName() + ".java")) {
			return sim.getStartSeed();
		}
		return com.eco.bio7.simulation.Simulation.DEFAULT_SEED;
	}

	private static ScriptSource findScript() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage page = window == null ? null : window.getActivePage();
		IEditorPart editor = page == null ? null : page.getActiveEditor();
		Model model = Compiled.getModel();
		com.eco.bio7.simulation.ScriptSimulation.Host host = model instanceof com.eco.bio7.simulation.ScriptSimulation.Host h ? h : null;
		if (editor instanceof ITextEditor te && editor.getEditorInput() instanceof IFileEditorInput input && isScript(input.getFile().getName())) {
			IDocument doc = te.getDocumentProvider().getDocument(editor.getEditorInput());
			String text = doc == null ? "" : doc.get();
			if (!text.contains("simulation(")) {
				return null;
			}
			ScriptSource s = new ScriptSource();
			s.text = text;
			s.name = input.getFile().getName();
			s.folder = input.getFile().getProject().getLocation() == null ? null : input.getFile().getProject().getLocation().toOSString();
			File running = host == null ? null : host.script().sourceFile();
			File edited = input.getFile().getLocation() == null ? null : input.getFile().getLocation().toFile();
			boolean same = running != null && edited != null && running.getAbsoluteFile().equals(edited.getAbsoluteFile());
			/* The running model of the script (else the script must be run first) */
			s.model = same || (running == null && host != null && host.script().spec().name != null && text.contains(host.script().spec().name)) ? model instanceof com.eco.bio7.simulation.Simulation sim ? sim : null : null;
			return s;
		}
		if (editor instanceof ITextEditor && editor.getEditorInput() instanceof IFileEditorInput input && input.getFile().getName().endsWith(".java")) {
			return null;
		}
		if (host != null && host.script().sourceFile() != null && host.script().sourceFile().exists()) {
			try {
				ScriptSource s = new ScriptSource();
				File f = host.script().sourceFile();
				s.text = java.nio.file.Files.readString(f.toPath());
				s.name = f.getName();
				s.folder = f.getParent();
				for (IFile w : ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(f.toURI())) {
					if (w.getProject().getLocation() != null) {
						s.folder = w.getProject().getLocation().toOSString();
					}
				}
				s.model = (com.eco.bio7.simulation.Simulation) model;
				return s;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	private static void runScript(Shell shell, ScriptSource source) {
		String title = "Export Simulation as HTML";
		if (source.model == null) {
			MessageDialog.openInformation(shell, title, "Run the script " + source.name
					+ " first: simulation(...) at the end of the script starts the model in Bio7. The page gets the settings and the data of the running model.");
			return;
		}
		SimulationExport.Result result = ScriptExport.create(source.model, source.text, source.name, null);
		if (!result.ok()) {
			MessageDialog.openError(shell, title, "The model " + source.name + " cannot be exported:\n\n" + String.join("\n", result.errors));
			return;
		}
		String folder = source.folder != null ? source.folder : System.getProperty("user.home");
		ExportSimulationDialog dialog = new ExportSimulationDialog(shell, result.modelName, new File(folder, result.modelName + ".html").getPath(), result.title, result.description,
				"AgentModel".equals(result.kind), "GeoGridModel".equals(result.kind) || "GeoAgentModel".equals(result.kind));
		boolean python = source.name.toLowerCase(java.util.Locale.ROOT).endsWith(".py");
		dialog.setNote("The script runs in the web browser with " + (python ? "Pyodide (Python and numpy in WebAssembly)" : "webR (R in WebAssembly)")
				+ ", loaded from the internet when the page is opened. The page gets the settings and the data of the running model.");
		if (dialog.open() != Window.OK) {
			return;
		}
		SimulationExport.Options options = new SimulationExport.Options();
		options.basemap = dialog.getBasemap();
		options.title = dialog.getPageTitle();
		options.description = dialog.getDescription();
		options.speed = dialog.getSpeed();
		options.seed = source.model != null ? source.model.getStartSeed() : com.eco.bio7.simulation.Simulation.DEFAULT_SEED;
		options.autoplay = dialog.isAutoplay();
		options.theme = dialog.getTheme();
		options.layout = dialog.getPageLayout();
		options.accent = dialog.getAccentColor();
		dialog.applyScene(options);
		result = ScriptExport.create(source.model, source.text, source.name, options);
		String file = dialog.getFile();
		try {
			result.page.export(file);
		} catch (IOException e) {
			MessageDialog.openError(shell, title, "The file could not be written:\n" + e.getMessage());
			return;
		}
		System.out.println("Exported the simulation " + result.modelName + ": " + file);
		refresh(file);
		if (dialog.isOpen()) {
			HtmlSimulation.open(file);
		}
	}

	/*
	 * The images and the 3D models of the model: of the running model of the class,
	 * else of the calls loadImage("name", "file") and loadModel(...) in the source
	 * (relative to the project)
	 */
	private static void assets(Source source, SimulationExport.Options options) {
		Model model = Compiled.getModel();
		String name = source.name.replaceFirst("\\.java$", "");
		if (model instanceof com.eco.bio7.simulation.Simulation sim && model.getClass().getSimpleName().equals(name)) {
			options.images.putAll(sim.getImageFiles());
			options.models.putAll(sim.getModelFiles());
		}
		Matcher m = Pattern.compile("load(Image|Model)\\(\\s*\"([^\"]+)\"\\s*,\\s*\"([^\"]+)\"").matcher(source.text);
		while (m.find()) {
			java.util.Map<String, String> target = m.group(1).equals("Image") ? options.images : options.models;
			if (!target.containsKey(m.group(2))) {
				File f = new File(m.group(3));
				if (!f.isAbsolute() && source.file != null && source.file.getProject().getLocation() != null) {
					f = new File(source.file.getProject().getLocation().toFile(), m.group(3));
				}
				target.put(m.group(2), f.getPath());
			}
		}
	}

	/* The data of the running geospatial model of the class (null if none) */
	private static java.util.Map<String, Object> geoData(String fileName) {
		Model model = Compiled.getModel();
		String name = fileName.replaceFirst("\\.java$", "");
		if (model instanceof com.eco.bio7.simulation.GeoGridModel geo && model.getClass().getSimpleName().equals(name)) {
			java.util.Map<String, Object> data = geo.exportData();
			return ((java.util.Map<?, ?>) data.get("layers")).isEmpty() && geo.width == 0 ? null : data;
		}
		return null;
	}

	/* The errors of the translation as problem markers of the Java file (the old ones are removed) */
	private static void markers(IFile file, List<String> errors) {
		if (file == null || !file.exists()) {
			return;
		}
		try {
			for (IMarker m : file.findMarkers(IMarker.PROBLEM, false, IResource.DEPTH_ZERO)) {
				if (m.getAttribute(MARKER_ATTRIBUTE, false)) {
					m.delete();
				}
			}
			Pattern line = Pattern.compile("^Line (\\d+): (.*)$");
			for (String e : errors) {
				Matcher matcher = line.matcher(e);
				IMarker m = file.createMarker(IMarker.PROBLEM);
				m.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
				m.setAttribute(IMarker.MESSAGE, "HTML export: " + (matcher.matches() ? matcher.group(2) : e));
				m.setAttribute(IMarker.LINE_NUMBER, matcher.matches() ? Integer.parseInt(matcher.group(1)) : 1);
				m.setAttribute(MARKER_ATTRIBUTE, true);
			}
		} catch (CoreException ex) {
			ex.printStackTrace();
		}
	}

	/* The source of a model: the text, the file name and the folder */
	private static final class Source {
		String text, name, folder;
		IFile file;
	}

	private static Source findSource() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage page = window == null ? null : window.getActivePage();
		IEditorPart editor = page == null ? null : page.getActiveEditor();
		/* The active Java editor (also unsaved changes) */
		if (editor instanceof ITextEditor te && editor.getEditorInput() instanceof IFileEditorInput input && input.getFile().getName().endsWith(".java")) {
			IDocument doc = te.getDocumentProvider().getDocument(editor.getEditorInput());
			if (doc != null && SIMULATION.matcher(doc.get()).find()) {
				return source(doc.get(), input.getFile());
			}
		}
		/* The compiled model: its source in the workspace */
		Model model = Compiled.getModel();
		if (model instanceof com.eco.bio7.simulation.Simulation) {
			IFile file = findFile(model.getClass().getName());
			if (file != null) {
				try {
					String text = new String(file.getContents().readAllBytes(), file.getCharset() == null ? StandardCharsets.UTF_8.name() : file.getCharset());
					return source(text, file);
				} catch (IOException | CoreException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	private static Source source(String text, IFile file) {
		Source s = new Source();
		s.text = text;
		s.name = file.getName();
		/* The page in the folder of the project */
		s.folder = file.getProject().getLocation() == null ? null : file.getProject().getLocation().toOSString();
		s.file = file;
		return s;
	}

	/* The Java file of the class (package folders below a source folder) */
	private static IFile findFile(String className) {
		String suffix = "/" + className.replace('.', '/') + ".java";
		List<IFile> found = new ArrayList<>();
		try {
			ResourcesPlugin.getWorkspace().getRoot().accept(resource -> {
				if (resource.getType() == IResource.FILE && resource.getFullPath().toString().endsWith(suffix)) {
					found.add((IFile) resource);
				}
				return resource.getType() != IResource.FILE && (resource.getType() != IResource.PROJECT || resource.getProject().isOpen());
			});
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return found.isEmpty() ? null : found.get(0);
	}

	/** The classpath of Bio7 for the translation (the API of the simulation, the libraries of Bio7). */
	public static String[] bio7Classpath() {
		List<String> paths = new ArrayList<>();
		for (String p : new ScanClassPath().scan().split(Pattern.quote(File.pathSeparator))) {
			if (!p.isBlank() && new File(p).exists()) {
				paths.add(p);
			}
		}
		return paths.toArray(new String[0]);
	}

	/* Shows a page written into the workspace in the Project Explorer */
	private static void refresh(String file) {
		try {
			for (IFile f : ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(new File(file).toURI())) {
				f.getParent().refreshLocal(IResource.DEPTH_ONE, null);
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
}
