/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.reditor.antlr.refactor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;

import com.eco.bio7.reditor.antlr.RFilter;
import com.eco.bio7.reditor.antlr.RLexer;
import com.eco.bio7.reditor.antlr.RParser;
import com.eco.bio7.reditor.antlr.RParser.ExprContext;
import com.eco.bio7.reditor.antlr.RParser.FormContext;
import com.eco.bio7.reditor.antlr.RParser.SubContext;

/**
 * The refactorings of the R editor (text in, text out): extract variable,
 * extract function (parameters and results are detected), inline variable,
 * surround with tryCatch, collect library calls.
 * <p>
 * The parse tree of the grammar is converted into a syntax tree with the
 * precedence of R: the grammar binds a call weaker than the operators and
 * assignments ('x &lt;- f(y)' is parsed as '(x &lt;- f)(y)'), the syntax tree
 * applies the call to the last operand. Offsets are offsets of the Java string.
 */
public final class RRefactor {

	/** The result: the new text and the selection (offset, length), an error or a message. */
	public record Result(String text, int offset, int length, String error, String message) {
		static Result error(String e) {
			return new Result(null, 0, 0, e, null);
		}
	}

	/* ---------------- The syntax tree ---------------- */

	enum Kind {
		PROG, BLOCK, ID, STR, CONST, CALL, ARG, INDEX, DOLLAR, NS, ASSIGN, BINOP, UNARY, FUN, FORMAL, IF, FOR, WHILE, REPEAT, PAREN, NEXT, BREAK, OTHER
	}

	static final class Node {
		final Kind kind;
		String text; // the name (ID, ARG, FORMAL, FOR), the operator (ASSIGN, BINOP, UNARY, INDEX, DOLLAR)
		int start, end;
		final List<Node> kids = new ArrayList<>();
		Node parent;

		Node(Kind kind, int start, int end) {
			this.kind = kind;
			this.start = start;
			this.end = end;
		}

		Node add(Node n) {
			if (n != null) {
				kids.add(n);
				n.parent = this;
			}
			return this;
		}

		Node kid(int i) {
			return i < kids.size() ? kids.get(i) : null;
		}

		/* The target and the value of an assignment (x <- v, v -> x) */
		boolean rightward() {
			return text.equals("->") || text.equals("->>");
		}

		Node target() {
			return kid(rightward() ? 1 : 0);
		}

		Node value() {
			return kid(rightward() ? 0 : 1);
		}

		boolean superAssign() {
			return text.equals("<<-") || text.equals("->>");
		}
	}

	/* Functions which depend on the frame of the function which calls them */
	private static final Set<String> FRAME_FUNCTIONS = Set.of("return", "missing", "on.exit", "sys.call", "sys.function", "match.call", "parent.frame",
			"environment", "nargs");

	private static final Set<String> RESERVED = Set.of("if", "else", "repeat", "while", "function", "for", "next", "break", "TRUE", "FALSE", "NULL", "Inf",
			"NaN", "NA", "in", "T", "F");

	private final String text;
	private final String nl;
	private Node root;
	private boolean errors;
	private int[] cpToChar;

	public RRefactor(String text) {
		this.text = text;
		this.nl = text.contains("\r\n") ? "\r\n" : "\n";
		parse();
	}

	/** True if the code has syntax errors. */
	public boolean hasErrors() {
		return errors;
	}

	private void parse() {
		RLexer lexer = new RLexer(CharStreams.fromString(text));
		lexer.removeErrorListeners();
		lexer.addErrorListener(new BaseErrorListener() {
			@Override
			public void syntaxError(Recognizer<?, ?> r, Object o, int line, int col, String msg, RecognitionException e) {
				errors = true;
			}
		});
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		RFilter filter = new RFilter(tokens);
		filter.removeErrorListeners();
		filter.stream();
		tokens.seek(0);
		RParser parser = new RParser(tokens);
		parser.removeErrorListeners();
		parser.addErrorListener(new BaseErrorListener() {
			@Override
			public void syntaxError(Recognizer<?, ?> r, Object o, int line, int col, String msg, RecognitionException e) {
				errors = true;
			}
		});
		RParser.ProgContext prog = parser.prog();
		if (text.codePointCount(0, text.length()) != text.length()) {
			int n = text.codePointCount(0, text.length());
			cpToChar = new int[n + 1];
			for (int i = 0, c = 0; i <= n; i++) {
				cpToChar[i] = c;
				if (i < n) {
					c += Character.charCount(text.codePointAt(c));
				}
			}
		}
		if (errors || hasErrorNodes(prog)) {
			errors = true;
			return;
		}
		root = new Node(Kind.PROG, 0, text.length());
		for (ExprContext e : prog.expr()) {
			root.add(fix(build(e)));
		}
	}

	/* ---------------- The precedence of R ---------------- */

	/* The precedence of an operator node (high: binds stronger); -1: no operator */
	private static int prec(Node n) {
		return switch (n.kind) {
		case ASSIGN -> n.text.equals("=") ? 1 : n.rightward() ? 3 : 2;
		case FUN -> 0;
		case UNARY -> switch (n.text) {
			case "?" -> 0;
			case "~" -> 4;
			case "!" -> 7;
			default -> 13; // - +
			};
		case BINOP -> switch (n.text) {
			case "?" -> 0;
			case "~" -> 4;
			case "||", "|" -> 5;
			case "&&", "&" -> 6;
			case "==", "!=", "<", ">", "<=", ">=" -> 8;
			case "+", "-" -> 9;
			case "*", "/" -> 10;
			case ":" -> 12;
			case "^" -> 14;
			default -> 11; // %op%, |>, =>
			};
		case INDEX -> 15;
		case DOLLAR -> 16;
		default -> -1;
		};
	}

