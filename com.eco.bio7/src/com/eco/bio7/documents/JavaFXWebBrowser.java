package com.eco.bio7.documents;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.osgi.framework.Bundle;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.collection.CustomView;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Worker;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebErrorEvent;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

/*A JavaFX browser implementation!*/
public class JavaFXWebBrowser {
	private ContextMenu menu;
	private WebEngine webEng;
	private WebView brow;
	private boolean html;
	private IPreferenceStore store;

	public class JSLogListener {

		public void log(String text) {
			System.out.println(text);
		}
	}

	public JavaFXWebBrowser(boolean html) {
		this.html = html;
		brow = new WebView();
		webEng = brow.getEngine();
		webEng.setJavaScriptEnabled(true);
		store = Bio7Plugin.getDefault().getPreferenceStore();
		java.net.CookieHandler.setDefault(new java.net.CookieManager());
		

		webEng.getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>() {

			@Override
			public void changed(ObservableValue<? extends Worker.State> observable, Worker.State oldValue, Worker.State newValue) {

				if (newValue != Worker.State.SUCCEEDED) {

					return;
				}
				if (store.getBoolean("ENABLE_BROWSER_SCROLLBARS") == false) {
					webEng.executeScript(" document.documentElement.style.overflow = 'hidden'; ");
				}

				if (store.getBoolean("SCROLL_TO_DOCUMENT_END")) {
					webEng.executeScript("window.scrollTo(0,document.body.scrollHeight)");
				}

				if (store.getBoolean("ENABLE_BROWSER_LOG")) {
					JSObject window = (JSObject) webEng.executeScript("window");
					window.setMember("java", new JSLogListener());
					webEng.executeScript("console.log = function(message){ java.log(message); };");
				}

				// webEng.executeScript(getFirebugScript());
				/*
				 * org.w3c.dom.Document doc = webEng.getDocument(); Element el = doc.getElementById("");
				 */
				/* Store the last selected page for a new instance, reload of the PDF.js viewer! */
				// webEng.executeScript("PDFViewerApplication.pdfViewer.sidebarViewOnLoad= 1;");
				/* If we load a PDF with 'pdf.js'! */
				if (html == false) {
					//webEng.executeScript("document.getElementById('viewerContainer').style.overflow = 'hidden';");
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
				} else {
					/*
					 * String markdownContent=MarkdownEditor.getSelectedContent(); Document doc = webEng.getDocument(); Element el = doc.getDocumentElement();
					 * 
					 * try { Transformer transformer = TransformerFactory.newInstance().newTransformer(); transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
					 * transformer.setOutputProperty(OutputKeys.METHOD, "xml"); transformer.setOutputProperty(OutputKeys.INDENT, "yes"); transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
					 * transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
					 * 
					 * transformer.transform(new DOMSource(doc), new StreamResult(new OutputStreamWriter(System.out, "UTF-8"))); } catch (Exception ex) { ex.printStackTrace(); }
					 * webEng.executeScript("");
					 */

				}

			}

		});
		webEng.setOnAlert(new EventHandler<WebEvent<String>>() {
			@Override
			public void handle(WebEvent<String> event) {
				System.out.println("Browser alert:\n\n" + event.getData());
			}
		});

