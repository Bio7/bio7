/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.eco.bio7.rpreferences.template;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateCompletionProcessor;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateException;
import org.eclipse.jface.text.templates.TemplateProposal;
import org.eclipse.swt.graphics.Image;

import com.eco.bio7.reditors.TemplateEditorUI;

/**
 * A completion processor for XML templates.
 */
public class RCompletionProcessor extends TemplateCompletionProcessor {

	private static final Comparator fgProposalComparator = new ProposalComparator();

	private static final String DEFAULT_IMAGE = "$nl$/icons/template.gif"; //$NON-NLS-1$

	public String[] statistics = { "abbreviate", "abs", "acf", "acf2AR", "acos", "acosh", "add.scope", "add1", "add1.default", "add1.glm", "add1.lm", "addmargins", "addNA", "addTaskCallback",
			"aggregate", "aggregate.data.frame", "aggregate.default", "aggregate.formula", "aggregate.ts", "agrep", "agrepl", "AIC", "alias", "alias.formula", "alias.lm", "alist", "all", "all.equal",
			"all.equal.character", "all.equal.default", "all.equal.factor", "all.equal.formula", "all.equal.language", "all.equal.list", "all.equal.numeric", "all.equal.POSIXt", "all.equal.raw",
			"all.names", "all.vars", "anova", "anova.glm", "anova.lm", "anova.lmlist", "anova.mlm", "anovalist.lm", "ansari.test", "ansari.test.default", "ansari.test.formula", "any",
			"anyDuplicated", "anyDuplicated.array", "anyDuplicated.data.frame", "anyDuplicated.default", "anyDuplicated.matrix", "anyMissing", "anyNA", "anyNA.numeric_version", "anyNA.POSIXlt",
			"aov", "aperm", "aperm.default", "aperm.table", "append", "apply", "approx", "approxfun", "ar", "ar.burg", "ar.burg.default", "ar.burg.mts", "ar.mle", "ar.ols", "ar.yw", "ar.yw.default",
			"ar.yw.mts", "Arg", "args", "arima", "arima.sim", "arima0", "arima0.diag", "Arithmetic", "ARMAacf", "ARMAtoMA", "array", "arrayInd", "as.array", "as.array.default", "as.call",
			"as.character", "as.character.condition", "as.character.Date", "as.character.default", "as.character.error", "as.character.factor", "as.character.hexmode", "as.character.numeric_version",
			"as.character.octmode", "as.character.POSIXt", "as.character.srcref", "as.complex", "as.data.frame", "as.data.frame.array", "as.data.frame.AsIs", "as.data.frame.character",
			"as.data.frame.complex", "as.data.frame.data.frame", "as.data.frame.Date", "as.data.frame.default", "as.data.frame.difftime", "as.data.frame.factor", "as.data.frame.integer",
			"as.data.frame.list", "as.data.frame.logical", "as.data.frame.matrix", "as.data.frame.model.matrix", "as.data.frame.numeric", "as.data.frame.numeric_version", "as.data.frame.ordered",
			"as.data.frame.POSIXct", "as.data.frame.POSIXlt", "as.data.frame.raw", "as.data.frame.table", "as.data.frame.ts", "as.data.frame.vector", "as.Date", "as.Date.character", "as.Date.date",
			"as.Date.dates", "as.Date.default", "as.Date.factor", "as.Date.numeric", "as.Date.POSIXct", "as.Date.POSIXlt", "as.dendrogram", "as.dendrogram.dendrogram", "as.dendrogram.hclust",
			"as.difftime", "as.dist", "as.dist.default", "as.double", "as.double.difftime", "as.double.POSIXlt", "as.environment", "as.expression", "as.expression.default", "as.factor", "as.formula",
			"as.function", "as.function.default", "as.hclust", "as.hclust.default", "as.hclust.dendrogram", "as.hclust.twins", "as.hexmode", "as.integer", "as.list", "as.list.data.frame",
			"as.list.Date", "as.list.default", "as.list.environment", "as.list.factor", "as.list.function", "as.list.numeric_version", "as.list.POSIXct", "as.logical", "as.logical.factor",
			"as.matrix", "as.matrix.data.frame", "as.matrix.default", "as.matrix.dist", "as.matrix.noquote", "as.matrix.POSIXlt", "as.name", "as.null", "as.null.default", "as.numeric",
			"as.numeric_version", "as.octmode", "as.ordered", "as.package_version", "as.pairlist", "as.POSIXct", "as.POSIXct.Date", "as.POSIXct.date", "as.POSIXct.dates", "as.POSIXct.default",
			"as.POSIXct.numeric", "as.POSIXct.POSIXlt", "as.POSIXlt", "as.POSIXlt.character", "as.POSIXlt.Date", "as.POSIXlt.date", "as.POSIXlt.dates", "as.POSIXlt.default", "as.POSIXlt.factor",
			"as.POSIXlt.numeric", "as.POSIXlt.POSIXct", "as.qr", "as.raw", "as.real", "as.single", "as.single.default", "as.stepfun", "as.symbol", "as.table", "as.table.default", "as.ts",
			"as.ts.default", "as.vector", "as.vector.factor", "asin", "asinh", "AsIs", "asNamespace", "asOneSidedFormula", "asS3", "asS4", "assign", "assignOps", "atan", "atan2", "atanh", "atomic",
			"attach", "attachNamespace", "attr", "attr.all.equal", "attr<-", "attributes", "attributes<-", "autoload", "autoloader", "Autoloads", "ave", "backquote", "backsolve", "backtick",
			"bandwidth.kernel", "bartlett.test", "bartlett.test.default", "bartlett.test.formula", "base", "base-defunct", "base-deprecated", "baseenv", "basename", "Bessel", "bessel", "besselI",
			"besselJ", "besselK", "besselY", "beta", "Beta", "BIC", "bindenv", "bindingIsActive", "bindingIsLocked", "bindtextdomain", "binom.test", "Binomial", "binomial", "biplot",
			"biplot.default", "biplot.prcomp", "biplot.princomp", "bitwAnd", "bitwNot", "bitwOr", "bitwShiftL", "bitwShiftR", "bitwXor", "body", "body<-", "Box.test", "bquote", "break", "browser",
			"browserCondition", "browserSetDebug", "browserText", "builtins", "bw.bcv", "bw.nrd", "bw.nrd0", "bw.SJ", "bw.ucv", "by", "by.data.frame", "by.default", "bzfile", "c", "C", "c.Date",
			"c.noquote", "c.numeric_version", "c.POSIXct", "c.POSIXlt", "call", "callCC", "cancor", "capabilities", "case.names", "case.names.lm", "casefold", "cat", "category", "Cauchy", "cbind",
			"cbind.data.frame", "cbind.ts", "ccf", "ceiling", "char.expand", "character", "charmatch", "charToRaw", "chartr", "check_tzones", "chisq.test", "Chisquare", "chol", "chol.default",
			"chol2inv", "choose", "class", "class<-", "clearNames", "clearPushBack", "clipboard", "close", "close.connection", "close.srcfile", "close.srcfilealias", "closeAllConnections", "closure",
			"cmdscale", "code point", "codes", "codes.factor", "codes.ordered", "codes<-", "coef", "coefficients", "col", "collation", "colMeans", "colnames", "colnames<-", "colon", "colSums",
			"commandArgs", "comment", "comment<-", "Comparison", "complete.cases", "Complex", "complex", "computeRestarts", "condition", "conditionCall", "conditionCall.condition",
			"conditionMessage", "conditionMessage.condition", "conditions", "confint", "confint.default", "confint.lm", "conflicts", "Conj", "connection", "connections", "Constants", "constrOptim",
			"contr.helmert", "contr.poly", "contr.SAS", "contr.sum", "contr.treatment", "contrasts", "contrasts<-", "contributors", "Control", "convolve", "cooks.distance", "cooks.distance.glm",
			"cooks.distance.lm", "cophenetic", "cophenetic.default", "cophenetic.dendrogram", "copyright", "copyrights", "cor", "cor.test", "cor.test.default", "cor.test.formula", "cos", "cosh",
			"cospi", "cov", "cov.wt", "cov2cor", "covratio", "cpgram", "crossprod", "Cstack_info", "cummax", "cummin", "cumprod", "cumsum", "cut", "cut.Date", "cut.default", "cut.dendrogram",
			"cut.POSIXt", "cutree", "cycle", "D", "data.class", "data.frame", "data.matrix", "data.restore", "Date", "date", "Dates", "date-time", "DateTimeClasses", "dbeta", "dbinom", "dcauchy",
			"dchisq", "debug", "debugonce", "decompose", "default.stringsAsFactors", "Defunct", "defunct", "delay", "delayedAssign", "delete.response", "deltat", "dendrapply", "dendrogram",
			"density", "density.default", "deparse", "Deprecated", "deprecated", "deriv", "deriv.default", "deriv.formula", "deriv3", "deriv3.default", "deriv3.formula", "det", "detach",
			"determinant", "determinant.matrix", "deviance", "dexp", "df", "df.kernel", "df.residual", "dfbeta", "dfbeta.lm", "dfbetas", "dfbetas.lm", "dffits", "dgamma", "dgeom", "dget", "dhyper",
			"diag", "diag<-", "diff", "diff.Date", "diff.default", "diff.POSIXt", "diff.ts", "diffinv", "diffinv.default", "diffinv.ts", "difftime", "digamma", "dim", "dim.data.frame", "dim<-",
			"dimnames", "dimnames.data.frame", "dimnames<-", "dimnames<-.data.frame", "dir", "dir.create", "dirname", "dist", "distribution", "Distributions", "distributions", "DLLInfo",
			"DLLInfoList", "DLLpath", "dlnorm", "dlogis", "dmultinom", "dnbinom", "dnchisq", "dnorm", "do.call", "dontCheck", "double", "dpois", "dput", "dQuote", "drop", "drop.scope", "drop.terms",
			"drop1", "drop1.default", "drop1.glm", "drop1.lm", "droplevels", "droplevels.data.frame", "droplevels.factor", "dsignrank", "dt", "dummy.coef", "dummy.coef.aovlist", "dummy.coef.lm",
			"dump", "dunif", "duplicated", "duplicated.array", "duplicated.data.frame", "duplicated.default", "duplicated.matrix", "duplicated.numeric_version", "duplicated.POSIXlt", "dweibull",
			"dwilcox", "dyn.load", "dyn.unload", "eapply", "ecdf", "eff.aovlist", "effects", "effects.glm", "effects.lm", "eigen", "else", "embed", "emptyenv", "enc2native", "enc2utf8", "enclosure",
			"encodeString", "Encoding", "Encoding<-", "end", "enquote", "env.profile", "environment", "environment variables", "environment<-", "environmentIsLocked", "environmentName", "Error",
			"estVar", "eval", "eval.parent", "evalq", "exists", "exp", "expand.grid", "expand.model.frame", "expm1", "Exponential", "expression", "Extract", "extractAIC", "F", "factanal", "factor",
			"factor.scope", "factorial", "FALSE", "family", "family.glm", "family.lm", "FDist", "fft", "fifo", "file", "file.access", "file.append", "file.choose", "file.copy", "file.create",
			"file.exists", "file.info", "file.link", "file.path", "file.remove", "file.rename", "file.show", "file.symlink", "files", "Filter", "filter", "finalizer", "Find", "find.package",
			"findInterval", "findPackageEnv", "findRestart", "finite", "fisher.test", "fitted", "fitted.default", "fitted.kmeans", "fitted.values", "fivenum", "fligner.test", "fligner.test.default",
			"fligner.test.formula", "floor", "flush", "flush.connection", "for", "force", "Foreign", "formals", "formals<-", "format", "format.AsIs", "format.char", "format.data.frame",
			"format.Date", "format.default", "format.difftime", "format.dist", "format.factor", "format.ftable", "format.hexmode", "format.info", "format.libraryIQR", "format.numeric_version",
			"format.octmode", "format.packageInfo", "format.POSIXct", "format.POSIXlt", "format.pval", "format.summaryDefault", "formatC", "formatDL", "formula", "formula.data.frame",
			"formula.default", "formula.formula", "formula.lm", "formula.nls", "formula.terms", "forwardsolve", "frequency", "friedman.test", "friedman.test.default", "friedman.test.formula",
			"ftable", "ftable.default", "ftable.formula", "function", "fuzzy matching", "gamma", "Gamma", "gammaCody", "GammaDist", "gaussian", "gc", "gc.time", "gcinfo", "gctorture", "gctorture2",
			"Geometric", "get", "get_all_vars", "getAllConnections", "getCall", "getCall.default", "getCallingDLL", "getCallingDLLe", "getConnection", "getDLLRegisteredRoutines",
			"getDLLRegisteredRoutines.character", "getDLLRegisteredRoutines.DLLInfo", "getElement", "getenv", "geterrmessage", "getExportedValue", "getHook", "getInitial", "getInitial.default",
			"getInitial.formula", "getInitial.selfStart", "getLoadedDLLs", "getNamespace", "getNamespaceExports", "getNamespaceImports", "getNamespaceInfo", "getNamespaceName", "getNamespaceUsers",
			"getNamespaceVersion", "getNativeSymbolInfo", "getOption", "getRversion", "getSrcLines", "getTaskCallbackNames", "gettext", "gettextf", "getwd", "gl", "glm", "glm.control", "glm.fit",
			"glm.fit.null", "globalenv", "gregexpr", "grep", "grepl", "grepRaw", "group generic", "groupGeneric", "GSC", "gsub", "gzcon", "gzfile", "hasTsp", "hat", "hatvalues", "hatvalues.lm",
			"hclust", "heatmap", "hexmode", "HoltWinters", "HOME", "httpclient", "Hypergeometric", "I", "iconv", "iconvlist", "icuSetCollate", "identical", "identify.hclust", "identity", "if",
			"ifelse", "Im", "importIntoEnv", "in", "Inf", "influence", "influence.glm", "influence.lm", "influence.measures", "inherits", "integer", "integrate", "interaction", "interaction.plot",
			"interactive", "internal generic", "InternalGenerics", "InternalMethods", "intersect", "intersection", "intToBits", "intToUtf8", "inverse.gaussian", "inverse.rle", "invisible",
			"invokeRestart", "invokeRestartInteractively", "IQR", "is.array", "is.atomic", "is.call", "is.character", "is.complex", "is.data.frame", "is.double", "is.element", "is.empty.model",
			"is.environment", "is.expression", "is.factor", "is.finite", "is.function", "is.infinite", "is.integer", "is.language", "is.leaf", "is.list", "is.loaded", "is.logical", "is.matrix",
			"is.mts", "is.na", "is.na.data.frame", "is.na.numeric_version", "is.na.POSIXlt", "is.na<-", "is.na<-.default", "is.na<-.factor", "is.name", "is.nan", "is.null", "is.numeric",
			"is.numeric.Date", "is.numeric.difftime", "is.numeric.POSIXt", "is.numeric_version", "is.object", "is.ordered", "is.package_version", "is.pairlist", "is.primitive", "is.qr", "is.R",
			"is.raw", "is.real", "is.recursive", "is.single", "is.stepfun", "is.symbol", "is.table", "is.ts", "is.tskernel", "is.unsorted", "is.vector", "isatty", "isBaseNamespace", "isdebugged",
			"isIncomplete", "isNamespace", "ISOdate", "ISOdatetime", "isOpen", "isoreg", "isRestart", "isS4", "isSeekable", "isSymmetric", "isSymmetric.matrix", "isTRUE", "jitter", "julian",
			"julian.Date", "julian.POSIXt", "KalmanForecast", "KalmanLike", "KalmanRun", "KalmanSmooth", "kappa", "kappa.default", "kappa.lm", "kappa.qr", "kernapply", "kernapply.default",
			"kernapply.ts", "kernapply.tskernel", "kernapply.vector", "kernel", "kmeans", "knots", "kronecker", "kruskal.test", "kruskal.test.default", "kruskal.test.formula", "ks.test", "ksmooth",
			"l10n_info", "La.chol", "La.chol2inv", "La.eigen", "La.svd", "La_version", "labels", "labels.default", "labels.dendrogram", "labels.dist", "labels.lm", "labels.terms", "lag",
			"lag.default", "lag.plot", "LANGUAGE", "language object", "language objects", "lapply", "last.warning", "lazyLoad", "lazyLoadDBexec", "lazyLoadDBfetch", "lbeta", "LC_ALL", "LC_COLLATE",
			"LC_CTYPE", "LC_MONETARY", "LC_NUMERIC", "LC_TIME", "lchoose", "length", "length.POSIXlt", "length<-", "length<-.factor", "LETTERS", "letters", "levels", "levels.default", "levels<-",
			"levels<-.factor", "lfactorial", "lgamma", "library", "library.dynam", "library.dynam.unload", "licence", "license", "line", "lines.isoreg", "lines.stepfun", "lines.ts", "list",
			"list.dirs", "list.files", "list2env", "lm", "lm.fit", "lm.fit.null", "lm.influence", "lm.wfit", "lm.wfit.null", "load", "loadedNamespaces", "loadingNamespaceInfo", "loadings",
			"loadNamespace", "loadURL", "local", "localeconv", "locales", "lockBinding", "lockEnvironment", "loess", "loess.control", "loess.smooth", "log", "log10", "log1p", "log2", "logb", "Logic",
			"logical", "Logistic", "logLik", "logLik.lm", "loglin", "Lognormal", "long vector", "Long vectors", "long vectors", "lookup.xport", "lower.tri", "lowess", "ls", "ls.diag", "ls.print",
			"lsfit", "Machine", "machine", "mad", "mahalanobis", "make.link", "make.names", "make.unique", "makeActiveBinding", "makeARIMA", "MAKEINDEX", "makepredictcall", "makepredictcall.default",
			"makepredictcall.poly", "manglePackageName", "manova", "mantelhaen.test", "Map", "mapply", "margin.table", "mat.or.vec", "match", "match.arg", "match.call", "match.fun", "Math",
			"Math.data.frame", "Math.Date", "Math.difftime", "Math.factor", "Math.POSIXlt", "Math.POSIXt", "matmult", "matrix", "mauchley.test", "mauchly.test", "mauchly.test.mlm",
			"mauchly.test.SSD", "max", "max.col", "mcnemar.test", "mean", "mean.Date", "mean.default", "mean.difftime", "mean.POSIXct", "mean.POSIXlt", "median", "median.default", "medpolish",
			"mem.limits", "memCompress", "memDecompress", "Memory", "memory.profile", "Memory-limits", "merge", "merge.data.frame", "merge.default", "merge.dendrogram", "message", "mget", "min",
			"missing", "Mod", "mode", "mode<-", "model.extract", "model.frame", "model.frame.aovlist", "model.frame.default", "model.frame.glm", "model.frame.lm", "model.matrix",
			"model.matrix.default", "model.matrix.lm", "model.offset", "model.response", "model.tables", "model.tables.aov", "model.tables.aovlist", "model.weights", "month.abb", "month.name",
			"monthplot", "monthplot.default", "monthplot.stl", "monthplot.StructTS", "monthplot.ts", "months", "months.Date", "months.POSIXt", "mood.test", "mood.test.default", "mood.test.formula",
			"mostattributes<-", "Multinomial", "mvfft", "NA", "na.action", "na.action.default", "na.contiguous", "na.contiguous.default", "na.exclude", "na.exclude.data.frame", "na.exclude.default",
			"na.fail", "na.fail.default", "na.omit", "na.omit.data.frame", "na.omit.default", "na.omit.ts", "na.pass", "NA_character_", "NA_complex_", "NA_integer_", "NA_real_", "name", "names",
			"names.default", "names.POSIXlt", "names<-", "names<-.default", "names<-.POSIXlt", "namespaceExport", "namespaceImport", "namespaceImportClasses", "namespaceImportFrom",
			"namespaceImportMethods", "NaN", "napredict", "napredict.default", "napredict.exclude", "naprint", "naprint.default", "naprint.exclude", "naprint.omit", "naresid", "naresid.default",
			"naresid.exclude", "nargs", "NativeSymbol", "NativeSymbolInfo", "nchar", "NCOL", "ncol", "Negate", "NegBinomial", "new.env", "next", "NextMethod", "nextn", "ngettext", "nlevels", "nlm",
			"nlminb", "nls", "nls.control", "NLSstAsymptotic", "NLSstAsymptotic.sortedXyData", "NLSstClosestX", "NLSstClosestX.sortedXyData", "NLSstLfAsymptote", "NLSstLfAsymptote.sortedXyData",
			"NLSstRtAsymptote", "NLSstRtAsymptote.sortedXyData", "nobs", "nobs.default", "noquote", "norm", "Normal", "normalizePath", "NotYetImplemented", "NotYetUsed", "NROW", "nrow", "NULL",
			"numeric", "numeric_version", "NumericConstants", "numericDeriv", "nzchar", "objects", "octmode", "offset", "oldClass", "oldClass<-", "OlsonNames", "on.exit", "oneway.test", "open",
			"open.connection", "open.srcfile", "open.srcfilealias", "open.srcfilecopy", "Ops", "Ops.data.frame", "Ops.Date", "Ops.difftime", "Ops.factor", "Ops.numeric_version", "Ops.ordered",
			"Ops.POSIXt", "Ops.ts", "optim", "optimHess", "optimise", "optimize", "option", "options", "order", "order.dendrogram", "ordered", "outer", "p.adjust", "p.adjust.methods", "pacf",
			"pacf.default", "package.description", "package_version", "packageEvent", "packageHasNamespace", "packageStartupMessage", "packBits", "pairlist", "pairwise.prop.test", "pairwise.t.test",
			"pairwise.table", "pairwise.wilcox.test", "Paren", "parent.env", "parent.env<-", "parent.frame", "parse", "parse.dcf", "parseNamespaceFile", "paste", "paste0", "path.expand",
			"path.package", "pbeta", "pbinom", "pbirthday", "pcauchy", "pchisq", "pentagamma", "pexp", "pf", "pgamma", "pgeom", "phyper", "pi", "pipe", "Platform", "plclust", "plnorm", "plogis",
			"plot.acf", "plot.decomposed.ts", "plot.dendrogram", "plot.density", "plot.ecdf", "plot.hclust", "plot.HoltWinters", "plot.isoreg", "plot.lm", "plot.mts", "plot.ppr", "plot.prcomp",
			"plot.princomp", "plot.profile.nls", "plot.spec", "plot.spec.coherency", "plot.spec.phase", "plot.stepfun", "plot.stl", "plot.ts", "plot.tskernel", "pmatch", "pmax", "pmax.int", "pmin",
			"pmin.int", "pnbinom", "pnchisq", "pnorm", "Poisson", "poisson", "poisson.test", "poly", "polym", "polyroot", "pos.to.env", "Position", "POSIXct", "POSIXlt", "POSIXt", "power",
			"power.anova.test", "power.prop.test", "power.t.test", "PP.test", "ppoints", "ppois", "ppr", "ppr.default", "ppr.formula", "prcomp", "prcomp.default", "prcomp.formula", "predict",
			"predict.ar", "predict.Arima", "predict.arima0", "predict.glm", "predict.HoltWinters", "predict.lm", "predict.loess", "predict.nls", "predict.poly", "predict.prcomp", "predict.princomp",
			"predict.smooth.spline", "predict.StructTS", "preplot", "pretty", "pretty.default", "prettyNum", "primitive", "princomp", "princomp.default", "princomp.formula", "print",
			"print.anova.glm", "print.anova.lm", "print.aov", "print.aovlist", "print.ar", "print.arima0", "print.AsIs", "print.atomic", "print.by", "print.coefmat", "print.condition",
			"print.connection", "print.data.frame", "print.Date", "print.default", "print.dendrogram", "print.difftime", "print.dist", "print.DLLInfo", "print.DLLInfoList",
			"print.DLLRegisteredRoutines", "print.ecdf", "print.factanal", "print.factor", "print.formula", "print.ftable", "print.function", "print.hclust", "print.hexmode", "print.HoltWinters",
			"print.htest", "print.integrate", "print.kmeans", "print.libraryIQR", "print.listof", "print.loadings", "print.NativeRoutineList", "print.noquote", "print.numeric_version",
			"print.octmode", "print.ordered", "print.packageInfo", "print.plot", "print.POSIXct", "print.POSIXlt", "print.power.htest", "print.prcomp", "print.princomp", "print.proc_time",
			"print.restart", "print.rle", "print.simple.list", "print.srcfile", "print.srcref", "print.stepfun", "print.StructTS", "print.summary.aov", "print.summary.aovlist", "print.summary.glm",
			"print.summary.lm", "print.summary.manova", "print.summary.nls", "print.summary.prcomp", "print.summary.princomp", "print.summary.table", "print.summaryDefault", "print.table",
			"print.tabular", "print.ts", "print.warnings", "print.xtabs", "printCoefmat", "printNoClass", "prmatrix", "proc.time", "prod", "profile", "profile.nls", "proj", "proj.aov",
			"proj.aovlist", "proj.default", "proj.lm", "promax", "promise", "promises", "prop.table", "prop.test", "prop.trend.test", "provide", "provideDimnames", "psigamma", "psignrank", "pt",
			"ptukey", "punif", "pushBack", "pushBackLength", "pweibull", "pwilcox", "q", "qbeta", "qbinom", "qbirthday", "qcauchy", "qchisq", "qexp", "qf", "qgamma", "qgeom", "qhyper", "qlnorm",
			"qlogis", "qnbinom", "qnchisq", "qnorm", "qpois", "qqline", "qqnorm", "qqnorm.default", "qqplot", "qr", "qr.coef", "qr.default", "qr.fitted", "qr.Q", "qr.qty", "qr.qy", "qr.R",
			"qr.resid", "qr.solve", "qr.X", "qsignrank", "qt", "qtukey", "quade.test", "quade.test.default", "quade.test.formula", "quantile", "quantile.default", "quantile.ecdf", "quarters",
			"quarters.Date", "quarters.POSIXt", "quasi", "quasibinomial", "quasipoisson", "quit", "qunif", "quote", "Quotes", "qweibull", "qwilcox", "R.home", "R.Version", "R.version",
			"R.version.string", "R_BATCH", "R_BROWSER", "R_COMPLETION", "R_DEFAULT_PACKAGES", "R_DOC_DIR", "R_ENVIRON", "R_ENVIRON_USER", "R_GCTORTURE", "R_GCTORTURE_INHIBIT_RELEASE",
			"R_GCTORTURE_WAIT", "R_GSCMD", "R_HISTFILE", "R_HISTSIZE", "R_HOME", "R_INCLUDE_DIR", "R_LIBS", "R_LIBS_SITE", "R_LIBS_USER", "R_PAPERSIZE", "R_PDFVIEWER", "R_PLATFORM", "R_PROFILE",
			"R_PROFILE_USER", "R_RD4PDF", "R_SHARE_DIR", "R_system_version", "R_TEXI2DVICMD", "R_UNZIPCMD", "R_USER", "R_ZIPCMD", "r2dtable", "Random", "Random.user", "range", "range.default",
			"rank", "rapply", "raw", "rawConnection", "rawConnectionValue", "rawShift", "rawToBits", "rawToChar", "rbeta", "rbind", "rbind.data.frame", "rbinom", "rcauchy", "rchisq", "rcond",
			"Rd2pdf", "Rdconv", "Re", "read.arff", "read.dbf", "read.dcf", "read.dta", "read.epiinfo", "read.ftable", "read.mtp", "read.octave", "read.S", "read.spss", "read.ssd", "read.systat",
			"read.table.url", "read.xport", "readBin", "readChar", "readline", "readLines", "readRDS", "readRenviron", "real", "Recall", "rect.hclust", "Reduce", "reformulate", "reg.finalizer",
			"regex", "regexec", "regexp", "regexpr", "RegisteredNativeSymbol", "registerS3method", "registerS3methods", "regmatches", "regmatches<-", "regular expression", "relevel",
			"relevel.default", "relevel.factor", "relevel.ordered", "remove", "removeTaskCallback", "Renviron", "Renviron.site", "reorder", "reorder.default", "reorder.dendrogram", "rep", "rep.Date",
			"rep.factor", "rep.int", "rep.numeric_version", "rep.POSIXct", "rep.POSIXlt", "rep_len", "repeat", "replace", "replicate", "replications", "require", "requireNamespace", "Reserved",
			"reserved", "reshape", "reshapeLong", "reshapeWide", "resid", "residuals", "residuals.glm", "residuals.HoltWinters", "residuals.lm", "residuals.tukeyline", "restart",
			"restartDescription", "restartFormals", "retracemem", "return", "rev", "rev.default", "rev.dendrogram", "rexp", "rf", "rgamma", "rgeom", "rhyper", "rle", "rlnorm", "rlogis", "rm",
			"rmultinom", "rnbinom", "rnchisq", "RNG", "RNGkind", "RNGversion", "rnorm", "round", "round.Date", "round.POSIXt", "row", "row.names", "row.names.data.frame", "row.names.default",
			"row.names<-", "row.names<-.data.frame", "row.names<-.default", "rowMeans", "rownames", "rownames<-", "rowsum", "rowsum.data.frame", "rowsum.default", "rowSums", "rpois", "Rprofile",
			"Rprofile.site", "rsignrank", "rstandard", "rstandard.glm", "rstandard.lm", "rstudent", "rstudent.glm", "rstudent.lm", "rt", "runif", "runmed", "rweibull", "rwilcox", "rWishart",
			"S3groupGeneric", "S3Methods", "S4", "SafePrediction", "sample", "sample.int", "sapply", "save", "save.image", "save.plot", "saveRDS", "scale", "scale.default", "scan", "scan.url",
			"scatter.smooth", "screeplot", "screeplot.default", "sd", "se.contrast", "se.contrast.aov", "se.contrast.aovlist", "search", "searchpaths", "seek", "seek.connection", "selfStart",
			"selfStart.default", "selfStart.formula", "seq", "seq.Date", "seq.default", "seq.int", "seq.POSIXt", "seq_along", "seq_len", "sequence", "serialize", "set.seed", "setdiff", "setequal",
			"setHook", "setNames", "setNamespaceInfo", "setSessionTimeLimit", "setTimeLimit", "setwd", "shapiro.test", "shell", "shell.exec", "showConnections", "shQuote", "sign", "signalCondition",
			"signif", "SignRank", "simpleCondition", "simpleError", "simpleMessage", "simpleWarning", "simplify2array", "simulate", "sin", "single", "sinh", "sink", "sink.number", "sinpi",
			"slice.index", "smooth", "smooth.spline", "smoothEnds", "socketConnection", "socketSelect", "solve", "solve.default", "solve.qr", "sort", "sort.default", "sort.int", "sort.list",
			"sort.POSIXlt", "sortedXyData", "sortedXyData.default", "source", "source.url", "spec", "spec.ar", "spec.pgram", "spec.taper", "Special", "spectrum", "spline", "splinefun", "splinefunH",
			"split", "split.data.frame", "split.Date", "split.default", "split.POSIXct", "split<-", "split<-.data.frame", "split<-.default", "sprintf", "sqrt", "sQuote", "srcfile", "srcfilealias",
			"srcfilealias-class", "srcfile-class", "srcfilecopy", "srcfilecopy-class", "srcref", "srcref-class", "SSasymp", "SSasympOff", "SSasympOrig", "SSbiexp", "SSD", "SSfol", "SSfpl",
			"SSgompertz", "SSlogis", "SSmicmen", "SSweibull", "standardGeneric", "start", "Startup", "stat.anova", "stats", "stats-defunct", "stats-deprecated", "stderr", "stdin", "stdout", "step",
			"stepfun", "stl", "stop", "stopifnot", "storage.mode", "storage.mode<-", "str.dendrogram", "str.POSIXt", "strftime", "strptime", "strsplit", "strtoi", "strtrim", "StructTS", "structure",
			"strwrap", "sub", "Subscript", "subset", "subset.data.frame", "subset.default", "subset.matrix", "substitute", "substr", "substr<-", "substring", "substring<-", "sum", "Summary",
			"summary", "summary.aov", "summary.aovlist", "summary.connection", "Summary.data.frame", "summary.data.frame", "Summary.Date", "summary.Date", "summary.default", "Summary.difftime",
			"summary.ecdf", "Summary.factor", "summary.factor", "summary.glm", "summary.lm", "summary.manova", "summary.matrix", "summary.mlm", "summary.nls", "Summary.numeric_version",
			"Summary.ordered", "Summary.POSIXct", "summary.POSIXct", "Summary.POSIXlt", "summary.POSIXlt", "summary.prcomp", "summary.princomp", "summary.proc_time", "summary.srcfile",
			"summary.srcref", "summary.stepfun", "summary.table", "suppressMessages", "suppressPackageStartupMessages", "suppressWarnings", "supsmu", "svd", "sweep", "switch", "symbol.C",
			"symbol.For", "symnum", "Syntax", "sys.call", "sys.calls", "Sys.chmod", "Sys.Date", "sys.frame", "sys.frames", "sys.function", "Sys.getenv", "Sys.getlocale", "Sys.getpid", "Sys.glob",
			"Sys.info", "Sys.junction", "sys.load.image", "Sys.localeconv", "sys.nframe", "sys.on.exit", "sys.parent", "sys.parents", "Sys.putenv", "Sys.readlink", "sys.save.image", "Sys.setenv",
			"Sys.setFileTime", "Sys.setlocale", "Sys.sleep", "sys.source", "sys.status", "Sys.time", "Sys.timezone", "Sys.umask", "Sys.unsetenv", "Sys.which", "system", "system.file", "system.test",
			"system.time", "system2", "T", "t", "t.data.frame", "t.default", "t.test", "t.test.default", "t.test.formula", "t.ts", "table", "tabulate", "tan", "tanh", "tanpi", "tapply",
			"taskCallbackManager", "tcrossprod", "TDist", "tempdir", "tempfile", "termplot", "terms", "terms.formula", "terms.object", "testPlatformEquivalence", "tetragamma", "textConnection",
			"textConnectionValue", "tilde", "tilde expansion", "time", "time interval", "time zone", "time zones", "time.default", "timezone", "timezones", "TMPDIR", "toeplitz", "tolower", "topenv",
			"toString", "toString.default", "toupper", "trace", "traceback", "tracemem", "tracingState", "transform", "transform.data.frame", "transform.default", "Trig", "trigamma", "TRUE", "trunc",
			"trunc.Date", "trunc.POSIXt", "truncate", "truncate.connection", "try", "tryCatch", "ts", "ts.intersect", "ts.plot", "ts.union", "tsdiag", "tsdiag.Arima", "tsdiag.arima0",
			"tsdiag.StructTS", "tsp", "tsp<-", "tsSmooth", "tsSmooth.StructTS", "Tukey", "TukeyHSD", "type", "typeof", "TZ", "TZDIR", "umask", "unclass", "undebug", "Unicode", "Uniform", "union",
			"unique", "unique.array", "unique.data.frame", "unique.default", "unique.matrix", "unique.numeric_version", "unique.POSIXlt", "uniroot", "units", "units.difftime", "units<-",
			"units<-.difftime", "unix", "unix.time", "unlink", "unlist", "unloadNamespace", "unlockBinding", "unname", "unserialize", "unsplit", "untrace", "untracemem", "unz", "update",
			"update.default", "update.formula", "upper.tri", "url", "UseMethod", "utf8ToInt", "vapply", "var", "var.test", "var.test.default", "var.test.formula", "variable.names",
			"variable.names.lm", "varimax", "vcov", "vcov.glm", "vcov.gls", "vcov.lm", "vcov.lme", "vcov.summary.glm", "vcov.summary.lm", "vector", "Vectorize", "Version", "version", "warning",
			"warnings", "weekdays", "weekdays.Date", "weekdays.POSIXt", "Weibull", "weighted.mean", "weighted.mean.default", "weighted.residuals", "weights", "weights.default", "weights.glm",
			"which", "which.max", "which.min", "while", "wilcox.test", "wilcox.test.default", "wilcox.test.formula", "Wilcoxon", "window", "window.default", "window.ts", "window<-", "window<-.ts",
			"with", "with.default", "withCallingHandlers", "within", "within.data.frame", "within.list", "withRestarts", "withVisible", "write", "write.arff", "write.dbf", "write.dcf", "write.dta",
			"write.foreign", "write.ftable", "write.table0", "writeBin", "writeChar", "writeLines", "xor", "xor.hexmode", "xor.octmode", "xpdrows.data.frame", "xtabs", "xtfrm", "xtfrm.AsIs",
			"xtfrm.Date", "xtfrm.default", "xtfrm.difftime", "xtfrm.factor", "xtfrm.numeric_version", "xtfrm.POSIXct", "xtfrm.POSIXlt", "xtfrm.Surv", "xzfile", "zapsmall" };

