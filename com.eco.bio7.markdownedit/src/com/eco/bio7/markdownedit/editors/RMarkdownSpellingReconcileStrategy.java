package com.eco.bio7.markdownedit.editors;

/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import org.commonmark.Extension;
import org.commonmark.ext.front.matter.YamlFrontMatterExtension;
import org.commonmark.parser.Parser;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ISynchronizable;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector;
import org.eclipse.ui.texteditor.spelling.SpellingAnnotation;
import org.eclipse.ui.texteditor.spelling.SpellingContext;
import org.eclipse.ui.texteditor.spelling.SpellingProblem;
import org.eclipse.ui.texteditor.spelling.SpellingService;
import java.util.Collections;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import org.commonmark.Extension;
import org.commonmark.ext.front.matter.YamlFrontMatterExtension;
import org.commonmark.parser.Parser;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.texteditor.IDocumentProvider;

import com.eco.bio7.markdownedit.Activator;
import com.eco.bio7.markdownedit.outline.MarkdownEditorOutlineNode;
import com.eco.bio7.markdownedit.parser.CustomVisitor;
import com.eco.bio7.markdownedit.parser.Yamlheader;

import com.eco.bio7.markdownedit.outline.MarkdownEditorOutlineNode;


/**
 * Reconcile strategy used for spell checking.
 *
 * @since 3.3
 */
public class RMarkdownSpellingReconcileStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension {


	/**
	 * Spelling problem collector.
	 */
	public class SpellingProblemCollector implements ISpellingProblemCollector {

		/** Annotation model. */
		private IAnnotationModel fAnnotationModel;

		/** Annotations to add. */
		private Map<Annotation, Position> fAddAnnotations;

		/** Lock object for modifying the annotations. */
		private Object fLockObject;
		
		/** The document to operate on. */
		private IDocument fDocument;

		
		
		
		
		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategy#setDocument(org. eclipse.jface.text.IDocument)
		 */
		public void setDocument(IDocument document) {
			this.fDocument = document;

		}
		
		
		
		

		/**
		 * Initializes this collector with the given annotation model.
		 *
		 * @param annotationModel the annotation model
		 */
		public SpellingProblemCollector(IAnnotationModel annotationModel) {
			Assert.isLegal(annotationModel != null);
			fAnnotationModel= annotationModel;
			if (fAnnotationModel instanceof ISynchronizable)
				fLockObject= ((ISynchronizable)fAnnotationModel).getLockObject();
			else
				fLockObject= fAnnotationModel;
			
			methods = new Stack<MarkdownEditorOutlineNode>();
			store = Activator.getDefault().getPreferenceStore();
		}

		@Override
		public void accept(SpellingProblem problem) {
			fAddAnnotations.put(new SpellingAnnotation(problem), new Position(problem.getOffset(), problem.getLength()));
		}

		@Override
		public void beginCollecting() {
			fAddAnnotations= new HashMap<>();
		}

		@Override
		public void endCollecting() {

			List<Annotation> toRemove= new ArrayList<>();

			synchronized (fLockObject) {
				Iterator<Annotation> iter= fAnnotationModel.getAnnotationIterator();
				while (iter.hasNext()) {
					Annotation annotation= iter.next();
					if (SpellingAnnotation.TYPE.equals(annotation.getType()))
						toRemove.add(annotation);
				}
				Annotation[] annotationsToRemove= toRemove.toArray(new Annotation[toRemove.size()]);

				if (fAnnotationModel instanceof IAnnotationModelExtension)
					((IAnnotationModelExtension)fAnnotationModel).replaceAnnotations(annotationsToRemove, fAddAnnotations);
				else {
					for (int i= 0; i < annotationsToRemove.length; i++)
						fAnnotationModel.removeAnnotation(annotationsToRemove[i]);
					for (iter= fAddAnnotations.keySet().iterator(); iter.hasNext();) {
						Annotation annotation= iter.next();
						fAnnotationModel.addAnnotation(annotation, fAddAnnotations.get(annotation));
					}
				}
			}

			fAddAnnotations= null;
		}
	}


	/** Text content type */
	private static final IContentType TEXT_CONTENT_TYPE= Platform.getContentTypeManager().getContentType(IContentTypeManager.CT_TEXT);

	/** The text editor to operate on. */
	private ISourceViewer fViewer;

	/** The document to operate on. */
	private IDocument fDocument;

	/** The progress monitor. */
	private IProgressMonitor fProgressMonitor;

	private SpellingService fSpellingService;

	private ISpellingProblemCollector fSpellingProblemCollector;

	/** The spelling context containing the Java source content type. */
	private SpellingContext fSpellingContext;

	/**
	 * Region array, used to prevent us from creating a new array on each reconcile pass.
	 * @since 3.4
	 */
	private IRegion[] fRegions= new IRegion[1];
	
	
	private Vector<MarkdownEditorOutlineNode> editorOldNodes;
	private Stack<MarkdownEditorOutlineNode> methods;
	public  Set<Extension> EXTENSIONS = Collections.singleton(YamlFrontMatterExtension.create());
	//private static final Parser PARSER = Parser.builder().extensions(EXTENSIONS).build();

	/** The offset of the next character to be read */
	protected int fOffset;

	/** The end offset of the range to be scanned */
	protected int fRangeEnd;

	private MarkdownEditor markdownEditor;
	
	private IPreferenceStore store;

	
	


