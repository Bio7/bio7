package com.eco.bio7.collection;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import com.eco.bio7.rcp.ApplicationWorkbenchWindowAdvisor;
import javafx.embed.swing.SwingNode;
import javafx.embed.swt.FXCanvas;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class SwingFxSwtView {
	private Scene scene;

	private Stage stage1;

	private Stage stage2;

	private Stage stage3;

	private Stage stage4;

	private Stage primaryStage;

	private FXCanvas canvas;

	private SwingNode swingNode;

	private StackPane pane;

	public void embedd(Composite top, JComponent comp) {

		canvas = new FXCanvas(top, SWT.NORMAL) {
			public Point computeSize(int wHint, int hHint, boolean changed) {
				getScene().getWindow().sizeToScene();
				int width = (int) getScene().getWidth();
				int height = (int) getScene().getHeight();
				return new Point(width, height);
			}
		};
		canvas.setData("false");
		canvas.addPaintListener(new PaintListener() {

			@Override
			public void paintControl(PaintEvent e) {
				String full = (String) canvas.getData();
				if (full.equals("true")) {
					GC gc = new GC(canvas);
					gc.fillRectangle(canvas.getBounds());
					gc.setFont(new Font(Display.getDefault(), "Arial", 20, SWT.NORMAL));
					gc.drawString("Fullscreen Mode! Press ESC in the fullscreen window", 10, 10);
					gc.drawString("to return the display to this view!", 10, 60);

				} else {

				}

			}
		});

		swingNode = new SwingNode();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				swingNode.setContent(comp);
			}
		});

		pane = new StackPane();
		pane.getChildren().add(swingNode);

		scene = new Scene(pane);
		scene.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {

			public void handle(KeyEvent ke) {
				fullscreen(ke, scene, top);

			}
		});
		canvas.setScene(scene);
		canvas.layout();
		top.layout();
		// contentPane.add(jpanel);
		// view.getCustomViewParent().layout();
	}

	private void fullscreen(KeyEvent ke, Scene scene, Composite top) {
		if (ke.getCode() == KeyCode.F2) {
			if (canvas.isDisposed() == false) {
				canvas.setData("true");
				canvas.redraw();
			}
			Screen screen1 = Screen.getScreens().get(0);
			stage1 = new Stage();
			stage1.setScene(scene);
			stage1.setX(screen1.getVisualBounds().getMinX());
			stage1.setY(screen1.getVisualBounds().getMinY());
			stage1.setWidth(screen1.getVisualBounds().getWidth());
			stage1.setHeight(screen1.getVisualBounds().getHeight());
			stage1.initStyle(StageStyle.UNDECORATED);
			// stage1.setFullScreen(true);
			stage1.show();
		} else if (ke.getCode() == KeyCode.F3) {

			int size = Screen.getScreens().size();
			if (size > 1) {
				if (canvas.isDisposed() == false) {
					canvas.setData("true");
					canvas.redraw();
				}
				Screen screen2 = Screen.getScreens().get(1);
				stage2 = new Stage();
				stage2.setScene(scene);
				stage2.setX(screen2.getVisualBounds().getMinX());
				stage2.setY(screen2.getVisualBounds().getMinY());
				stage2.setWidth(screen2.getVisualBounds().getWidth());
				stage2.setHeight(screen2.getVisualBounds().getHeight());
				stage2.initStyle(StageStyle.UNDECORATED);
				// stage2.setFullScreen(true);
				stage2.show();
			}

		} else if (ke.getCode() == KeyCode.F4) {

			int size = Screen.getScreens().size();
			if (size > 2) {
				if (canvas.isDisposed() == false) {
					canvas.setData("true");
					canvas.redraw();
				}
				Screen screen3 = Screen.getScreens().get(2);
				stage3 = new Stage();
				stage3.setScene(scene);
				stage3.setX(screen3.getVisualBounds().getMinX());
				stage3.setY(screen3.getVisualBounds().getMinY());
				stage3.setWidth(screen3.getVisualBounds().getWidth());
				stage3.setHeight(screen3.getVisualBounds().getHeight());
				stage3.initStyle(StageStyle.UNDECORATED);
				// stage3.setFullScreen(true);
				stage3.show();
			}

		} else if (ke.getCode() == KeyCode.F5) {

			int size = Screen.getScreens().size();
			if (size > 3) {
				if (canvas.isDisposed() == false) {
					canvas.setData("true");
					canvas.redraw();
				}
				Screen screen4 = Screen.getScreens().get(3);
				stage4 = new Stage();
				stage4.setScene(scene);
				stage4.setX(screen4.getVisualBounds().getMinX());
				stage4.setY(screen4.getVisualBounds().getMinY());
				stage4.setWidth(screen4.getVisualBounds().getWidth());
				stage4.setHeight(screen4.getVisualBounds().getHeight());
				stage4.initStyle(StageStyle.UNDECORATED);
				// stage3.setFullScreen(true);
				stage4.show();
			}

		}

		else if (ke.getCode() == KeyCode.F1) {
			if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Windows")) {
				if (canvas.isDisposed() == false) {
					canvas.setData("true");
					canvas.redraw();
				}
				int mainMonitor = 0;
				Screen primaryMonitor = Screen.getPrimary();
				for (int i = 0; i < Screen.getScreens().size(); i++) {
					if (Screen.getScreens().get(i).equals(primaryMonitor)) {
						mainMonitor = i;

						break;
					}
				}
				Screen primaryScreen = Screen.getScreens().get(mainMonitor);
				primaryStage = new Stage();
				primaryStage.setScene(scene);
				primaryStage.setX(primaryScreen.getVisualBounds().getMinX());
				primaryStage.setY(primaryScreen.getVisualBounds().getMinY());
				primaryStage.setFullScreen(true);
				primaryStage.show();
			} else {
				System.out.println("True fullscreen for Linux disabled!");
			}
		} else if (ke.getCode() == KeyCode.ESCAPE) {

			if (canvas.isDisposed() == false) {
				canvas.setData("false");
				canvas.redraw();
				canvas.setScene(scene);
				top.layout();
			}
			if (stage1 != null) {
				stage1.close();
				stage1 = null;
			}
			if (stage2 != null) {
				stage2.close();
				stage2 = null;
			} else if (stage3 != null) {
				stage3.close();
				stage3 = null;
			} else if (stage4 != null) {
				stage4.close();
				stage4 = null;

			} else if (primaryStage != null) {
				primaryStage.close();
				primaryStage = null;
			}
		}
	}

}