	public String[] statisticsContext = { "abbreviate", "abs", "acf", "acf2AR", "acos", "acosh", "add.scope", "add1", "add1.default", "add1.glm", "add1.lm", "addmargins", "addNA", "addTaskCallback",
			"aggregate", "aggregate.data.frame", "aggregate.default", "aggregate.formula", "aggregate.ts", "agrep", "agrepl", "AIC", "alias", "alias.formula", "alias.lm", "alist", "all", "all.equal",
			"all.equal.character", "all.equal.default", "all.equal.factor", "all.equal.formula", "all.equal.language", "all.equal.list", "all.equal.numeric", "all.equal.POSIXt", "all.equal.raw",
			"all.names", "all.vars", "anova", "anova.glm", "anova.lm", "anova.lmlist", "anova.mlm", "anovalist.lm", "ansari.test", "ansari.test.default", "ansari.test.formula", "any",
			"anyDuplicated", "anyDuplicated.array", "anyDuplicated.data.frame", "anyDuplicated.default", "anyDuplicated.matrix", "anyMissing", "anyNA", "anyNA.numeric_version", "anyNA.POSIXlt",
			"aov", "aperm", "aperm.default", "aperm.table", "append", "apply", "approx", "approxfun", "ar", "ar.burg", "ar.burg.default", "ar.burg.mts", "ar.mle", "ar.ols", "ar.yw", "ar.yw.default",
			"ar.yw.mts", "Arg", "args", "arima", "arima.sim", "arima0", "arima0.diag", "Arithmetic", "ARMAacf", "ARMAtoMA", "array", "arrayInd", "as.array", "as.array.default", "as.call",
			"as.character", "as.character.condition", "as.character.Date", "as.character.default", "as.character.error", "as.character.factor", "as.character.hexmode", "as.character.numeric_version",
			"as.character.octmode", "as.character.POSIXt", "as.character.srcref", "as.complex", "as.data.frame", "as.data.frame.array", "as.data.frame.AsIs", "as.data.frame.character",
			"as.data.frame.complex", "as.data.frame.data.frame", "as.data.frame.Date", "as.data.frame.default", "as.data.frame.difftime", "as.data.frame.factor", "as.data.frame.integer",
			"as.data.frame.list", "as.data.frame.logical", "as.data.frame.matrix", "as.data.frame.model.matrix", "as.data.frame.numeric", "as.data.frame.numeric_version", "as.data.frame.ordered",
			"as.data.frame.POSIXct", "as.data.frame.POSIXlt", "as.data.frame.raw", "as.data.frame.table", "as.data.frame.ts", "as.data.frame.vector", "as.Date", "as.Date.character", "as.Date.date",
			"as.Date.dates", "as.Date.default", "as.Date.factor", "as.Date.numeric", "as.Date.POSIXct", "as.Date.POSIXlt", "as.dendrogram", "as.dendrogram.dendrogram", "as.dendrogram.hclust",
			"as.difftime", "as.dist", "as.dist.default", "as.double", "as.double.difftime", "as.double.POSIXlt", "as.environment", "as.expression", "as.expression.default", "as.factor", "as.formula",
			"as.function", "as.function.default", "as.hclust", "as.hclust.default", "as.hclust.dendrogram", "as.hclust.twins", "as.hexmode", "as.integer", "as.list", "as.list.data.frame",
			"as.list.Date", "as.list.default", "as.list.environment", "as.list.factor", "as.list.function", "as.list.numeric_version", "as.list.POSIXct", "as.logical", "as.logical.factor",
			"as.matrix", "as.matrix.data.frame", "as.matrix.default", "as.matrix.dist", "as.matrix.noquote", "as.matrix.POSIXlt", "as.name", "as.null", "as.null.default", "as.numeric",
			"as.numeric_version", "as.octmode", "as.ordered", "as.package_version", "as.pairlist", "as.POSIXct", "as.POSIXct.Date", "as.POSIXct.date", "as.POSIXct.dates", "as.POSIXct.default",
			"as.POSIXct.numeric", "as.POSIXct.POSIXlt", "as.POSIXlt", "as.POSIXlt.character", "as.POSIXlt.Date", "as.POSIXlt.date", "as.POSIXlt.dates", "as.POSIXlt.default", "as.POSIXlt.factor",
			"as.POSIXlt.numeric", "as.POSIXlt.POSIXct", "as.qr", "as.raw", "as.real", "as.single", "as.single.default", "as.stepfun", "as.symbol", "as.table", "as.table.default", "as.ts",
			"as.ts.default", "as.vector", "as.vector.factor", "asin", "asinh", "AsIs", "asNamespace", "asOneSidedFormula", "asS3", "asS4", "assign", "assignOps", "atan", "atan2", "atanh", "atomic",
			"attach", "attachNamespace", "attr", "attr.all.equal", "attr<-", "attributes", "attributes<-", "autoload", "autoloader", "Autoloads", "ave", "backquote", "backsolve", "backtick",
			"bandwidth.kernel", "bartlett.test", "bartlett.test.default", "bartlett.test.formula", "base", "base-defunct", "base-deprecated", "baseenv", "basename", "Bessel", "bessel", "besselI",
			"besselJ", "besselK", "besselY", "beta", "Beta", "BIC", "bindenv", "bindingIsActive", "bindingIsLocked", "bindtextdomain", "binom.test", "Binomial", "binomial", "biplot",
			"biplot.default", "biplot.prcomp", "biplot.princomp", "bitwAnd", "bitwNot", "bitwOr", "bitwShiftL", "bitwShiftR", "bitwXor", "body", "body<-", "Box.test", "bquote", "break", "browser",
			"browserCondition", "browserSetDebug", "browserText", "builtins", "bw.bcv", "bw.nrd", "bw.nrd0", "bw.SJ", "bw.ucv", "by", "by.data.frame", "by.default", "bzfile", "c", "C", "c.Date",
			"c.noquote", "c.numeric_version", "c.POSIXct", "c.POSIXlt", "call", "callCC", "cancor", "capabilities", "case.names", "case.names.lm", "casefold", "cat", "category", "Cauchy", "cbind",
			"cbind.data.frame", "cbind.ts", "ccf", "ceiling", "char.expand", "character", "charmatch", "charToRaw", "chartr", "check_tzones", "chisq.test", "Chisquare", "chol", "chol.default",
			"chol2inv", "choose", "class", "class<-", "clearNames", "clearPushBack", "clipboard", "close", "close.connection", "close.srcfile", "close.srcfilealias", "closeAllConnections", "closure",
			"cmdscale", "code point", "codes", "codes.factor", "codes.ordered", "codes<-", "coef", "coefficients", "col", "collation", "colMeans", "colnames", "colnames<-", "colon", "colSums",
			"commandArgs", "comment", "comment<-", "Comparison", "complete.cases", "Complex", "complex", "computeRestarts", "condition", "conditionCall", "conditionCall.condition",
			"conditionMessage", "conditionMessage.condition", "conditions", "confint", "confint.default", "confint.lm", "conflicts", "Conj", "connection", "connections", "Constants", "constrOptim",
			"contr.helmert", "contr.poly", "contr.SAS", "contr.sum", "contr.treatment", "contrasts", "contrasts<-", "contributors", "Control", "convolve", "cooks.distance", "cooks.distance.glm",
			"cooks.distance.lm", "cophenetic", "cophenetic.default", "cophenetic.dendrogram", "copyright", "copyrights", "cor", "cor.test", "cor.test.default", "cor.test.formula", "cos", "cosh",
			"cospi", "cov", "cov.wt", "cov2cor", "covratio", "cpgram", "crossprod", "Cstack_info", "cummax", "cummin", "cumprod", "cumsum", "cut", "cut.Date", "cut.default", "cut.dendrogram",
			"cut.POSIXt", "cutree", "cycle", "D", "data.class", "data.frame", "data.matrix", "data.restore", "Date", "date", "Dates", "date-time", "DateTimeClasses", "dbeta", "dbinom", "dcauchy",
			"dchisq", "debug", "debugonce", "decompose", "default.stringsAsFactors", "Defunct", "defunct", "delay", "delayedAssign", "delete.response", "deltat", "dendrapply", "dendrogram",
			"density", "density.default", "deparse", "Deprecated", "deprecated", "deriv", "deriv.default", "deriv.formula", "deriv3", "deriv3.default", "deriv3.formula", "det", "detach",
			"determinant", "determinant.matrix", "deviance", "dexp", "df", "df.kernel", "df.residual", "dfbeta", "dfbeta.lm", "dfbetas", "dfbetas.lm", "dffits", "dgamma", "dgeom", "dget", "dhyper",
			"diag", "diag<-", "diff", "diff.Date", "diff.default", "diff.POSIXt", "diff.ts", "diffinv", "diffinv.default", "diffinv.ts", "difftime", "digamma", "dim", "dim.data.frame", "dim<-",
			"dimnames", "dimnames.data.frame", "dimnames<-", "dimnames<-.data.frame", "dir", "dir.create", "dirname", "dist", "distribution", "Distributions", "distributions", "DLLInfo",
			"DLLInfoList", "DLLpath", "dlnorm", "dlogis", "dmultinom", "dnbinom", "dnchisq", "dnorm", "do.call", "dontCheck", "double", "dpois", "dput", "dQuote", "drop", "drop.scope", "drop.terms",
			"drop1", "drop1.default", "drop1.glm", "drop1.lm", "droplevels", "droplevels.data.frame", "droplevels.factor", "dsignrank", "dt", "dummy.coef", "dummy.coef.aovlist", "dummy.coef.lm",
			"dump", "dunif", "duplicated", "duplicated.array", "duplicated.data.frame", "duplicated.default", "duplicated.matrix", "duplicated.numeric_version", "duplicated.POSIXlt", "dweibull",
			"dwilcox", "dyn.load", "dyn.unload", "eapply", "ecdf", "eff.aovlist", "effects", "effects.glm", "effects.lm", "eigen", "else", "embed", "emptyenv", "enc2native", "enc2utf8", "enclosure",
			"encodeString", "Encoding", "Encoding<-", "end", "enquote", "env.profile", "environment", "environment variables", "environment<-", "environmentIsLocked", "environmentName", "Error",
			"estVar", "eval", "eval.parent", "evalq", "exists", "exp", "expand.grid", "expand.model.frame", "expm1", "Exponential", "expression", "Extract", "extractAIC", "F", "factanal", "factor",
			"factor.scope", "factorial", "FALSE", "family", "family.glm", "family.lm", "FDist", "fft", "fifo", "file", "file.access", "file.append", "file.choose", "file.copy", "file.create",
			"file.exists", "file.info", "file.link", "file.path", "file.remove", "file.rename", "file.show", "file.symlink", "files", "Filter", "filter", "finalizer", "Find", "find.package",
			"findInterval", "findPackageEnv", "findRestart", "finite", "fisher.test", "fitted", "fitted.default", "fitted.kmeans", "fitted.values", "fivenum", "fligner.test", "fligner.test.default",
			"fligner.test.formula", "floor", "flush", "flush.connection", "for", "force", "Foreign", "formals", "formals<-", "format", "format.AsIs", "format.char", "format.data.frame",
			"format.Date", "format.default", "format.difftime", "format.dist", "format.factor", "format.ftable", "format.hexmode", "format.info", "format.libraryIQR", "format.numeric_version",
			"format.octmode", "format.packageInfo", "format.POSIXct", "format.POSIXlt", "format.pval", "format.summaryDefault", "formatC", "formatDL", "formula", "formula.data.frame",
			"formula.default", "formula.formula", "formula.lm", "formula.nls", "formula.terms", "forwardsolve", "frequency", "friedman.test", "friedman.test.default", "friedman.test.formula",
			"ftable", "ftable.default", "ftable.formula", "function", "fuzzy matching", "gamma", "Gamma", "gammaCody", "GammaDist", "gaussian", "gc", "gc.time", "gcinfo", "gctorture", "gctorture2",
			"Geometric", "get", "get_all_vars", "getAllConnections", "getCall", "getCall.default", "getCallingDLL", "getCallingDLLe", "getConnection", "getDLLRegisteredRoutines",
			"getDLLRegisteredRoutines.character", "getDLLRegisteredRoutines.DLLInfo", "getElement", "getenv", "geterrmessage", "getExportedValue", "getHook", "getInitial", "getInitial.default",
			"getInitial.formula", "getInitial.selfStart", "getLoadedDLLs", "getNamespace", "getNamespaceExports", "getNamespaceImports", "getNamespaceInfo", "getNamespaceName", "getNamespaceUsers",
			"getNamespaceVersion", "getNativeSymbolInfo", "getOption", "getRversion", "getSrcLines", "getTaskCallbackNames", "gettext", "gettextf", "getwd", "gl", "glm", "glm.control", "glm.fit",
			"glm.fit.null", "globalenv", "gregexpr", "grep", "grepl", "grepRaw", "group generic", "groupGeneric", "GSC", "gsub", "gzcon", "gzfile", "hasTsp", "hat", "hatvalues", "hatvalues.lm",
			"hclust", "heatmap", "hexmode", "HoltWinters", "HOME", "httpclient", "Hypergeometric", "I", "iconv", "iconvlist", "icuSetCollate", "identical", "identify.hclust", "identity", "if",
			"ifelse", "Im", "importIntoEnv", "in", "Inf", "influence", "influence.glm", "influence.lm", "influence.measures", "inherits", "integer", "integrate", "interaction", "interaction.plot",
			"interactive", "internal generic", "InternalGenerics", "InternalMethods", "intersect", "intersection", "intToBits", "intToUtf8", "inverse.gaussian", "inverse.rle", "invisible",
			"invokeRestart", "invokeRestartInteractively", "IQR", "is.array", "is.atomic", "is.call", "is.character", "is.complex", "is.data.frame", "is.double", "is.element", "is.empty.model",
			"is.environment", "is.expression", "is.factor", "is.finite", "is.function", "is.infinite", "is.integer", "is.language", "is.leaf", "is.list", "is.loaded", "is.logical", "is.matrix",
			"is.mts", "is.na", "is.na.data.frame", "is.na.numeric_version", "is.na.POSIXlt", "is.na<-", "is.na<-.default", "is.na<-.factor", "is.name", "is.nan", "is.null", "is.numeric",
			"is.numeric.Date", "is.numeric.difftime", "is.numeric.POSIXt", "is.numeric_version", "is.object", "is.ordered", "is.package_version", "is.pairlist", "is.primitive", "is.qr", "is.R",
			"is.raw", "is.real", "is.recursive", "is.single", "is.stepfun", "is.symbol", "is.table", "is.ts", "is.tskernel", "is.unsorted", "is.vector", "isatty", "isBaseNamespace", "isdebugged",
			"isIncomplete", "isNamespace", "ISOdate", "ISOdatetime", "isOpen", "isoreg", "isRestart", "isS4", "isSeekable", "isSymmetric", "isSymmetric.matrix", "isTRUE", "jitter", "julian",
			"julian.Date", "julian.POSIXt", "KalmanForecast", "KalmanLike", "KalmanRun", "KalmanSmooth", "kappa", "kappa.default", "kappa.lm", "kappa.qr", "kernapply", "kernapply.default",
			"kernapply.ts", "kernapply.tskernel", "kernapply.vector", "kernel", "kmeans", "knots", "kronecker", "kruskal.test", "kruskal.test.default", "kruskal.test.formula", "ks.test", "ksmooth",
			"l10n_info", "La.chol", "La.chol2inv", "La.eigen", "La.svd", "La_version", "labels", "labels.default", "labels.dendrogram", "labels.dist", "labels.lm", "labels.terms", "lag",
			"lag.default", "lag.plot", "LANGUAGE", "language object", "language objects", "lapply", "last.warning", "lazyLoad", "lazyLoadDBexec", "lazyLoadDBfetch", "lbeta", "LC_ALL", "LC_COLLATE",
			"LC_CTYPE", "LC_MONETARY", "LC_NUMERIC", "LC_TIME", "lchoose", "length", "length.POSIXlt", "length<-", "length<-.factor", "LETTERS", "letters", "levels", "levels.default", "levels<-",
			"levels<-.factor", "lfactorial", "lgamma", "library", "library.dynam", "library.dynam.unload", "licence", "license", "line", "lines.isoreg", "lines.stepfun", "lines.ts", "list",
			"list.dirs", "list.files", "list2env", "lm", "lm.fit", "lm.fit.null", "lm.influence", "lm.wfit", "lm.wfit.null", "load", "loadedNamespaces", "loadingNamespaceInfo", "loadings",
			"loadNamespace", "loadURL", "local", "localeconv", "locales", "lockBinding", "lockEnvironment", "loess", "loess.control", "loess.smooth", "log", "log10", "log1p", "log2", "logb", "Logic",
			"logical", "Logistic", "logLik", "logLik.lm", "loglin", "Lognormal", "long vector", "Long vectors", "long vectors", "lookup.xport", "lower.tri", "lowess", "ls", "ls.diag", "ls.print",
			"lsfit", "Machine", "machine", "mad", "mahalanobis", "make.link", "make.names", "make.unique", "makeActiveBinding", "makeARIMA", "MAKEINDEX", "makepredictcall", "makepredictcall.default",
			"makepredictcall.poly", "manglePackageName", "manova", "mantelhaen.test", "Map", "mapply", "margin.table", "mat.or.vec", "match", "match.arg", "match.call", "match.fun", "Math",
			"Math.data.frame", "Math.Date", "Math.difftime", "Math.factor", "Math.POSIXlt", "Math.POSIXt", "matmult", "matrix", "mauchley.test", "mauchly.test", "mauchly.test.mlm",
			"mauchly.test.SSD", "max", "max.col", "mcnemar.test", "mean", "mean.Date", "mean.default", "mean.difftime", "mean.POSIXct", "mean.POSIXlt", "median", "median.default", "medpolish",
			"mem.limits", "memCompress", "memDecompress", "Memory", "memory.profile", "Memory-limits", "merge", "merge.data.frame", "merge.default", "merge.dendrogram", "message", "mget", "min",
			"missing", "Mod", "mode", "mode<-", "model.extract", "model.frame", "model.frame.aovlist", "model.frame.default", "model.frame.glm", "model.frame.lm", "model.matrix",
			"model.matrix.default", "model.matrix.lm", "model.offset", "model.response", "model.tables", "model.tables.aov", "model.tables.aovlist", "model.weights", "month.abb", "month.name",
			"monthplot", "monthplot.default", "monthplot.stl", "monthplot.StructTS", "monthplot.ts", "months", "months.Date", "months.POSIXt", "mood.test", "mood.test.default", "mood.test.formula",
			"mostattributes<-", "Multinomial", "mvfft", "NA", "na.action", "na.action.default", "na.contiguous", "na.contiguous.default", "na.exclude", "na.exclude.data.frame", "na.exclude.default",
			"na.fail", "na.fail.default", "na.omit", "na.omit.data.frame", "na.omit.default", "na.omit.ts", "na.pass", "NA_character_", "NA_complex_", "NA_integer_", "NA_real_", "name", "names",
			"names.default", "names.POSIXlt", "names<-", "names<-.default", "names<-.POSIXlt", "namespaceExport", "namespaceImport", "namespaceImportClasses", "namespaceImportFrom",
			"namespaceImportMethods", "NaN", "napredict", "napredict.default", "napredict.exclude", "naprint", "naprint.default", "naprint.exclude", "naprint.omit", "naresid", "naresid.default",
			"naresid.exclude", "nargs", "NativeSymbol", "NativeSymbolInfo", "nchar", "NCOL", "ncol", "Negate", "NegBinomial", "new.env", "next", "NextMethod", "nextn", "ngettext", "nlevels", "nlm",
			"nlminb", "nls", "nls.control", "NLSstAsymptotic", "NLSstAsymptotic.sortedXyData", "NLSstClosestX", "NLSstClosestX.sortedXyData", "NLSstLfAsymptote", "NLSstLfAsymptote.sortedXyData",
			"NLSstRtAsymptote", "NLSstRtAsymptote.sortedXyData", "nobs", "nobs.default", "noquote", "norm", "Normal", "normalizePath", "NotYetImplemented", "NotYetUsed", "NROW", "nrow", "NULL",
			"numeric", "numeric_version", "NumericConstants", "numericDeriv", "nzchar", "objects", "octmode", "offset", "oldClass", "oldClass<-", "OlsonNames", "on.exit", "oneway.test", "open",
			"open.connection", "open.srcfile", "open.srcfilealias", "open.srcfilecopy", "Ops", "Ops.data.frame", "Ops.Date", "Ops.difftime", "Ops.factor", "Ops.numeric_version", "Ops.ordered",
			"Ops.POSIXt", "Ops.ts", "optim", "optimHess", "optimise", "optimize", "option", "options", "order", "order.dendrogram", "ordered", "outer", "p.adjust", "p.adjust.methods", "pacf",
			"pacf.default", "package.description", "package_version", "packageEvent", "packageHasNamespace", "packageStartupMessage", "packBits", "pairlist", "pairwise.prop.test", "pairwise.t.test",
			"pairwise.table", "pairwise.wilcox.test", "Paren", "parent.env", "parent.env<-", "parent.frame", "parse", "parse.dcf", "parseNamespaceFile", "paste", "paste0", "path.expand",
			"path.package", "pbeta", "pbinom", "pbirthday", "pcauchy", "pchisq", "pentagamma", "pexp", "pf", "pgamma", "pgeom", "phyper", "pi", "pipe", "Platform", "plclust", "plnorm", "plogis",
			"plot.acf", "plot.decomposed.ts", "plot.dendrogram", "plot.density", "plot.ecdf", "plot.hclust", "plot.HoltWinters", "plot.isoreg", "plot.lm", "plot.mts", "plot.ppr", "plot.prcomp",
			"plot.princomp", "plot.profile.nls", "plot.spec", "plot.spec.coherency", "plot.spec.phase", "plot.stepfun", "plot.stl", "plot.ts", "plot.tskernel", "pmatch", "pmax", "pmax.int", "pmin",
			"pmin.int", "pnbinom", "pnchisq", "pnorm", "Poisson", "poisson", "poisson.test", "poly", "polym", "polyroot", "pos.to.env", "Position", "POSIXct", "POSIXlt", "POSIXt", "power",
			"power.anova.test", "power.prop.test", "power.t.test", "PP.test", "ppoints", "ppois", "ppr", "ppr.default", "ppr.formula", "prcomp", "prcomp.default", "prcomp.formula", "predict",
			"predict.ar", "predict.Arima", "predict.arima0", "predict.glm", "predict.HoltWinters", "predict.lm", "predict.loess", "predict.nls", "predict.poly", "predict.prcomp", "predict.princomp",
			"predict.smooth.spline", "predict.StructTS", "preplot", "pretty", "pretty.default", "prettyNum", "primitive", "princomp", "princomp.default", "princomp.formula", "print",
			"print.anova.glm", "print.anova.lm", "print.aov", "print.aovlist", "print.ar", "print.arima0", "print.AsIs", "print.atomic", "print.by", "print.coefmat", "print.condition",
			"print.connection", "print.data.frame", "print.Date", "print.default", "print.dendrogram", "print.difftime", "print.dist", "print.DLLInfo", "print.DLLInfoList",
			"print.DLLRegisteredRoutines", "print.ecdf", "print.factanal", "print.factor", "print.formula", "print.ftable", "print.function", "print.hclust", "print.hexmode", "print.HoltWinters",
			"print.htest", "print.integrate", "print.kmeans", "print.libraryIQR", "print.listof", "print.loadings", "print.NativeRoutineList", "print.noquote", "print.numeric_version",
			"print.octmode", "print.ordered", "print.packageInfo", "print.plot", "print.POSIXct", "print.POSIXlt", "print.power.htest", "print.prcomp", "print.princomp", "print.proc_time",
			"print.restart", "print.rle", "print.simple.list", "print.srcfile", "print.srcref", "print.stepfun", "print.StructTS", "print.summary.aov", "print.summary.aovlist", "print.summary.glm",
			"print.summary.lm", "print.summary.manova", "print.summary.nls", "print.summary.prcomp", "print.summary.princomp", "print.summary.table", "print.summaryDefault", "print.table",
			"print.tabular", "print.ts", "print.warnings", "print.xtabs", "printCoefmat", "printNoClass", "prmatrix", "proc.time", "prod", "profile", "profile.nls", "proj", "proj.aov",
			"proj.aovlist", "proj.default", "proj.lm", "promax", "promise", "promises", "prop.table", "prop.test", "prop.trend.test", "provide", "provideDimnames", "psigamma", "psignrank", "pt",
			"ptukey", "punif", "pushBack", "pushBackLength", "pweibull", "pwilcox", "q", "qbeta", "qbinom", "qbirthday", "qcauchy", "qchisq", "qexp", "qf", "qgamma", "qgeom", "qhyper", "qlnorm",
			"qlogis", "qnbinom", "qnchisq", "qnorm", "qpois", "qqline", "qqnorm", "qqnorm.default", "qqplot", "qr", "qr.coef", "qr.default", "qr.fitted", "qr.Q", "qr.qty", "qr.qy", "qr.R",
			"qr.resid", "qr.solve", "qr.X", "qsignrank", "qt", "qtukey", "quade.test", "quade.test.default", "quade.test.formula", "quantile", "quantile.default", "quantile.ecdf", "quarters",
			"quarters.Date", "quarters.POSIXt", "quasi", "quasibinomial", "quasipoisson", "quit", "qunif", "quote", "Quotes", "qweibull", "qwilcox", "R.home", "R.Version", "R.version",
			"R.version.string", "R_BATCH", "R_BROWSER", "R_COMPLETION", "R_DEFAULT_PACKAGES", "R_DOC_DIR", "R_ENVIRON", "R_ENVIRON_USER", "R_GCTORTURE", "R_GCTORTURE_INHIBIT_RELEASE",
			"R_GCTORTURE_WAIT", "R_GSCMD", "R_HISTFILE", "R_HISTSIZE", "R_HOME", "R_INCLUDE_DIR", "R_LIBS", "R_LIBS_SITE", "R_LIBS_USER", "R_PAPERSIZE", "R_PDFVIEWER", "R_PLATFORM", "R_PROFILE",
			"R_PROFILE_USER", "R_RD4PDF", "R_SHARE_DIR", "R_system_version", "R_TEXI2DVICMD", "R_UNZIPCMD", "R_USER", "R_ZIPCMD", "r2dtable", "Random", "Random.user", "range", "range.default",
			"rank", "rapply", "raw", "rawConnection", "rawConnectionValue", "rawShift", "rawToBits", "rawToChar", "rbeta", "rbind", "rbind.data.frame", "rbinom", "rcauchy", "rchisq", "rcond",
			"Rd2pdf", "Rdconv", "Re", "read.arff", "read.dbf", "read.dcf", "read.dta", "read.epiinfo", "read.ftable", "read.mtp", "read.octave", "read.S", "read.spss", "read.ssd", "read.systat",
			"read.table.url", "read.xport", "readBin", "readChar", "readline", "readLines", "readRDS", "readRenviron", "real", "Recall", "rect.hclust", "Reduce", "reformulate", "reg.finalizer",
			"regex", "regexec", "regexp", "regexpr", "RegisteredNativeSymbol", "registerS3method", "registerS3methods", "regmatches", "regmatches<-", "regular expression", "relevel",
			"relevel.default", "relevel.factor", "relevel.ordered", "remove", "removeTaskCallback", "Renviron", "Renviron.site", "reorder", "reorder.default", "reorder.dendrogram", "rep", "rep.Date",
			"rep.factor", "rep.int", "rep.numeric_version", "rep.POSIXct", "rep.POSIXlt", "rep_len", "repeat", "replace", "replicate", "replications", "require", "requireNamespace", "Reserved",
			"reserved", "reshape", "reshapeLong", "reshapeWide", "resid", "residuals", "residuals.glm", "residuals.HoltWinters", "residuals.lm", "residuals.tukeyline", "restart",
			"restartDescription", "restartFormals", "retracemem", "return", "rev", "rev.default", "rev.dendrogram", "rexp", "rf", "rgamma", "rgeom", "rhyper", "rle", "rlnorm", "rlogis", "rm",
			"rmultinom", "rnbinom", "rnchisq", "RNG", "RNGkind", "RNGversion", "rnorm", "round", "round.Date", "round.POSIXt", "row", "row.names", "row.names.data.frame", "row.names.default",
			"row.names<-", "row.names<-.data.frame", "row.names<-.default", "rowMeans", "rownames", "rownames<-", "rowsum", "rowsum.data.frame", "rowsum.default", "rowSums", "rpois", "Rprofile",
			"Rprofile.site", "rsignrank", "rstandard", "rstandard.glm", "rstandard.lm", "rstudent", "rstudent.glm", "rstudent.lm", "rt", "runif", "runmed", "rweibull", "rwilcox", "rWishart",
			"S3groupGeneric", "S3Methods", "S4", "SafePrediction", "sample", "sample.int", "sapply", "save", "save.image", "save.plot", "saveRDS", "scale", "scale.default", "scan", "scan.url",
			"scatter.smooth", "screeplot", "screeplot.default", "sd", "se.contrast", "se.contrast.aov", "se.contrast.aovlist", "search", "searchpaths", "seek", "seek.connection", "selfStart",
			"selfStart.default", "selfStart.formula", "seq", "seq.Date", "seq.default", "seq.int", "seq.POSIXt", "seq_along", "seq_len", "sequence", "serialize", "set.seed", "setdiff", "setequal",
			"setHook", "setNames", "setNamespaceInfo", "setSessionTimeLimit", "setTimeLimit", "setwd", "shapiro.test", "shell", "shell.exec", "showConnections", "shQuote", "sign", "signalCondition",
			"signif", "SignRank", "simpleCondition", "simpleError", "simpleMessage", "simpleWarning", "simplify2array", "simulate", "sin", "single", "sinh", "sink", "sink.number", "sinpi",
			"slice.index", "smooth", "smooth.spline", "smoothEnds", "socketConnection", "socketSelect", "solve", "solve.default", "solve.qr", "sort", "sort.default", "sort.int", "sort.list",
			"sort.POSIXlt", "sortedXyData", "sortedXyData.default", "source", "source.url", "spec", "spec.ar", "spec.pgram", "spec.taper", "Special", "spectrum", "spline", "splinefun", "splinefunH",
			"split", "split.data.frame", "split.Date", "split.default", "split.POSIXct", "split<-", "split<-.data.frame", "split<-.default", "sprintf", "sqrt", "sQuote", "srcfile", "srcfilealias",
			"srcfilealias-class", "srcfile-class", "srcfilecopy", "srcfilecopy-class", "srcref", "srcref-class", "SSasymp", "SSasympOff", "SSasympOrig", "SSbiexp", "SSD", "SSfol", "SSfpl",
			"SSgompertz", "SSlogis", "SSmicmen", "SSweibull", "standardGeneric", "start", "Startup", "stat.anova", "stats", "stats-defunct", "stats-deprecated", "stderr", "stdin", "stdout", "step",
			"stepfun", "stl", "stop", "stopifnot", "storage.mode", "storage.mode<-", "str.dendrogram", "str.POSIXt", "strftime", "strptime", "strsplit", "strtoi", "strtrim", "StructTS", "structure",
			"strwrap", "sub", "Subscript", "subset", "subset.data.frame", "subset.default", "subset.matrix", "substitute", "substr", "substr<-", "substring", "substring<-", "sum", "Summary",
			"summary", "summary.aov", "summary.aovlist", "summary.connection", "Summary.data.frame", "summary.data.frame", "Summary.Date", "summary.Date", "summary.default", "Summary.difftime",
			"summary.ecdf", "Summary.factor", "summary.factor", "summary.glm", "summary.lm", "summary.manova", "summary.matrix", "summary.mlm", "summary.nls", "Summary.numeric_version",
			"Summary.ordered", "Summary.POSIXct", "summary.POSIXct", "Summary.POSIXlt", "summary.POSIXlt", "summary.prcomp", "summary.princomp", "summary.proc_time", "summary.srcfile",
			"summary.srcref", "summary.stepfun", "summary.table", "suppressMessages", "suppressPackageStartupMessages", "suppressWarnings", "supsmu", "svd", "sweep", "switch", "symbol.C",
			"symbol.For", "symnum", "Syntax", "sys.call", "sys.calls", "Sys.chmod", "Sys.Date", "sys.frame", "sys.frames", "sys.function", "Sys.getenv", "Sys.getlocale", "Sys.getpid", "Sys.glob",
			"Sys.info", "Sys.junction", "sys.load.image", "Sys.localeconv", "sys.nframe", "sys.on.exit", "sys.parent", "sys.parents", "Sys.putenv", "Sys.readlink", "sys.save.image", "Sys.setenv",
			"Sys.setFileTime", "Sys.setlocale", "Sys.sleep", "sys.source", "sys.status", "Sys.time", "Sys.timezone", "Sys.umask", "Sys.unsetenv", "Sys.which", "system", "system.file", "system.test",
			"system.time", "system2", "T", "t", "t.data.frame", "t.default", "t.test", "t.test.default", "t.test.formula", "t.ts", "table", "tabulate", "tan", "tanh", "tanpi", "tapply",
			"taskCallbackManager", "tcrossprod", "TDist", "tempdir", "tempfile", "termplot", "terms", "terms.formula", "terms.object", "testPlatformEquivalence", "tetragamma", "textConnection",
			"textConnectionValue", "tilde", "tilde expansion", "time", "time interval", "time zone", "time zones", "time.default", "timezone", "timezones", "TMPDIR", "toeplitz", "tolower", "topenv",
			"toString", "toString.default", "toupper", "trace", "traceback", "tracemem", "tracingState", "transform", "transform.data.frame", "transform.default", "Trig", "trigamma", "TRUE", "trunc",
			"trunc.Date", "trunc.POSIXt", "truncate", "truncate.connection", "try", "tryCatch", "ts", "ts.intersect", "ts.plot", "ts.union", "tsdiag", "tsdiag.Arima", "tsdiag.arima0",
			"tsdiag.StructTS", "tsp", "tsp<-", "tsSmooth", "tsSmooth.StructTS", "Tukey", "TukeyHSD", "type", "typeof", "TZ", "TZDIR", "umask", "unclass", "undebug", "Unicode", "Uniform", "union",
			"unique", "unique.array", "unique.data.frame", "unique.default", "unique.matrix", "unique.numeric_version", "unique.POSIXlt", "uniroot", "units", "units.difftime", "units<-",
			"units<-.difftime", "unix", "unix.time", "unlink", "unlist", "unloadNamespace", "unlockBinding", "unname", "unserialize", "unsplit", "untrace", "untracemem", "unz", "update",
			"update.default", "update.formula", "upper.tri", "url", "UseMethod", "utf8ToInt", "vapply", "var", "var.test", "var.test.default", "var.test.formula", "variable.names",
			"variable.names.lm", "varimax", "vcov", "vcov.glm", "vcov.gls", "vcov.lm", "vcov.lme", "vcov.summary.glm", "vcov.summary.lm", "vector", "Vectorize", "Version", "version", "warning",
			"warnings", "weekdays", "weekdays.Date", "weekdays.POSIXt", "Weibull", "weighted.mean", "weighted.mean.default", "weighted.residuals", "weights", "weights.default", "weights.glm",
			"which", "which.max", "which.min", "while", "wilcox.test", "wilcox.test.default", "wilcox.test.formula", "Wilcoxon", "window", "window.default", "window.ts", "window<-", "window<-.ts",
			"with", "with.default", "withCallingHandlers", "within", "within.data.frame", "within.list", "withRestarts", "withVisible", "write", "write.arff", "write.dbf", "write.dcf", "write.dta",
			"write.foreign", "write.ftable", "write.table0", "writeBin", "writeChar", "writeLines", "xor", "xor.hexmode", "xor.octmode", "xpdrows.data.frame", "xtabs", "xtfrm", "xtfrm.AsIs",
			"xtfrm.Date", "xtfrm.default", "xtfrm.difftime", "xtfrm.factor", "xtfrm.numeric_version", "xtfrm.POSIXct", "xtfrm.POSIXlt", "xtfrm.Surv", "xzfile", "zapsmall" };

