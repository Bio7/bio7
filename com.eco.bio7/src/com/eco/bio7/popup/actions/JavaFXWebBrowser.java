package com.eco.bio7.popup.actions;

import java.io.File;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.jface.preference.IPreferenceStore;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.collection.CustomView;

import javafx.collections.ListChangeListener;
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

	public JavaFXWebBrowser() {
		brow = new WebView();
		webEng = brow.getEngine();
		webEng.setJavaScriptEnabled(true);
	}

	public void createBrowser(String url) {

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
		 * @Override public void handle(MouseEvent mouse) { if
		 * (mouse.getButton() == MouseButton.SECONDARY) { menu = new
		 * ContextMenu(); //add some menu items here menu.show(brow,
		 * mouse.getScreenX(), mouse.getScreenY()); } else { if (menu != null) {
		 * menu.hide(); } } } });
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
		 * public void handle(KeyEvent event) { if (event.getCode() ==
		 * KeyCode.TAB && event.isControlDown()) { } }
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
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		boolean openInBrowserInExtraView = store.getBoolean("OPEN_BOWSER_IN_EXTRA_VIEW");
		CustomView view = new CustomView();
		if (openInBrowserInExtraView) {
			String id = UUID.randomUUID().toString();
			view.setSceneCanvas("File_" + id);
		} else {
			view.setSceneCanvas(FilenameUtils.getName(url));
		}

		Scene scene = new Scene(anchorPane);

		view.addScene(scene);
	}

	public WebEngine getWebEngine() {
		return webEng;
	}

}
