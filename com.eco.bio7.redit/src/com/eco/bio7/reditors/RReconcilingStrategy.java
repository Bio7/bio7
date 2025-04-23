package com.eco.bio7.reditors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
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

import com.eco.bio7.reditor.Bio7REditorPlugin;
import com.eco.bio7.reditor.antlr.Parse;

public class RReconcilingStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension {

	private REditor editor;

	private IDocument fDocument;

	private SpellingService fSpellingService;

	private ISpellingProblemCollector fSpellingProblemCollector;

	/** The spelling context containing the Java source content type. */
	private SpellingContext fSpellingContext;

	/** The offset of the next character to be read */
	protected int fOffset;

	/** The end offset of the range to be scanned */
	protected int fRangeEnd;

	private ISourceViewer fViewer;

	private IProgressMonitor fProgressMonitor;

	/**
	 * Region array, used to prevent us from creating a new array on each reconcile
	 * pass.
	 * 
	 * @since 3.4
	 */
	private IRegion[] fRegions = new IRegion[1];

	private IPreferenceStore storeR;

	public RReconcilingStrategy(ISourceViewer viewer, SpellingService spellingService) {
		Assert.isNotNull(viewer);
		Assert.isNotNull(spellingService);
		fViewer = viewer;
		fSpellingService = spellingService;
		fSpellingContext = new SpellingContext();
		fSpellingContext.setContentType(getContentType());
		storeR = Bio7REditorPlugin.getDefault().getPreferenceStore();

	}

	public class SpellingProblemCollector implements ISpellingProblemCollector {

		/** Annotation model. */
		private IAnnotationModel fAnnotationModel;

		/** Annotations to add. */
		private Map<Annotation, Position> fAddAnnotations;

		/** Lock object for modifying the annotations. */
		private Object fLockObject;

		/** The document to operate on. */
		protected IDocument fDocument;

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategy#setDocument(org.
		 * eclipse.jface.text.IDocument)
		 */
		public void setDocument(IDocument document) {
			this.fDocument = document;

		}

		/**
		 * Initializes this collector with the given annotation model.
		 *
		 * @param annotationModel
		 *            the annotation model
		 */
		public SpellingProblemCollector(IAnnotationModel annotationModel) {
			Assert.isLegal(annotationModel != null);
			fAnnotationModel = annotationModel;
			if (fAnnotationModel instanceof ISynchronizable)
				fLockObject = ((ISynchronizable) fAnnotationModel).getLockObject();
			else
				fLockObject = fAnnotationModel;

		}

		@Override
		public void accept(SpellingProblem problem) {
			fAddAnnotations.put(new SpellingAnnotation(problem), new Position(problem.getOffset(), problem.getLength()));
		}

		@Override
		public void beginCollecting() {
			fAddAnnotations = new HashMap<>();
		}

		@Override
		public void endCollecting() {

			List<Annotation> toRemove = new ArrayList<>();

			synchronized (fLockObject) {
				Iterator<Annotation> iter = fAnnotationModel.getAnnotationIterator();
				while (iter.hasNext()) {
					Annotation annotation = iter.next();
					if (SpellingAnnotation.TYPE.equals(annotation.getType()))
						toRemove.add(annotation);
				}
				Annotation[] annotationsToRemove = toRemove.toArray(new Annotation[toRemove.size()]);

				if (fAnnotationModel instanceof IAnnotationModelExtension)
					((IAnnotationModelExtension) fAnnotationModel).replaceAnnotations(annotationsToRemove, fAddAnnotations);
				else {
					for (int i = 0; i < annotationsToRemove.length; i++)
						fAnnotationModel.removeAnnotation(annotationsToRemove[i]);
					for (iter = fAddAnnotations.keySet().iterator(); iter.hasNext();) {
						Annotation annotation = iter.next();
						fAnnotationModel.addAnnotation(annotation, fAddAnnotations.get(annotation));
					}
				}
			}

			fAddAnnotations = null;
		}
	}

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
	 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategy#setDocument(org.
	 * eclipse.jface.text.IDocument)
	 */
	public void setDocument(IDocument document) {
		this.fDocument = document;
		fSpellingProblemCollector = createSpellingProblemCollector();

	}

	/**
	 * Creates a new spelling problem collector.
	 *
	 * @return the collector or <code>null</code> if none is available
	 */
	protected ISpellingProblemCollector createSpellingProblemCollector() {
		IAnnotationModel model = getAnnotationModel();
		if (model == null)
			return null;
		return new SpellingProblemCollector(model);
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

	/**
	 * Returns the content type of the underlying editor input.
	 *
	 * @return the content type of the underlying editor input or <code>null</code>
	 *         if none could be determined
	 */
	protected IContentType getContentType() {
		return Platform.getContentTypeManager().getContentType("com.eco.bio7.reditor.rfile");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategy#reconcile(org.
	 * eclipse.jface.text.reconciler.DirtyRegion, org.eclipse.jface.text.IRegion)
	 */
	public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
		Parse parse = editor.getParser();
		parse.parse();

		try {
			IRegion startLineInfo = fDocument.getLineInformationOfOffset(subRegion.getOffset());
			IRegion endLineInfo = fDocument.getLineInformationOfOffset(subRegion.getOffset() + Math.max(0, subRegion.getLength() - 1));
			if (startLineInfo.getOffset() == endLineInfo.getOffset())
				subRegion = startLineInfo;
			else
				subRegion = new Region(startLineInfo.getOffset(), endLineInfo.getOffset() + Math.max(0, endLineInfo.getLength() - 1) - startLineInfo.getOffset());

		} catch (BadLocationException e) {
			subRegion = new Region(0, fDocument.getLength());
		}
		reconcile(subRegion);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategy#reconcile(org.
	 * eclipse.jface.text.IRegion)
	 */
	public void reconcile(IRegion region) {
		// initialReconcile();
		Parse parse = editor.getParser();
		if (parse != null) {
			parse.parse();
		}

		boolean checkSpelling = storeR.getBoolean("CHECK_SPELLING_REDITOR");
		if (checkSpelling) {
			if (getAnnotationModel() == null || fSpellingProblemCollector == null)
				return;

			fRegions[0] = region;
			fSpellingService.check(fDocument, fRegions, fSpellingContext, fSpellingProblemCollector, fProgressMonitor);

		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension#
	 * setProgressMonitor(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void setProgressMonitor(IProgressMonitor monitor) {
		// TODO Auto-generated method stub
		fProgressMonitor = monitor;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension#
	 * initialReconcile()
	 */
	public void initialReconcile() {
		if (fDocument != null) {
			reconcile(new Region(0, fDocument.getLength()));
		}

	}

	/**
	 * next character position - used locally and only valid while
	 * {@link #calculatePositions()} is in progress.
	 */

}