	private boolean triggerNext;

	/**
	 * We watch for angular brackets since those are often part of XML
	 * templates.
	 * 
	 * @param viewer
	 *            the viewer
	 * @param offset
	 *            the offset left of which the prefix is detected
	 * @return the detected prefix
	 */
	protected String extractPrefix(ITextViewer viewer, int offset) {
		IDocument document = viewer.getDocument();
		int i = offset;
		if (i > document.getLength())
			return "";

		try {
			while (i > 0) {
				char ch = document.getChar(i - 1);
				if (ch != '<' && !Character.isJavaIdentifierPart(ch))
					break;
				i--;
			}
			return document.get(i, offset - i);
		} catch (BadLocationException e) {
			return "";
		}
	}

	/**
	 * Cut out angular brackets for relevance sorting, since the template name
	 * does not contain the brackets.
	 * 
	 * @param template
	 *            the template
	 * @param prefix
	 *            the prefix
	 * @return the relevance of the <code>template</code> for the given
	 *         <code>prefix</code>
	 */
	protected int getRelevance(Template template, String prefix) {
		if (prefix.startsWith("<"))
			prefix = prefix.substring(1);
		if (template.getName().startsWith(prefix))
			return 90;
		return 0;
	}

	private static final class ProposalComparator implements Comparator {
		public int compare(Object o1, Object o2) {
			return ((TemplateProposal) o2).getRelevance() - ((TemplateProposal) o1).getRelevance();
		}
	}

	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {

		ITextSelection selection = (ITextSelection) viewer.getSelectionProvider().getSelection();

		// adjust offset to end of normalized selection
		if (selection.getOffset() == offset)
			offset = selection.getOffset() + selection.getLength();

		String prefix = extractPrefix(viewer, offset);
		Region region = new Region(offset - prefix.length(), prefix.length());
		TemplateContext context = createContext(viewer, region);
		if (context == null)
			return new ICompletionProposal[0];

		context.setVariable("selection", selection.getText()); // name of the selection variables {line, word}_selection //$NON-NLS-1$

		Template[] templates = getTemplates(context.getContextType().getId());
		List matches = new ArrayList();
		for (int i = 0; i < templates.length; i++) {
			Template template = templates[i];
			try {
				context.getContextType().validate(template.getPattern());
			} catch (TemplateException e) {
				continue;
			}
			if (template.matches(prefix, context.getContextType().getId())) {
				matches.add(createProposal(template, context, (IRegion) region, getRelevance(template, prefix)));
			}

		}

		/* Proposals from List! */
		// if(triggerNext){
		Template[] temp = new Template[statistics.length];

		for (int i = 0; i < temp.length; i++) {
			temp[i] = new Template(statistics[i], statisticsContext[i], context.getContextType().getId(), statistics[i], true);

		}
		for (int i = 0; i < temp.length; i++) {
			Template template = temp[i];
			try {
				context.getContextType().validate(template.getPattern());
			} catch (TemplateException e) {
				continue;
			}
			if (template.matches(prefix, context.getContextType().getId()))
				matches.add(createProposal(template, context, (IRegion) region, getRelevance(template, prefix)));

			// }

			// triggerNext=false;
		}

		Collections.sort(matches, fgProposalComparator);

		// ICompletionProposal com=new CompletionProposal();

		ICompletionProposal[] pro = (ICompletionProposal[]) matches.toArray(new ICompletionProposal[matches.size()]);

		triggerNext = true;

		return pro;

	}

