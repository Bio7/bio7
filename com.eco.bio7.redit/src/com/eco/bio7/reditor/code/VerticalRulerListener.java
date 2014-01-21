package com.eco.bio7.reditor.code;

import org.eclipse.jface.text.source.IVerticalRulerListener;
import org.eclipse.jface.text.source.VerticalRulerEvent;
import org.eclipse.swt.widgets.Menu;

public class VerticalRulerListener implements IVerticalRulerListener {
		public VerticalRulerListener() {
			System.out.println("VerticalRulerListener.VerticalRulerListener()");
		}
		public void annotationSelected(VerticalRulerEvent event) {
			System.out.println("VerticalRulerListener.annotationSelected()");
		}
		public void annotationDefaultSelected(VerticalRulerEvent event) {
			System.out.println("VerticalRulerListener.annotationDefaultSelected()");
		}
		public void annotationContextMenuAboutToShow(VerticalRulerEvent event, Menu menu) {
			System.out.println("VerticalRulerListener.annotationContextMenuAboutToShow()");
		}
		
		
	}