	private static boolean rightAssoc(Node n) {
		return n.kind == Kind.BINOP && n.text.equals("^") || n.kind == Kind.ASSIGN && !n.rightward();
	}

	/*
	 * The grouping of R: the grammar binds a call weaker than the operators, the
	 * operators after a call are grouped with the wrong operand (e.g., 'y <- f(x) + 1'
	 * as '(y <- f(x)) + 1'). A left operand with a lower precedence is rotated:
	 * P(L(a, b), c) -> L(a, P(b, c)). Named arguments (f(a = 1)) are no assignments.
	 */
	private Node fix(Node n) {
		for (int i = 0; i < n.kids.size(); i++) {
			Node k = fix(n.kids.get(i));
			n.kids.set(i, k);
			k.parent = n;
		}
		if (n.kind == Kind.ARG && n.text == null && n.kid(0) != null && n.kid(0).kind == Kind.ASSIGN && n.kid(0).text.equals("=")
				&& (n.kid(0).kid(0).kind == Kind.ID || n.kid(0).kid(0).kind == Kind.STR)) {
			Node a = n.kid(0);
			n.text = src(a.kid(0)).replaceAll("^[\"'`]|[\"'`]$", "");
			n.kids.set(0, a.kid(1));
			a.kid(1).parent = n;
			return n;
		}
		if (n.kind == Kind.BINOP || n.kind == Kind.INDEX || n.kind == Kind.DOLLAR) {
			Node l = n.kid(0);
			int pl = prec(l), pn = prec(n);
			boolean lower = l.kind == Kind.FUN || (l.kind == Kind.BINOP || l.kind == Kind.UNARY || l.kind == Kind.ASSIGN && !l.rightward())
					&& (pl < pn || pl == pn && rightAssoc(n) && l.kind != Kind.UNARY);
			if (lower) {
				int last = l.kids.size() - 1;
				Node b = l.kids.get(last);
				n.kids.set(0, b);
				b.parent = n;
				n.start = b.start;
				Node inner = fix(n);
				l.kids.set(last, inner);
				inner.parent = l;
				l.end = Math.max(l.end, inner.end);
				l.parent = null;
				return l;
			}
		}
		return n;
	}

	private static boolean hasErrorNodes(ParseTree t) {
		if (t instanceof ErrorNode || t.getClass().getSimpleName().startsWith("Err")) {
			return true;
		}
		for (int i = 0; i < t.getChildCount(); i++) {
			if (hasErrorNodes(t.getChild(i))) {
				return true;
			}
		}
		return false;
	}

	private int off(int cp) {
		return cpToChar == null ? cp : cpToChar[Math.min(cp, cpToChar.length - 1)];
	}

	private int start(ParserRuleContext c) {
		return off(c.getStart().getStartIndex());
	}

	private int end(ParserRuleContext c) {
		Token stop = c.getStop();
		return stop == null || stop.getStopIndex() < c.getStart().getStartIndex() ? start(c) : off(stop.getStopIndex() + 1);
	}

	private Node node(Kind k, ParserRuleContext c) {
		return new Node(k, start(c), end(c));
	}

	private static String op(ParserRuleContext c, int child) {
		return c.getChildCount() > child ? c.getChild(child).getText() : "";
	}