	/*
	 * public static ICompletionProposal[] join(ICompletionProposal [] ...
	 * parms) { // calculate size of target array int size = 0; for
	 * (ICompletionProposal[] array : parms) { size += array.length; }
	 * 
	 * ICompletionProposal[] result = new ICompletionProposal[size];
	 * 
	 * int j = 0; for (ICompletionProposal[] array : parms) { for
	 * (ICompletionProposal s : array) { result[j++] = s; } } return result; }
	 */

	/**
	 * Simply return all templates.
	 * 
	 * @param contextTypeId
	 *            the context type, ignored in this implementation
	 * @return all templates
	 */
	protected Template[] getTemplates(String contextTypeId) {
		return TemplateEditorUI.getDefault().getTemplateStore().getTemplates();
	}

	// add the chars for Completion here !!!
	/*
	 * public char[] getCompletionProposalAutoActivationCharacters() { return
	 * new char[] { 'a','b','c','d','e','f' }; }
	 */

	/**
	 * Return the R context type that is supported by this plug-in.
	 * 
	 * @param viewer
	 *            the viewer, ignored in this implementation
	 * @param region
	 *            the region, ignored in this implementation
	 * @return the supported R context type
	 */
	protected TemplateContextType getContextType(ITextViewer viewer, IRegion region) {
		return TemplateEditorUI.getDefault().getContextTypeRegistry().getContextType(RContextType.XML_CONTEXT_TYPE);
	}

	/**
	 * Always return the default image.
	 * 
	 * @param template
	 *            the template, ignored in this implementation
	 * @return the default template image
	 */
	protected Image getImage(Template template) {
		ImageRegistry registry = TemplateEditorUI.getDefault().getImageRegistry();
		Image image = registry.get(DEFAULT_IMAGE);
		if (image == null) {
			ImageDescriptor desc = TemplateEditorUI.imageDescriptorFromPlugin("com.eco.bio7.redit", DEFAULT_IMAGE); //$NON-NLS-1$
			registry.put(DEFAULT_IMAGE, desc);
			image = registry.get(DEFAULT_IMAGE);
		}
		return image;
	}

}
