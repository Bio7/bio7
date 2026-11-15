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
package com.eco.bio7.reditor.antlr;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.misc.ParseCancellationException;

/**
 * Parses R code in two stages: first with the fast SLL prediction (stops at the
 * first error), then, only if that failed, with the full LL prediction and the
 * error messages of the editor. A successful SLL parse gives the same tree as
 * LL; it fails on real errors and on some constructs which need the full
 * prediction (e.g., if ... else in braces).
 */
public final class TwoStageParse {

	private TwoStageParse() {
	}

	/**
	 * Parses the program. Afterwards the listener is attached to the parser (the
	 * tree listeners report their warnings to it) and the error strategy of the
	 * editor is set.
	 *
	 * @param parser   a new parser of the tokens (no error listeners needed)
	 * @param tokens   the tokens (at the start)
	 * @param listener the listener of the errors and warnings
	 */
	public static RParser.ProgContext prog(RParser parser, CommonTokenStream tokens, ANTLRErrorListener listener) {
		parser.removeErrorListeners();
		parser.setErrorHandler(new BailErrorStrategy());
		parser.getInterpreter().setPredictionMode(PredictionMode.SLL);
		RParser.ProgContext tree;
		try {
			tree = parser.prog();
		} catch (ParseCancellationException e) {
			/* An error or a construct which needs the full prediction: parse again */
			tokens.seek(0);
			parser.reset();
			parser.setErrorHandler(new RErrorStrategy());
			parser.getInterpreter().setPredictionMode(PredictionMode.LL);
			parser.addErrorListener(listener);
			return parser.prog();
		}
		parser.setErrorHandler(new RErrorStrategy());
		parser.getInterpreter().setPredictionMode(PredictionMode.LL);
		parser.addErrorListener(listener);
		return tree;
	}
}
