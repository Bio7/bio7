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
package com.eco.bio7.windowbuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TextBlock;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

/**
 * WindowBuilder Shiny: the user interface of a Shiny app from an SWT GUI class
 * (a Composite designed with the WindowBuilder). The class is only read (the
 * code of its constructor, as the WindowBuilder writes it), not run.
 * <p>
 * The widgets: Scale, Slider (sliderInput), Spinner (numericInput), Text
 * (textInput, textAreaInput, passwordInput; a name starting with "out":
 * verbatimTextOutput), Combo, List (selectInput), Button (actionButton,
 * checkboxInput, radioButtons for the radio buttons of a parent), DateTime
 * (dateInput), Label (text; a name starting with "out": textOutput), Link
 * (HTML), Canvas (plotOutput), Table (tableOutput), Browser (uiOutput). The
 * containers: Composite, Group (wellPanel with a title), TabFolder and
 * CTabFolder (tabsetPanel), SashForm. The layouts: GridLayout (fluidRow and
 * column with the widths of the columns and the spans), FillLayout, RowLayout
 * (flowLayout); others: the widgets one below the other. The ids of Shiny are
 * the names of the variables (input$spinnerN). Other widgets are a comment in
 * the user interface.
 */
public final class WindowBuilderShiny {

	/** The result: the code of ui.R and server.R (the template), the elements and the notes. */
	public record Result(String ui, String server, List<String> inputs, List<String> buttons, Map<String, String> outputs, List<String> notes) {
	}

	/* A widget of the GUI (or the GUI itself: "this") */
	private static final class Widget {
		final String name;
		final String type;
		final Set<String> style;
		final Widget parent;
		final List<Widget> children = new ArrayList<>();
		final Map<String, Object> props = new HashMap<>();
		/* The layout of a container: grid (columns), fill (horizontal or vertical), row, none */
		String layout = "none";
		int columns = 1;
		boolean vertical;
		/* The cell in a GridLayout: the span, takes the extra width, the size hints (-1: none) */
		int span = 1;
		boolean grab;
		int widthHint = -1;
		int heightHint = -1;
		final List<String> items = new ArrayList<>();

		Widget(String name, String type, Set<String> style, Widget parent) {
			this.name = name;
			this.type = type;
			this.style = style;
			this.parent = parent;
			if (parent != null) {
				parent.children.add(this);
			}
		}

		boolean is(String... types) {
			for (String t : types) {
				if (type.equals(t)) {
					return true;
				}
			}
			return false;
		}

		String text() {
			Object t = props.get("text");
			return t instanceof String s ? s : "";
		}

		int number(String key, int fallback) {
			return props.get(key) instanceof Integer i ? i : fallback;
		}
	}

	private final Map<String, Widget> widgets = new LinkedHashMap<>();
	/* The variables of GridData objects: span, grab, widthHint, heightHint */
	private final Map<String, int[]> gridDatas = new HashMap<>();
	private final Widget root = new Widget("this", "Composite", Set.of(), null);
	private final List<String> notes = new ArrayList<>();
	private final List<String> inputs = new ArrayList<>();
	private final List<String> buttons = new ArrayList<>();
	private final Map<String, String> outputs = new LinkedHashMap<>();
	private int unnamed;
	private TypeDeclaration type;
	private final Set<String> inlined = new HashSet<>();

	private WindowBuilderShiny() {
	}

	/**
	 * Converts the source of a GUI class (a Composite) into the code of a Shiny
	 * app. title: the title of the page (null: none).
	 */
	public static Result convert(String javaSource, String title) {
		return convert(javaSource, title, null);
	}

	/** As convert(String, String); origin: the GUI file in the header (e.g., "X.java in the project P"), null: the class. */
	public static Result convert(String javaSource, String title, String origin) {
		WindowBuilderShiny c = new WindowBuilderShiny();
		c.read(javaSource);
		String ui = c.ui(title);
		if (origin != null && c.type != null) {
			ui = ui.replace("# from the GUI class " + c.type.getName().getIdentifier() + ".java.", "# from the GUI class " + origin + ".");
		}
		return new Result(ui, c.serverTemplate(), c.inputs, c.buttons, c.outputs, c.notes);
	}

	/* ------------------------------------------------------------ reading */

	private void read(String source) {
		ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
		Map<String, String> options = JavaCore.getOptions();
		JavaCore.setComplianceOptions(JavaCore.latestSupportedJavaVersion(), options);
		parser.setCompilerOptions(options);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(source.toCharArray());
		CompilationUnit cu = (CompilationUnit) parser.createAST(null);
		for (Object t : cu.types()) {
			if (t instanceof TypeDeclaration td) {
				type = td;
				break;
			}
		}
		if (type == null) {
			notes.add("No class found");
			return;
		}
		/* The constructors (the code of the WindowBuilder), called methods of the class one level deep */
		for (MethodDeclaration m : type.getMethods()) {
			if (m.isConstructor() && m.getBody() != null) {
				statements(m.getBody());
			}
		}
	}

	private void statements(Block block) {
		for (Object o : block.statements()) {
			statement((Statement) o);
		}
	}

