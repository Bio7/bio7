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
package com.eco.bio7.simulation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.TextBlock;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.YieldStatement;

/**
 * Translates a simulation model (a subclass of {@link GridModel},
 * {@link ValueGridModel} or {@link AgentModel}) from Java to JavaScript for the HTML export. The
 * translation supports a subset of Java: fields, methods (no overloading),
 * constructors, static nested classes, the statements (no try/catch), arrays,
 * Math, lambdas, ArrayList and HashMap (the common methods), String methods
 * and the integer division. Other constructs are reported as errors with the
 * line number.
 */
public class JavaToJs {

	/** A setting of the model (a field with the annotation Param). */
	public static class ParamInfo {
		public String name, jsName, label, kind;
		public double min = Double.NaN, max = Double.NaN, step = Double.NaN;
		public String[] options = new String[0];
		public boolean restart;
		public Object value;
		/** The group (a heading), "" without. */
		public String group = "";
	}

	/** An action of the model (a method with the annotation Action): a button. */
	public static class ActionInfo {
		public String name, jsName, label, group = "";
	}

	private static final String API = "com.eco.bio7.simulation.";

	/* The words of JavaScript which are no words of Java (renamed with a _) */
	private static final Set<String> RESERVED = Set.of("arguments", "await", "delete", "eval", "export", "function", "in", "let", "typeof", "var", "with", "yield", "undefined", "NaN", "Infinity", "model", "sim",
			"SimRandom", "Simulation", "GridModel", "AgentModel", "Agent", "GeoAgent", "GeoAgentModel", "GeoGridModel", "ValueGridModel", "Map", "Set", "Array", "Object", "Number", "String", "Math", "Error", "console", "window", "document");

	/* The members of the JavaScript runtime (a field of the model must not hide them) */
	private static final Set<String> API_METHODS = Set.of("init", "step", "seed", "random", "randomInt", "chart", "size", "palette", "get", "set", "setNext", "swap", "countNeighbors", "count", "box", "range", "colorMap", "sum4", "sum8", "mean", "chance", "randomGaussian", "randomExponential", "randomPoisson",
			"randomElement", "shuffle", "count4", "laplacian", "distance", "neighbors", "nearest", "keepInBox", "wrapInBox", "layer", "showValues", "showStates", "colAt",
			"rowAt", "lon", "lat", "gradient", "slopeDegrees", "value", "load", "grid", "raster", "vector", "addLayer", "area", "distanceKm", "move", "moveTowards",
			"neighborsKm", "showOnGlobe", "cosLat", "cosDeg", "sinDeg", "attribution", "randomAt", "cameraFollow", "links", "neighborPairs", "drawLine", "drawLine3", "drawPath", "drawPolygon", "drawCircle", "drawRect", "drawText", "drawRectOverlay", "drawTextOverlay", "perspectiveLabel", "perspectiveIcon", "globeGrid", "globeOutline", "showBackground", "cellShape", "backgroundNames", "record", "stop", "isStopped", "addAgent", "removeAgent", "forward", "moveTo", "face", "agentDistance", "agentsAt", "countAgentsAt", "agentsNear", "index", "link", "unlink", "removeLinks", "clearLinks", "linked", "linkNeighbors", "degree", "linkColorOf", "setLinkColor", "getLinks", "linkCount", "linkRandom", "linkSmallWorld", "linkScaleFree", "linkNearby", "components", "shortestPath", "clustering", "layoutCircle", "layoutSpring", "drawImage", "drawPillar", "drawCone", "drawPoint3", "drawPath3", "drawBox", "drawSphere", "drawPolygon3", "placeModel", "removeModel", "clearModels","loadImage", "loadModel", "chartXY", "chartBars", "chartColors", "dot", "norm", "matMul", "matVec", "transpose", "solve", "inverse", "determinant", "linearRegression", "correlation", "quantile", "median", "normalPdf", "normalCdf", "noise", "fractalNoise", "headingOf", "randomBinomial", "randomWeighted");

	private final List<String> errors = new ArrayList<>();
	private final List<String> warnings = new ArrayList<>();
	private final List<ParamInfo> params = new ArrayList<>();
	private final List<ActionInfo> actions = new ArrayList<>();
	/* The input methods of the model: click(x, y), drag(x, y), key(key) */
	private final List<String> inputs = new ArrayList<>();
	private final Set<String> chartNames = new java.util.LinkedHashSet<>();
	private CompilationUnit unit;
	private String modelName;
	private String kind;
	private String description = "";
	private int tmp;

	/**
	 * Translates the source of the model class.
	 *
	 * @param source    the source of the Java file
	 * @param unitName  the name of the file (e.g., "Life.java")
	 * @param classpath the classpath (the API of Bio7) for the types
	 * @return the JavaScript code of the classes (null if there are errors, see getErrors())
	 */
	public String translate(String source, String unitName, String[] classpath) {
		ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(source.toCharArray());
		parser.setUnitName(unitName);
		parser.setEnvironment(classpath, null, null, true);
		parser.setResolveBindings(true);
		parser.setBindingsRecovery(true);
		Map<String, String> options = JavaCore.getOptions();
		JavaCore.setComplianceOptions(JavaCore.latestSupportedJavaVersion(), options);
		parser.setCompilerOptions(options);
		unit = (CompilationUnit) parser.createAST(null);
		/* The methods which are not exported (e.g., settings with a WindowBuilder panel): their problems are ignored */
		List<int[]> skipped = new ArrayList<>();
		for (Object o : unit.types()) {
			if (o instanceof TypeDeclaration td) {
				for (MethodDeclaration md : td.getMethods()) {
					if (isDisplayMethod(md)) {
						skipped.add(new int[] { md.getStartPosition(), md.getStartPosition() + md.getLength() });
					}
				}
			}
		}
		for (IProblem p : unit.getProblems()) {
			boolean inSkipped = false;
			for (int[] r : skipped) {
				inSkipped |= p.getSourceStart() >= r[0] && p.getSourceStart() < r[1];
			}
			if (p.isError() && !inSkipped) {
				errors.add("Line " + p.getSourceLineNumber() + ": " + p.getMessage());
			}
		}
		if (!errors.isEmpty()) {
			return null;
		}
		TypeDeclaration model = null;
		for (Object o : unit.types()) {
			if (o instanceof TypeDeclaration td && Modifier.isPublic(td.getModifiers())) {
				model = td;
			}
		}
		if (model == null) {
			errors.add("No public class found");
			return null;
		}
		ITypeBinding binding = model.resolveBinding();
		for (ITypeBinding s = binding == null ? null : binding.getSuperclass(); s != null && kind == null; s = s.getSuperclass()) {
			String n = s.getErasure().getQualifiedName();
			if (n.equals(API + "GeoGridModel")) {
				kind = "GeoGridModel";
			} else if (n.equals(API + "GeoAgentModel")) {
				kind = "GeoAgentModel";
			} else if (n.equals(API + "GridModel")) {
				kind = "GridModel";
			} else if (n.equals(API + "AgentModel")) {
				kind = "AgentModel";
			} else if (n.equals(API + "ValueGridModel")) {
				kind = "ValueGridModel";
			}
		}
		if (kind == null || !binding.getSuperclass().getErasure().getQualifiedName().startsWith(API)) {
			errors.add("The class " + model.getName() + " must extend GridModel, ValueGridModel, GeoGridModel, AgentModel or GeoAgentModel (com.eco.bio7.simulation)");
			return null;
		}
		modelName = model.getName().getIdentifier();
		if (model.getJavadoc() != null) {
			description = javadocText(model.getJavadoc().toString());
		}
		StringBuilder out = new StringBuilder();
		/* The nested classes first (a superclass before its subclasses), then the model */
		List<TypeDeclaration> nested = new ArrayList<>();
		for (Object o : model.bodyDeclarations()) {
			if (o instanceof TypeDeclaration td) {
				if (td.isInterface()) {
					error(td, "Interfaces are not supported");
				} else if (!Modifier.isStatic(td.getModifiers())) {
					error(td, "The nested class " + td.getName() + " must be static (static class " + td.getName() + ")");
				} else {
					nested.add(td);
				}
			} else if (o instanceof AbstractTypeDeclaration atd) {
				error(atd, "Enums and records are not supported");
			}
		}
		for (TypeDeclaration td : sortBySuperclass(nested)) {
			classCode(td, out, false);
		}
		classCode(model, out, true);
		for (Object o : unit.types()) {
			if (o != model) {
				error((ASTNode) o, "Only one class per file (use static nested classes)");
			}
		}
		return errors.isEmpty() ? out.toString() : null;
	}

