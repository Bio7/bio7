package com.eco.bio7.popup.actions;

import java.io.File;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jface.preference.IPreferenceStore;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.collection.CustomView;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Worker;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

/*A browser for rmarkdown and knitr documents!*/
public class JavaFXWebBrowser {
	private ContextMenu menu;
	private WebEngine webEng;
	private WebView brow;
	private boolean html;

	public JavaFXWebBrowser(boolean html) {
		this.html = html;
		brow = new WebView();
		webEng = brow.getEngine();
		webEng.setJavaScriptEnabled(true);
		webEng.getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>() {

			@Override
			public void changed(ObservableValue<? extends Worker.State> observable, Worker.State oldValue, Worker.State newValue) {

				if (newValue != Worker.State.SUCCEEDED) {

					return;
				}
				/*
				 * org.w3c.dom.Document doc = webEng.getDocument(); Element el = doc.getElementById("");
				 */
				/* Store the last selected page for a new instance, reload of the PDF.js viewer! */
				// webEng.executeScript("PDFViewerApplication.pdfViewer.sidebarViewOnLoad= 1;");
				/*If we load a PDF with 'pdf.js'!*/
				if (html == false) {
					webEng.executeScript("PDFViewerApplication.pdfViewer.currentPageNumber=" + JavaFXBrowserHelper.pageNumber + "");
					/* Set the bookmark to select the page! */
					webEng.executeScript("PDFViewerApplication.initialBookmark = \"" + JavaFXBrowserHelper.pageNumber + "\";");

					Document doc = webEng.getDocument();

					Element el = doc.getDocumentElement();

					((EventTarget) el).addEventListener("click", new EventListener() {

						@Override
						public void handleEvent(Event evt) {
							JavaFXBrowserHelper.pageNumber = (int) (webEng.executeScript("PDFViewerApplication.pdfViewer.currentPageNumber;"));

						}
					}, false);

					((EventTarget) el).addEventListener("keyup", new EventListener() {

						@Override
						public void handleEvent(Event evt) {
							JavaFXBrowserHelper.pageNumber = (int) (webEng.executeScript("PDFViewerApplication.pdfViewer.currentPageNumber;"));
							/*
							 * System.out.println(String.valueOf(((com.sun.webkit.dom.KeyboardEventImpl) evt).getKeyCode())); com.sun.webkit.dom.KeyboardEventImpl event =
							 * (com.sun.webkit.dom.KeyboardEventImpl) evt; System.out.println(event.getKeyIdentifier());
							 */

						}
					}, false);
				}

			}

		});

	}

	public void createBrowser(String url, String name) {

		AnchorPane anchorPane = new AnchorPane();

		brow.getChildrenUnmodifiable().addListener(new ListChangeListener<Node>() {
			@Override
			public void onChanged(Change<? extends Node> change) {
				Set<Node> scrolls = brow.lookupAll(".scroll-bar");
				for (Node scroll : scrolls) {
					scroll.setVisible(false);
				}
			}
		});

		/*
		 * brow.setOnMouseClicked(new EventHandler<MouseEvent>() {
		 * 
		 * 
		 * 
		 * @Override public void handle(MouseEvent mouse) { if (mouse.getButton() == MouseButton.SECONDARY) { menu = new ContextMenu(); //add some menu items here menu.show(brow, mouse.getScreenX(),
		 * mouse.getScreenY()); } else { if (menu != null) { menu.hide(); } } } });
		 */

		// brow.setTop(scrollWheelStatus);
		brow.setOnScroll(new EventHandler<ScrollEvent>() {
			@Override
			public void handle(ScrollEvent event) {
				if (event.isControlDown()) {
					double scrollZoom = 1.1;
					double deltaY = event.getDeltaY();
					if (deltaY < 0) {
						scrollZoom = 2.0 - scrollZoom;
					}
					brow.setZoom(brow.getZoom() * scrollZoom);

					event.consume();
				}

			}
		});

		/*
		 * public void handle(KeyEvent event) { if (event.getCode() == KeyCode.TAB && event.isControlDown()) { } }
		 */

		brow.setOnKeyTyped(new EventHandler<KeyEvent>() {
			public void handle(KeyEvent ke) {
				// String text = "Key Typed: " + ke.getCharacter();
				// System.out.println(text);
				if (ke.isAltDown()) {

				}
				if (ke.getCharacter().equals("+")) {

					brow.setZoom(brow.getZoom() * 1.1);

					ke.consume();

				}
				if (ke.getCharacter().equals("-")) {

					brow.setZoom(brow.getZoom() / 1.1);

					ke.consume();

				}
				if (ke.isMetaDown()) {

				}
				if (ke.isShiftDown()) {

				}

			}
		});

		AnchorPane.setTopAnchor(brow, 0.0);
		AnchorPane.setBottomAnchor(brow, 0.0);
		AnchorPane.setLeftAnchor(brow, 0.0);
		AnchorPane.setRightAnchor(brow, 0.0);

		anchorPane.getChildren().add(brow);

		webEng.load(url);

		CustomView view = new CustomView();

		view.setSceneCanvas(name);

		Scene scene = new Scene(anchorPane);

		view.addScene(scene);
	}

	public WebEngine getWebEngine() {
		return webEng;
	}

}