	/* The syntax tree of an expression */
	private Node build(ExprContext e) {
		if (e instanceof RParser.E20CallFunctionContext c) {
			Node fn = build(c.expr());
			List<Node> args = args(c.sublist());
			return applyCall(fn, args, end(c));
		}
		if (e instanceof RParser.E30Context) {
			Node n = node(Kind.ID, e);
			n.text = e.getText();
			return n;
		}
		if (e instanceof RParser.E31Context || e instanceof RParser.E42Context) {
			return node(Kind.STR, e);
		}
		if (e instanceof RParser.E1Context || e instanceof RParser.E2Context) {
			Node n = node(Kind.INDEX, e);
			n.text = op(e, 1);
			n.add(build((ExprContext) e.getChild(0)));
			args(e instanceof RParser.E1Context i1 ? i1.sublist() : ((RParser.E2Context) e).sublist()).forEach(n::add);
			return n;
		}
		if (e instanceof RParser.E3Context) {
			return node(Kind.NS, e);
		}
		if (e instanceof RParser.E4Context d) {
			Node n = node(Kind.DOLLAR, e);
			n.text = op(e, 1);
			n.add(build(d.expr(0)));
			return n;
		}
		if (e instanceof RParser.E17VariableDeclarationContext a) {
			Node n = node(Kind.ASSIGN, e);
			n.text = op(e, 1);
			n.add(build(a.expr(0))).add(build(a.expr(1)));
			return n;
		}
		if (e instanceof RParser.E19DefFunctionContext f) {
			Node n = node(Kind.FUN, e);
			if (f.formlist() != null) {
				for (FormContext form : f.formlist().form()) {
					Node fm = new Node(Kind.FORMAL, start(form), end(form));
					fm.text = form.ID() != null ? form.ID().getText() : "...";
					if (form.expr() != null) {
						fm.add(build(form.expr()));
					}
					n.add(fm);
				}
			}
			n.add(build(f.expr())); // the body: the last kid
			return n;
		}
		if (e instanceof RParser.E18Context b) {
			Node n = node(Kind.BLOCK, e);
			for (ExprContext s : b.exprlist().expr()) {
				n.add(build(s));
			}
			return n;
		}
		if (e instanceof RParser.E21Context || e instanceof RParser.E22Context) {
			Node n = node(Kind.IF, e);
			for (int i = 0; i < e.getChildCount(); i++) {
				if (e.getChild(i) instanceof ExprContext x) {
					n.add(build(x));
				}
			}
			return n;
		}
		if (e instanceof RParser.E23ForLoopContext l) {
			Node n = node(Kind.FOR, e);
			n.text = l.ID().getText();
			n.add(build(l.expr(0))).add(build(l.expr(1)));
			return n;
		}
		if (e instanceof RParser.E24Context w) {
			return node(Kind.WHILE, e).add(build(w.expr(0))).add(build(w.expr(1)));
		}
		if (e instanceof RParser.E25Context r) {
			return node(Kind.REPEAT, e).add(build(r.expr()));
		}
		if (e instanceof RParser.E29Context p) {
			return node(Kind.PAREN, e).add(build(p.expr()));
		}
		if (e instanceof RParser.E27Context) {
			return node(Kind.NEXT, e);
		}
		if (e instanceof RParser.E28Context) {
			return node(Kind.BREAK, e);
		}
		/* Operators: binary (two expressions) or unary (an operator and an expression) */
		List<ExprContext> xs = new ArrayList<>();
		for (int i = 0; i < e.getChildCount(); i++) {
			if (e.getChild(i) instanceof ExprContext x) {
				xs.add(x);
			}
		}
		if (xs.size() == 2) {
			Node n = node(Kind.BINOP, e);
			n.text = op(e, 1);
			return n.add(build(xs.get(0))).add(build(xs.get(1)));
		}
		if (xs.size() == 1) {
			Node n = node(Kind.UNARY, e);
			n.text = op(e, 0);
			return n.add(build(xs.get(0)));
		}
		return node(e.getChildCount() == 1 ? Kind.CONST : Kind.OTHER, e);
	}

	private List<Node> args(RParser.SublistContext list) {
		List<Node> out = new ArrayList<>();
		if (list == null) {
			return out;
		}
		for (SubContext s : list.sub()) {
			if (s.getChildCount() == 0) {
				continue;
			}
			Node a = new Node(Kind.ARG, start(s), end(s));
			if (s.getChildCount() >= 2 && s.getChild(1).getText().equals("=")) {
				a.text = s.getChild(0).getText();
			} else if (s.getText().equals("...")) {
				a.text = "...";
			}
			if (s.expr() != null) {
				a.add(build(s.expr()));
			}
			out.add(a);
		}
		return out;
	}

	/* A call: applied to the last operand of operators (the precedence of R) */
	private Node applyCall(Node fn, List<Node> args, int end) {
		if ((fn.kind == Kind.ASSIGN && !fn.rightward()) || fn.kind == Kind.BINOP || fn.kind == Kind.UNARY || fn.kind == Kind.FUN) {
			int last = fn.kids.size() - 1;
			Node replaced = applyCall(fn.kids.get(last), args, end);
			fn.kids.set(last, replaced);
			replaced.parent = fn;
			fn.end = end;
			return fn;
		}
		Node call = new Node(Kind.CALL, fn.start, end);
		call.add(fn);
		args.forEach(call::add);
		return call;
	}

	/* ---------------- Helpers ---------------- */

	private String src(Node n) {
		return text.substring(n.start, n.end);
	}

	private static boolean isStatement(Node n) {
		return n.parent != null && (n.parent.kind == Kind.PROG || n.parent.kind == Kind.BLOCK);
	}

	/* The function of a node or the program */
	private Node scopeOf(Node n) {
		Node p = n.parent;
		while (p != null && p.kind != Kind.FUN && p.kind != Kind.PROG) {
			p = p.parent;
		}
		return p == null ? root : p;
	}

	private static Node body(Node scope) {
		return scope.kind == Kind.FUN ? scope.kids.get(scope.kids.size() - 1) : scope;
	}

	private static List<Node> formals(Node fun) {
		List<Node> out = new ArrayList<>();
		for (Node k : fun.kids) {
			if (k.kind == Kind.FORMAL) {
				out.add(k);
			}
		}
		return out;
	}

	/* The variable of a target: x of x, names(x), x$a, x[i], x[[i]] */
	private static String baseName(Node t) {
		if (t == null) {
			return null;
		}
		return switch (t.kind) {
		case ID -> t.text;
		case INDEX, DOLLAR, PAREN -> baseName(t.kid(0));
		case CALL -> t.kids.size() > 1 && t.kid(1).kid(0) != null ? baseName(t.kid(1).kid(0)) : null;
		case STR -> t.parent != null && t.parent.kind == Kind.ASSIGN ? null : null;
		default -> null;
		};
	}

	private String targetName(Node assign) {
		Node t = assign.target();
		if (t.kind == Kind.STR) {
			String s = src(t);
			return s.length() >= 2 ? s.substring(1, s.length() - 1) : s;
		}
		return baseName(t);
	}

	private static void ids(Node n, Set<String> out) {
		if (n.kind == Kind.ID) {
			out.add(n.text);
		}
		for (Node k : n.kids) {
			ids(k, out);
		}
	}

	private Set<String> allNames() {
		Set<String> s = new HashSet<>();
		ids(root, s);
		return s;
	}