	public List<String> getErrors() {
		return errors;
	}

	public List<String> getWarnings() {
		return warnings;
	}

	public List<ParamInfo> getParams() {
		return params;
	}

	/** The actions (sorted by the label, as the settings of Bio7). */
	public List<ActionInfo> getActions() {
		return actions;
	}

	/** The input methods of the model ("click", "drag", "key"). */
	public List<String> getInputs() {
		return inputs;
	}

	/** The names of the chart series (the constant names of chart(name, value)). */
	public Set<String> getChartNames() {
		return chartNames;
	}

	/** "GridModel", "ValueGridModel", "GeoGridModel", "AgentModel" or "GeoAgentModel". */
	public String getKind() {
		return kind;
	}

	public String getModelName() {
		return modelName;
	}

	/** The first paragraph of the comment of the class. */
	public String getDescription() {
		return description;
	}

	private static String javadocText(String doc) {
		/* The first paragraph (the next ones are often the instructions for Bio7) */
		int paragraph = doc.toLowerCase().indexOf("<p>");
		if (paragraph > 0) {
			doc = doc.substring(0, paragraph);
		}
		String text = doc.replaceAll("/\\*\\*|\\*/", "").replaceAll("(?m)^\\s*\\*", " ").replaceAll("<pre>[\\s\\S]*?</pre>", " ").replaceAll("\\{@\\w+\\s+([^}]*)\\}", "$1").replaceAll("(?m)^\\s*@.*$", "")
				.replaceAll("<[^>]+>", "").replaceAll("\\s+", " ").trim();
		return text;
	}

	private List<TypeDeclaration> sortBySuperclass(List<TypeDeclaration> types) {
		List<TypeDeclaration> sorted = new ArrayList<>();
		Set<String> done = new HashSet<>();
		while (sorted.size() < types.size()) {
			boolean progress = false;
			for (TypeDeclaration td : types) {
				String name = td.getName().getIdentifier();
				if (done.contains(name)) {
					continue;
				}
				String sup = td.getSuperclassType() == null ? null : td.getSuperclassType().resolveBinding().getErasure().getName();
				boolean local = false;
				for (TypeDeclaration other : types) {
					local |= other.getName().getIdentifier().equals(sup);
				}
				if (!local || done.contains(sup)) {
					sorted.add(td);
					done.add(name);
					progress = true;
				}
			}
			if (!progress) {
				break;
			}
		}
		return sorted;
	}

	/* ===================== Classes ===================== */

	private void classCode(TypeDeclaration td, StringBuilder out, boolean isModel) {
		String name = td.getName().getIdentifier();
		if (!td.typeParameters().isEmpty() && !isModel) {
			warnings.add("Line " + line(td) + ": the type parameters of " + name + " are ignored");
		}
		String sup = null;
		if (td.getSuperclassType() != null) {
			ITypeBinding sb = td.getSuperclassType().resolveBinding();
			String q = sb == null ? "" : sb.getErasure().getQualifiedName();
			if (q.startsWith(API)) {
				sup = sb.getErasure().getName();
			} else if (sb != null && sb.isFromSource()) {
				sup = sb.getErasure().getName();
			} else {
				error(td, "The superclass " + q + " is not available in JavaScript");
			}
		}
		if (!td.superInterfaceTypes().isEmpty()) {
			warnings.add("Line " + line(td) + ": the interfaces of " + name + " are ignored");
		}
		/* The fields and methods (no overloading, no field with the name of a method) */
		List<FieldDeclaration> instanceFields = new ArrayList<>();
		List<FieldDeclaration> staticFields = new ArrayList<>();
		List<MethodDeclaration> methods = new ArrayList<>();
		MethodDeclaration constructor = null;
		Set<String> methodNames = new HashSet<>();
		for (Object o : td.bodyDeclarations()) {
			BodyDeclaration bd = (BodyDeclaration) o;
			if (bd instanceof FieldDeclaration fd) {
				(Modifier.isStatic(fd.getModifiers()) ? staticFields : instanceFields).add(fd);
			} else if (bd instanceof MethodDeclaration md) {
				if (md.isConstructor()) {
					if (constructor != null) {
						error(md, "Only one constructor per class is supported");
					}
					constructor = md;
				} else if (md.getBody() == null) {
					continue;
				} else if (isModel && isDisplayMethod(md)) {
					if (md.getName().getIdentifier().equals("settings") || md.getName().getIdentifier().equals("load")) {
						/* The panel of the settings in Bio7 (the page creates the controls of the annotations) */
						continue;
					}
					warnings.add("Line " + line(md) + ": the method " + md.getName() + " (Bio7 display) is not exported");
				} else {
					if (!methodNames.add(md.getName().getIdentifier())) {
						error(md, "The method " + md.getName() + " is overloaded (JavaScript has no overloading, use another name)");
					}
					methods.add(md);
				}
			} else if (bd instanceof Initializer) {
				error(bd, "Initializer blocks are not supported");
			}
		}
		for (FieldDeclaration fd : instanceFields) {
			for (Object f : fd.fragments()) {
				String fn = ((VariableDeclarationFragment) f).getName().getIdentifier();
				if (methodNames.contains(fn) || (isModel && API_METHODS.contains(fn))) {
					error(fd, "The field " + fn + " has the name of a method (not possible in JavaScript)");
				}
			}
		}
		if (isModel) {
			collectParams(instanceFields);
			collectActions(methods);
			for (MethodDeclaration md : methods) {
				String n = md.getName().getIdentifier();
				int count = n.equals("key") ? 1 : 2;
				if ((n.equals("click") || n.equals("drag") || n.equals("key")) && md.parameters().size() == count && !Modifier.isStatic(md.getModifiers()) && !inputs.contains(n)) {
					inputs.add(n);
				}
			}
		}
		out.append("class ").append(name);
		if (sup != null) {
			out.append(" extends ").append(sup);
		}
		out.append(" {\n");
		/* The constructor: super(...), the fields, the body */
		List<String> paramNames = new ArrayList<>();
		Block body = constructor == null ? null : constructor.getBody();
		if (constructor != null) {
			for (Object p : constructor.parameters()) {
				paramNames.add(parameter((SingleVariableDeclaration) p));
			}
		}
		out.append("  constructor(").append(String.join(", ", paramNames)).append(") {\n");
		List<?> statements = body == null ? List.of() : body.statements();
		int first = 0;
		if (!statements.isEmpty() && statements.get(0) instanceof SuperConstructorInvocation sci) {
			out.append("    super(").append(arguments(sci.arguments())).append(");\n");
			first = 1;
		} else if (!statements.isEmpty() && statements.get(0) instanceof ConstructorInvocation ci) {
			error(ci, "this(...) is not supported");
		} else if (sup != null) {
			out.append("    super();\n");
		}
		for (FieldDeclaration fd : instanceFields) {
			for (Object f : fd.fragments()) {
				VariableDeclarationFragment vdf = (VariableDeclarationFragment) f;
				out.append("    this.").append(jsName(vdf.getName().getIdentifier())).append(" = ").append(initialValue(vdf, fd.getType())).append(";\n");
			}
		}
		for (int i = first; i < statements.size(); i++) {
			statement((Statement) statements.get(i), out, "    ");
		}
		out.append("  }\n");
		for (MethodDeclaration md : methods) {
			out.append("  ");
			if (Modifier.isStatic(md.getModifiers())) {
				out.append("static ");
			}
			List<String> ps = new ArrayList<>();
			for (Object p : md.parameters()) {
				ps.add(parameter((SingleVariableDeclaration) p));
			}
			out.append(jsName(md.getName().getIdentifier())).append("(").append(String.join(", ", ps)).append(") {\n");
			for (Object s : md.getBody().statements()) {
				statement((Statement) s, out, "    ");
			}
			out.append("  }\n");
		}
		out.append("}\n");
		for (FieldDeclaration fd : staticFields) {
			for (Object f : fd.fragments()) {
				VariableDeclarationFragment vdf = (VariableDeclarationFragment) f;
				out.append(name).append(".").append(jsName(vdf.getName().getIdentifier())).append(" = ").append(initialValue(vdf, fd.getType())).append(";\n");
			}
		}
		out.append("\n");
	}