	private void statement(Statement s) {
		if (s instanceof VariableDeclarationStatement vds) {
			for (Object f : vds.fragments()) {
				VariableDeclarationFragment frag = (VariableDeclarationFragment) f;
				if (frag.getInitializer() != null) {
					assign(frag.getName().getIdentifier(), frag.getInitializer());
				}
			}
		} else if (s instanceof ExpressionStatement es) {
			Expression e = es.getExpression();
			if (e instanceof Assignment a) {
				Expression left = a.getLeftHandSide();
				if (left instanceof SimpleName n) {
					assign(n.getIdentifier(), a.getRightHandSide());
				} else if (left instanceof FieldAccess fa && fa.getExpression() instanceof ThisExpression) {
					assign(fa.getName().getIdentifier(), a.getRightHandSide());
				} else if (left instanceof QualifiedName q && gridDatas.containsKey(q.getQualifier().getFullyQualifiedName())) {
					/* gd.horizontalSpan = 2, gd.heightHint = 250, gd.grabExcessHorizontalSpace = true */
					int[] g = gridDatas.get(q.getQualifier().getFullyQualifiedName());
					Integer v = intValue(a.getRightHandSide());
					switch (q.getName().getIdentifier()) {
					case "horizontalSpan" -> g[0] = v == null || v < 1 ? g[0] : v;
					case "grabExcessHorizontalSpace" -> g[1] = a.getRightHandSide() instanceof BooleanLiteral b && b.booleanValue() ? 1 : 0;
					case "widthHint" -> g[2] = v == null ? g[2] : v;
					case "heightHint" -> g[3] = v == null ? g[3] : v;
					default -> {
					}
					}
				}
			} else if (e instanceof ClassInstanceCreation cic) {
				create(null, cic);
			} else if (e instanceof MethodInvocation mi) {
				call(mi);
			}
		} else if (s instanceof Block b) {
			statements(b);
		}
	}

	private void assign(String name, Expression value) {
		if (value instanceof ClassInstanceCreation cic) {
			String t = simpleType(cic);
			if (t.equals("GridData")) {
				gridDatas.put(name, gridData(cic));
			} else {
				create(name, cic);
			}
		}
	}

	private static String simpleType(ClassInstanceCreation cic) {
		String t = cic.getType().toString();
		return t.substring(t.lastIndexOf('.') + 1);
	}

	/* A widget: new Type(parent, style) */
	private void create(String name, ClassInstanceCreation cic) {
		List<?> args = cic.arguments();
		if (args.isEmpty() || cic.getAnonymousClassDeclaration() != null) {
			return;
		}
		Widget parent = widgetOf((Expression) args.get(0));
		if (parent == null) {
			return;
		}
		Set<String> style = new HashSet<>();
		if (args.size() > 1) {
			flags((Expression) args.get(1), style);
		}
		String t = simpleType(cic);
		String n = name != null ? name : t.substring(0, 1).toLowerCase(Locale.ROOT) + t.substring(1) + (++unnamed);
		widgets.put(n, new Widget(n, t, style, parent));
	}

	private Widget widgetOf(Expression e) {
		if (e instanceof ThisExpression) {
			return root;
		}
		if (e instanceof SimpleName n) {
			return widgets.get(n.getIdentifier());
		}
		if (e instanceof FieldAccess fa && fa.getExpression() instanceof ThisExpression) {
			return widgets.get(fa.getName().getIdentifier());
		}
		return null;
	}

	private static void flags(Expression e, Set<String> out) {
		if (e instanceof QualifiedName q) {
			out.add(q.getName().getIdentifier());
		} else if (e instanceof InfixExpression in) {
			flags(in.getLeftOperand(), out);
			flags(in.getRightOperand(), out);
			for (Object x : in.extendedOperands()) {
				flags((Expression) x, out);
			}
		} else if (e instanceof ParenthesizedExpression p) {
			flags(p.getExpression(), out);
		}
	}

	/* A call on a widget: setText, setLayout, setLayoutData, the values (or a method of the class) */
	private void call(MethodInvocation mi) {
		String m = mi.getName().getIdentifier();
		Expression target = mi.getExpression();
		Widget w = target == null ? root : widgetOf(target);
		List<?> args = mi.arguments();
		if (target == null && args.isEmpty() && w == root && !m.startsWith("set")) {
			/* A method of the class which creates widgets (e.g., createContents()) */
			inline(m);
			return;
		}
		if (w == null || args.isEmpty()) {
			return;
		}
		Expression a0 = (Expression) args.get(0);
		switch (m) {
		case "setText", "setToolTipText", "setMessage" -> {
			String s = stringValue(a0);
			if (s != null) {
				w.props.put(m.equals("setText") ? "text" : m.equals("setMessage") ? "placeholder" : "tooltip", s);
			}
		}
		case "setMinimum", "setMaximum", "setIncrement", "setDigits", "setPageIncrement" -> {
			Integer v = intValue(a0);
			if (v != null) {
				w.props.put(m.substring(3).toLowerCase(Locale.ROOT), v);
			}
		}
		case "setSelection" -> {
			Integer v = intValue(a0);
			if (v != null) {
				w.props.put("selection", v);
			} else if (a0 instanceof BooleanLiteral b) {
				w.props.put("checked", b.booleanValue());
			}
		}
		case "select" -> {
			Integer v = intValue(a0);
			if (v != null) {
				w.props.put("selected", v);
			}
		}
		case "setItems" -> {
			w.items.clear();
			w.items.addAll(strings(a0));
		}
		case "add" -> {
			String s = stringValue(a0);
			if (s != null) {
				w.items.add(s);
			}
		}
		case "setControl", "setContent" -> {
			Widget c = widgetOf(a0);
			if (c != null) {
				w.props.put("control", c.name);
			}
		}
		case "setLayout" -> layout(w, a0);
		case "setLayoutData" -> {
			int[] g = a0 instanceof ClassInstanceCreation cic && simpleType(cic).equals("GridData") ? gridData(cic)
					: a0 instanceof SimpleName n ? gridDatas.get(n.getIdentifier()) : null;
			if (g != null) {
				w.span = g[0];
				w.grab = g[1] == 1;
				w.widthHint = g[2];
				w.heightHint = g[3];
			}
		}
		default -> {
			/* Not used */
		}
		}
	}

	private void inline(String method) {
		if (type == null || !inlined.add(method)) {
			return;
		}
		for (MethodDeclaration md : type.getMethods()) {
			if (md.getName().getIdentifier().equals(method) && md.parameters().isEmpty() && md.getBody() != null) {
				statements(md.getBody());
			}
		}
	}

