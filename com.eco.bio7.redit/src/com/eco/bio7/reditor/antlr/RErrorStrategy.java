package com.eco.bio7.reditor.antlr;

import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.FailedPredicateException;
import org.antlr.v4.runtime.InputMismatchException;
import org.antlr.v4.runtime.NoViableAltException;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.misc.IntervalSet;
import org.antlr.v4.runtime.misc.NotNull;

public class RErrorStrategy extends DefaultErrorStrategy {

	public void reportError(Parser recognizer, RecognitionException e) {
		// if we've already reported an error and have not matched a token
		// yet successfully, don't report any errors.
		if (inErrorRecoveryMode(recognizer)) {
			// System.err.print("[SPURIOUS] ");
			return; // don't report spurious errors
		}
		beginErrorCondition(recognizer);
		if (e instanceof NoViableAltException) {
			reportNoViableAlternative(recognizer, (NoViableAltException) e);
		} else if (e instanceof InputMismatchException) {
			reportInputMismatch(recognizer, (InputMismatchException) e);
		} else if (e instanceof FailedPredicateException) {
			reportFailedPredicate(recognizer, (FailedPredicateException) e);
		} else {
			System.err.println("Bio7: unknown recognition error type: " + e.getClass().getName());
			recognizer.notifyErrorListeners(e.getOffendingToken(), e.getMessage(), e);
		}
	}

	/**
	 * This is called by {@link #reportError} when the exception is a
	 * {@link NoViableAltException}.
	 *
	 * @see #reportError
	 *
	 * @param recognizer
	 *            the parser instance
	 * @param e
	 *            the recognition exception
	 */
	protected void reportNoViableAlternative(@NotNull Parser recognizer, @NotNull NoViableAltException e) {
		TokenStream tokens = recognizer.getInputStream();
		String input;
		if (tokens != null) {
			if (e.getStartToken().getType() == Token.EOF)
				input = "<EOF>";
			else
				input = tokens.getText(e.getStartToken(), e.getOffendingToken());
		} else {
			input = "<unknown input>";
		}
		String msg = "Bio7 no viable alternative at input " + escapeWSAndQuote(input);
		recognizer.notifyErrorListeners(e.getOffendingToken(), msg, e);
	}

	/**
	 * This is called by {@link #reportError} when the exception is an
	 * {@link InputMismatchException}.
	 *
	 * @see #reportError
	 *
	 * @param recognizer
	 *            the parser instance
	 * @param e
	 *            the recognition exception
	 */
	protected void reportInputMismatch(@NotNull Parser recognizer, @NotNull InputMismatchException e) {
		String msg = "Bio7 mismatched input " + getTokenErrorDisplay(e.getOffendingToken()) + " expecting the following symbols: " + "\n"+e.getExpectedTokens().toString(recognizer.getTokenNames());
		recognizer.notifyErrorListeners(e.getOffendingToken(), msg, e);
	}
	
	
	/**
	 * This is called by {@link #reportError} when the exception is a
	 * {@link FailedPredicateException}.
	 *
	 * @see #reportError
	 *
	 * @param recognizer
	 *            the parser instance
	 * @param e
	 *            the recognition exception
	 */

	protected void reportUnwantedToken(@NotNull Parser recognizer) {
		if (inErrorRecoveryMode(recognizer)) {
			return;
		}
		beginErrorCondition(recognizer);
		Token t = recognizer.getCurrentToken();
		String tokenName = getTokenErrorDisplay(t);
		IntervalSet expecting = getExpectedTokens(recognizer);
		String msg = "Bio7 extraneous input " + tokenName + " expecting the following symbols: " + "\n"+ expecting.toString(recognizer.getTokenNames());
		recognizer.notifyErrorListeners(t, msg, null);
	}

	/**
	 * This method is called to report a syntax error which requires the
	 * insertion of a missing token into the input stream. At the time this
	 * method is called, the missing token has not yet been inserted. When this
	 * method returns, {@code recognizer} is in error recovery mode.
	 *
	 * <p>
	 * This method is called when {@link #singleTokenInsertion} identifies
	 * single-token insertion as a viable recovery strategy for a mismatched
	 * input error.
	 * </p>
	 *
	 * <p>
	 * The default implementation simply returns if the handler is already in
	 * error recovery mode. Otherwise, it calls {@link #beginErrorCondition} to
	 * enter error recovery mode, followed by calling
	 * {@link Parser#notifyErrorListeners}.
	 * </p>
	 *
	 * @param recognizer
	 *            the parser instance
	 */
	protected void reportMissingToken(@NotNull Parser recognizer) {
		if (inErrorRecoveryMode(recognizer)) {
			return;
		}
		beginErrorCondition(recognizer);
		Token t = recognizer.getCurrentToken();
		IntervalSet expecting = getExpectedTokens(recognizer);
		String msg = "Bio7 missing " + expecting.toString(recognizer.getTokenNames()) + " at " + getTokenErrorDisplay(t);
		recognizer.notifyErrorListeners(t, msg, null);
	}

}