	private boolean isDisplayMethod(MethodDeclaration md) {
		String n = md.getName().getIdentifier();
		if ((n.equals("run") || n.equals("setup")) && !md.parameters().isEmpty()) {
			return true;
		}
		return n.equals("display") || n.equals("prepareDisplay") || n.equals("close") || n.equals("settings") || (n.equals("load") && md.parameters().isEmpty());
	}

	private String parameter(SingleVariableDeclaration p) {
		return (p.isVarargs() ? "..." : "") + jsName(p.getName().getIdentifier());
	}

	private String initialValue(VariableDeclaration vd, Type type) {
		if (vd.getInitializer() != null) {
			return expression(vd.getInitializer());
		}
		if (vd.getExtraDimensions() > 0) {
			return "null";
		}
		return defaultValue(type.resolveBinding());
	}

	private static String defaultValue(ITypeBinding b) {
		if (b != null && b.isPrimitive()) {
			return b.getName().equals("boolean") ? "false" : "0";
		}
		return "null";
	}

	/* ===================== The settings (Param) ===================== */

	private void collectParams(List<FieldDeclaration> fields) {
		for (FieldDeclaration fd : fields) {
			Annotation param = null;
			for (Object m : fd.modifiers()) {
				if (m instanceof Annotation a && (a.getTypeName().getFullyQualifiedName().equals("Param") || a.getTypeName().getFullyQualifiedName().equals(API + "Param"))) {
					param = a;
				}
			}
			if (param == null) {
				continue;
			}
			ITypeBinding tb = fd.getType().resolveBinding();
			String type = tb == null ? "" : tb.getName();
			for (Object f : fd.fragments()) {
				VariableDeclarationFragment vdf = (VariableDeclarationFragment) f;
				ParamInfo p = new ParamInfo();
				p.name = vdf.getName().getIdentifier();
				p.jsName = jsName(p.name);
				p.label = p.name;
				switch (type) {
				case "int", "long", "short", "byte" -> p.kind = "int";
				case "double", "float" -> p.kind = "double";
				case "boolean" -> p.kind = "boolean";
				case "String" -> p.kind = "string";
				default -> {
					error(fd, "A Param must be a number, a boolean or a String");
					continue;
				}
				}
				Object v = vdf.getInitializer() == null ? null : constant(vdf.getInitializer());
				if (vdf.getInitializer() != null && v == null) {
					error(fd, "The value of the Param " + p.name + " must be a constant");
				}
				p.value = v != null ? v : p.kind.equals("boolean") ? Boolean.FALSE : p.kind.equals("string") ? "" : (Object) 0;
				if (param instanceof NormalAnnotation na) {
					for (Object o : na.values()) {
						MemberValuePair mvp = (MemberValuePair) o;
						String key = mvp.getName().getIdentifier();
						Object c = constant(mvp.getValue());
						switch (key) {
						case "label" -> p.label = String.valueOf(c);
						case "min" -> p.min = number(c);
						case "max" -> p.max = number(c);
						case "step" -> p.step = number(c);
						case "restart" -> p.restart = Boolean.TRUE.equals(c);
						case "group" -> p.group = String.valueOf(c);
						case "options" -> {
							List<String> opts = new ArrayList<>();
							if (mvp.getValue() instanceof ArrayInitializer ai) {
								for (Object e : ai.expressions()) {
									opts.add(String.valueOf(constant((Expression) e)));
								}
							} else {
								opts.add(String.valueOf(c));
							}
							p.options = opts.toArray(new String[0]);
						}
						default -> {
						}
						}
					}
				} else if (param instanceof SingleMemberAnnotation sma) {
					p.label = String.valueOf(constant(sma.getValue()));
				}
				params.add(p);
			}
		}
	}

	private void collectActions(List<MethodDeclaration> methods) {
		for (MethodDeclaration md : methods) {
			Annotation action = null;
			for (Object m : md.modifiers()) {
				if (m instanceof Annotation a && (a.getTypeName().getFullyQualifiedName().equals("Action") || a.getTypeName().getFullyQualifiedName().equals(API + "Action"))) {
					action = a;
				}
			}
			if (action == null) {
				continue;
			}
			if (!md.parameters().isEmpty() || Modifier.isStatic(md.getModifiers())) {
				error(md, "An Action must be a method without parameters (not static)");
				continue;
			}
			ActionInfo info = new ActionInfo();
			info.name = md.getName().getIdentifier();
			info.jsName = jsName(info.name);
			info.label = info.name;
			if (action instanceof NormalAnnotation na) {
				for (Object o : na.values()) {
					MemberValuePair mvp = (MemberValuePair) o;
					Object c = constant(mvp.getValue());
					if (mvp.getName().getIdentifier().equals("label")) {
						info.label = String.valueOf(c);
					} else if (mvp.getName().getIdentifier().equals("group")) {
						info.group = String.valueOf(c);
					}
				}
			} else if (action instanceof SingleMemberAnnotation sma) {
				info.label = String.valueOf(constant(sma.getValue()));
			}
			actions.add(info);
		}
		actions.sort(java.util.Comparator.comparing(a -> a.label));
	}

	private static double number(Object c) {
		return c instanceof Number n ? n.doubleValue() : Double.NaN;
	}

	private Object constant(Expression e) {
		Object c = e.resolveConstantExpressionValue();
		if (c == null && e instanceof PrefixExpression pe && pe.getOperator() == PrefixExpression.Operator.MINUS) {
			Object inner = constant(pe.getOperand());
			if (inner instanceof Number n) {
				return -n.doubleValue();
			}
		}
		if (c == null && e instanceof BooleanLiteral bl) {
			return bl.booleanValue();
		}
		return c;
	}

	/* ===================== Statements ===================== */

