package com.eco.bio7.popup.actions;

import java.util.Set;

import com.eco.bio7.collection.CustomView;

import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;


/*A browser for rmarkdown and knitr documents!*/
public class JavaFXWebBrowser {
	 private ContextMenu menu;
	 
	public void createBrowser(String url){
	
	AnchorPane anchorPane = new AnchorPane();

	WebView brow = new WebView();
	brow.getChildrenUnmodifiable().addListener(new ListChangeListener<Node>() {
		@Override
		public void onChanged(Change<? extends Node> change) {
			Set<Node> scrolls = brow.lookupAll(".scroll-bar");
			for (Node scroll : scrolls) {
				scroll.setVisible(false);
			}
		}
	});
	
	/*brow.setOnMouseClicked(new EventHandler<MouseEvent>() {

      

		@Override
        public void handle(MouseEvent mouse) {
            if (mouse.getButton() == MouseButton.SECONDARY) {
                menu = new ContextMenu();
               //add some menu items here
               menu.show(brow, mouse.getScreenX(), mouse.getScreenY());
            } else {
                if (menu != null) {
                    menu.hide();
                }
            }
        }
    });*/
	
	//brow.setTop(scrollWheelStatus);
    brow.setOnScroll(new EventHandler<ScrollEvent>() {
      @Override
      public void handle(ScrollEvent event) {
    	  if(event. isControlDown()){
    	  double scrollZoom = 1.1;
          double deltaY = event.getDeltaY();
          if (deltaY < 0){
            scrollZoom = 2.0 - scrollZoom;
          }
         brow.setZoom(brow.getZoom() * scrollZoom);
         
    	event.consume();
    	  }
        
      }
    });
	
	final WebEngine webEng = brow.getEngine();
	
	webEng.setJavaScriptEnabled(true);
	
	AnchorPane.setTopAnchor(brow, 0.0);
	AnchorPane.setBottomAnchor(brow, 0.0);
	AnchorPane.setLeftAnchor(brow, 0.0);
	AnchorPane.setRightAnchor(brow, 0.0);

	anchorPane.getChildren().add(brow);
	
	
	webEng.load(url);
	CustomView view = new CustomView();
	view.setSceneCanvas("HTML");

	Scene scene = new Scene(anchorPane);
	
	view.addScene(scene);
	}
	

}