	private void layout(Widget w, Expression e) {
		if (!(e instanceof ClassInstanceCreation cic)) {
			return;
		}
		String t = simpleType(cic);
		List<?> args = cic.arguments();
		switch (t) {
		case "GridLayout" -> {
			w.layout = "grid";
			Integer n = args.isEmpty() ? Integer.valueOf(1) : intValue((Expression) args.get(0));
			w.columns = n == null || n < 1 ? 1 : n;
		}
		case "FillLayout" -> {
			w.layout = "fill";
			Set<String> f = new HashSet<>();
			if (!args.isEmpty()) {
				flags((Expression) args.get(0), f);
			}
			w.vertical = f.contains("VERTICAL");
		}
		case "RowLayout" -> {
			Set<String> f = new HashSet<>();
			if (!args.isEmpty()) {
				flags((Expression) args.get(0), f);
			}
			w.layout = f.contains("VERTICAL") ? "none" : "row";
		}
		default -> w.layout = "none";
		}
	}

	/* new GridData(h, v, grabH, grabV, hSpan, vSpan): span, grab, widthHint, heightHint */
	private static int[] gridData(ClassInstanceCreation cic) {
		List<?> args = cic.arguments();
		int[] g = { 1, 0, -1, -1 };
		if (args.size() >= 3 && args.get(2) instanceof BooleanLiteral b) {
			g[1] = b.booleanValue() ? 1 : 0;
		}
		if (args.size() >= 5) {
			Integer v = intValue((Expression) args.get(4));
			g[0] = v == null || v < 1 ? 1 : v;
		}
		return g;
	}

	private static Integer intValue(Expression e) {
		if (e instanceof NumberLiteral n) {
			try {
				return Integer.decode(n.getToken().replace("_", ""));
			} catch (NumberFormatException x) {
				return null;
			}
		}
		if (e instanceof PrefixExpression p && p.getOperator() == PrefixExpression.Operator.MINUS) {
			Integer v = intValue(p.getOperand());
			return v == null ? null : -v;
		}
		if (e instanceof ParenthesizedExpression p) {
			return intValue(p.getExpression());
		}
		return null;
	}

	private static String stringValue(Expression e) {
		if (e instanceof StringLiteral s) {
			return s.getLiteralValue();
		}
		if (e instanceof TextBlock tb) {
			return tb.getLiteralValue();
		}
		if (e instanceof InfixExpression in && in.getOperator() == InfixExpression.Operator.PLUS) {
			StringBuilder sb = new StringBuilder();
			List<Expression> parts = new ArrayList<>();
			parts.add(in.getLeftOperand());
			parts.add(in.getRightOperand());
			for (Object x : in.extendedOperands()) {
				parts.add((Expression) x);
			}
			for (Expression p : parts) {
				String s = stringValue(p);
				if (s == null) {
					return null;
				}
				sb.append(s);
			}
			return sb.toString();
		}
		return null;
	}

	private static List<String> strings(Expression e) {
		List<String> out = new ArrayList<>();
		ArrayInitializer init = e instanceof ArrayCreation ac ? ac.getInitializer() : e instanceof ArrayInitializer ai ? ai : null;
		if (init != null) {
			for (Object o : init.expressions()) {
				String s = stringValue((Expression) o);
				if (s != null) {
					out.add(s);
				}
			}
		}
		return out;
	}

	/* ------------------------------------------------------------ the user interface */

	private String ui(String title) {
		List<String> body = new ArrayList<>();
		if (title != null && !title.isBlank()) {
			body.add("titlePanel(" + r(title) + ")");
		}
		body.addAll(children(root, 1));
		StringBuilder sb = new StringBuilder();
		sb.append("# The user interface of the Shiny app, created by Bio7 (WindowBuilder Shiny)\n");
		sb.append("# from the GUI class ").append(type == null ? "" : type.getName().getIdentifier()).append(".java.\n");
		sb.append("# Change the GUI with the WindowBuilder and create the app again: this file is\n");
		sb.append("# written again (the code of server.R stays).\n");
		sb.append("library(shiny)\n\n");
		sb.append("fluidPage(\n");
		sb.append(join(body, 1));
		sb.append("\n)\n");
		return sb.toString();
	}