	private void statement(Statement s, StringBuilder out, String in) {
		if (s instanceof Block b) {
			out.append(in).append("{\n");
			for (Object o : b.statements()) {
				statement((Statement) o, out, in + "  ");
			}
			out.append(in).append("}\n");
		} else if (s instanceof ExpressionStatement es) {
			out.append(in).append(expression(es.getExpression())).append(";\n");
		} else if (s instanceof VariableDeclarationStatement vds) {
			out.append(in).append("let ").append(declarations(vds.fragments(), vds.getType())).append(";\n");
		} else if (s instanceof IfStatement is) {
			out.append(in).append("if (").append(expression(is.getExpression())).append(") ");
			nested(is.getThenStatement(), out, in);
			if (is.getElseStatement() != null) {
				out.append(in).append("else ");
				nested(is.getElseStatement(), out, in);
			}
		} else if (s instanceof ForStatement fs) {
			List<String> init = new ArrayList<>();
			for (Object o : fs.initializers()) {
				if (o instanceof VariableDeclarationExpression vde) {
					init.add("let " + declarations(vde.fragments(), vde.getType()));
				} else {
					init.add(expression((Expression) o));
				}
			}
			List<String> upd = new ArrayList<>();
			for (Object o : fs.updaters()) {
				upd.add(expression((Expression) o));
			}
			out.append(in).append("for (").append(String.join(", ", init)).append("; ").append(fs.getExpression() == null ? "" : expression(fs.getExpression())).append("; ").append(String.join(", ", upd))
					.append(") ");
			nested(fs.getBody(), out, in);
		} else if (s instanceof EnhancedForStatement efs) {
			out.append(in).append("for (let ").append(jsName(efs.getParameter().getName().getIdentifier())).append(" of ").append(expression(efs.getExpression())).append(") ");
			nested(efs.getBody(), out, in);
		} else if (s instanceof WhileStatement ws) {
			out.append(in).append("while (").append(expression(ws.getExpression())).append(") ");
			nested(ws.getBody(), out, in);
		} else if (s instanceof DoStatement ds) {
			out.append(in).append("do ");
			nested(ds.getBody(), out, in);
			out.append(in).append("while (").append(expression(ds.getExpression())).append(");\n");
		} else if (s instanceof ReturnStatement rs) {
			out.append(in).append("return").append(rs.getExpression() == null ? "" : " " + expression(rs.getExpression())).append(";\n");
		} else if (s instanceof BreakStatement bs) {
			out.append(in).append("break").append(bs.getLabel() == null ? "" : " " + bs.getLabel().getIdentifier()).append(";\n");
		} else if (s instanceof ContinueStatement cs) {
			out.append(in).append("continue").append(cs.getLabel() == null ? "" : " " + cs.getLabel().getIdentifier()).append(";\n");
		} else if (s instanceof LabeledStatement ls) {
			out.append(in).append(ls.getLabel().getIdentifier()).append(":\n");
			statement(ls.getBody(), out, in);
		} else if (s instanceof SwitchStatement ss) {
			switchStatement(ss, out, in);
		} else if (s instanceof ThrowStatement ts) {
			out.append(in).append("throw ").append(throwable(ts.getExpression())).append(";\n");
		} else if (s instanceof YieldStatement ys && ys.isImplicit()) {
			/* The expression of a rule of a switch statement (case X -> expression;) */
			out.append(in).append(expression(ys.getExpression())).append(";\n");
		} else if (s instanceof SynchronizedStatement sy) {
			statement(sy.getBody(), out, in);
		} else if (s instanceof EmptyStatement) {
			out.append(in).append(";\n");
		} else if (s instanceof SuperConstructorInvocation || s instanceof ConstructorInvocation) {
			error(s, "super(...) and this(...) must be the first statement of the constructor");
		} else {
			error(s, "This statement is not supported in the HTML export (" + s.getClass().getSimpleName().replace("Statement", "") + ")");
		}
	}

	private void nested(Statement s, StringBuilder out, String in) {
		if (s instanceof Block b) {
			out.append("{\n");
			for (Object o : b.statements()) {
				statement((Statement) o, out, in + "  ");
			}
			out.append(in).append("}\n");
		} else {
			out.append("{\n");
			statement(s, out, in + "  ");
			out.append(in).append("}\n");
		}
	}

	private String declarations(List<?> fragments, Type type) {
		List<String> parts = new ArrayList<>();
		for (Object o : fragments) {
			VariableDeclarationFragment vdf = (VariableDeclarationFragment) o;
			String n = jsName(vdf.getName().getIdentifier());
			parts.add(vdf.getInitializer() == null ? n : n + " = " + expression(vdf.getInitializer()));
		}
		return String.join(", ", parts);
	}

	private void switchStatement(SwitchStatement ss, StringBuilder out, String in) {
		out.append(in).append("switch (").append(expression(ss.getExpression())).append(") {\n");
		boolean rules = false;
		for (Object o : ss.statements()) {
			if (o instanceof SwitchCase sc) {
				rules |= sc.isSwitchLabeledRule();
			}
		}
		for (Object o : ss.statements()) {
			if (o instanceof SwitchCase sc) {
				if (sc.isDefault()) {
					out.append(in).append("  default:\n");
				} else {
					for (Object e : sc.expressions()) {
						out.append(in).append("  case ").append(expression((Expression) e)).append(":\n");
					}
				}
			} else {
				Statement st = (Statement) o;
				if (rules) {
					/* case X -> statement: no fall through */
					out.append(in).append("    {\n");
					if (st instanceof Block b) {
						for (Object x : b.statements()) {
							statement((Statement) x, out, in + "      ");
						}
					} else {
						statement(st, out, in + "      ");
					}
					out.append(in).append("    }\n").append(in).append("    break;\n");
				} else {
					statement(st, out, in + "    ");
				}
			}
		}
		out.append(in).append("}\n");
	}

	private String throwable(Expression e) {
		if (e instanceof ClassInstanceCreation cic) {
			ITypeBinding b = cic.resolveTypeBinding();
			if (b != null && !b.isFromSource()) {
				return "new Error(" + (cic.arguments().isEmpty() ? "'" + b.getName() + "'" : "String(" + expression((Expression) cic.arguments().get(0)) + ")") + ")";
			}
		}
		return expression(e);
	}

	/* ===================== Expressions ===================== */

	private String arguments(List<?> args) {
		List<String> parts = new ArrayList<>();
		for (Object o : args) {
			parts.add(expression((Expression) o));
		}
		return String.join(", ", parts);
	}

	/* The arguments of a call: an array for the varargs parameter is spread (palette(colors) -> palette(...colors)) */
	private String callArguments(IMethodBinding mb, List<?> args) {
		if (mb != null && mb.isVarargs() && args.size() == mb.getParameterTypes().length) {
			Expression last = (Expression) args.get(args.size() - 1);
			ITypeBinding lt = last.resolveTypeBinding();
			if (lt != null && lt.isArray() && lt.getDimensions() == mb.getParameterTypes()[args.size() - 1].getDimensions()) {
				List<String> parts = new ArrayList<>();
				for (int i = 0; i < args.size() - 1; i++) {
					parts.add(expression((Expression) args.get(i)));
				}
				parts.add("..." + expression(last));
				return String.join(", ", parts);
			}
		}
		return arguments(args);
	}

	private static boolean isIntegral(ITypeBinding b) {
		if (b == null) {
			return false;
		}
		String n = b.getQualifiedName();
		return switch (n) {
		case "int", "long", "short", "byte", "char", "java.lang.Integer", "java.lang.Long", "java.lang.Short", "java.lang.Byte", "java.lang.Character" -> true;
		default -> false;
		};
	}

	private static boolean isLong(ITypeBinding b) {
		return b != null && (b.getName().equals("long") || b.getQualifiedName().equals("java.lang.Long"));
	}

	private static boolean isDouble(ITypeBinding b) {
		return b != null && (b.getName().equals("double") || b.getName().equals("float") || b.getQualifiedName().equals("java.lang.Double") || b.getQualifiedName().equals("java.lang.Float"));
	}

	private static boolean isString(ITypeBinding b) {
		return b != null && b.getQualifiedName().equals("java.lang.String");
	}

	private static boolean isChar(ITypeBinding b) {
		return b != null && (b.getName().equals("char") || b.getQualifiedName().equals("java.lang.Character"));
	}

