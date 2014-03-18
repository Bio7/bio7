/*******************************************************************************
 * Copyright (c) 2007-2013 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/


package com.eco.bio7.image;

import java.awt.EventQueue;
import java.awt.Panel;
import java.util.Vector;

import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.embed.swt.FXCanvas;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javax.media.opengl.awt.GLCanvas;
import javax.swing.JApplet;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

public class SwtAwtCustom {

	private JApplet panel;

	private Composite top;

	

	private java.awt.Container contentPane;

	private java.awt.Frame frame;

	private JPanel jpanel;
	
	private Panel awtPanel;

	private Vector ve;

	private GLCanvas canvas;

	private CustomView view;

	protected Scene scene;

	protected Stage stage2;

	public SwtAwtCustom(JPanel Jpanel,CustomView view) {
		 this.view=view;
		this.jpanel = Jpanel;
		ve = new Vector();
		ve.add(Jpanel);

	}
	
	public SwtAwtCustom(Panel panel,CustomView view) {
		 this.view=view;
		this.awtPanel = panel;
		ve = new Vector();
		ve.add(panel);

	}

	/*public SwtAwtCustom(GLCanvas canvas, com.sun.opengl.util.Animator anim,CustomView view) {

		ve = new Vector();
		ve.add(anim);
		this.canvas = canvas;

	}*/
	

	public void addTab(final String title) {
		
		//top = new Composite(view.getCustomViewParent(), SWT.NO_BACKGROUND | SWT.EMBEDDED);
		final FXCanvas canvas = new FXCanvas(view.getCustomViewParent(), SWT.NONE);
		
		view.getCustomViewParent().setData(ve);
	
		
		final SwingNode swingNode = new SwingNode();
		
        swingNode.setContent(jpanel);
           
		
        final StackPane pane = new StackPane();
        pane.getChildren().add(swingNode);	
		
		 scene = new Scene(pane);
		 scene.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {

				public void handle(KeyEvent ke) {
					if (ke.getCode() == KeyCode.F2) {
					Screen screen2 = Screen.getScreens().get(0);
					 stage2 = new Stage();
					stage2.setScene(scene);
					stage2.setX(screen2.getVisualBounds().getMinX());
					stage2.setY(screen2.getVisualBounds().getMinY());
					stage2.setWidth(screen2.getVisualBounds().getWidth());
					stage2.setHeight(screen2.getVisualBounds().getHeight());
					//stage2.initStyle(StageStyle.UNDECORATED);
					stage2.setFullScreen(true);
					stage2.show();
					}
					else if (ke.getCode() == KeyCode.ESCAPE) {
						stage2.close();
					}

				}
			});
	    canvas.setScene(scene);	
		canvas.layout();
		//contentPane.add(jpanel);
		view.getCustomViewParent().layout();
		
		
		
		/*Display dis = view.getCustomViewParent().getDisplay();
		dis.syncExec(new Runnable() {
			public void run() {

				
				top = new Composite(view.getCustomViewParent(), SWT.NO_BACKGROUND | SWT.EMBEDDED);
				try {
					System.setProperty("sun.awt.noerasebackground", "true");
				} catch (NoSuchMethodError error) {
				}
				
				view.getCustomViewParent().setData(ve);

				frame = SWT_AWT.new_Frame(top);

				 panel = new JApplet() {
				      public void update(java.awt.Graphics g) {
				         Do not erase the background 
				        paint(g);
				      }
				    };

				frame.add(panel);

				JRootPane root = new JRootPane();
				panel.add(root);
				contentPane = root.getContentPane();

				contentPane.add(jpanel);
				view.getCustomViewParent().layout();
				
				

			}
		});
*/
	}
	
	
	
	public void addAWTTab(final String title) {
		Display dis = view.getCustomViewParent().getDisplay();
		dis.syncExec(new Runnable() {
			public void run() {

				

				top = new Composite(view.getCustomViewParent(), SWT.NO_BACKGROUND | SWT.EMBEDDED);
				try {
					System.setProperty("sun.awt.noerasebackground", "true");
				} catch (NoSuchMethodError error) {
				}
				
				view.getCustomViewParent().setData(ve);

				frame = SWT_AWT.new_Frame(top);

				 panel = new JApplet() {
				      public void update(java.awt.Graphics g) {
				        /* Do not erase the background */
				        paint(g);
				      }
				    };

				frame.add(panel);

				JRootPane root = new JRootPane();
				panel.add(root);
				contentPane = root.getContentPane();

				contentPane.add(awtPanel);

				view.getCustomViewParent().layout();
				

			}
		});

	}


	

}