	private static String join(List<String> items, int depth) {
		String ind = "  ".repeat(depth);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < items.size(); i++) {
			String item = items.get(i);
			boolean comment = item.startsWith("#");
			sb.append(ind).append(item.replace("\n", "\n" + ind));
			/* A comma after each element (not after a comment, not after the last element) */
			if (!comment && hasElementAfter(items, i)) {
				sb.append(",");
			}
			if (i < items.size() - 1) {
				sb.append("\n");
			}
		}
		return sb.toString();
	}

	private static boolean hasElementAfter(List<String> items, int i) {
		for (int k = i + 1; k < items.size(); k++) {
			if (!items.get(k).startsWith("#")) {
				return true;
			}
		}
		return false;
	}

	/* The elements of the children of a container, arranged by its layout */
	private List<String> children(Widget c, int depth) {
		List<Widget> kids = new ArrayList<>();
		for (Widget k : c.children) {
			/* The items of tab folders and the content of a tab are drawn by the folder */
			if (!k.is("TabItem", "CTabItem")) {
				kids.add(k);
			}
		}
		Set<String> tabContents = new HashSet<>();
		for (Widget k : c.children) {
			if (k.is("TabItem", "CTabItem") && k.props.get("control") instanceof String ctl) {
				tabContents.add(ctl);
			}
		}
		kids.removeIf(k -> tabContents.contains(k.name));
		/* Radio buttons of one parent: one radioButtons (in the cell of the first) */
		List<Object> cells = new ArrayList<>();
		List<Widget> radios = new ArrayList<>();
		for (Widget k : kids) {
			if (k.is("Button") && k.style.contains("RADIO")) {
				if (radios.isEmpty()) {
					cells.add(radios);
				}
				radios.add(k);
			} else {
				cells.add(k);
			}
		}
		List<String> elements = new ArrayList<>();
		List<Integer> spans = new ArrayList<>();
		for (Object cell : cells) {
			if (cell instanceof Widget w) {
				elements.add(element(w, depth));
				spans.add(Math.max(1, w.span));
			} else {
				@SuppressWarnings("unchecked")
				List<Widget> group = (List<Widget>) cell;
				elements.add(radioButtons(c, group));
				spans.add(1);
			}
		}
		switch (c.layout) {
		case "grid" -> {
			return grid(elements, spans, c.columns);
		}
		case "fill" -> {
			if (c.vertical || elements.size() < 2) {
				return elements;
			}
			int w = Math.max(1, 12 / elements.size());
			List<String> cols = new ArrayList<>();
			for (String e : elements) {
				cols.add("column(" + w + ",\n" + indent(e) + "\n)");
			}
			return List.of("fluidRow(\n" + join(cols, 1) + "\n)");
		}
		case "row" -> {
			return List.of("flowLayout(\n" + join(elements, 1) + "\n)");
		}
		default -> {
			return elements;
		}
		}
	}

	/* A GridLayout: rows (fluidRow) of columns (column) with the widths of the spans */
	private static List<String> grid(List<String> elements, List<Integer> spans, int columns) {
		List<String> rows = new ArrayList<>();
		List<String> cols = new ArrayList<>();
		int used = 0;
		for (int i = 0; i < elements.size(); i++) {
			int span = Math.min(spans.get(i), columns);
			if (used + span > columns) {
				rows.add(row(cols));
				cols = new ArrayList<>();
				used = 0;
			}
			int width = Math.max(1, Math.round(12f * span / columns));
			String e = elements.get(i);
			cols.add(e.equals("div()") ? "column(" + width + ")" : "column(" + width + ",\n" + indent(e) + "\n)");
			used += span;
		}
		if (!cols.isEmpty()) {
			rows.add(row(cols));
		}
		return rows;
	}

	private static String row(List<String> cols) {
		return "fluidRow(\n" + join(cols, 1) + "\n)";
	}

	private static String indent(String s) {
		return "  " + s.replace("\n", "\n  ");
	}

	/* The element of a widget */
	private String element(Widget w, int depth) {
		String id = r(w.name);
		boolean out = w.name.startsWith("out");
		switch (w.type) {
		case "Composite", "SashForm", "ScrolledComposite" -> {
			if (w.is("SashForm")) {
				w.layout = "fill";
				w.vertical = w.style.contains("VERTICAL");
			}
			if (w.is("ScrolledComposite") && w.props.get("control") instanceof String ctl && widgets.get(ctl) != null) {
				return element(widgets.get(ctl), depth);
			}
			List<String> inner = children(w, depth + 1);
			return inner.isEmpty() ? "div()" : "div(\n" + join(inner, 1) + "\n)";
		}
		case "Group" -> {
			List<String> inner = new ArrayList<>();
			if (!w.text().isEmpty()) {
				inner.add("h4(" + r(w.text()) + ")");
			}
			inner.addAll(children(w, depth + 1));
			return "wellPanel(\n" + join(inner, 1) + "\n)";
		}
		case "TabFolder", "CTabFolder" -> {
			List<String> tabs = new ArrayList<>();
			for (Widget k : w.children) {
				if (k.is("TabItem", "CTabItem")) {
					Widget ctl = k.props.get("control") instanceof String s ? widgets.get(s) : null;
					String content = ctl == null ? "" : ",\n" + indent(element(ctl, depth + 1));
					tabs.add("tabPanel(" + r(k.text().isEmpty() ? k.name : k.text()) + content + "\n)");
				}
			}
			inputs.add(w.name);
			return "tabsetPanel(id = " + id + (tabs.isEmpty() ? "" : ",\n" + join(tabs, 1)) + "\n)";
		}
		case "Label", "CLabel" -> {
			if (out) {
				outputs.put(w.name, "renderText");
				return "textOutput(" + id + ")";
			}
			return w.text().isEmpty() ? "div()" : "tags$label(" + r(w.text()) + ")";
		}
		case "Link" -> {
			return "HTML(" + r(w.text()) + ")";
		}
		case "Text", "StyledText" -> {
			if (out) {
				outputs.put(w.name, "renderPrint");
				return "verbatimTextOutput(" + id + ")";
			}
			inputs.add(w.name);
			String value = w.text().isEmpty() ? "" : ", value = " + r(w.text());
			String ph = w.props.get("placeholder") instanceof String p ? ", placeholder = " + r(p) : "";
			if (w.style.contains("PASSWORD")) {
				return "passwordInput(" + id + ", NULL" + value + ")";
			}
			if (w.style.contains("MULTI") || w.is("StyledText")) {
				return "textAreaInput(" + id + ", NULL" + value + ph + ")";
			}
			return "textInput(" + id + ", NULL" + value + ph + ")";
		}
		case "Scale", "Slider" -> {
			inputs.add(w.name);
			int min = w.number("minimum", 0);
			int max = w.number("maximum", 100);
			int value = Math.max(min, Math.min(max, w.number("selection", min)));
			String step = w.props.containsKey("increment") ? ", step = " + w.number("increment", 1) : "";
			return "sliderInput(" + id + ", NULL, min = " + min + ", max = " + max + ", value = " + value + step + ")";
		}
		case "Spinner" -> {
			inputs.add(w.name);
			int digits = w.number("digits", 0);
			double f = Math.pow(10, digits);
			String step = w.props.containsKey("increment") || digits > 0 ? ", step = " + num(w.number("increment", 1) / f) : "";
			return "numericInput(" + id + ", NULL, value = " + num(w.number("selection", 0) / f) + ", min = " + num(w.number("minimum", 0) / f) + ", max = "
					+ num(w.number("maximum", 100) / f) + step + ")";
		}
		case "Combo", "CCombo" -> {
			inputs.add(w.name);
			int sel = w.number("selected", 0);
			String selected = sel >= 0 && sel < w.items.size() ? ", selected = " + r(w.items.get(sel)) : "";
			return "selectInput(" + id + ", NULL, choices = " + choices(w.items) + selected + ")";
		}
		case "List" -> {
			inputs.add(w.name);
			boolean multiple = w.style.contains("MULTI");
			return "selectInput(" + id + ", NULL, choices = " + choices(w.items) + (multiple ? ", multiple = TRUE" : "") + ", selectize = FALSE, size = "
					+ Math.max(2, Math.min(10, w.items.size())) + ")";
		}
		case "Button" -> {
			if (w.style.contains("CHECK") || w.style.contains("TOGGLE")) {
				inputs.add(w.name);
				return "checkboxInput(" + id + ", " + r(w.text()) + ", value = " + (Boolean.TRUE.equals(w.props.get("checked")) ? "TRUE" : "FALSE") + ")";
			}
			buttons.add(w.name);
			return "actionButton(" + id + ", " + r(w.text().isEmpty() ? w.name : w.text()) + ")";
		}
		case "DateTime" -> {
			inputs.add(w.name);
			if (w.style.contains("TIME")) {
				return "textInput(" + id + ", NULL, placeholder = \"hh:mm\")";
			}
			return "dateInput(" + id + ", NULL)";
		}
		case "Canvas", "GLCanvas" -> {
			if (isWebGl(w)) {
				/* 3D with the R package rgl (WebGL in the browser) */
				outputs.put(w.name, "rgl::renderRglwidget");
				return "rgl::rglwidgetOutput(" + id + ")";
			}
			outputs.put(w.name, "renderPlot");
			return "plotOutput(" + id + ")";
		}
		case "Table" -> {
			outputs.put(w.name, "renderTable");
			return "tableOutput(" + id + ")";
		}
		case "Browser" -> {
			outputs.put(w.name, "renderUI");
			return "uiOutput(" + id + ")";
		}
		default -> {
			notes.add(w.type + " " + w.name + " is not converted");
			return "# " + w.type + " " + w.name + ": not converted by WindowBuilder Shiny";
		}
		}
	}

	/*
	 * A WebGL canvas: a GLCanvas of SWT or a Canvas whose name starts with "gl"
	 * and an uppercase letter, a digit or "_" (glView, gl3d; not globalMap)
	 */
	private static boolean isWebGl(Widget w) {
		return w.is("GLCanvas") || (w.is("Canvas") && w.name.matches("gl[A-Z0-9_].*"));
	}

	/* The radio buttons of a parent: one radioButtons (the id: the parent if it is a Group, else the first button) */
	private String radioButtons(Widget parent, List<Widget> radios) {
		String id = parent.is("Group") ? parent.name : radios.get(0).name;
		inputs.add(id);
		List<String> labels = new ArrayList<>();
		String selected = null;
		for (Widget w : radios) {
			String label = w.text().isEmpty() ? w.name : w.text();
			labels.add(label);
			if (Boolean.TRUE.equals(w.props.get("checked"))) {
				selected = label;
			}
		}
		return "radioButtons(" + r(id) + ", NULL, choices = " + choices(labels) + (selected != null ? ", selected = " + r(selected) : "") + ")";
	}

	private static String choices(List<String> items) {
		List<String> q = new ArrayList<>();
		for (String s : items) {
			q.add(r(s));
		}
		return "c(" + String.join(", ", q) + ")";
	}

	private static String num(double v) {
		return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
	}

	/** An R string literal. */
	static String r(String s) {
		return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "").replace("\t", "\\t") + "\"";
	}

	/* ------------------------------------------------------------ JavaScript (an HTML page) */

	/** The result of the JavaScript version: index.html, app.js (the template), style.css, the ids, the notes. */
	public record WebResult(String html, String js, String css, List<String> ids, List<String> notes) {
	}

	/* The ids of the elements of the page (for app.js) and their kind */
	private final Map<String, String> webIds = new LinkedHashMap<>();

	/**
	 * Converts the source of a GUI class (a Composite) into an HTML page with a
	 * JavaScript file (WindowBuilder JavaScript). title: the title of the page
	 * (null: the class name).
	 */
	public static WebResult convertWeb(String javaSource, String title) {
		return convertWeb(javaSource, title, null);
	}

	/** As convertWeb(String, String); origin: the GUI file in the header (e.g., "X.java in the project P"), null: the class. */
	public static WebResult convertWeb(String javaSource, String title, String origin) {
		WindowBuilderShiny c = new WindowBuilderShiny();
		c.read(javaSource);
		String name = c.type == null ? "App" : c.type.getName().getIdentifier();
		String t = title == null || title.isBlank() ? name : title;
		String body = c.webChildren(c.root);
		String html = """
				<!DOCTYPE html>
				<!-- The page of the app, created by Bio7 (WindowBuilder JavaScript) from the GUI
				     class %1$s. Change the GUI with the WindowBuilder and create the app
				     again: this file is written again (the code of app.js and style.css stays). -->
				<html lang="en">
				<head>
				<meta charset="utf-8">
				<meta name="viewport" content="width=device-width, initial-scale=1">
				<title>%2$s</title>
				<link rel="stylesheet" href="style.css">
				</head>
				<body>
				<h1>%2$s</h1>
				%3$s
				<script>
				/* The values of the sliders, the tabs (part of the generated page) */
				document.querySelectorAll("input[type=range]").forEach(r => {
				  const o = document.getElementById(r.id + "_value");
				  if (o) r.addEventListener("input", () => o.textContent = r.value);
				});
				document.querySelectorAll(".wb-tabs").forEach(t => {
				  const bar = t.querySelectorAll(":scope > .wb-tabbar > button");
				  const panels = t.querySelectorAll(":scope > .wb-tab");
				  bar.forEach((b, i) => b.addEventListener("click", () => {
				    bar.forEach((x, k) => x.classList.toggle("active", k === i));
				    panels.forEach((p, k) => p.hidden = k !== i);
				  }));
				});
				</script>
				<script src="app.js"></script>
				</body>
				</html>
				""".formatted(origin != null ? html(origin) : name + ".java", html(t), body);
		return new WebResult(html, c.jsTemplate(name), CSS, new ArrayList<>(c.webIds.keySet()), c.notes);
	}

	/* The children of a container, arranged by its layout (a CSS grid or flex box) */
	private String webChildren(Widget c) {
		List<Widget> kids = new ArrayList<>();
		Set<String> tabContents = new HashSet<>();
		for (Widget k : c.children) {
			if (k.is("TabItem", "CTabItem") && k.props.get("control") instanceof String ctl) {
				tabContents.add(ctl);
			}
		}
		for (Widget k : c.children) {
			if (!k.is("TabItem", "CTabItem") && !tabContents.contains(k.name)) {
				kids.add(k);
			}
		}
		/* Radio buttons of one parent: one group */
		List<Object> cells = new ArrayList<>();
		List<Widget> radios = new ArrayList<>();
		for (Widget k : kids) {
			if (k.is("Button") && k.style.contains("RADIO")) {
				if (radios.isEmpty()) {
					cells.add(radios);
				}
				radios.add(k);
			} else {
				cells.add(k);
			}
		}
		StringBuilder sb = new StringBuilder();
		switch (c.layout) {
		case "grid" -> {
			/* The columns: 1fr if a widget of the column takes the extra width, else auto */
			boolean[] grab = new boolean[c.columns];
			int col = 0;
			for (Object cell : cells) {
				int span = cell instanceof Widget w ? Math.min(Math.max(1, w.span), c.columns) : 1;
				if (col + span > c.columns) {
					col = 0;
				}
				if (cell instanceof Widget w && span == 1 && w.grab) {
					grab[col] = true;
				}
				col = (col + span) % c.columns;
			}
			StringBuilder cols = new StringBuilder();
			for (boolean g : grab) {
				cols.append(cols.length() > 0 ? " " : "").append(g ? "1fr" : "auto");
			}
			sb.append("<div class=\"wb-grid\" style=\"grid-template-columns: ").append(cols).append("\">\n");
			for (Object cell : cells) {
				int span = cell instanceof Widget w ? Math.min(Math.max(1, w.span), c.columns) : 1;
				sb.append("<div class=\"wb-cell\"").append(span > 1 ? " style=\"grid-column: span " + span + "\"" : "").append(">")
						.append(webCell(c, cell)).append("</div>\n");
			}
			sb.append("</div>");
		}
		case "fill" -> {
			sb.append("<div class=\"wb-fill\" style=\"flex-direction: ").append(c.vertical ? "column" : "row").append("\">\n");
			for (Object cell : cells) {
				sb.append("<div class=\"wb-cell\">").append(webCell(c, cell)).append("</div>\n");
			}
			sb.append("</div>");
		}
		case "row" -> {
			sb.append("<div class=\"wb-row\">\n");
			for (Object cell : cells) {
				sb.append(webCell(c, cell)).append("\n");
			}
			sb.append("</div>");
		}
		default -> {
			for (Object cell : cells) {
				sb.append("<div class=\"wb-cell\">").append(webCell(c, cell)).append("</div>\n");
			}
		}
		}
		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	private String webCell(Widget parent, Object cell) {
		return cell instanceof Widget w ? webElement(w) : webRadios(parent, (List<Widget>) cell);
	}

	/* The element of a widget */
	private String webElement(Widget w) {
		String id = w.name;
		boolean out = w.name.startsWith("out");
		String tip = w.props.get("tooltip") instanceof String t ? " title=\"" + html(t) + "\"" : "";
		switch (w.type) {
		case "Composite", "SashForm", "ScrolledComposite" -> {
			if (w.is("SashForm")) {
				w.layout = "fill";
				w.vertical = w.style.contains("VERTICAL");
			}
			if (w.is("ScrolledComposite") && w.props.get("control") instanceof String ctl && widgets.get(ctl) != null) {
				return webElement(widgets.get(ctl));
			}
			return "<div id=\"" + id + "\">\n" + webChildren(w) + "\n</div>";
		}
		case "Group" -> {
			webIds.put(id, "group");
			return "<fieldset id=\"" + id + "\">" + (w.text().isEmpty() ? "" : "<legend>" + html(w.text()) + "</legend>") + "\n" + webChildren(w) + "\n</fieldset>";
		}
		case "TabFolder", "CTabFolder" -> {
			StringBuilder bar = new StringBuilder();
			StringBuilder panels = new StringBuilder();
			int i = 0;
			for (Widget k : w.children) {
				if (k.is("TabItem", "CTabItem")) {
					Widget ctl = k.props.get("control") instanceof String s ? widgets.get(s) : null;
					bar.append("<button type=\"button\"").append(i == 0 ? " class=\"active\"" : "").append(">").append(html(k.text().isEmpty() ? k.name : k.text()))
							.append("</button>");
					panels.append("<div class=\"wb-tab\"").append(i == 0 ? "" : " hidden").append(">\n").append(ctl == null ? "" : webElement(ctl)).append("\n</div>\n");
					i++;
				}
			}
			return "<div class=\"wb-tabs\" id=\"" + id + "\">\n<div class=\"wb-tabbar\">" + bar + "</div>\n" + panels + "</div>";
		}
		case "Label", "CLabel" -> {
			if (out) {
				webIds.put(id, "output");
				return "<span id=\"" + id + "\" class=\"wb-output\"" + tip + ">" + html(w.text()) + "</span>";
			}
			return w.text().isEmpty() ? "" : "<label" + tip + ">" + html(w.text()) + "</label>";
		}
		case "Link" -> {
			return "<span>" + w.text() + "</span>";
		}
		case "Text", "StyledText" -> {
			if (out) {
				webIds.put(id, "output");
				return "<pre id=\"" + id + "\" class=\"wb-output\">" + html(w.text()) + "</pre>";
			}
			webIds.put(id, "input");
			String ph = w.props.get("placeholder") instanceof String p ? " placeholder=\"" + html(p) + "\"" : "";
			if (w.style.contains("MULTI") || w.is("StyledText")) {
				return "<textarea id=\"" + id + "\" rows=\"4\"" + ph + tip + ">" + html(w.text()) + "</textarea>";
			}
			String type = w.style.contains("PASSWORD") ? "password" : "text";
			return "<input type=\"" + type + "\" id=\"" + id + "\" value=\"" + html(w.text()) + "\"" + ph + tip + ">";
		}
		case "Scale", "Slider" -> {
			webIds.put(id, "input");
			int min = w.number("minimum", 0);
			int max = w.number("maximum", 100);
			int value = Math.max(min, Math.min(max, w.number("selection", min)));
			int step = w.number("increment", 1);
			return "<span class=\"wb-slider\"><input type=\"range\" id=\"" + id + "\" min=\"" + min + "\" max=\"" + max + "\" step=\"" + step + "\" value=\"" + value + "\"" + tip
					+ "><output id=\"" + id + "_value\" for=\"" + id + "\">" + value + "</output></span>";
		}
		case "Spinner" -> {
			webIds.put(id, "input");
			int digits = w.number("digits", 0);
			double f = Math.pow(10, digits);
			return "<input type=\"number\" id=\"" + id + "\" min=\"" + num(w.number("minimum", 0) / f) + "\" max=\"" + num(w.number("maximum", 100) / f) + "\" step=\""
					+ num(w.number("increment", 1) / f) + "\" value=\"" + num(w.number("selection", 0) / f) + "\"" + tip + ">";
		}
		case "Combo", "CCombo", "List" -> {
			webIds.put(id, "input");
			boolean list = w.is("List");
			int sel = w.number("selected", list ? -1 : 0);
			StringBuilder o = new StringBuilder();
			for (int i = 0; i < w.items.size(); i++) {
				o.append("<option").append(i == sel ? " selected" : "").append(">").append(html(w.items.get(i))).append("</option>");
			}
			String attrs = list ? " size=\"" + Math.max(2, Math.min(10, w.items.size())) + "\"" + (w.style.contains("MULTI") ? " multiple" : "") : "";
			return "<select id=\"" + id + "\"" + attrs + tip + ">" + o + "</select>";
		}
		case "Button" -> {
			if (w.style.contains("CHECK") || w.style.contains("TOGGLE")) {
				webIds.put(id, "input");
				return "<label" + tip + "><input type=\"checkbox\" id=\"" + id + "\"" + (Boolean.TRUE.equals(w.props.get("checked")) ? " checked" : "") + "> "
						+ html(w.text()) + "</label>";
			}
			webIds.put(id, "button");
			return "<button type=\"button\" id=\"" + id + "\"" + tip + ">" + html(w.text().isEmpty() ? w.name : w.text()) + "</button>";
		}
		case "DateTime" -> {
			webIds.put(id, "input");
			return "<input type=\"" + (w.style.contains("TIME") ? "time" : "date") + "\" id=\"" + id + "\"" + tip + ">";
		}
		case "Canvas", "GLCanvas" -> {
			boolean webgl = isWebGl(w);
			webIds.put(id, webgl ? "webgl" : "canvas");
			int width = w.widthHint > 0 ? w.widthHint : 600;
			int height = w.heightHint > 0 ? w.heightHint : 300;
			return "<canvas id=\"" + id + "\"" + (webgl ? " class=\"wb-webgl\"" : "") + " width=\"" + width + "\" height=\"" + height + "\"" + tip + "></canvas>";
		}
		case "Table" -> {
			webIds.put(id, "table");
			return "<table id=\"" + id + "\" class=\"wb-table\"" + tip + "></table>";
		}
		case "Browser" -> {
			webIds.put(id, "output");
			return "<div id=\"" + id + "\" class=\"wb-output\"></div>";
		}
		default -> {
			notes.add(w.type + " " + w.name + " is not converted");
			return "<!-- " + w.type + " " + w.name + ": not converted by WindowBuilder JavaScript -->";
		}
		}
	}

	/* The radio buttons of a parent (the name: the parent if it is a Group, else the first button) */
	private String webRadios(Widget parent, List<Widget> radios) {
		String name = parent.is("Group") ? parent.name : radios.get(0).name;
		webIds.put(name, "radios");
		StringBuilder sb = new StringBuilder("<span class=\"wb-radios\">");
		for (Widget w : radios) {
			String label = w.text().isEmpty() ? w.name : w.text();
			sb.append("<label><input type=\"radio\" name=\"").append(name).append("\" id=\"").append(w.name).append("\" value=\"").append(html(label)).append("\"")
					.append(Boolean.TRUE.equals(w.props.get("checked")) ? " checked" : "").append("> ").append(html(label)).append("</label>");
		}
		return sb.append("</span>").toString();
	}

	private static String html(String s) {
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	/* The template of app.js: the elements, the events, a draw function for each canvas */
	private String jsTemplate(String name) {
		StringBuilder sb = new StringBuilder();
		sb.append("// The code of the app (WindowBuilder JavaScript, GUI class ").append(name).append(".java).\n");
		sb.append("// Bio7 writes this file only once: your code stays when index.html is created again.\n\n");
		sb.append("// The elements of the page (the ids are the names of the variables of the GUI class)\n");
		for (Map.Entry<String, String> e : webIds.entrySet()) {
			if (!e.getValue().equals("radios")) {
				sb.append("const ").append(e.getKey()).append(" = document.getElementById(\"").append(e.getKey()).append("\");\n");
			}
		}
		sb.append("\n// The value of a group of radio buttons (its name)\n");
		sb.append("function radioValue(name) {\n  const r = document.querySelector('input[name=\"' + name + '\"]:checked');\n  return r ? r.value : null;\n}\n");
		for (Map.Entry<String, String> e : webIds.entrySet()) {
			sb.append(webStub(e.getKey(), e.getValue()));
		}
		return sb.toString();
	}

	/** The code of app.js for an element of the page (kind: button, canvas, table, ...), "" if none. */
	public static String webStub(String id, String kind) {
		return switch (kind) {
		case "button" -> "\n// The button " + id + "\n" + id + ".addEventListener(\"click\", () => {\n});\n";
		case "canvas" -> {
			String f = "draw" + Character.toUpperCase(id.charAt(0)) + id.substring(1);
			yield "\n// Draws " + id + "\nfunction " + f + "() {\n  const g = " + id + ".getContext(\"2d\");\n  g.clearRect(0, 0, " + id + ".width, " + id
					+ ".height);\n}\n" + f + "();\n";
		}
		case "webgl" -> {
			String f = "draw" + Character.toUpperCase(id.charAt(0)) + id.substring(1);
			yield "\n// The WebGL canvas " + id + " (WebGL 2, else WebGL 1)\nconst " + id + "Gl = " + id + ".getContext(\"webgl2\") || " + id
					+ ".getContext(\"webgl\");\n\n// Draws " + id + " (with requestAnimationFrame for an animation)\nfunction " + f + "() {\n  const gl = " + id
					+ "Gl;\n  gl.viewport(0, 0, " + id + ".width, " + id + ".height);\n  gl.clearColor(0.1, 0.1, 0.15, 1);\n  gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);\n}\n" + f
					+ "();\n";
		}
		case "table" -> "\n// Fills the table " + id + " (rows: arrays of values, the first row: the header)\nfunction fill" + Character.toUpperCase(id.charAt(0))
				+ id.substring(1) + "(rows) {\n  " + id + ".innerHTML = rows.map((r, i) => \"<tr>\" + r.map(v => i === 0 ? \"<th>\" + v + \"</th>\" : \"<td>\" + v + \"</td>\").join(\"\") + \"</tr>\").join(\"\");\n}\n";
		default -> "";
		};
	}

	/* The style of the page (written once, can be changed) */
	private static final String CSS = """
			/* The style of the app (WindowBuilder JavaScript): written once, change it as you like */
			body { font-family: system-ui, -apple-system, "Segoe UI", sans-serif; margin: 16px; color: #222; background: #fafafa; }
			h1 { font-size: 1.4em; margin: 0 0 12px; }
			.wb-grid { display: grid; gap: 8px 12px; align-items: center; }
			.wb-fill { display: flex; gap: 12px; }
			.wb-fill > .wb-cell { flex: 1; }
			.wb-row { display: flex; flex-wrap: wrap; gap: 12px; align-items: center; }
			.wb-cell input[type=text], .wb-cell input[type=number], .wb-cell select, .wb-cell textarea { width: 100%; box-sizing: border-box; }
			.wb-slider { display: flex; gap: 8px; align-items: center; }
			.wb-slider input { flex: 1; }
			.wb-slider output { min-width: 3em; text-align: right; font-variant-numeric: tabular-nums; }
			fieldset { border: 1px solid #ccc; border-radius: 6px; padding: 8px 12px; margin: 0; }
			/* A canvas keeps its size (the size hints of the design), smaller only on narrow screens */
			canvas { max-width: 100%; height: auto; border: 1px solid #ccc; background: #fff; }
			canvas.wb-webgl { background: #1a1a24; }
			.wb-output { font-variant-numeric: tabular-nums; }
			.wb-table { border-collapse: collapse; }
			.wb-table th, .wb-table td { border: 1px solid #ccc; padding: 2px 8px; text-align: right; }
			.wb-tabbar button { border: 1px solid #ccc; background: #eee; padding: 4px 12px; }
			.wb-tabbar button.active { background: #fff; border-bottom-color: #fff; }
			.wb-tab { border: 1px solid #ccc; padding: 8px; }
			button { padding: 4px 12px; }
			@media (prefers-color-scheme: dark) {
			  body { color: #ddd; background: #1e1e1e; }
			  canvas { background: #2a2a2a; border-color: #555; }
			  fieldset, .wb-table th, .wb-table td, .wb-tab { border-color: #555; }
			}
			""";

	/* ------------------------------------------------------------ the server template */

	private String serverTemplate() {
		StringBuilder sb = new StringBuilder();
		sb.append("# The server of the Shiny app (WindowBuilder Shiny). Bio7 writes this file only\n");
		sb.append("# once: your code stays when the user interface (ui.R) is created again.\n");
		if (!inputs.isEmpty()) {
			sb.append("#\n# The inputs of the GUI: ");
			List<String> in = new ArrayList<>();
			for (String i : inputs) {
				in.add("input$" + i);
			}
			sb.append(String.join(", ", in)).append("\n");
		}
		sb.append("library(shiny)\n\n");
		sb.append("function(input, output, session) {\n");
		for (String b : buttons) {
			sb.append(stub(b, null));
		}
		for (Map.Entry<String, String> o : outputs.entrySet()) {
			sb.append(stub(o.getKey(), o.getValue()));
		}
		sb.append("}\n");
		return sb.toString();
	}

	/** The code of the server for a button (render == null) or an output. */
	public static String stub(String name, String render) {
		if (render == null) {
			return "\n  # The button " + name + "\n  observeEvent(input$" + name + ", {\n  })\n";
		}
		String body = switch (render) {
		case "renderPlot" -> "    plot(runif(100))";
		case "renderTable" -> "    head(iris)";
		case "renderPrint" -> "    summary(runif(100))";
		case "renderUI" -> "    tags$p(\"Hello\")";
		case "rgl::renderRglwidget" -> "    rgl::open3d(useNULL = TRUE)\n    rgl::plot3d(rnorm(100), rnorm(100), rnorm(100), col = \"steelblue\")\n    rgl::rglwidget()";
		default -> "    \"\"";
		};
		return "\n  output$" + name + " <- " + render + "({\n" + body + "\n  })\n";
	}
}