	private String expression(Expression e) {
		if (e == null) {
			return "";
		}
		if (e instanceof NumberLiteral nl) {
			return number(nl);
		} else if (e instanceof StringLiteral sl) {
			return quote(sl.getLiteralValue());
		} else if (e instanceof TextBlock tb) {
			return quote(tb.getLiteralValue());
		} else if (e instanceof CharacterLiteral cl) {
			return String.valueOf((int) cl.charValue());
		} else if (e instanceof BooleanLiteral bl) {
			return String.valueOf(bl.booleanValue());
		} else if (e instanceof NullLiteral) {
			return "null";
		} else if (e instanceof ThisExpression te) {
			if (te.getQualifier() != null) {
				error(e, "Qualified this is not supported");
			}
			return "this";
		} else if (e instanceof ParenthesizedExpression pe) {
			return "(" + expression(pe.getExpression()) + ")";
		} else if (e instanceof SimpleName sn) {
			return simpleName(sn);
		} else if (e instanceof QualifiedName qn) {
			return qualifiedName(qn);
		} else if (e instanceof FieldAccess fa) {
			return expression(fa.getExpression()) + "." + fieldName(fa.resolveFieldBinding(), fa.getName().getIdentifier());
		} else if (e instanceof InfixExpression ie) {
			return infix(ie);
		} else if (e instanceof PrefixExpression pe) {
			return pe.getOperator().toString() + expression(pe.getOperand());
		} else if (e instanceof PostfixExpression pe) {
			return expression(pe.getOperand()) + pe.getOperator().toString();
		} else if (e instanceof Assignment a) {
			return assignment(a);
		} else if (e instanceof ConditionalExpression ce) {
			return "(" + expression(ce.getExpression()) + " ? " + expression(ce.getThenExpression()) + " : " + expression(ce.getElseExpression()) + ")";
		} else if (e instanceof CastExpression ce) {
			return cast(ce);
		} else if (e instanceof ArrayAccess aa) {
			return expression(aa.getArray()) + "[" + expression(aa.getIndex()) + "]";
		} else if (e instanceof ArrayCreation ac) {
			return arrayCreation(ac);
		} else if (e instanceof ArrayInitializer ai) {
			return "[" + arguments(ai.expressions()) + "]";
		} else if (e instanceof MethodInvocation mi) {
			return methodInvocation(mi);
		} else if (e instanceof SuperMethodInvocation smi) {
			return "super." + jsName(smi.getName().getIdentifier()) + "(" + arguments(smi.arguments()) + ")";
		} else if (e instanceof ClassInstanceCreation cic) {
			return creation(cic);
		} else if (e instanceof InstanceofExpression io) {
			ITypeBinding t = io.getRightOperand().resolveBinding();
			if (isString(t)) {
				return "(typeof " + expression(io.getLeftOperand()) + " === 'string')";
			}
			if (t != null && !t.isFromSource() && !t.getQualifiedName().startsWith(API)) {
				error(e, "instanceof " + t.getName() + " is not supported");
			}
			return "(" + expression(io.getLeftOperand()) + " instanceof " + (t == null ? "Object" : t.getErasure().getName()) + ")";
		} else if (e instanceof LambdaExpression le) {
			return lambda(le);
		} else if (e instanceof VariableDeclarationExpression vde) {
			return "let " + declarations(vde.fragments(), vde.getType());
		}
		error(e, "This expression is not supported in the HTML export (" + e.getClass().getSimpleName().replace("Expression", "") + ")");
		return "undefined";
	}

	private String number(NumberLiteral nl) {
		String t = nl.getToken().replace("_", "");
		String lower = t.toLowerCase();
		boolean hex = lower.startsWith("0x"), bin = lower.startsWith("0b");
		if (lower.endsWith("l")) {
			t = t.substring(0, t.length() - 1);
		} else if (!hex && (lower.endsWith("f") || lower.endsWith("d"))) {
			t = t.substring(0, t.length() - 1);
		}
		if (!hex && !bin && t.length() > 1 && t.startsWith("0") && t.chars().allMatch(Character::isDigit)) {
			/* An octal number of Java */
			return String.valueOf(Long.parseLong(t, 8));
		}
		if (t.endsWith(".")) {
			t = t + "0";
		}
		if (t.startsWith(".")) {
			t = "0" + t;
		}
		return t;
	}

	static String quote(String s) {
		StringBuilder b = new StringBuilder("\"");
		for (char c : s.toCharArray()) {
			switch (c) {
			case '"' -> b.append("\\\"");
			case '\\' -> b.append("\\\\");
			case '\n' -> b.append("\\n");
			case '\r' -> b.append("\\r");
			case '\t' -> b.append("\\t");
			case '<' -> b.append("\\u003c");
			default -> {
				if (c < 32 || c > 126) {
					b.append(String.format("\\u%04x", (int) c));
				} else {
					b.append(c);
				}
			}
			}
		}
		return b.append('"').toString();
	}

	private String simpleName(SimpleName sn) {
		IBinding b = sn.resolveBinding();
		if (b instanceof IVariableBinding vb && vb.isField()) {
			ITypeBinding decl = vb.getDeclaringClass();
			if (Modifier.isStatic(vb.getModifiers())) {
				return (decl == null ? modelName : decl.getErasure().getName()) + "." + jsName(sn.getIdentifier());
			}
			return "this." + fieldName(vb, sn.getIdentifier());
		}
		if (b instanceof ITypeBinding tb) {
			return tb.getErasure().getName();
		}
		return jsName(sn.getIdentifier());
	}

	private String qualifiedName(QualifiedName qn) {
		IBinding qb = qn.getQualifier().resolveBinding();
		String n = qn.getName().getIdentifier();
		if (qb instanceof ITypeBinding tb) {
			String q = tb.getQualifiedName();
			switch (q + "." + n) {
			case "java.lang.Math.PI":
				return "Math.PI";
			case "java.lang.Math.E":
				return "Math.E";
			case "java.lang.Integer.MAX_VALUE":
				return "2147483647";
			case "java.lang.Integer.MIN_VALUE":
				return "-2147483648";
			case "java.lang.Double.MAX_VALUE":
				return "Number.MAX_VALUE";
			case "java.lang.Double.MIN_VALUE":
				return "Number.MIN_VALUE";
			case "java.lang.Double.POSITIVE_INFINITY":
				return "Infinity";
			case "java.lang.Double.NEGATIVE_INFINITY":
				return "-Infinity";
			case "java.lang.Double.NaN":
				return "NaN";
			case "java.lang.Long.MAX_VALUE":
				warnings.add("Line " + line(qn) + ": Long.MAX_VALUE is a double in JavaScript (Number.MAX_SAFE_INTEGER is used)");
				return "Number.MAX_SAFE_INTEGER";
			default:
				break;
			}
			if (tb.isFromSource() || q.startsWith(API)) {
				return tb.getErasure().getName() + "." + jsName(n);
			}
			error(qn, "The field " + q + "." + n + " is not available in JavaScript");
			return "undefined";
		}
		ITypeBinding qt = qn.getQualifier().resolveTypeBinding();
		if (qt != null && qt.isArray() && n.equals("length")) {
			return expression(qn.getQualifier()) + ".length";
		}
		return expression(qn.getQualifier()) + "." + fieldName(qn.resolveBinding() instanceof IVariableBinding v ? v : null, n);
	}