		webEng.setOnError(new EventHandler<WebErrorEvent>() {
			@Override
			public void handle(WebErrorEvent event) {
				System.out.println("Browser error:\n\n" + event.getMessage());
			}
		});

	}

	public void createBrowser(String url, String name) {

		AnchorPane anchorPane = new AnchorPane();
		if (store.getBoolean("ENABLE_JAVAFXWEBKIT_SCROLLBARS") == false) {
			brow.getChildrenUnmodifiable().addListener(new ListChangeListener<Node>() {
				@Override
				public void onChanged(Change<? extends Node> change) {
					Set<Node> scrolls = brow.lookupAll(".scroll-bar");
					for (Node scroll : scrolls) {
						scroll.setVisible(false);
					}
				}
			});
		}

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
				if (ke.getCharacter().equals("t")) {
					if (html == false) {
						webEng.executeScript("if (document.getElementById('toolbarContainer').style.display == '')" + "{ " + "document.getElementById('viewerContainer').style.overflow = 'hidden';document.getElementById('toolbarContainer').style.display='none';document.getElementById('viewerContainer').style.top=0;}"

								+ "else{" + "document.getElementById('viewerContainer').style.overflow = 'visible';document.getElementById('toolbarContainer').style.display='';document.getElementById('viewerContainer').style.top=32;" + "}");
					}
				}

			}
		});

		brow.setOnDragOver(new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent event) {
				Dragboard db = event.getDragboard();
				if (db.hasFiles()) {
					event.acceptTransferModes(TransferMode.COPY);
				} else {
					event.consume();
				}
			}
		});

		brow.setOnDragDropped(new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent event) {
				Dragboard db = event.getDragboard();
				boolean success = false;
				if (db.hasFiles()) {
					success = true;
					String filePath = null;
					for (File file : db.getFiles()) {
						filePath = file.getAbsolutePath();
						System.out.println(filePath);

						String path = null;
						try {
							path = file.toURI().toURL().toString();
						} catch (MalformedURLException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}

						if (FilenameUtils.isExtension(file.getName(), "html") || FilenameUtils.isExtension(file.getName(), "htm")) {
							JavaFXWebBrowser br = new JavaFXWebBrowser(true);
							// WebEngine webEngine = br.getWebEngine();
							// webEngine.load(path);
							br.createBrowser(path, "R_Display");
						}

						else if (FilenameUtils.isExtension(file.getName(), "pdf")) {
							String pathBundle = getPdfjsPath();
							System.out.println("Path: " + path);

							// webEng.load("file:///" + pathBundle + "");
							/* We have to create a new browser instance to inject the path as variable! */
							JavaFXWebBrowser br = new JavaFXWebBrowser(false);
							WebEngine webEngine = br.getWebEngine();
							webEngine.executeScript("var DEFAULT_URL ='" + path + "'");
							br.createBrowser("file:///" + pathBundle + "", "R_Display");
						}

						else {
							Bio7Dialog.message("Filetype is not supported!\nDrag and Drop PDF or HTML files only!");
						}

					}
				}
				event.setDropCompleted(success);
				event.consume();
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
		scene.getAccelerators().put(new KeyCodeCombination(KeyCode.B, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN), new Runnable() {
			@Override
			public void run() {

				goBack();
			}
		});

		scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN), new Runnable() {
			@Override
			public void run() {
				goBack();
			}
		});

		view.addScene(scene);
	}

	public WebEngine getWebEngine() {
		return webEng;
	}

	private static String getPdfjsPath() {
		Bundle bundle = Platform.getBundle("com.eco.bio7.libs");
		Path path = new Path("pdfjs/web/viewer.html");
		URL locationURL = FileLocator.find(bundle, path, null);

		URL fileUrl = null;
		try {
			fileUrl = FileLocator.toFileURL(locationURL);
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		String pathBundle = fileUrl.getFile();
		return pathBundle;
	}

	/*
	 * private static String getFirebugScript() { Bundle bundle = Platform.getBundle("com.eco.bio7.libs"); Path path = new Path("pdfjs/firebug-lite-132.js"); URL locationURL = FileLocator.find(bundle,
	 * path, null);
	 * 
	 * URL fileUrl = null; try { fileUrl = FileLocator.toFileURL(locationURL); } catch (IOException e2) { // TODO Auto-generated catch block e2.printStackTrace(); } String pathBundle =
	 * fileUrl.getFile(); StringBuilder libraryContents = new StringBuilder();
	 * 
	 * File initialFile = new File(pathBundle); InputStream inputStream = null; try { inputStream = new FileInputStream(initialFile); } catch (FileNotFoundException e) { // TODO Auto-generated catch
	 * block e.printStackTrace(); } try { BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF8")); String line = reader.readLine(); while (line != null) {
	 * libraryContents.append(line); line = reader.readLine(); } } catch (IOException exception) { return null; } return libraryContents.toString(); }
	 */

	public void goBack() {
		webEng.executeScript("history.back()");
	}

	public void goForward() {
		webEng.executeScript("history.back()");
	}

}