	/**
	 * Creates a new comment reconcile strategy.
	 *
	 * @param viewer the source viewer
	 * @param spellingService the spelling service to use
	 */
	public RMarkdownSpellingReconcileStrategy(ISourceViewer viewer, SpellingService spellingService) {
		Assert.isNotNull(viewer);
		Assert.isNotNull(spellingService);
		fViewer= viewer;
		fSpellingService= spellingService;
		fSpellingContext= new SpellingContext();
		fSpellingContext.setContentType(getContentType());

	}
	
	/* Update the HTML Editor view! */
	private void doReconcile() {
		editorOldNodes = markdownEditor.nodes;
		/* Create the category base node for the outline! */
		markdownEditor.createNodes();

		Parser parser = Parser.builder().build();
		IDocumentProvider dp = markdownEditor.getDocumentProvider();
		IDocument doc = dp.getDocument(markdownEditor.getEditorInput());
		String source = doc.get();
		String selSource = "";
		int offset = 0;
		int length = 0;
		int lines = doc.getNumberOfLines();
		for (int i = 0; i < lines; i++) {

			try {
				offset = doc.getLineOffset(i);

				length = doc.getLineLength(i);

				selSource = doc.get(offset, length);
			} catch (BadLocationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (selSource.contains("```{r") || selSource.contains("```{R")) {
				methods.push(new MarkdownEditorOutlineNode("{R Chunk}", i + 1, "RMarkdown", markdownEditor.baseNode));
			}
		}
		/*
		 * org.commonmark.node.Node document = parser.parse(source); HtmlRenderer renderer = HtmlRenderer.builder().build(); renderer.render(document); //
		 */

		/*
		 * Yamlheader yamlVisitor = new Yamlheader(methods, markdownEditor); org.commonmark.node.Node nodey = PARSER.parse(source);
		 * 
		 * nodey.accept(yamlVisitor);
		 */

		/*
		 * org.commonmark.node.Node node = parser.parse(source); CustomVisitor visitor = new CustomVisitor(methods, markdownEditor); node.accept(visitor);
		 */

		//
		/* Update the outline! */

		Display.getDefault().asyncExec(new Runnable() {
			public void run() {

				// editor.updateFoldingStructure(fPositions);

				markdownEditor.outlineInputChanged(editorOldNodes, markdownEditor.nodes);
			}

		});

	}
	
	private void triggerRMarkdownAction() {
		boolean markdownReconcile = store.getBoolean("RECONCILE_MARKDOWN");
		if (markdownReconcile) {
			String commandId = "com.eco.bio7.RMarkdownAction";
			IHandlerService handlerService = (IHandlerService) (IHandlerService) PlatformUI.getWorkbench().getService(IHandlerService.class);
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					try {
						handlerService.executeCommand(commandId, null);
					} catch (ExecutionException | NotDefinedException | NotEnabledException | NotHandledException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
		}
	}

	@Override
	public void initialReconcile() {
		reconcile(new Region(0, fDocument.getLength()));
	}

	@Override
	public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
		try {
			IRegion startLineInfo= fDocument.getLineInformationOfOffset(subRegion.getOffset());
			IRegion endLineInfo= fDocument.getLineInformationOfOffset(subRegion.getOffset() + Math.max(0, subRegion.getLength() - 1));
			if (startLineInfo.getOffset() == endLineInfo.getOffset())
				subRegion= startLineInfo;
			else
				subRegion= new Region(startLineInfo.getOffset(), endLineInfo.getOffset() + Math.max(0, endLineInfo.getLength() - 1) - startLineInfo.getOffset());

		} catch (BadLocationException e) {
			subRegion= new Region(0, fDocument.getLength());
		}
		reconcile(subRegion);
	}

	@Override
	public void reconcile(IRegion region) {
		if (getAnnotationModel() == null || fSpellingProblemCollector == null)
			return;

		fRegions[0]= region;
		fSpellingService.check(fDocument, fRegions, fSpellingContext, fSpellingProblemCollector, fProgressMonitor);
	
		doReconcile();
		triggerRMarkdownAction();
		
		
	}

	/**
	 * Returns the content type of the underlying editor input.
	 *
	 * @return the content type of the underlying editor input or
	 *         <code>null</code> if none could be determined
	 */
	protected IContentType getContentType() {
		return TEXT_CONTENT_TYPE;
	}

	/**
	 * Returns the document which is spell checked.
	 *
	 * @return the document
	 */
	protected final IDocument getDocument() {
		return fDocument;
	}

	@Override
	public void setDocument(IDocument document) {
		fDocument= document;
		fSpellingProblemCollector= createSpellingProblemCollector();
	}

	/**
	 * Creates a new spelling problem collector.
	 *
	 * @return the collector or <code>null</code> if none is available
	 */
	protected ISpellingProblemCollector createSpellingProblemCollector() {
		IAnnotationModel model= getAnnotationModel();
		if (model == null)
			return null;
		return new SpellingProblemCollector(model);
	}

	@Override
	public final void setProgressMonitor(IProgressMonitor monitor) {
		fProgressMonitor= monitor;
	}

	/**
	 * Returns the annotation model to be used by this reconcile strategy.
	 *
	 * @return the annotation model of the underlying editor input or
	 *         <code>null</code> if none could be determined
	 */
	protected IAnnotationModel getAnnotationModel() {
		return fViewer.getAnnotationModel();
	}
	public void setEditor(MarkdownEditor markdownEditor) {
		this.markdownEditor = markdownEditor;
	}


}