	private String infix(InfixExpression ie) {
		InfixExpression.Operator op = ie.getOperator();
		List<Expression> operands = new ArrayList<>();
		operands.add(ie.getLeftOperand());
		operands.add(ie.getRightOperand());
		for (Object o : ie.extendedOperands()) {
			operands.add((Expression) o);
		}
		boolean plus = op == InfixExpression.Operator.PLUS;
		String result = expression(operands.get(0));
		ITypeBinding leftType = operands.get(0).resolveTypeBinding();
		/* true when the left part is a String (Java evaluates from left to right: 1 + 2 + "x" is "3x") */
		boolean text = isString(leftType);
		/* The kind of the left part as text: 'c' a char, 'd' a double, else a number or an object */
		char leftKind = isChar(leftType) ? 'c' : isDouble(leftType) ? 'd' : 'o';
		for (int i = 1; i < operands.size(); i++) {
			Expression right = operands.get(i);
			ITypeBinding rightType = right.resolveTypeBinding();
			String r = expression(right);
			if (plus && (text || isString(rightType))) {
				if (!text) {
					result = leftKind == 'c' ? "String.fromCharCode(" + result + ")" : leftKind == 'd' ? "Bio7Sim.str(" + result + ")" : result;
					text = true;
				}
				result = result + " + " + textValue(r, rightType);
				leftType = ie.resolveTypeBinding();
				continue;
			}
			boolean ints = isIntegral(leftType) && isIntegral(rightType);
			boolean longs = ints && (isLong(leftType) || isLong(rightType));
			if (op == InfixExpression.Operator.DIVIDE && ints) {
				result = "Math.trunc(" + result + " / " + r + ")";
			} else if ((op == InfixExpression.Operator.PLUS || op == InfixExpression.Operator.MINUS) && ints && !longs) {
				/* int arithmetic wraps around at 32 bits in Java */
				result = "((" + result + " " + op + " " + r + ") | 0)";
			} else if (op == InfixExpression.Operator.LEFT_SHIFT && isLong(leftType)) {
				result = "(" + result + " * Math.pow(2, " + r + "))";
			} else if (op == InfixExpression.Operator.RIGHT_SHIFT_SIGNED && isLong(leftType)) {
				result = "Math.floor(" + result + " / Math.pow(2, " + r + "))";
			} else if (op == InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED && isLong(leftType)) {
				error(ie, "The operator >>> is not supported for long values");
			} else if (op == InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED && ints) {
				result = "((" + result + " >>> " + r + ") | 0)";
			} else if (op == InfixExpression.Operator.TIMES && ints && !longs) {
				result = "Math.imul(" + result + ", " + r + ")";
			} else if (op == InfixExpression.Operator.EQUALS) {
				result = result + " === " + r;
			} else if (op == InfixExpression.Operator.NOT_EQUALS) {
				result = result + " !== " + r;
			} else {
				result = result + " " + op + " " + r;
			}
			/* The type of the left part for the next operand */
			leftKind = isDouble(leftType) || isDouble(rightType) || leftKind == 'd' ? 'd' : 'o';
			if (!ints) {
				leftType = ie.resolveTypeBinding();
			} else if (longs) {
				leftType = isLong(leftType) ? leftType : rightType;
			} else if (isChar(leftType)) {
				leftType = rightType != null && !isChar(rightType) ? rightType : leftType;
			}
		}
		return result;
	}

	/* A value in a String concatenation like Java (a char as a character, a double like 2.0) */
	private static String textValue(String js, ITypeBinding type) {
		if (isChar(type)) {
			return "String.fromCharCode(" + js + ")";
		}
		if (isDouble(type)) {
			return "Bio7Sim.str(" + js + ")";
		}
		return js;
	}

	private String assignment(Assignment a) {
		String lhs = expression(a.getLeftHandSide());
		String rhs = expression(a.getRightHandSide());
		Assignment.Operator op = a.getOperator();
		ITypeBinding lt = a.getLeftHandSide().resolveTypeBinding();
		ITypeBinding rt = a.getRightHandSide().resolveTypeBinding();
		if (op == Assignment.Operator.ASSIGN) {
			if (isChar(rt) && isString(lt)) {
				return lhs + " = String.fromCharCode(" + rhs + ")";
			}
			return lhs + " = " + rhs;
		}
		String bin = op.toString().substring(0, op.toString().length() - 1);
		if (isIntegral(lt)) {
			/* The compound assignment of Java converts to the type of the left side (e.g., i += 0.5) */
			if (op == Assignment.Operator.DIVIDE_ASSIGN || !isIntegral(rt)) {
				return lhs + " = Math.trunc(" + lhs + " " + bin + " (" + rhs + "))";
			}
			if (op == Assignment.Operator.RIGHT_SHIFT_UNSIGNED_ASSIGN) {
				return lhs + " = (" + lhs + " >>> (" + rhs + ")) | 0";
			}
			if (op == Assignment.Operator.TIMES_ASSIGN && !isLong(lt) && !isLong(rt)) {
				return lhs + " = Math.imul(" + lhs + ", " + rhs + ")";
			}
			if ((op == Assignment.Operator.PLUS_ASSIGN || op == Assignment.Operator.MINUS_ASSIGN) && !isLong(lt) && !isLong(rt)) {
				return lhs + " = (" + lhs + " " + bin + " (" + rhs + ")) | 0";
			}
			if (op == Assignment.Operator.LEFT_SHIFT_ASSIGN && isLong(lt)) {
				return lhs + " = " + lhs + " * Math.pow(2, " + rhs + ")";
			}
			if (op == Assignment.Operator.RIGHT_SHIFT_SIGNED_ASSIGN && isLong(lt)) {
				return lhs + " = Math.floor(" + lhs + " / Math.pow(2, " + rhs + "))";
			}
		}
		if (isString(lt) && isChar(rt)) {
			return lhs + " " + op + " String.fromCharCode(" + rhs + ")";
		}
		if (isString(lt) && isDouble(rt)) {
			return lhs + " " + op + " Bio7Sim.str(" + rhs + ")";
		}
		return lhs + " " + op + " " + rhs;
	}

	private String cast(CastExpression ce) {
		ITypeBinding target = ce.getType().resolveBinding();
		ITypeBinding source = ce.getExpression().resolveTypeBinding();
		String inner = expression(ce.getExpression());
		if (target != null && target.isPrimitive() && isIntegral(target) && !isIntegral(source)) {
			return "Math.trunc(" + inner + ")";
		}
		if (target != null && target.isPrimitive() && (target.getName().equals("int") || target.getName().equals("short") || target.getName().equals("byte")) && source != null && source.getName().equals("long")) {
			return "(" + inner + " | 0)";
		}
		return "(" + inner + ")";
	}

	private String arrayCreation(ArrayCreation ac) {
		if (ac.getInitializer() != null) {
			return expression(ac.getInitializer());
		}
		ITypeBinding element = ac.getType().getElementType().resolveBinding();
		int dims = ac.getType().getDimensions();
		List<String> sizes = new ArrayList<>();
		for (Object o : ac.dimensions()) {
			sizes.add(expression((Expression) o));
		}
		String leaf = sizes.size() == dims ? arrayKind(element) : "null";
		if (sizes.size() == 1) {
			return newArray(leaf, sizes.get(0));
		}
		return "Bio7Sim.array([" + String.join(", ", sizes) + "], " + quote(leaf) + ")";
	}

	private static String arrayKind(ITypeBinding element) {
		String n = element == null ? "" : element.getName();
		return switch (n) {
		case "int" -> "Int32Array";
		case "short" -> "Int16Array";
		case "byte" -> "Int8Array";
		case "char" -> "Uint16Array";
		case "double", "long" -> "Float64Array";
		case "float" -> "Float32Array";
		case "boolean" -> "false";
		default -> "null";
		};
	}

	private static String newArray(String kind, String size) {
		if (kind.endsWith("Array")) {
			return "new " + kind + "(" + size + ")";
		}
		return "new Array(" + size + ").fill(" + kind + ")";
	}

	private String lambda(LambdaExpression le) {
		List<String> ps = new ArrayList<>();
		for (Object o : le.parameters()) {
			ps.add(jsName(((VariableDeclaration) o).getName().getIdentifier()));
		}
		String head = "(" + String.join(", ", ps) + ") => ";
		if (le.getBody() instanceof Block b) {
			StringBuilder out = new StringBuilder("{\n");
			for (Object o : b.statements()) {
				statement((Statement) o, out, "      ");
			}
			return head + out + "    }";
		}
		return head + "(" + expression((Expression) le.getBody()) + ")";
	}

