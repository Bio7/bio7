package com.eco.bio7.reditors;

/*******************************************************************************
 * Copyright (c) 2005 Prashant Deva and Gerd Castan
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License - v 1.0
 * which is available at http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import java.util.ArrayList;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import com.eco.bio7.reditor.antlr.RFilter;
import com.eco.bio7.reditor.antlr.RLexer;
import com.eco.bio7.reditor.antlr.RParser;

public class RReconcilingStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension {

	private REditor editor;

	private IDocument fDocument;

	/** holds the calculated positions */
	protected final ArrayList fPositions = new ArrayList();

	/** The offset of the next character to be read */
	protected int fOffset;

	/** The end offset of the range to be scanned */
	protected int fRangeEnd;

	/**
	 * @return Returns the editor.
	 */
	public REditor getEditor() {
		return editor;
	}

	public void setEditor(REditor editor) {
		this.editor = editor;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategy#setDocument(org.eclipse.jface.text.IDocument)
	 */
	public void setDocument(IDocument document) {
		this.fDocument = document;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategy#reconcile(org.eclipse.jface.text.reconciler.DirtyRegion, org.eclipse.jface.text.IRegion)
	 */
	public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
		initialReconcile();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategy#reconcile(org.eclipse.jface.text.IRegion)
	 */
	public void reconcile(IRegion partition) {
		initialReconcile();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension#setProgressMonitor(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void setProgressMonitor(IProgressMonitor monitor) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension#initialReconcile()
	 */
	public void initialReconcile() {
		fOffset = 0;
		fRangeEnd = fDocument.getLength();
		calculatePositions();

	}

	/**
	 * next character position - used locally and only valid while {@link #calculatePositions()} is in progress.
	 */
	protected int cNextPos = 0;

	/** number of newLines found by {@link #classifyTag()} */
	protected int cNewLines = 0;

	protected char cLastNLChar = ' ';

	protected static final int START_TAG = 1;

	protected static final int LEAF_TAG = 2;

	protected static final int END_TAG = 3;

	protected static final int EOR_TAG = 4;

	protected static final int COMMENT_TAG = 5;

	protected static final int PI_TAG = 6;

	/**
	 * uses {@link #fDocument}, {@link #fOffset} and {@link #fRangeEnd} to calculate {@link #fPositions}. About syntax errors: this method is not a validator, it is useful.
	 */
	protected void calculatePositions() {

		IDocument doc = fDocument;
		//System.out.println(doc.get());
		ANTLRInputStream input = new ANTLRInputStream(doc.get());
		RLexer lexer = new RLexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		
		// Print tokens BEFORE filtering
		// tokens.fill();
		// for (Object tok : tokens.getTokens()) {
		// System.out.println(tok);
		// }

		RFilter filter = new RFilter(tokens);
		filter.stream(); // call start rule: stream
		tokens.reset();
		// Print tokens AFTER filtering
		// for (Object tok : tokens.getTokens()) {
		// System.out.println(tok);
		// }
		RParser parser = new RParser(tokens);
		parser.setBuildParseTree(true);
        //Token to= parser.match(0);
        
		//System.out.println("Errors: " + parser.getNumberOfSyntaxErrors());

		RuleContext tree = parser.prog();
       
		Interval sourceInterval = tree.getSourceInterval();
		
		//int line = firstToken.getLine();
		// tree.save(parser, "/tmp/R.ps"); // Generate postscript

		//System.out.println(line);
		
		

		fPositions.clear();
		cNextPos = fOffset;

		/*
		 * try { recursiveTokens(0); } catch (BadLocationException e) { e.printStackTrace(); }
		 */

		// Collections.sort(fPositions, new RangeTokenComparator());
		fPositions.add(new Position(15, 60));
		fPositions.add(new Position(90, 200));

		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				editor.updateFoldingStructure(fPositions);
			}

		});
	}

	/**
	 * emits tokens to {@link #fPositions}.
	 * 
	 * @return number of newLines
	 * @throws BadLocationException
	 */
	protected int recursiveTokens(int depth) throws BadLocationException {
		int newLines = 0;
		while (cNextPos < fRangeEnd) {
			while (cNextPos < fRangeEnd) {
				char ch = fDocument.getChar(cNextPos++);
				switch (ch) {
				case '<':
					int startOffset = cNextPos - 1;
					int startNewLines = newLines;
					int classification = classifyTag();
					String tagString = fDocument.get(startOffset, Math.min(cNextPos - startOffset, fRangeEnd - startOffset)); // this is to see where we are in the debugger
					newLines += cNewLines; // cNewLines is written by
											// classifyTag()

					switch (classification) {
					case START_TAG:
						newLines += recursiveTokens(depth + 1);
						if (newLines > startNewLines + 1) {
							emitPosition(startOffset, cNextPos - startOffset);
						}
						break;
					case LEAF_TAG:
						if (newLines > startNewLines + 1) {
							emitPosition(startOffset, cNextPos - startOffset);
						}
						break;
					case COMMENT_TAG:
						if (newLines > startNewLines + 1) {
							emitPosition(startOffset, cNextPos - startOffset);
						}
						break;
					case PI_TAG:
						break;
					case END_TAG:
					case EOR_TAG:
						return newLines;
					default:
						break;
					}
					break;
				case '\n':
				case '\r':
					if ((ch == cLastNLChar) || (' ' == cLastNLChar)) {
						newLines++;
						cLastNLChar = ch;
					}
					break;
				default:
					break;
				}
			}

		}
		return newLines;
	}

	protected void emitPosition(int startOffset, int length) {
		fPositions.add(new Position(startOffset, length));
	}

	/**
	 * classsifies a tag: <br />
	 * &lt;?...?&gt;: {@link #PI_TAG} <br />
	 * &lt;!...--&gt;: {@link #COMMENT_TAG} <br />
	 * &lt;...&gt;: {@link #START_TAG} <br />
	 * &lt;.../&gt;: {@link #LEAF_TAG} <br />
	 * &lt;/...&gt;: {@link #END_TAG} <br />
	 * &lt;...: {@link #EOR_TAG} (end of range reached before closing &gt; is found). <br />
	 * when this method is called, {@link #cNextPos} must point to the character after &lt;, when it returns, it points to the character after &gt; or after the range. About syntax errors: this method is not a validator, it is useful. Side effect: writes number of found newLines to
	 * {@link #cNewLines}.
	 * 
	 * @return the tag classification
	 */
	protected int classifyTag() {
		try {
			char ch = fDocument.getChar(cNextPos++);
			cNewLines = 0;

			// processing instruction?
			if ('?' == ch) {
				boolean piFlag = false;
				while (cNextPos < fRangeEnd) {
					ch = fDocument.getChar(cNextPos++);
					if (('>' == ch) && piFlag)
						return PI_TAG;
					piFlag = ('?' == ch);
				}
				return EOR_TAG;
			}

			// comment?
			if ('!' == ch) {
				cNextPos++; // must be '-' but we don't care if not
				cNextPos++; // must be '-' but we don't care if not
				int commEnd = 0;
				while (cNextPos < fRangeEnd) {
					ch = fDocument.getChar(cNextPos++);
					if (('>' == ch) && (commEnd >= 2))
						return COMMENT_TAG;
					if (('\n' == ch) || ('\r' == ch)) {
						if ((ch == cLastNLChar) || (' ' == cLastNLChar)) {
							cNewLines++;
							cLastNLChar = ch;
						}
					}
					if ('-' == ch) {
						commEnd++;
					} else {
						commEnd = 0;
					}
				}
				return EOR_TAG;
			}

			// consume whitespaces
			while ((' ' == ch) || ('\t' == ch) || ('\n' == ch) || ('\r' == ch)) {
				ch = fDocument.getChar(cNextPos++);
				if (cNextPos > fRangeEnd)
					return EOR_TAG;
			}

			// end tag?
			if ('/' == ch) {
				while (cNextPos < fRangeEnd) {
					ch = fDocument.getChar(cNextPos++);
					if ('>' == ch) {
						cNewLines += eatToEndOfLine();
						return END_TAG;
					}
					if ('"' == ch) {
						ch = fDocument.getChar(cNextPos++);
						while ((cNextPos < fRangeEnd) && ('"' != ch)) {
							ch = fDocument.getChar(cNextPos++);
						}
					} else if ('\'' == ch) {
						ch = fDocument.getChar(cNextPos++);
						while ((cNextPos < fRangeEnd) && ('\'' != ch)) {
							ch = fDocument.getChar(cNextPos++);
						}
					}
				}
				return EOR_TAG;
			}

			// start tag or leaf tag?
			while (cNextPos < fRangeEnd) {
				ch = fDocument.getChar(cNextPos++);
				// end tag?
				s: switch (ch) {
				case '/':
					while (cNextPos < fRangeEnd) {
						ch = fDocument.getChar(cNextPos++);
						if ('>' == ch) {
							cNewLines += eatToEndOfLine();
							return LEAF_TAG;
						}
					}
					return EOR_TAG;
				case '"':
					while (cNextPos < fRangeEnd) {
						ch = fDocument.getChar(cNextPos++);
						if ('"' == ch)
							break s;
					}
					return EOR_TAG;
				case '\'':
					while (cNextPos < fRangeEnd) {
						ch = fDocument.getChar(cNextPos++);
						if ('\'' == ch)
							break s;
					}
					return EOR_TAG;
				case '>':
					cNewLines += eatToEndOfLine();
					return START_TAG;
				default:
					break;
				}

			}
			return EOR_TAG;

		} catch (BadLocationException e) {
			// should not happen, but we treat it as end of range
			return EOR_TAG;
		}
	}

	protected int eatToEndOfLine() throws BadLocationException {
		if (cNextPos >= fRangeEnd) {
			return 0;
		}
		char ch = fDocument.getChar(cNextPos++);
		// 1. eat all spaces and tabs
		while ((cNextPos < fRangeEnd) && ((' ' == ch) || ('\t' == ch))) {
			ch = fDocument.getChar(cNextPos++);
		}
		if (cNextPos >= fRangeEnd) {
			cNextPos--;
			return 0;
		}

		// now ch is a new line or a non-whitespace
		if ('\n' == ch) {
			if (cNextPos < fRangeEnd) {
				ch = fDocument.getChar(cNextPos++);
				if ('\r' != ch) {
					cNextPos--;
				}
			} else {
				cNextPos--;
			}
			return 1;
		}

		if ('\r' == ch) {
			if (cNextPos < fRangeEnd) {
				ch = fDocument.getChar(cNextPos++);
				if ('\n' != ch) {
					cNextPos--;
				}
			} else {
				cNextPos--;
			}
			return 1;
		}

		return 0;
	}
}
