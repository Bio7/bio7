package com.eco.bio7.markdownedit.editors;


/**Source adapted from:
*
* @author Ankur Sharma	(sharma.ankur@gmail.com)
* http://eclipsepde.wordpress.com
*/

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.spelling.SpellingAnnotation;
import org.eclipse.ui.texteditor.spelling.SpellingReconcileStrategy;
import org.eclipse.ui.texteditor.spelling.SpellingService;

public class MarkdownSpellingReconcileStrategy extends SpellingReconcileStrategy {

	public MarkdownSpellingReconcileStrategy(ISourceViewer sourceViewer, SpellingService spellingService) {
		super(sourceViewer, spellingService);
	}

	public void reconcile(IRegion region) {
		
		AbstractDocument document = (AbstractDocument) getDocument();
		IDocumentPartitioner docPartitioner = document.getDocumentPartitioner(IDocument.DEFAULT_CONTENT_TYPE);

		IAnnotationModel model = getAnnotationModel();
		if (region.getOffset() == 0 && region.getLength() == document.getLength()) {
			//reconciling whole document
			super.reconcile(region);
			deleteUnwantedAnnotations();
		} else {
			//partial reconciliation
			//preserve spelling annotations first
			Iterator iter = model.getAnnotationIterator();
			Map spellingErrors = new HashMap(1);
			while (iter.hasNext()) {
				Annotation annotation = (Annotation) iter.next();
				if (annotation instanceof SpellingAnnotation) {
					SpellingAnnotation spellingAnnotation = (SpellingAnnotation) annotation;
					Position position = model.getPosition(spellingAnnotation);
					if(docPartitioner!=null){
					String contentType = docPartitioner.getContentType(position.getOffset());

					if (IDocument.DEFAULT_CONTENT_TYPE.equalsIgnoreCase(contentType)) {
						spellingErrors.put(spellingAnnotation, model.getPosition(annotation));
					}
					}
				}
			}

			//reconcile
			super.reconcile(region);

			//restore annotations
			model = getAnnotationModel();
			iter = spellingErrors.keySet().iterator();
			while (iter.hasNext()) {
				Annotation annotation = (Annotation) iter.next();
				model.addAnnotation(annotation, (Position) spellingErrors.get(annotation));
			}
			deleteUnwantedAnnotations();
		}

	}

	/**
	 * Deletes the spelling annotations
	 */
	private void deleteUnwantedAnnotations() {
		AbstractDocument document = (AbstractDocument) getDocument();
		
		IDocumentPartitioner docPartitioner = document.getDocumentPartitioner(IDocument.DEFAULT_CONTENT_TYPE);
		IAnnotationModel model = getAnnotationModel();
		Iterator iter = model.getAnnotationIterator();

		while (iter.hasNext()) {
			Annotation annotation = (Annotation) iter.next();
			if (annotation instanceof SpellingAnnotation) {
				SpellingAnnotation spellingAnnotation = (SpellingAnnotation) annotation;
				Position position = model.getPosition(spellingAnnotation);
				
				if(docPartitioner!=null){
				String contentType = docPartitioner.getContentType(position.getOffset());
				if (!IDocument.DEFAULT_CONTENT_TYPE.equalsIgnoreCase(contentType)) {
					model.removeAnnotation(spellingAnnotation);
				}
				}
			}
		}
	}

}