	private String creation(ClassInstanceCreation cic) {
		if (cic.getAnonymousClassDeclaration() != null) {
			error(cic, "Anonymous classes are not supported (use a lambda or a static nested class)");
			return "undefined";
		}
		ITypeBinding b = cic.resolveTypeBinding();
		String q = b == null ? "" : b.getErasure().getQualifiedName();
		List<?> args = cic.arguments();
		switch (q) {
		case "java.util.ArrayList", "java.util.LinkedList", "java.util.ArrayDeque", "java.util.Vector":
			if (!args.isEmpty() && !isIntegral(((Expression) args.get(0)).resolveTypeBinding())) {
				return "[..." + expression((Expression) args.get(0)) + "]";
			}
			return "[]";
		case "java.util.HashMap", "java.util.LinkedHashMap", "java.util.TreeMap":
			return "new Map()";
		case "java.util.HashSet", "java.util.LinkedHashSet":
			return "new Set()";
		case "java.lang.StringBuilder":
			error(cic, "StringBuilder is not supported (use String concatenation)");
			return "undefined";
		case "java.util.Random":
			error(cic, "java.util.Random is not supported: use random() and randomInt(n) of the model (the same numbers in Java and JavaScript)");
			return "undefined";
		default:
			break;
		}
		if (b != null && (b.isFromSource() || q.startsWith(API))) {
			return "new " + b.getErasure().getName() + "(" + callArguments(cic.resolveConstructorBinding(), args) + ")";
		}
		error(cic, "The class " + q + " is not available in JavaScript");
		return "undefined";
	}

	private String methodInvocation(MethodInvocation mi) {
		IMethodBinding mb = mi.resolveMethodBinding();
		String name = mi.getName().getIdentifier();
		List<?> args = mi.arguments();
		String a = callArguments(mb, args);
		Expression recv = mi.getExpression();
		/* The series of the first chart (chart(name, value); chart(chart, name, value) is another chart) */
		if (name.equals("chart") && args.size() == 2) {
			Object c = constant((Expression) args.get(0));
			if (c != null) {
				chartNames.add(String.valueOf(c));
			}
		}
		if (recv == null) {
			if (mb != null && Modifier.isStatic(mb.getModifiers())) {
				return mb.getDeclaringClass().getErasure().getName() + "." + jsName(name) + "(" + a + ")";
			}
			return "this." + jsName(name) + "(" + a + ")";
		}
		/* A static method of a class (e.g., Math.sqrt) */
		if (recv instanceof Name n && n.resolveBinding() instanceof ITypeBinding tb) {
			return staticCall(mi, tb, name, args, a);
		}
		ITypeBinding rt = recv.resolveTypeBinding();
		/* System.out.println(...) and System.err.println(...): the console of the browser */
		if (rt != null && rt.getErasure().getQualifiedName().equals("java.io.PrintStream") && (name.equals("println") || name.equals("print"))) {
			return "console.log(" + (args.isEmpty() ? "''" : textOf((Expression) args.get(0))) + ")";
		}
		String r = expression(recv);
		if (rt != null && rt.isArray()) {
			if (name.equals("clone")) {
				return r + ".slice()";
			}
			error(mi, "The array method " + name + " is not supported");
			return "undefined";
		}
		String q = rt == null ? "" : rt.getErasure().getQualifiedName();
		/* The method of a functional interface (e.g., a lambda in a variable): a call of the function */
		IMethodBinding functional = rt == null ? null : rt.getFunctionalInterfaceMethod();
		if (functional != null && mb != null && functional.getName().equals(name) && !mb.isDefaultConstructor() && !Modifier.isStatic(mb.getModifiers())
				&& (mb.getModifiers() & Modifier.DEFAULT) == 0) {
			return r + "(" + a + ")";
		}
		if (q.equals("java.io.PrintStream") && (name.equals("println") || name.equals("print"))) {
			return "console.log(" + (args.isEmpty() ? "''" : textOf((Expression) args.get(0))) + ")";
		}
		if (isString(rt)) {
			return stringCall(mi, r, name, args, a);
		}
		if (q.equals("java.util.stream.IntStream")) {
			return switch (name) {
			case "parallel", "sequential" -> r + "." + name + "()";
			case "forEach", "forEachOrdered" -> r + ".forEach(" + a + ")";
			case "sum" -> r + ".sum()";
			default -> unsupported(mi, q, name);
			};
		}
		if (isCollection(rt)) {
			return listCall(mi, r, name, args, a);
		}
		if (isMap(rt)) {
			return mapCall(mi, r, name, a);
		}
		if (q.startsWith("java.util.Set") || q.equals("java.util.HashSet") || q.equals("java.util.LinkedHashSet")) {
			return switch (name) {
			case "add" -> r + ".add(" + a + ")";
			case "contains" -> r + ".has(" + a + ")";
			case "remove" -> r + ".delete(" + a + ")";
			case "size" -> r + ".size";
			case "isEmpty" -> "(" + r + ".size === 0)";
			case "clear" -> r + ".clear()";
			default -> unsupported(mi, q, name);
			};
		}
		if (rt != null && (rt.isFromSource() || q.startsWith(API))) {
			return r + "." + jsName(name) + "(" + a + ")";
		}
		if (name.equals("equals") && args.size() == 1) {
			return "(" + r + " === " + a + ")";
		}
		return unsupported(mi, q, name);
	}

	/* A value as text like Java (a char as a character, a double like 2.0) */
	private String textOf(Expression e) {
		ITypeBinding t = e.resolveTypeBinding();
		String v = expression(e);
		if (isChar(t)) {
			return "String.fromCharCode(" + v + ")";
		}
		if (isDouble(t)) {
			return "Bio7Sim.str(" + v + ")";
		}
		return "String(" + v + ")";
	}

	private String unsupported(ASTNode node, String type, String name) {
		error(node, "The method " + type + "." + name + "() is not available in JavaScript");
		return "undefined";
	}

	private static boolean isCollection(ITypeBinding t) {
		if (t == null) {
			return false;
		}
		String q = t.getErasure().getQualifiedName();
		return q.equals("java.util.ArrayList") || q.equals("java.util.List") || q.equals("java.util.LinkedList") || q.equals("java.util.Collection") || q.equals("java.util.ArrayDeque")
				|| q.equals("java.util.Deque") || q.equals("java.util.Vector");
	}

	private static boolean isMap(ITypeBinding t) {
		if (t == null) {
			return false;
		}
		String q = t.getErasure().getQualifiedName();
		return q.equals("java.util.Map") || q.equals("java.util.HashMap") || q.equals("java.util.LinkedHashMap") || q.equals("java.util.TreeMap");
	}

	private String listCall(MethodInvocation mi, String r, String name, List<?> args, String a) {
		switch (name) {
		case "add":
		case "addLast":
		case "offer":
		case "offerLast":
		case "push":
			if (args.size() == 2) {
				return r + ".splice(" + expression((Expression) args.get(0)) + ", 0, " + expression((Expression) args.get(1)) + ")";
			}
			return (name.equals("push") ? r + ".unshift(" : r + ".push(") + a + ")";
		case "addFirst":
		case "offerFirst":
			return r + ".unshift(" + a + ")";
		case "get":
			return r + "[" + a + "]";
		case "set":
			return "(" + r + "[" + expression((Expression) args.get(0)) + "] = " + expression((Expression) args.get(1)) + ")";
		case "size":
			return r + ".length";
		case "isEmpty":
			return "(" + r + ".length === 0)";
		case "clear":
			return "(" + r + ".length = 0)";
		case "contains":
			return r + ".includes(" + a + ")";
		case "indexOf":
			return r + ".indexOf(" + a + ")";
		case "addAll":
			return r + ".push(..." + a + ")";
		case "poll":
		case "pollFirst":
		case "pop":
		case "removeFirst":
			return r + ".shift()";
		case "pollLast":
		case "removeLast":
			return r + ".pop()";
		case "peek":
		case "peekFirst":
		case "getFirst":
			return r + "[0]";
		case "peekLast":
		case "getLast":
			return r + "[" + r + ".length - 1]";
		case "remove":
			if (args.isEmpty()) {
				return r + ".shift()";
			}
			if (isIntegral(((Expression) args.get(0)).resolveTypeBinding()) && ((Expression) args.get(0)).resolveTypeBinding().isPrimitive()) {
				return r + ".splice(" + a + ", 1)[0]";
			}
			return "Bio7Sim.remove(" + r + ", " + a + ")";
		case "removeIf":
			return "Bio7Sim.removeIf(" + r + ", " + a + ")";
		case "forEach":
			return r + ".forEach(" + a + ")";
		case "sort":
			if (args.isEmpty() || args.get(0) instanceof NullLiteral) {
				return r + ".sort((p, q) => p < q ? -1 : p > q ? 1 : 0)";
			}
			return r + ".sort(" + a + ")";
		default:
			ITypeBinding rt = mi.getExpression().resolveTypeBinding();
			return unsupported(mi, rt.getErasure().getQualifiedName(), name);
		}
	}