	private static String validName(String name) {
		if (name == null || !name.matches("[a-zA-Z.][a-zA-Z0-9._]*") || name.matches("\\.\\d.*") || RESERVED.contains(name)) {
			return "'" + name + "' is not a valid R name";
		}
		return null;
	}

	private int lineStart(int offset) {
		int i = offset;
		while (i > 0 && text.charAt(i - 1) != '\n') {
			i--;
		}
		return i;
	}

	private int lineEnd(int offset) {
		int i = offset;
		while (i < text.length() && text.charAt(i) != '\n') {
			i++;
		}
		return i < text.length() ? i + 1 : i;
	}

	private String indentAt(int offset) {
		int s = lineStart(offset), i = s;
		while (i < text.length() && (text.charAt(i) == ' ' || text.charAt(i) == '\t')) {
			i++;
		}
		return text.substring(s, i);
	}

	/* The selection as complete lines: [from, to) */
	private int[] lines(int selStart, int selEnd) {
		int from = lineStart(selStart);
		int to = selEnd > selStart && selEnd == lineStart(selEnd) ? selEnd : lineEnd(Math.max(selStart, selEnd - 1));
		return new int[] { from, to };
	}

	/* The range of a statement with a following ';' (and spaces) or a ';' before it */
	private int[] removalRange(Node s) {
		int ls = lineStart(s.start), le = lineEnd(Math.max(s.start, s.end - 1));
		String before = text.substring(ls, s.start);
		String after = text.substring(s.end, le).strip();
		if (before.isBlank() && (after.isEmpty() || after.equals(";"))) {
			return new int[] { ls, le };
		}
		int stop = s.end, begin = s.start;
		if (after.startsWith(";")) {
			stop = text.indexOf(';', s.end) + 1;
			while (stop < text.length() && text.charAt(stop) == ' ') {
				stop++;
			}
		} else {
			/* the last statement of the line: the ';' before it */
			int b = s.start;
			while (b > ls && text.charAt(b - 1) == ' ') {
				b--;
			}
			if (b > ls && text.charAt(b - 1) == ';') {
				begin = b - 1;
			}
		}
		return new int[] { begin, stop };
	}

	/* ---------------- The names: reads and writes in the order of evaluation ---------------- */

	private final class Flow {
		final List<String> reads = new ArrayList<>(); // read before the first write
		final Set<String> allReads = new HashSet<>();
		final Set<String> writes = new LinkedHashSet<>();
		String bad;
		int loop;

		void read(String n) {
			allReads.add(n);
			if (!writes.contains(n) && !reads.contains(n)) {
				reads.add(n);
			}
		}

		void visit(Node n) {
			if (n == null) {
				return;
			}
			switch (n.kind) {
			case ASSIGN -> {
				visit(n.value());
				if (n.superAssign()) {
					bad = "'<<-' in the selection";
				}
				Node t = n.target();
				if (t.kind == Kind.ID) {
					writes.add(t.text);
				} else if (t.kind == Kind.STR) {
					writes.add(targetName(n));
				} else {
					/* names(x) <- v, x$a <- v, x[i] <- v: x is read and changed */
					String base = baseName(t);
					targetParts(t);
					if (base != null) {
						read(base);
						writes.add(base);
					}
				}
			}
			case FUN -> {
				/* A nested function: its free names are read */
				Flow inner = new Flow();
				for (Node f : formals(n)) {
					inner.writes.add(f.text);
				}
				for (Node f : formals(n)) {
					f.kids.forEach(inner::visit);
				}
				inner.visit(body(n));
				inner.reads.forEach(this::read);
			}
			case FOR -> {
				visit(n.kid(0));
				writes.add(n.text);
				loop++;
				visit(n.kid(1));
				loop--;
			}
			case WHILE, REPEAT -> {
				loop++;
				n.kids.forEach(this::visit);
				loop--;
			}
			case DOLLAR -> visit(n.kid(0));
			case NS, STR, CONST -> {
			}
			case CALL -> {
				Node fn = n.kid(0);
				if (fn.kind == Kind.ID && FRAME_FUNCTIONS.contains(fn.text)) {
					bad = "'" + fn.text + "()' in the selection";
				}
				n.kids.forEach(this::visit);
			}
			case NEXT, BREAK -> {
				if (loop == 0) {
					bad = "'next' or 'break' without its loop in the selection";
				}
			}
			case ID -> read(n.text);
			default -> n.kids.forEach(this::visit);
			}
		}

		/* The evaluated parts of a target (indices, further arguments), not its variable */
		private void targetParts(Node t) {
			switch (t.kind) {
			case INDEX -> {
				targetParts(t.kid(0));
				for (int i = 1; i < t.kids.size(); i++) {
					visit(t.kid(i));
				}
			}
			case DOLLAR, PAREN -> targetParts(t.kid(0));
			case CALL -> {
				if (t.kids.size() > 1 && t.kid(1).kid(0) != null) {
					targetParts(t.kid(1).kid(0));
				}
				for (int i = 2; i < t.kids.size(); i++) {
					visit(t.kid(i));
				}
			}
			default -> {
			}
			}
		}
	}

	/* The local variables of a scope: the arguments and the assigned names (in the program not the functions) */
	private Set<String> scopeLocals(Node scope) {
		Set<String> names = new HashSet<>();
		if (scope.kind == Kind.FUN) {
			for (Node f : formals(scope)) {
				names.add(f.text);
			}
		}
		collectWrites(body(scope), names, scope.kind == Kind.PROG);
		return names;
	}

