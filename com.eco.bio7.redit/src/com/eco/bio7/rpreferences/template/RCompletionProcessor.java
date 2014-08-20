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

	

	public String[] statistics = { 
			"abbreviate", "abs", "acos","acosh", "addNA", "addTaskCallback", "agrep", "agrepl", "alist", "all", "all.equal", "all.equal.character", "all.equal.default", "all.equal.factor", "all.equal.formula",
			"all.equal.language", "all.equal.list", "all.equal.numeric", "all.equal.POSIXt", "all.equal.raw", "all.names", "all.vars", "any", "anyDuplicated", "anyDuplicated.array",
			"anyDuplicated.data.frame", "anyDuplicated.default", "anyDuplicated.matrix", "anyNA", "anyNA.numeric_version", "anyNA.POSIXlt", "aperm", "aperm.default", "aperm.table", "append", "apply",
			"Arg", "args", "array", "arrayInd", "as.array", "as.array.default", "as.call", "as.character", "as.character.condition", "as.character.Date", "as.character.default", "as.character.error",
			"as.character.factor", "as.character.hexmode", "as.character.numeric_version", "as.character.octmode", "as.character.POSIXt", "as.character.srcref", "as.complex", "as.data.frame",
			"as.data.frame.array", "as.data.frame.AsIs", "as.data.frame.character", "as.data.frame.complex", "as.data.frame.data.frame", "as.data.frame.Date", "as.data.frame.default",
			"as.data.frame.difftime", "as.data.frame.factor", "as.data.frame.integer", "as.data.frame.list", "as.data.frame.logical", "as.data.frame.matrix", "as.data.frame.model.matrix",
			"as.data.frame.numeric", "as.data.frame.numeric_version", "as.data.frame.ordered", "as.data.frame.POSIXct", "as.data.frame.POSIXlt", "as.data.frame.raw", "as.data.frame.table",
			"as.data.frame.ts", "as.data.frame.vector", "as.Date", "as.Date.character", "as.Date.date", "as.Date.dates", "as.Date.default", "as.Date.factor", "as.Date.numeric", "as.Date.POSIXct",
			"as.Date.POSIXlt", "as.difftime", "as.double", "as.double.difftime", "as.double.POSIXlt", "as.environment", "as.expression", "as.expression.default", "as.factor", "as.function",
			"as.function.default", "as.hexmode", "as.integer", "as.list", "as.list.data.frame", "as.list.Date", "as.list.default", "as.list.environment", "as.list.factor", "as.list.function",
			"as.list.numeric_version", "as.list.POSIXct", "as.logical", "as.logical.factor", "as.matrix", "as.matrix.data.frame", "as.matrix.default", "as.matrix.noquote", "as.matrix.POSIXlt",
			"as.name", "as.null", "as.null.default", "as.numeric", "as.numeric_version", "as.octmode", "as.ordered", "as.package_version", "as.pairlist", "as.POSIXct", "as.POSIXct.date",
			"as.POSIXct.Date", "as.POSIXct.dates", "as.POSIXct.default", "as.POSIXct.numeric", "as.POSIXct.POSIXlt", "as.POSIXlt", "as.POSIXlt.character", "as.POSIXlt.date", "as.POSIXlt.Date",
			"as.POSIXlt.dates", "as.POSIXlt.default", "as.POSIXlt.factor", "as.POSIXlt.numeric", "as.POSIXlt.POSIXct", "as.qr", "as.raw", "as.single", "as.single.default", "as.symbol", "as.table",
			"as.table.default", "as.vector", "as.vector.factor", "asin", "asinh", "asNamespace", "asS3", "asS4", "assign", "atan", "atan2", "atanh", "attach", "attachNamespace", "attr",
			"attr.all.equal", "attr<-", "attributes", "attributes<-", "autoload", "autoloader", "backsolve", "baseenv", "basename", "besselI", "besselJ", "besselK", "besselY", "beta",
			"bindingIsActive", "bindingIsLocked", "bindtextdomain", "bitwAnd", "bitwNot", "bitwOr", "bitwShiftL", "bitwShiftR", "bitwXor", "body", "body<-", "bquote", "break", "browser",
			"browserCondition", "browserSetDebug", "browserText", "builtins", "by", "by.data.frame", "by.default", "bzfile", "c", "c.Date", "c.noquote", "c.numeric_version", "c.POSIXct", "c.POSIXlt",
			"call", "callCC", "capabilities", "casefold", "cat", "cbind", "cbind.data.frame", "ceiling", "char.expand", "character", "charmatch", "charToRaw", "chartr", "check_tzones", "chol",
			"chol.default", "chol2inv", "choose", "class", "class<-", "clearPushBack", "close", "close.connection", "close.srcfile", "close.srcfilealias", "closeAllConnections", "col", "colMeans",
			"colnames", "colnames<-", "colSums", "commandArgs", "comment", "comment<-", "complex", "computeRestarts", "conditionCall", "conditionCall.condition", "conditionMessage",
			"conditionMessage.condition", "conflicts", "Conj", "contributors", "cos", "cosh", "cospi", "crossprod", "Cstack_info", "cummax", "cummin", "cumprod", "cumsum", "cut", "cut.Date",
			"cut.default", "cut.POSIXt", "data.class", "data.frame", "data.matrix", "date", "debug", "debugonce", "default.stringsAsFactors", "delayedAssign", "deparse", "det", "detach",
			"determinant", "determinant.matrix", "dget", "diag", "diag<-", "diff", "diff.Date", "diff.default", "diff.POSIXt", "difftime", "digamma", "dim", "dim.data.frame", "dim<-", "dimnames",
			"dimnames.data.frame", "dimnames<-", "dimnames<-.data.frame", "dir", "dir.create", "dirname", "do.call", "dontCheck", "double", "dput", "dQuote", "drop", "droplevels",
			"droplevels.data.frame", "droplevels.factor", "dump", "duplicated", "duplicated.array", "duplicated.data.frame", "duplicated.default", "duplicated.matrix", "duplicated.numeric_version",
			"duplicated.POSIXlt", "dyn.load", "dyn.unload", "eapply", "eigen", "emptyenv", "enc2native", "enc2utf8", "encodeString", "Encoding", "Encoding<-", "enquote", "env.profile", "environment",
			"environment<-", "environmentIsLocked", "environmentName", "eval", "eval.parent", "evalq", "exists", "exp", "expand.grid", "expm1", "expression", "F", "factor", "factorial", "fifo",
			"file", "file.access", "file.append", "file.choose", "file.copy", "file.create", "file.exists", "file.info", "file.link", "file.path", "file.remove", "file.rename", "file.show",
			"file.symlink", "Filter", "Find", "find.package", "findInterval", "findPackageEnv", "findRestart", "floor", "flush", "flush.connection", "for", "force", "formals", "formals<-", "format",
			"format.AsIs", "format.data.frame", "format.Date", "format.default", "format.difftime", "format.factor", "format.hexmode", "format.info", "format.libraryIQR", "format.numeric_version",
			"format.octmode", "format.packageInfo", "format.POSIXct", "format.POSIXlt", "format.pval", "format.summaryDefault", "formatC", "formatDL", "forwardsolve", "function", "gamma", "gc",
			"gc.time", "gcinfo", "gctorture", "gctorture2", "get", "getAllConnections", "getCallingDLL", "getCallingDLLe", "getConnection", "getDLLRegisteredRoutines",
			"getDLLRegisteredRoutines.character", "getDLLRegisteredRoutines.DLLInfo", "getElement", "geterrmessage", "getExportedValue", "getHook", "getLoadedDLLs", "getNamespace",
			"getNamespaceExports", "getNamespaceImports", "getNamespaceInfo", "getNamespaceName", "getNamespaceUsers", "getNamespaceVersion", "getNativeSymbolInfo", "getOption", "getRversion",
			"getSrcLines", "getTaskCallbackNames", "gettext", "gettextf", "getwd", "gl", "globalenv", "gregexpr", "grep", "grepl", "grepRaw", "gsub", "gzcon", "gzfile", "I", "iconv", "iconvlist",
			"icuSetCollate", "identical", "identity", "if", "ifelse", "Im", "importIntoEnv", "inherits", "integer", "interaction", "interactive", "intersect", "intToBits", "intToUtf8", "inverse.rle",
			"invisible", "invokeRestart", "invokeRestartInteractively", "is.array", "is.atomic", "is.call", "is.character", "is.complex", "is.data.frame", "is.double", "is.element", "is.environment",
			"is.expression", "is.factor", "is.finite", "is.function", "is.infinite", "is.integer", "is.language", "is.list", "is.loaded", "is.logical", "is.matrix", "is.na", "is.na.data.frame",
			"is.na.numeric_version", "is.na.POSIXlt", "is.na<-", "is.na<-.default", "is.na<-.factor", "is.name", "is.nan", "is.null", "is.numeric", "is.numeric.Date", "is.numeric.difftime",
			"is.numeric.POSIXt", "is.numeric_version", "is.object", "is.ordered", "is.package_version", "is.pairlist", "is.primitive", "is.qr", "is.R", "is.raw", "is.recursive", "is.single",
			"is.symbol", "is.table", "is.unsorted", "is.vector", "isatty", "isBaseNamespace", "isdebugged", "isIncomplete", "isNamespace", "ISOdate", "ISOdatetime", "isOpen", "isRestart", "isS4",
			"isSeekable", "isSymmetric", "isSymmetric.matrix", "isTRUE", "jitter", "julian", "julian.Date", "julian.POSIXt", "kappa", "kappa.default", "kappa.lm", "kappa.qr", "kronecker",
			"l10n_info", "La.svd", "La_version", "labels", "labels.default", "lapply", "lazyLoad", "lazyLoadDBexec", "lazyLoadDBfetch", "lbeta", "lchoose", "length", "length.POSIXlt", "length<-",
			"length<-.factor", "letters", "LETTERS", "levels", "levels.default", "levels<-", "levels<-.factor", "lfactorial", "lgamma", "library", "library.dynam", "library.dynam.unload", "licence",
			"license", "list", "list.dirs", "list.files", "list2env", "load", "loadedNamespaces", "loadingNamespaceInfo", "loadNamespace", "local", "lockBinding", "lockEnvironment", "log", "log10",
			"log1p", "log2", "logb", "logical", "lower.tri", "ls", "make.names", "make.unique", "makeActiveBinding", "Map", "mapply", "margin.table", "mat.or.vec", "match", "match.arg", "match.call",
			"match.fun", "Math.data.frame", "Math.Date", "Math.difftime", "Math.factor", "Math.POSIXt", "matrix", "max", "max.col", "mean", "mean.Date", "mean.default", "mean.difftime",
			"mean.POSIXct", "mean.POSIXlt", "mem.limits", "memCompress", "memDecompress", "memory.profile", "merge", "merge.data.frame", "merge.default", "message", "mget", "min", "missing", "Mod",
			"mode", "mode<-", "month.abb", "month.name", "months", "months.Date", "months.POSIXt", "mostattributes<-", "names", "names.POSIXlt", "names<-", "names<-.POSIXlt", "namespaceExport",
			"namespaceImport", "namespaceImportClasses", "namespaceImportFrom", "namespaceImportMethods", "nargs", "nchar", "ncol", "NCOL", "Negate", "new.env", "next", "NextMethod", "ngettext",
			"nlevels", "noquote", "norm", "normalizePath", "nrow", "NROW", "numeric", "numeric_version", "nzchar", "objects", "oldClass", "oldClass<-", "OlsonNames", "on.exit", "open",
			"open.connection", "open.srcfile", "open.srcfilealias", "open.srcfilecopy", "Ops.data.frame", "Ops.Date", "Ops.difftime", "Ops.factor", "Ops.numeric_version", "Ops.ordered", "Ops.POSIXt",
			"options", "order", "ordered", "outer", "package_version", "packageEvent", "packageHasNamespace", "packageStartupMessage", "packBits", "pairlist", "parent.env", "parent.env<-",
			"parent.frame", "parse", "parseNamespaceFile", "paste", "paste0", "path.expand", "path.package", "pi", "pipe", "pmatch", "pmax", "pmax.int", "pmin", "pmin.int", "polyroot", "pos.to.env",
			"Position", "pretty", "pretty.default", "prettyNum", "print", "print.AsIs", "print.by", "print.condition", "print.connection", "print.data.frame", "print.Date", "print.default",
			"print.difftime", "print.DLLInfo", "print.DLLInfoList", "print.DLLRegisteredRoutines", "print.factor", "print.function", "print.hexmode", "print.libraryIQR", "print.listof",
			"print.NativeRoutineList", "print.noquote", "print.numeric_version", "print.octmode", "print.packageInfo", "print.POSIXct", "print.POSIXlt", "print.proc_time", "print.restart",
			"print.rle", "print.simple.list", "print.srcfile", "print.srcref", "print.summary.table", "print.summaryDefault", "print.table", "print.warnings", "prmatrix", "proc.time", "prod",
			"prop.table", "provideDimnames", "psigamma", "pushBack", "pushBackLength", "q", "qr", "qr.coef", "qr.default", "qr.fitted", "qr.Q", "qr.qty", "qr.qy", "qr.R", "qr.resid", "qr.solve",
			"qr.X", "quarters", "quarters.Date", "quarters.POSIXt", "quit", "quote", "R.home", "R.version", "R.Version", "R.version.string", "R_system_version", "range", "range.default", "rank",
			"rapply", "raw", "rawConnection", "rawConnectionValue", "rawShift", "rawToBits", "rawToChar", "rbind", "rbind.data.frame", "rcond", "Re", "read.dcf", "readBin", "readChar", "readline",
			"readLines", "readRDS", "readRenviron", "Recall", "Reduce", "reg.finalizer", "regexec", "regexpr", "registerS3method", "registerS3methods", "regmatches", "regmatches<-", "remove",
			"removeTaskCallback", "rep", "rep.Date", "rep.factor", "rep.int", "rep.numeric_version", "rep.POSIXct", "rep.POSIXlt", "rep_len", "repeat", "replace", "replicate", "require",
			"requireNamespace", "restartDescription", "restartFormals", "retracemem", "return", "rev", "rev.default", "rle", "rm", "RNGkind", "RNGversion", "round", "round.Date", "round.POSIXt",
			"row", "row.names", "row.names.data.frame", "row.names.default", "row.names<-", "row.names<-.data.frame", "row.names<-.default", "rowMeans", "rownames", "rownames<-", "rowsum",
			"rowsum.data.frame", "rowsum.default", "rowSums", "sample", "sample.int", "sapply", "save", "save.image", "saveRDS", "scale", "scale.default", "scan", "search", "searchpaths", "seek",
			"seek.connection", "seq", "seq.Date", "seq.default", "seq.int", "seq.POSIXt", "seq_along", "seq_len", "sequence", "serialize", "set.seed", "setdiff", "setequal", "setHook",
			"setNamespaceInfo", "setSessionTimeLimit", "setTimeLimit", "setwd", "shell", "shell.exec", "showConnections", "shQuote", "sign", "signalCondition", "signif", "simpleCondition",
			"simpleError", "simpleMessage", "simpleWarning", "simplify2array", "sin", "single", "sinh", "sink", "sink.number", "sinpi", "slice.index", "socketConnection", "socketSelect", "solve",
			"solve.default", "solve.qr", "sort", "sort.default", "sort.int", "sort.list", "sort.POSIXlt", "source", "split", "split.data.frame", "split.Date", "split.default", "split.POSIXct",
			"split<-", "split<-.data.frame", "split<-.default", "sprintf", "sqrt", "sQuote", "srcfile", "srcfilealias", "srcfilecopy", "srcref", "standardGeneric", "stderr", "stdin", "stdout",
			"stop", "stopifnot", "storage.mode", "storage.mode<-", "strftime", "strptime", "strsplit", "strtoi", "strtrim", "structure", "strwrap", "sub", "subset", "subset.data.frame",
			"subset.default", "subset.matrix", "substitute", "substr", "substr<-", "substring", "substring<-", "sum", "summary", "summary.connection", "summary.data.frame", "Summary.data.frame",
			"summary.Date", "Summary.Date", "summary.default", "Summary.difftime", "summary.factor", "Summary.factor", "summary.matrix", "Summary.numeric_version", "Summary.ordered",
			"summary.POSIXct", "Summary.POSIXct", "summary.POSIXlt", "Summary.POSIXlt", "summary.proc_time", "summary.srcfile", "summary.srcref", "summary.table", "suppressMessages",
			"suppressPackageStartupMessages", "suppressWarnings", "svd", "sweep", "switch", "sys.call", "sys.calls", "Sys.chmod", "Sys.Date", "sys.frame", "sys.frames", "sys.function", "Sys.getenv",
			"Sys.getlocale", "Sys.getpid", "Sys.glob", "Sys.info", "Sys.junction", "sys.load.image", "Sys.localeconv", "sys.nframe", "sys.on.exit", "sys.parent", "sys.parents", "Sys.readlink",
			"sys.save.image", "Sys.setenv", "Sys.setFileTime", "Sys.setlocale", "Sys.sleep", "sys.source", "sys.status", "Sys.time", "Sys.timezone", "Sys.umask", "Sys.unsetenv", "Sys.which",
			"system", "system.file", "system.time", "system2", "t", "T", "t.data.frame", "t.default", "table", "tabulate", "tan", "tanh", "tanpi", "tapply", "taskCallbackManager", "tcrossprod",
			"tempdir", "tempfile", "testPlatformEquivalence", "textConnection", "textConnectionValue", "tolower", "topenv", "toString", "toString.default", "toupper", "trace", "traceback",
			"tracemem", "tracingState", "transform", "transform.data.frame", "transform.default", "trigamma", "trunc", "trunc.Date", "trunc.POSIXt", "truncate", "truncate.connection", "try",
			"tryCatch", "typeof", "unclass", "undebug", "union", "unique", "unique.array", "unique.data.frame", "unique.default", "unique.matrix", "unique.numeric_version", "unique.POSIXlt", "units",
			"units.difftime", "units<-", "units<-.difftime", "unix.time", "unlink", "unlist", "unloadNamespace", "unlockBinding", "unname", "unserialize", "unsplit", "untrace", "untracemem", "unz",
			"upper.tri", "url", "UseMethod", "utf8ToInt", "vapply", "vector", "Vectorize", "version", "warning", "warnings", "weekdays", "weekdays.Date", "weekdays.POSIXt", "which", "which.max",
			"which.min", "while", "with", "with.default", "withCallingHandlers", "within", "within.data.frame", "within.list", "withRestarts", "withVisible", "write", "write.dcf", "writeBin",
			"writeChar", "writeLines", "xor", "xor.hexmode", "xor.octmode", "xpdrows.data.frame", "xtfrm", "xtfrm.AsIs", "xtfrm.Date", "xtfrm.default", "xtfrm.difftime", "xtfrm.factor",
			"xtfrm.numeric_version", "xtfrm.POSIXct", "xtfrm.POSIXlt", "xtfrm.Surv", "xzfile", "zapsmall" };

	public String[] statisticsContext = {
			"abbreviate", "abs", "acos", "acosh", "addNA", "addTaskCallback", "agrep", "agrepl", "alist", "all", "all.equal", "all.equal.character", "all.equal.default", "all.equal.factor",
			"all.equal.formula", "all.equal.language", "all.equal.list", "all.equal.numeric", "all.equal.POSIXt", "all.equal.raw", "all.names", "all.vars", "any", "anyDuplicated",
			"anyDuplicated.array", "anyDuplicated.data.frame", "anyDuplicated.default", "anyDuplicated.matrix", "anyNA", "anyNA.numeric_version", "anyNA.POSIXlt", "aperm", "aperm.default",
			"aperm.table", "append", "apply", "Arg", "args", "array", "arrayInd", "as.array", "as.array.default", "as.call", "as.character", "as.character.condition", "as.character.Date",
			"as.character.default", "as.character.error", "as.character.factor", "as.character.hexmode", "as.character.numeric_version", "as.character.octmode", "as.character.POSIXt",
			"as.character.srcref", "as.complex", "as.data.frame", "as.data.frame.array", "as.data.frame.AsIs", "as.data.frame.character", "as.data.frame.complex", "as.data.frame.data.frame",
			"as.data.frame.Date", "as.data.frame.default", "as.data.frame.difftime", "as.data.frame.factor", "as.data.frame.integer", "as.data.frame.list", "as.data.frame.logical",
			"as.data.frame.matrix", "as.data.frame.model.matrix", "as.data.frame.numeric", "as.data.frame.numeric_version", "as.data.frame.ordered", "as.data.frame.POSIXct", "as.data.frame.POSIXlt",
			"as.data.frame.raw", "as.data.frame.table", "as.data.frame.ts", "as.data.frame.vector", "as.Date", "as.Date.character", "as.Date.date", "as.Date.dates", "as.Date.default",
			"as.Date.factor", "as.Date.numeric", "as.Date.POSIXct", "as.Date.POSIXlt", "as.difftime", "as.double", "as.double.difftime", "as.double.POSIXlt", "as.environment", "as.expression",
			"as.expression.default", "as.factor", "as.function", "as.function.default", "as.hexmode", "as.integer", "as.list", "as.list.data.frame", "as.list.Date", "as.list.default",
			"as.list.environment", "as.list.factor", "as.list.function", "as.list.numeric_version", "as.list.POSIXct", "as.logical", "as.logical.factor", "as.matrix", "as.matrix.data.frame",
			"as.matrix.default", "as.matrix.noquote", "as.matrix.POSIXlt", "as.name", "as.null", "as.null.default", "as.numeric", "as.numeric_version", "as.octmode", "as.ordered",
			"as.package_version", "as.pairlist", "as.POSIXct", "as.POSIXct.date", "as.POSIXct.Date", "as.POSIXct.dates", "as.POSIXct.default", "as.POSIXct.numeric", "as.POSIXct.POSIXlt",
			"as.POSIXlt", "as.POSIXlt.character", "as.POSIXlt.date", "as.POSIXlt.Date", "as.POSIXlt.dates", "as.POSIXlt.default", "as.POSIXlt.factor", "as.POSIXlt.numeric", "as.POSIXlt.POSIXct",
			"as.qr", "as.raw", "as.single", "as.single.default", "as.symbol", "as.table", "as.table.default", "as.vector", "as.vector.factor", "asin", "asinh", "asNamespace", "asS3", "asS4",
			"assign", "atan", "atan2", "atanh", "attach", "attachNamespace", "attr", "attr.all.equal", "attr<-", "attributes", "attributes<-", "autoload", "autoloader", "backsolve", "baseenv",
			"basename", "besselI", "besselJ", "besselK", "besselY", "beta", "bindingIsActive", "bindingIsLocked", "bindtextdomain", "bitwAnd", "bitwNot", "bitwOr", "bitwShiftL", "bitwShiftR",
			"bitwXor", "body", "body<-", "bquote", "break", "browser", "browserCondition", "browserSetDebug", "browserText", "builtins", "by", "by.data.frame", "by.default", "bzfile", "c", "c.Date",
			"c.noquote", "c.numeric_version", "c.POSIXct", "c.POSIXlt", "call", "callCC", "capabilities", "casefold", "cat", "cbind", "cbind.data.frame", "ceiling", "char.expand", "character",
			"charmatch", "charToRaw", "chartr", "check_tzones", "chol", "chol.default", "chol2inv", "choose", "class", "class<-", "clearPushBack", "close", "close.connection", "close.srcfile",
			"close.srcfilealias", "closeAllConnections", "col", "colMeans", "colnames", "colnames<-", "colSums", "commandArgs", "comment", "comment<-", "complex", "computeRestarts", "conditionCall",
			"conditionCall.condition", "conditionMessage", "conditionMessage.condition", "conflicts", "Conj", "contributors", "cos", "cosh", "cospi", "crossprod", "Cstack_info", "cummax", "cummin",
			"cumprod", "cumsum", "cut", "cut.Date", "cut.default", "cut.POSIXt", "data.class", "data.frame", "data.matrix", "date", "debug", "debugonce", "default.stringsAsFactors", "delayedAssign",
			"deparse", "det", "detach", "determinant", "determinant.matrix", "dget", "diag", "diag<-", "diff", "diff.Date", "diff.default", "diff.POSIXt", "difftime", "digamma", "dim",
			"dim.data.frame", "dim<-", "dimnames", "dimnames.data.frame", "dimnames<-", "dimnames<-.data.frame", "dir", "dir.create", "dirname", "do.call", "dontCheck", "double", "dput", "dQuote",
			"drop", "droplevels", "droplevels.data.frame", "droplevels.factor", "dump", "duplicated", "duplicated.array", "duplicated.data.frame", "duplicated.default", "duplicated.matrix",
			"duplicated.numeric_version", "duplicated.POSIXlt", "dyn.load", "dyn.unload", "eapply", "eigen", "emptyenv", "enc2native", "enc2utf8", "encodeString", "Encoding", "Encoding<-", "enquote",
			"env.profile", "environment", "environment<-", "environmentIsLocked", "environmentName", "eval", "eval.parent", "evalq", "exists", "exp", "expand.grid", "expm1", "expression", "F",
			"factor", "factorial", "fifo", "file", "file.access", "file.append", "file.choose", "file.copy", "file.create", "file.exists", "file.info", "file.link", "file.path", "file.remove",
			"file.rename", "file.show", "file.symlink", "Filter", "Find", "find.package", "findInterval", "findPackageEnv", "findRestart", "floor", "flush", "flush.connection", "for", "force",
			"formals", "formals<-", "format", "format.AsIs", "format.data.frame", "format.Date", "format.default", "format.difftime", "format.factor", "format.hexmode", "format.info",
			"format.libraryIQR", "format.numeric_version", "format.octmode", "format.packageInfo", "format.POSIXct", "format.POSIXlt", "format.pval", "format.summaryDefault", "formatC", "formatDL",
			"forwardsolve", "function", "gamma", "gc", "gc.time", "gcinfo", "gctorture", "gctorture2", "get", "getAllConnections", "getCallingDLL", "getCallingDLLe", "getConnection",
			"getDLLRegisteredRoutines", "getDLLRegisteredRoutines.character", "getDLLRegisteredRoutines.DLLInfo", "getElement", "geterrmessage", "getExportedValue", "getHook", "getLoadedDLLs",
			"getNamespace", "getNamespaceExports", "getNamespaceImports", "getNamespaceInfo", "getNamespaceName", "getNamespaceUsers", "getNamespaceVersion", "getNativeSymbolInfo", "getOption",
			"getRversion", "getSrcLines", "getTaskCallbackNames", "gettext", "gettextf", "getwd", "gl", "globalenv", "gregexpr", "grep", "grepl", "grepRaw", "gsub", "gzcon", "gzfile", "I", "iconv",
			"iconvlist", "icuSetCollate", "identical", "identity", "if", "ifelse", "Im", "importIntoEnv", "inherits", "integer", "interaction", "interactive", "intersect", "intToBits", "intToUtf8",
			"inverse.rle", "invisible", "invokeRestart", "invokeRestartInteractively", "is.array", "is.atomic", "is.call", "is.character", "is.complex", "is.data.frame", "is.double", "is.element",
			"is.environment", "is.expression", "is.factor", "is.finite", "is.function", "is.infinite", "is.integer", "is.language", "is.list", "is.loaded", "is.logical", "is.matrix", "is.na",
			"is.na.data.frame", "is.na.numeric_version", "is.na.POSIXlt", "is.na<-", "is.na<-.default", "is.na<-.factor", "is.name", "is.nan", "is.null", "is.numeric", "is.numeric.Date",
			"is.numeric.difftime", "is.numeric.POSIXt", "is.numeric_version", "is.object", "is.ordered", "is.package_version", "is.pairlist", "is.primitive", "is.qr", "is.R", "is.raw",
			"is.recursive", "is.single", "is.symbol", "is.table", "is.unsorted", "is.vector", "isatty", "isBaseNamespace", "isdebugged", "isIncomplete", "isNamespace", "ISOdate", "ISOdatetime",
			"isOpen", "isRestart", "isS4", "isSeekable", "isSymmetric", "isSymmetric.matrix", "isTRUE", "jitter", "julian", "julian.Date", "julian.POSIXt", "kappa", "kappa.default", "kappa.lm",
			"kappa.qr", "kronecker", "l10n_info", "La.svd", "La_version", "labels", "labels.default", "lapply", "lazyLoad", "lazyLoadDBexec", "lazyLoadDBfetch", "lbeta", "lchoose", "length",
			"length.POSIXlt", "length<-", "length<-.factor", "letters", "LETTERS", "levels", "levels.default", "levels<-", "levels<-.factor", "lfactorial", "lgamma", "library", "library.dynam",
			"library.dynam.unload", "licence", "license", "list", "list.dirs", "list.files", "list2env", "load", "loadedNamespaces", "loadingNamespaceInfo", "loadNamespace", "local", "lockBinding",
			"lockEnvironment", "log", "log10", "log1p", "log2", "logb", "logical", "lower.tri", "ls", "make.names", "make.unique", "makeActiveBinding", "Map", "mapply", "margin.table", "mat.or.vec",
			"match", "match.arg", "match.call", "match.fun", "Math.data.frame", "Math.Date", "Math.difftime", "Math.factor", "Math.POSIXt", "matrix", "max", "max.col", "mean", "mean.Date",
			"mean.default", "mean.difftime", "mean.POSIXct", "mean.POSIXlt", "mem.limits", "memCompress", "memDecompress", "memory.profile", "merge", "merge.data.frame", "merge.default", "message",
			"mget", "min", "missing", "Mod", "mode", "mode<-", "month.abb", "month.name", "months", "months.Date", "months.POSIXt", "mostattributes<-", "names", "names.POSIXlt", "names<-",
			"names<-.POSIXlt", "namespaceExport", "namespaceImport", "namespaceImportClasses", "namespaceImportFrom", "namespaceImportMethods", "nargs", "nchar", "ncol", "NCOL", "Negate", "new.env",
			"next", "NextMethod", "ngettext", "nlevels", "noquote", "norm", "normalizePath", "nrow", "NROW", "numeric", "numeric_version", "nzchar", "objects", "oldClass", "oldClass<-", "OlsonNames",
			"on.exit", "open", "open.connection", "open.srcfile", "open.srcfilealias", "open.srcfilecopy", "Ops.data.frame", "Ops.Date", "Ops.difftime", "Ops.factor", "Ops.numeric_version",
			"Ops.ordered", "Ops.POSIXt", "options", "order", "ordered", "outer", "package_version", "packageEvent", "packageHasNamespace", "packageStartupMessage", "packBits", "pairlist",
			"parent.env", "parent.env<-", "parent.frame", "parse", "parseNamespaceFile", "paste", "paste0", "path.expand", "path.package", "pi", "pipe", "pmatch", "pmax", "pmax.int", "pmin",
			"pmin.int", "polyroot", "pos.to.env", "Position", "pretty", "pretty.default", "prettyNum", "print", "print.AsIs", "print.by", "print.condition", "print.connection", "print.data.frame",
			"print.Date", "print.default", "print.difftime", "print.DLLInfo", "print.DLLInfoList", "print.DLLRegisteredRoutines", "print.factor", "print.function", "print.hexmode",
			"print.libraryIQR", "print.listof", "print.NativeRoutineList", "print.noquote", "print.numeric_version", "print.octmode", "print.packageInfo", "print.POSIXct", "print.POSIXlt",
			"print.proc_time", "print.restart", "print.rle", "print.simple.list", "print.srcfile", "print.srcref", "print.summary.table", "print.summaryDefault", "print.table", "print.warnings",
			"prmatrix", "proc.time", "prod", "prop.table", "provideDimnames", "psigamma", "pushBack", "pushBackLength", "q", "qr", "qr.coef", "qr.default", "qr.fitted", "qr.Q", "qr.qty", "qr.qy",
			"qr.R", "qr.resid", "qr.solve", "qr.X", "quarters", "quarters.Date", "quarters.POSIXt", "quit", "quote", "R.home", "R.version", "R.Version", "R.version.string", "R_system_version",
			"range", "range.default", "rank", "rapply", "raw", "rawConnection", "rawConnectionValue", "rawShift", "rawToBits", "rawToChar", "rbind", "rbind.data.frame", "rcond", "Re", "read.dcf",
			"readBin", "readChar", "readline", "readLines", "readRDS", "readRenviron", "Recall", "Reduce", "reg.finalizer", "regexec", "regexpr", "registerS3method", "registerS3methods",
			"regmatches", "regmatches<-", "remove", "removeTaskCallback", "rep", "rep.Date", "rep.factor", "rep.int", "rep.numeric_version", "rep.POSIXct", "rep.POSIXlt", "rep_len", "repeat",
			"replace", "replicate", "require", "requireNamespace", "restartDescription", "restartFormals", "retracemem", "return", "rev", "rev.default", "rle", "rm", "RNGkind", "RNGversion", "round",
			"round.Date", "round.POSIXt", "row", "row.names", "row.names.data.frame", "row.names.default", "row.names<-", "row.names<-.data.frame", "row.names<-.default", "rowMeans", "rownames",
			"rownames<-", "rowsum", "rowsum.data.frame", "rowsum.default", "rowSums", "sample", "sample.int", "sapply", "save", "save.image", "saveRDS", "scale", "scale.default", "scan", "search",
			"searchpaths", "seek", "seek.connection", "seq", "seq.Date", "seq.default", "seq.int", "seq.POSIXt", "seq_along", "seq_len", "sequence", "serialize", "set.seed", "setdiff", "setequal",
			"setHook", "setNamespaceInfo", "setSessionTimeLimit", "setTimeLimit", "setwd", "shell", "shell.exec", "showConnections", "shQuote", "sign", "signalCondition", "signif", "simpleCondition",
			"simpleError", "simpleMessage", "simpleWarning", "simplify2array", "sin", "single", "sinh", "sink", "sink.number", "sinpi", "slice.index", "socketConnection", "socketSelect", "solve",
			"solve.default", "solve.qr", "sort", "sort.default", "sort.int", "sort.list", "sort.POSIXlt", "source", "split", "split.data.frame", "split.Date", "split.default", "split.POSIXct",
			"split<-", "split<-.data.frame", "split<-.default", "sprintf", "sqrt", "sQuote", "srcfile", "srcfilealias", "srcfilecopy", "srcref", "standardGeneric", "stderr", "stdin", "stdout",
			"stop", "stopifnot", "storage.mode", "storage.mode<-", "strftime", "strptime", "strsplit", "strtoi", "strtrim", "structure", "strwrap", "sub", "subset", "subset.data.frame",
			"subset.default", "subset.matrix", "substitute", "substr", "substr<-", "substring", "substring<-", "sum", "summary", "summary.connection", "summary.data.frame", "Summary.data.frame",
			"summary.Date", "Summary.Date", "summary.default", "Summary.difftime", "summary.factor", "Summary.factor", "summary.matrix", "Summary.numeric_version", "Summary.ordered",
			"summary.POSIXct", "Summary.POSIXct", "summary.POSIXlt", "Summary.POSIXlt", "summary.proc_time", "summary.srcfile", "summary.srcref", "summary.table", "suppressMessages",
			"suppressPackageStartupMessages", "suppressWarnings", "svd", "sweep", "switch", "sys.call", "sys.calls", "Sys.chmod", "Sys.Date", "sys.frame", "sys.frames", "sys.function", "Sys.getenv",
			"Sys.getlocale", "Sys.getpid", "Sys.glob", "Sys.info", "Sys.junction", "sys.load.image", "Sys.localeconv", "sys.nframe", "sys.on.exit", "sys.parent", "sys.parents", "Sys.readlink",
			"sys.save.image", "Sys.setenv", "Sys.setFileTime", "Sys.setlocale", "Sys.sleep", "sys.source", "sys.status", "Sys.time", "Sys.timezone", "Sys.umask", "Sys.unsetenv", "Sys.which",
			"system", "system.file", "system.time", "system2", "t", "T", "t.data.frame", "t.default", "table", "tabulate", "tan", "tanh", "tanpi", "tapply", "taskCallbackManager", "tcrossprod",
			"tempdir", "tempfile", "testPlatformEquivalence", "textConnection", "textConnectionValue", "tolower", "topenv", "toString", "toString.default", "toupper", "trace", "traceback",
			"tracemem", "tracingState", "transform", "transform.data.frame", "transform.default", "trigamma", "trunc", "trunc.Date", "trunc.POSIXt", "truncate", "truncate.connection", "try",
			"tryCatch", "typeof", "unclass", "undebug", "union", "unique", "unique.array", "unique.data.frame", "unique.default", "unique.matrix", "unique.numeric_version", "unique.POSIXlt", "units",
			"units.difftime", "units<-", "units<-.difftime", "unix.time", "unlink", "unlist", "unloadNamespace", "unlockBinding", "unname", "unserialize", "unsplit", "untrace", "untracemem", "unz",
			"upper.tri", "url", "UseMethod", "utf8ToInt", "vapply", "vector", "Vectorize", "version", "warning", "warnings", "weekdays", "weekdays.Date", "weekdays.POSIXt", "which", "which.max",
			"which.min", "while", "with", "with.default", "withCallingHandlers", "within", "within.data.frame", "within.list", "withRestarts", "withVisible", "write", "write.dcf", "writeBin",
			"writeChar", "writeLines", "xor", "xor.hexmode", "xor.octmode", "xpdrows.data.frame", "xtfrm", "xtfrm.AsIs", "xtfrm.Date", "xtfrm.default", "xtfrm.difftime", "xtfrm.factor",
			"xtfrm.numeric_version", "xtfrm.POSIXct", "xtfrm.POSIXlt", "xtfrm.Surv", "xzfile", "zapsmall" };

	

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
			if (template.matches(prefix, context.getContextType().getId())){
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
	 * Return the XML context type that is supported by this plug-in.
	 * 
	 * @param viewer
	 *            the viewer, ignored in this implementation
	 * @param region
	 *            the region, ignored in this implementation
	 * @return the supported XML context type
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