	private String mapCall(MethodInvocation mi, String r, String name, String a) {
		return switch (name) {
		case "put" -> r + ".set(" + a + ")";
		case "get" -> r + ".get(" + a + ")";
		case "getOrDefault" -> "Bio7Sim.getOrDefault(" + r + ", " + a + ")";
		case "containsKey" -> r + ".has(" + a + ")";
		case "remove" -> r + ".delete(" + a + ")";
		case "size" -> r + ".size";
		case "isEmpty" -> "(" + r + ".size === 0)";
		case "clear" -> r + ".clear()";
		case "keySet" -> "[..." + r + ".keys()]";
		case "values" -> "[..." + r + ".values()]";
		default -> unsupported(mi, "java.util.Map", name);
		};
	}

	private String stringCall(MethodInvocation mi, String r, String name, List<?> args, String a) {
		return switch (name) {
		case "length" -> r + ".length";
		case "charAt" -> r + ".charCodeAt(" + a + ")";
		case "equals" -> "(" + r + " === " + a + ")";
		case "equalsIgnoreCase" -> "(" + r + ".toLowerCase() === String(" + a + ").toLowerCase())";
		case "isEmpty" -> "(" + r + ".length === 0)";
		case "contains" -> r + ".includes(" + a + ")";
		case "substring", "indexOf", "lastIndexOf", "startsWith", "endsWith", "toUpperCase", "toLowerCase", "trim", "split", "replace", "concat" -> r + "." + name + "(" + a + ")";
		case "compareTo" -> "(" + r + " < " + a + " ? -1 : " + r + " > " + a + " ? 1 : 0)";
		default -> unsupported(mi, "String", name);
		};
	}

	private String staticCall(MethodInvocation mi, ITypeBinding tb, String name, List<?> args, String a) {
		String q = tb.getQualifiedName();
		if (tb.isFromSource() || q.startsWith(API)) {
			/* A static method of the model, of a nested class or of the API (e.g., Simulation.clamp) */
			return tb.getErasure().getName() + "." + jsName(name) + "(" + a + ")";
		}
		switch (q) {
		case "java.lang.Math", "java.lang.StrictMath":
			switch (name) {
			case "abs", "min", "max", "sqrt", "cbrt", "sin", "cos", "tan", "asin", "acos", "atan", "atan2", "exp", "log", "log10", "pow", "floor", "ceil", "hypot", "sinh", "cosh", "tanh", "expm1", "log1p":
				return "Math." + name + "(" + a + ")";
			case "round":
				return "Math.round(" + a + ")";
			case "rint":
				return "Bio7Sim.rint(" + a + ")";
			case "signum":
				return "Math.sign(" + a + ")";
			case "toRadians":
				return "((" + a + ") * (Math.PI / 180))";
			case "toDegrees":
				return "((" + a + ") * (180 / Math.PI))";
			case "floorMod":
				return "Bio7Sim.floorMod(" + a + ")";
			case "floorDiv":
				return "Math.floor((" + expression((Expression) args.get(0)) + ") / (" + expression((Expression) args.get(1)) + "))";
			case "random":
				error(mi, "Math.random() is not supported: use random() of the model (the same numbers in Java and JavaScript)");
				return "undefined";
			default:
				return unsupported(mi, "Math", name);
			}
		case "java.lang.Integer", "java.lang.Long":
			return switch (name) {
			case "parseInt", "parseLong" -> "parseInt(" + a + ", 10)";
			case "toString", "valueOf" -> "String(" + a + ")";
			case "max", "min" -> "Math." + name + "(" + a + ")";
			case "compare" -> "Math.sign(" + expression((Expression) args.get(0)) + " - " + expression((Expression) args.get(1)) + ")";
			default -> unsupported(mi, q, name);
			};
		case "java.lang.Double", "java.lang.Float":
			return switch (name) {
			case "parseDouble", "parseFloat" -> "parseFloat(" + a + ")";
			case "isNaN" -> "Number.isNaN(" + a + ")";
			case "isInfinite" -> "(Math.abs(" + a + ") === Infinity)";
			case "isFinite" -> "Number.isFinite(" + a + ")";
			case "toString", "valueOf" -> "Bio7Sim.str(" + a + ")";
			case "compare" -> "Math.sign(" + expression((Expression) args.get(0)) + " - " + expression((Expression) args.get(1)) + ")";
			case "max", "min" -> "Math." + name + "(" + a + ")";
			default -> unsupported(mi, q, name);
			};
		case "java.lang.String":
			if (name.equals("valueOf")) {
				return textOf((Expression) args.get(0));
			}
			if (name.equals("format")) {
				warnings.add("Line " + line(mi) + ": String.format supports %d, %s and %.Nf in JavaScript");
				return "Bio7Sim.format(" + a + ")";
			}
			return unsupported(mi, q, name);
		case "java.lang.System":
			if (name.equals("arraycopy")) {
				return "Bio7Sim.arraycopy(" + a + ")";
			}
			if (name.equals("currentTimeMillis")) {
				return "Date.now()";
			}
			if (name.equals("nanoTime")) {
				return "Math.round(performance.now() * 1e6)";
			}
			return unsupported(mi, q, name);
		case "java.util.Arrays":
			return switch (name) {
			case "fill" -> args.size() == 2 ? expression((Expression) args.get(0)) + ".fill(" + expression((Expression) args.get(1)) + ")" : unsupported(mi, q, name);
			case "copyOf" -> "Bio7Sim.copyOf(" + a + ")";
			case "sort" -> args.size() == 1 ? expression((Expression) args.get(0)) + ".sort((p, q) => p < q ? -1 : p > q ? 1 : 0)" : unsupported(mi, q, name);
			case "toString" -> "('[' + Array.from(" + a + ").join(', ') + ']')";
			default -> unsupported(mi, q, name);
			};
		case "java.util.stream.IntStream":
			/* IntStream.range(a, b).parallel().forEach(i -> ...): a loop in JavaScript */
			if (name.equals("range")) {
				return "Bio7Sim.intRange(" + a + ")";
			}
			if (name.equals("rangeClosed") && args.size() == 2) {
				return "Bio7Sim.intRange(" + expression((Expression) args.get(0)) + ", (" + expression((Expression) args.get(1)) + ") + 1)";
			}
			return unsupported(mi, q, name);
		case "java.util.Collections":
			if (name.equals("shuffle") && args.size() == 1) {
				return "Bio7Sim.shuffle(" + a + ", this)";
			}
			return unsupported(mi, q, name);
		default:
			return unsupported(mi, q, name);
		}
	}

	/* ===================== Names and errors ===================== */

	private static String jsName(String n) {
		return RESERVED.contains(n) ? n + "_" : n;
	}

	/* A field: the fields of the API (e.g., Agent.model) keep their names (the runtime uses them), the others as jsName */
	private static String fieldName(IVariableBinding vb, String n) {
		if (vb != null && vb.getDeclaringClass() != null && vb.getDeclaringClass().getErasure().getQualifiedName().startsWith(API)) {
			return n;
		}
		return jsName(n);
	}

	private int line(ASTNode node) {
		return unit.getLineNumber(node.getStartPosition());
	}

	private void error(ASTNode node, String message) {
		String m = "Line " + line(node) + ": " + message;
		if (!errors.contains(m)) {
			errors.add(m);
		}
	}
}