	private void collectWrites(Node n, Set<String> names, boolean program) {
		if (n.kind == Kind.FUN) {
			return;
		}
		if (n.kind == Kind.ASSIGN && !n.superAssign()) {
			String base = targetName(n);
			if (base != null && !(program && n.value().kind == Kind.FUN)) {
				names.add(base);
			}
		}
		if (n.kind == Kind.FOR) {
			names.add(n.text);
		}
		for (Node k : n.kids) {
			collectWrites(k, names, program);
		}
	}

	/* ---------------- Extract variable ---------------- */

	/** The number of occurrences of the selected expression (for the question "replace all"); 0: not an expression. */
	public int occurrences(int selStart, int selEnd) {
		Object[] found = findExpression(selStart, selEnd);
		return found[0] instanceof Node n ? sameExpressions(n, (Node) found[1]).size() : 0;
	}

	/* The selected expression and its statement (or an error message) */
	private Object[] findExpression(int selStart, int selEnd) {
		if (errors) {
			return new Object[] { "The code has syntax errors" };
		}
		String sel = text.substring(selStart, selEnd);
		int s = selStart + (sel.length() - sel.stripLeading().length()), e = selEnd - (sel.length() - sel.stripTrailing().length());
		if (s >= e) {
			return new Object[] { "Please select an expression" };
		}
		Node[] found = new Node[1];
		find(root, s, e, found);
		Node node = found[0];
		if (node == null || node.kind == Kind.ARG || node.kind == Kind.FORMAL || node.kind == Kind.PROG) {
			return new Object[] { "Please select a complete expression" };
		}
		if (node.kind == Kind.ASSIGN) {
			return new Object[] { "Please select an expression (not an assignment)" };
		}
		if (node.parent.kind == Kind.ASSIGN && node.parent.target() == node) {
			return new Object[] { "The target of an assignment can't be extracted" };
		}
		if (node.parent.kind == Kind.CALL && node.parent.kid(0) == node && node.kind == Kind.ID) {
			return new Object[] { "Please select the call (with its arguments)" };
		}
		/* The statement and the checks of the way from the expression to it */
		Node child = node, p = node.parent;
		while (!isStatement(child)) {
			if (p.kind == Kind.FUN) {
				return new Object[] { "Not possible in a function without { }" };
			}
			if (p.kind == Kind.WHILE && child == p.kid(0)) {
				return new Object[] { "Not possible in the condition of 'while' (the value would be computed only once)" };
			}
			if ((p.kind == Kind.FOR || p.kind == Kind.WHILE) && child == p.kid(1) || p.kind == Kind.REPEAT) {
				return new Object[] { "Not possible in the body of a loop without { }" };
			}
			if (p.kind == Kind.IF && child != p.kid(0)) {
				return new Object[] { "Not possible in a branch of 'if' without { }" };
			}
			if (p.kind == Kind.BINOP && (p.text.equals("&&") || p.text.equals("||")) && child == p.kid(1)) {
				return new Object[] { "Not possible on the right side of && or || (the value would always be computed)" };
			}
			if (p.kind == Kind.BINOP && p.text.equals("~") || p.kind == Kind.UNARY && p.text.equals("~")) {
				return new Object[] { "Not possible in a formula" };
			}
			child = p;
			p = p.parent;
		}
		return new Object[] { node, child };
	}

	private static void find(Node n, int s, int e, Node[] found) {
		if (n.start == s && n.end == e) {
			found[0] = n; // the innermost node with this range
		}
		for (Node k : n.kids) {
			if (k.start <= s && k.end >= e) {
				find(k, s, e, found);
			}
		}
	}

	/* The same expressions (code without spaces) in the scope after the statement, until a variable of it changes */
	private List<Node> sameExpressions(Node node, Node statement) {
		String key = compact(node);
		Node scope = scopeOf(node);
		Flow f = new Flow();
		f.visit(node);
		int[] limit = { Integer.MAX_VALUE };
		firstWrite(body(scope), f.allReads, statement.end, limit);
		List<Node> out = new ArrayList<>();
		same(body(scope), key, statement.start, limit[0], out);
		if (!out.contains(node)) {
			out.add(node);
		}
		return out;
	}

	private void firstWrite(Node n, Set<String> names, int after, int[] limit) {
		if (n.kind == Kind.FUN && n != root) {
			return;
		}
		if (n.kind == Kind.ASSIGN && n.start >= after && names.contains(targetName(n))) {
			limit[0] = Math.min(limit[0], n.start);
		}
		if (n.kind == Kind.FOR && n.start >= after && names.contains(n.text)) {
			limit[0] = Math.min(limit[0], n.start);
		}
		for (Node k : n.kids) {
			firstWrite(k, names, after, limit);
		}
	}

	private void same(Node n, String key, int from, int to, List<Node> out) {
		if (n.kind == Kind.FUN) {
			return;
		}
		if (n.start >= from && n.end <= to && n.kind != Kind.ARG && n.kind != Kind.ASSIGN && compact(n).equals(key)
				&& !(n.parent != null && n.parent.kind == Kind.ASSIGN && n.parent.target() == n)) {
			out.add(n);
			return;
		}
		for (Node k : n.kids) {
			same(k, key, from, to, out);
		}
	}

	private String compact(Node n) {
		return src(n).replaceAll("\\s+", "");
	}

	/** Extracts the selected expression into a variable (all: the same expressions after it too). */
	public Result extractVariable(int selStart, int selEnd, String name, boolean all) {
		String err = validName(name);
		if (err != null) {
			return Result.error(err);
		}
		Object[] found = findExpression(selStart, selEnd);
		if (!(found[0] instanceof Node node)) {
			return Result.error((String) found[0]);
		}
		if (allNames().contains(name)) {
			return Result.error("'" + name + "' is already used in this file");
		}
		Node statement = (Node) found[1];
		List<Node> targets = new ArrayList<>(all ? sameExpressions(node, statement) : List.of(node));
		targets.sort((x, y) -> Integer.compare(y.start, x.start));
		StringBuilder b = new StringBuilder(text);
		for (Node t : targets) {
			b.replace(t.start, t.end, name);
		}
		int at = statement.start;
		String assign = name + " <- " + src(node).strip();
		String insert;
		int insertAt;
		if (text.substring(lineStart(at), at).isBlank()) {
			insertAt = lineStart(at);
			insert = indentAt(at) + assign + nl;
		} else {
			insertAt = at;
			insert = assign + nl + indentAt(at);
		}
		b.insert(insertAt, insert);
		String result = b.toString();
		if (new RRefactor(result).errors) {
			return Result.error("The result has syntax errors (the selection can't be extracted)");
		}
		return new Result(result, insertAt + insert.indexOf(name), name.length(), null,
				targets.size() > 1 ? targets.size() + " occurrences replaced" : null);
	}

	/* ---------------- Extract function ---------------- */

	/** Extracts the selected statements (complete lines) into a function before the statement of the program. */
	public Result extractFunction(int selStart, int selEnd, String name) {
		String err = validName(name);
		if (err != null) {
			return Result.error(err);
		}
		if (errors) {
			return Result.error("The code has syntax errors");
		}
		if (allNames().contains(name)) {
			return Result.error("'" + name + "' is already used in this file");
		}
		int[] range = lines(selStart, selEnd);
		int from = range[0], to = range[1];
		/* The innermost block with complete statements in the lines */
		List<Node> chosen = null;
		Node block = null;
		for (Node b : blocks()) {
			List<Node> inside = new ArrayList<>(), partial = new ArrayList<>();
			for (Node s : b.kids) {
				if (s.start >= from && s.end <= to) {
					inside.add(s);
				} else if (s.start < to && s.end > from) {
					partial.add(s);
				}
			}
			if (!inside.isEmpty() && partial.isEmpty()) {
				chosen = inside;
				block = b;
				break;
			}
		}
		if (chosen == null) {
			return Result.error("Please select complete statements (of one block)");
		}
		int selFrom = chosen.get(0).start, selTo = chosen.get(chosen.size() - 1).end;
		String outside = text.substring(from, selFrom) + text.substring(selTo, to);
		if (!outside.replaceAll("#[^\n]*", "").replace(";", "").isBlank()) {
			return Result.error("Please select complete lines (the selection contains a part of a block)");
		}
		Node scope = block.kind == Kind.PROG ? root : scopeOf(block);
		Flow flow = new Flow();
		chosen.forEach(flow::visit);
		if (flow.bad != null) {
			return Result.error("Not possible: " + flow.bad);
		}
		Set<String> locals = scopeLocals(scope);
		List<String> params = new ArrayList<>();
		for (String r : flow.reads) {
			if (locals.contains(r)) {
				params.add(r);
			}
		}
		/* The results: changed names which are read elsewhere in the scope */
		Flow rest = new Flow();
		outside(body(scope), selFrom, selTo, rest);
		List<String> results = new ArrayList<>();
		for (String w : flow.writes) {
			if (rest.allReads.contains(w)) {
				results.add(w);
			}
		}
		/* The function: the selected lines indented by 2 spaces */
		String[] lines = text.substring(from, to).split("\r?\n", -1);
		int count = lines.length > 0 && lines[lines.length - 1].isEmpty() ? lines.length - 1 : lines.length;
		int base = Integer.MAX_VALUE;
		for (int i = 0; i < count; i++) {
			if (!lines[i].isBlank()) {
				base = Math.min(base, lines[i].length() - lines[i].stripLeading().length());
			}
		}
		StringBuilder fn = new StringBuilder(name + " <- function(" + String.join(", ", params) + ") {" + nl);
		for (int i = 0; i < count; i++) {
			fn.append(lines[i].isBlank() ? "" : "  " + lines[i].substring(Math.min(base, lines[i].length()))).append(nl);
		}
		if (results.size() == 1) {
			fn.append("  ").append(results.get(0)).append(nl);
		} else if (results.size() > 1) {
			StringBuilder l = new StringBuilder();
			for (String r : results) {
				l.append(l.isEmpty() ? "" : ", ").append(r).append(" = ").append(r);
			}
			fn.append("  list(").append(l).append(")").append(nl);
		}
		fn.append("}").append(nl);
		/* The call */
		String indent = indentAt(selFrom);
		String call = name + "(" + String.join(", ", params) + ")";
		StringBuilder callText = new StringBuilder();
		if (results.isEmpty()) {
			callText.append(indent).append(call).append(nl);
		} else if (results.size() == 1) {
			callText.append(indent).append(results.get(0)).append(" <- ").append(call).append(nl);
		} else {
			String res = allNames().contains("res") ? name + "_result" : "res";
			callText.append(indent).append(res).append(" <- ").append(call).append(nl);
			for (String r : results) {
				callText.append(indent).append(r).append(" <- ").append(res).append("$").append(r).append(nl);
			}
		}
		/* Before the statement of the program which contains the selection (and its comments) */
		Node top = null;
		for (Node s : root.kids) {
			if (s.start <= selFrom && s.end >= selTo) {
				top = s;
			}
		}
		int insertAt = lineStart(top == null ? selFrom : top.start);
		while (insertAt > 0 && text.substring(lineStart(insertAt - 1), insertAt).strip().startsWith("#")) {
			insertAt = lineStart(insertAt - 1);
		}
		String before = insertAt > 0 && !text.substring(lineStart(insertAt - 1), insertAt).isBlank() ? nl : "";
		StringBuilder b = new StringBuilder(text);
		b.replace(from, to, callText.toString());
		b.insert(insertAt, before + fn + nl);
		String result = b.toString();
		if (new RRefactor(result).errors) {
			return Result.error("The result has syntax errors (the selection can't be extracted)");
		}
		return new Result(result, insertAt + before.length(), name.length(), null,
				"Parameters: " + (params.isEmpty() ? "none" : String.join(", ", params)) + "\nResults: " + (results.isEmpty() ? "none" : String.join(", ", results)));
	}

	/* The blocks (the program, { }): the innermost first */
	private List<Node> blocks() {
		List<Node> out = new ArrayList<>();
		collectBlocks(root, out);
		out.sort((a, b) -> Integer.compare(a.end - a.start, b.end - b.start));
		return out;
	}

	private static void collectBlocks(Node n, List<Node> out) {
		if (n.kind == Kind.PROG || n.kind == Kind.BLOCK) {
			out.add(n);
		}
		for (Node k : n.kids) {
			collectBlocks(k, out);
		}
	}

	/* The names read outside of the range (in the rest of the scope, in nested functions too) */
	private void outside(Node n, int from, int to, Flow f) {
		if (n.start >= from && n.end <= to) {
			return;
		}
		if (n.end <= from || n.start >= to) {
			Set<String> s = new HashSet<>();
			readIds(n, s);
			s.forEach(f::read);
			return;
		}
		if (n.kind == Kind.ID) {
			f.read(n.text);
		}
		for (Node k : n.kids) {
			outside(k, from, to, f);
		}
	}

	/* The names which are read (not the targets of assignments, not x of a$x) */
	private static void readIds(Node n, Set<String> out) {
		if (n.kind == Kind.ID) {
			out.add(n.text);
			return;
		}
		if (n.kind == Kind.ASSIGN) {
			Node t = n.target();
			if (t.kind != Kind.ID && t.kind != Kind.STR) {
				readIds(t, out);
			}
			readIds(n.value(), out);
			return;
		}
		for (Node k : n.kids) {
			readIds(k, out);
		}
	}

	/* ---------------- Inline variable ---------------- */

	/** Replaces the variable at the offset with its value (assigned once with 'name <- value' in its scope). */
	public Result inline(int offset) {
		if (errors) {
			return Result.error("The code has syntax errors");
		}
		Node at = idAt(root, offset);
		if (at == null) {
			return Result.error("No variable at the cursor");
		}
		String name = at.text;
		Node scope = scopeOf(at);
		if (scope.kind == Kind.FUN) {
			for (Node f : formals(scope)) {
				if (f.text.equals(name)) {
					return Result.error("'" + name + "' is an argument of the function");
				}
			}
		}
		List<Node> assigns = new ArrayList<>(), uses = new ArrayList<>();
		String[] problem = new String[1];
		scan(body(scope), name, assigns, uses, problem);
		if (problem[0] != null) {
			return Result.error(problem[0]);
		}
		if (assigns.size() != 1) {
			return Result.error("'" + name + "' must be assigned exactly once in its scope (" + assigns.size() + " assignments)");
		}
		Node a = assigns.get(0);
		if (!(a.text.equals("<-") || a.text.equals("=")) || !isStatement(a) || a.target().kind != Kind.ID) {
			return Result.error("'" + name + "' is not assigned with 'name <- value' as a statement");
		}
		if (uses.isEmpty()) {
			return Result.error("'" + name + "' is not used");
		}
		for (Node u : uses) {
			if (u.start < a.end) {
				return Result.error("'" + name + "' is used before the assignment");
			}
		}
		Node value = a.value();
		String v = src(value);
		boolean atomic = switch (value.kind) {
		case ID, STR, CONST, CALL, INDEX, DOLLAR, NS, PAREN, BLOCK -> true;
		default -> false;
		};
		List<int[]> edits = new ArrayList<>();
		List<String> texts = new ArrayList<>();
		for (Node u : uses) {
			Node p = u.parent;
			boolean plain = atomic || isStatement(u) || p.kind == Kind.ARG || p.kind == Kind.PAREN || p.kind == Kind.ASSIGN && p.value() == u
					|| p.kind == Kind.IF && p.kid(0) == u || p.kind == Kind.FORMAL;
			edits.add(new int[] { u.start, u.end });
			texts.add(plain ? v : "(" + v + ")");
		}
		int[] rm = removalRange(a);
		edits.add(rm);
		texts.add("");
		StringBuilder b = new StringBuilder(text);
		Integer[] order = new Integer[edits.size()];
		for (int i = 0; i < order.length; i++) {
			order[i] = i;
		}
		java.util.Arrays.sort(order, (x, y) -> Integer.compare(edits.get(y)[0], edits.get(x)[0]));
		for (int i : order) {
			b.replace(edits.get(i)[0], edits.get(i)[1], texts.get(i));
		}
		String result = b.toString();
		if (new RRefactor(result).errors) {
			return Result.error("The result has syntax errors");
		}
		return new Result(result, Math.min(rm[0], result.length()), 0, null, uses.size() + " use" + (uses.size() == 1 ? "" : "s") + " replaced");
	}

	private static Node idAt(Node n, int offset) {
		if (n.kind == Kind.ID && n.start <= offset && offset <= n.end) {
			return n;
		}
		for (Node k : n.kids) {
			if (k.start <= offset && offset <= k.end) {
				Node r = idAt(k, offset);
				if (r != null) {
					return r;
				}
			}
		}
		return null;
	}

	private void scan(Node n, String name, List<Node> assigns, List<Node> uses, String[] problem) {
		switch (n.kind) {
		case FUN -> {
			/* A nested function: the value would be computed at its call */
			Flow inner = new Flow();
			inner.visit(n);
			if (inner.allReads.contains(name)) {
				problem[0] = "'" + name + "' is used in a nested function";
			}
			return;
		}
		case ASSIGN -> {
			if (name.equals(targetName(n))) {
				assigns.add(n);
				if (n.target().kind != Kind.ID) {
					problem[0] = "'" + name + "' is changed with a replacement (e.g., names(x) <- ...)";
				}
				if (n.superAssign()) {
					problem[0] = "'" + name + "' is assigned with '<<-'";
				}
				scan(n.value(), name, assigns, uses, problem);
				return;
			}
		}
		case FOR -> {
			if (n.text.equals(name)) {
				problem[0] = "'" + name + "' is the variable of a for loop";
			}
		}
		case BINOP, UNARY -> {
			if (n.text.equals("~")) {
				Set<String> s = new HashSet<>();
				ids(n, s);
				if (s.contains(name)) {
					problem[0] = "'" + name + "' is used in a formula";
				}
			}
		}
		case DOLLAR -> {
			scan(n.kid(0), name, assigns, uses, problem);
			return;
		}
		case NS -> {
			return;
		}
		case ID -> {
			if (n.text.equals(name)) {
				uses.add(n);
			}
		}
		default -> {
		}
		}
		for (Node k : n.kids) {
			scan(k, name, assigns, uses, problem);
		}
	}

	/* ---------------- Surround with tryCatch ---------------- */

	/** Surrounds the selected lines with tryCatch (the code runs in the same environment). */
	public Result surroundTryCatch(int selStart, int selEnd) {
		int[] range = lines(selStart, selEnd);
		int from = range[0], to = range[1];
		String[] lines = text.substring(from, to).split("\r?\n", -1);
		int count = lines.length > 0 && lines[lines.length - 1].isEmpty() ? lines.length - 1 : lines.length;
		String indent = null;
		for (int i = 0; i < count; i++) {
			if (!lines[i].isBlank()) {
				String ind = lines[i].substring(0, lines[i].length() - lines[i].stripLeading().length());
				if (indent == null || ind.length() < indent.length()) {
					indent = ind;
				}
			}
		}
		if (indent == null) {
			return Result.error("Please select one or more lines");
		}
		String handler = "message(\"Error: \", conditionMessage(e))";
		StringBuilder b = new StringBuilder();
		b.append(indent).append("tryCatch({").append(nl);
		for (int i = 0; i < count; i++) {
			b.append(lines[i].isBlank() ? "" : "  " + lines[i]).append(nl);
		}
		b.append(indent).append("}, error = function(e) {").append(nl);
		b.append(indent).append("  ").append(handler).append(nl);
		b.append(indent).append("})").append(nl);
		String result = text.substring(0, from) + b + text.substring(to);
		if (!errors && new RRefactor(result).errors) {
			return Result.error("Please select complete statements");
		}
		return new Result(result, from + b.indexOf(handler), handler.length(), null, null);
	}

	/* ---------------- Library calls ---------------- */

	/**
	 * Collects the library() and require() calls of the program at the position
	 * of the first one (the order stays, duplicates are removed).
	 */
	public Result collectLibraries() {
		if (errors) {
			return Result.error("The code has syntax errors");
		}
		List<Node> calls = new ArrayList<>();
		for (Node s : root.kids) {
			if (s.kind == Kind.CALL && s.kid(0).kind == Kind.ID && (s.kid(0).text.equals("library") || s.kid(0).text.equals("require")) && s.kids.size() > 1
					&& s.kid(1).kid(0) != null) {
				calls.add(s);
			}
		}
		if (calls.isEmpty()) {
			return Result.error("No library() or require() calls at the top level");
		}
		Map<String, String> unique = new LinkedHashMap<>();
		for (Node s : calls) {
			String pkg = src(s.kid(1).kid(0)).replaceAll("^[\"']|[\"']$", "");
			unique.putIfAbsent(pkg, src(s).strip());
		}
		int insertAt = lineStart(calls.get(0).start);
		StringBuilder block = new StringBuilder();
		for (String code : unique.values()) {
			block.append(code).append(nl);
		}
		StringBuilder b = new StringBuilder(text);
		for (int i = calls.size() - 1; i >= 0; i--) {
			int[] r = removalRange(calls.get(i));
			b.delete(r[0], r[1]);
		}
		b.insert(insertAt, block);
		String result = b.toString();
		if (result.equals(text)) {
			return new Result(text, insertAt, 0, null, "The library calls are already collected");
		}
		int removed = calls.size() - unique.size();
		return new Result(result, insertAt, block.length(), null,
				unique.size() + " library calls" + (removed > 0 ? ", " + removed + " duplicate" + (removed == 1 ? "" : "s") + " removed" : ""));
	}
}
